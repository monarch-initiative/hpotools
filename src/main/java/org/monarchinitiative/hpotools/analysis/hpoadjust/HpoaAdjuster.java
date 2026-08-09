package org.monarchinitiative.hpotools.analysis.hpoadjust;

import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class HpoaAdjuster {

    private static final Logger LOGGER = LoggerFactory.getLogger(HpoaAdjuster.class);

    private final HpoaFile hpoa;
    private final CohortSource cohortSource;

    public HpoaAdjuster(HpoaFile hpoa) {
        this(hpoa, CohortSource.empty());
    }

    public HpoaAdjuster(HpoaFile hpoa, CohortSource cohortSource) {
        this.hpoa = hpoa;
        this.cohortSource = cohortSource;
    }

    private record FileAdjustment(HpoaFile adjusted,
                                  List<HpoaAnnotationLine> removedLines,
                                  List<HpoaAnnotationLine> subtractedLines,
                                  int undecomposableRemoved) {
    }

    public List<CaseResult> adjustAll(List<PhenopacketCase> cases, Path outputDirectory) {
        try {
            Files.createDirectories(outputDirectory);
        } catch (IOException e) {
            throw new PhenolRuntimeException("Could not create output directory " + outputDirectory + ": " + e.getMessage());
        }
        Map<TermId, List<PhenopacketCase>> casesByPmid = cases.stream()
                .collect(Collectors.groupingBy(PhenopacketCase::pmid, LinkedHashMap::new, Collectors.toList()));
        List<CaseResult> results = new ArrayList<>();
        for (Map.Entry<TermId, List<PhenopacketCase>> entry : casesByPmid.entrySet()) {
            TermId pmid = entry.getKey();
            FileAdjustment adjustment = adjustFile(pmid);
            Optional<Path> outputFile = Optional.empty();
            if (!adjustment.removedLines().isEmpty() || !adjustment.subtractedLines().isEmpty()) {
                Path path = outputDirectory.resolve(fileNameFor(pmid));
                adjustment.adjusted().write(path);
                outputFile = Optional.of(path);
                LOGGER.info("{}: removed {} lines, subtracted cohort counts from {} lines, wrote {}",
                        pmid, adjustment.removedLines().size(), adjustment.subtractedLines().size(), path);
            } else {
                LOGGER.info("{} is not cited in the HPOA, no adjusted file needed", pmid);
            }
            for (PhenopacketCase phenopacketCase : entry.getValue()) {
                results.add(evaluateCase(phenopacketCase, adjustment, outputFile));
            }
        }
        return results;
    }

    public HpoaFile adjustFor(TermId pmid) {
        return adjustFile(pmid).adjusted();
    }

    private FileAdjustment adjustFile(TermId pmid) {
        List<HpoaAnnotationLine> retained = new ArrayList<>();
        List<HpoaAnnotationLine> removed = new ArrayList<>();
        List<HpoaAnnotationLine> subtracted = new ArrayList<>();
        int undecomposable = 0;
        for (HpoaAnnotationLine line : hpoa.annotationLines()) {
            if (!line.cites(pmid)) {
                retained.add(line);
                continue;
            }
            if (!line.hasMultipleReferences()) {
                removed.add(line);
                continue;
            }
            Optional<HpoaAnnotationLine> decomposed = subtractCohort(line, pmid);
            if (decomposed.isPresent()) {
                retained.add(decomposed.get());
                subtracted.add(line);
            } else {
                removed.add(line);
                undecomposable++;
            }
        }
        return new FileAdjustment(new HpoaFile(hpoa.headerLines(), retained), removed, subtracted, undecomposable);
    }

    private Optional<HpoaAnnotationLine> subtractCohort(HpoaAnnotationLine line, TermId pmid) {
        Optional<Ratio> frequency = line.frequencyRatio();
        Optional<CohortCounts> counts = cohortSource.lookup(line.diseaseId(), pmid);
        if (frequency.isEmpty() || counts.isEmpty()) {
            return Optional.empty();
        }
        Ratio adjusted = frequency.get()
                .minus(counts.get().countOf(line.hpoId()), counts.get().cohortSize());
        if (!adjusted.isInformative()) {
            return Optional.empty();
        }
        return Optional.of(line.withCohortSubtracted(pmid, adjusted));
    }

    private CaseResult evaluateCase(PhenopacketCase phenopacketCase,
                                    FileAdjustment adjustment,
                                    Optional<Path> outputFile) {
        TermId diseaseId = phenopacketCase.diseaseId();
        int removedForDisease = countForDisease(adjustment.removedLines(), diseaseId);
        int subtractedForDisease = countForDisease(adjustment.subtractedLines(), diseaseId);
        long remainingPhenotypeLines = adjustment.adjusted().phenotypeLineCount(diseaseId);
        CaseStatus status;
        if (removedForDisease == 0 && subtractedForDisease == 0) {
            status = CaseStatus.NO_PMID_EVIDENCE;
        } else if (remainingPhenotypeLines == 0) {
            status = CaseStatus.INSUFFICIENT_DATA;
            LOGGER.warn("Skipping {}: no phenotype annotations left for {} after removing {}",
                    phenopacketCase.phenopacketId(), diseaseId, phenopacketCase.pmid());
        } else {
            status = CaseStatus.ADJUSTED;
        }
        return new CaseResult(phenopacketCase, status, removedForDisease, subtractedForDisease,
                adjustment.removedLines().size(), adjustment.subtractedLines().size(),
                adjustment.undecomposableRemoved(), remainingPhenotypeLines, outputFile);
    }

    private static int countForDisease(List<HpoaAnnotationLine> lines, TermId diseaseId) {
        return (int) lines.stream()
                .filter(line -> line.diseaseId().equals(diseaseId))
                .count();
    }

    static String fileNameFor(TermId pmid) {
        return "phenotype_" + pmid.getValue().replace(':', '_') + ".hpoa";
    }
}
