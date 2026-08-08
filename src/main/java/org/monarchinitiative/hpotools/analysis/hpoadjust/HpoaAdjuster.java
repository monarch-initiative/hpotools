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

    public HpoaAdjuster(HpoaFile hpoa) {
        this.hpoa = hpoa;
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
            List<HpoaAnnotationLine> removedLines = hpoa.linesCiting(pmid);
            HpoaFile adjusted = hpoa.withoutPmid(pmid);
            Optional<Path> outputFile = Optional.empty();
            if (!removedLines.isEmpty()) {
                Path path = outputDirectory.resolve(fileNameFor(pmid));
                adjusted.write(path);
                outputFile = Optional.of(path);
                LOGGER.info("Removed {} lines citing {} and wrote {}", removedLines.size(), pmid, path);
            } else {
                LOGGER.info("{} is not cited in the HPOA, no adjusted file needed", pmid);
            }
            for (PhenopacketCase phenopacketCase : entry.getValue()) {
                results.add(evaluateCase(phenopacketCase, adjusted, removedLines, outputFile));
            }
        }
        return results;
    }

    private CaseResult evaluateCase(PhenopacketCase phenopacketCase,
                                    HpoaFile adjusted,
                                    List<HpoaAnnotationLine> removedLines,
                                    Optional<Path> outputFile) {
        TermId diseaseId = phenopacketCase.diseaseId();
        int removedForDisease = (int) removedLines.stream()
                .filter(line -> line.diseaseId().equals(diseaseId))
                .count();
        int multiReferenceRemoved = (int) removedLines.stream()
                .filter(HpoaAnnotationLine::hasMultipleReferences)
                .count();
        long remainingPhenotypeLines = adjusted.phenotypeLineCount(diseaseId);
        CaseStatus status;
        if (removedForDisease == 0) {
            status = CaseStatus.NO_PMID_EVIDENCE;
        } else if (remainingPhenotypeLines == 0) {
            status = CaseStatus.INSUFFICIENT_DATA;
            LOGGER.warn("Skipping {}: no phenotype annotations left for {} after removing {}",
                    phenopacketCase.phenopacketId(), diseaseId, phenopacketCase.pmid());
        } else {
            status = CaseStatus.ADJUSTED;
        }
        return new CaseResult(phenopacketCase, status, removedForDisease, removedLines.size(),
                multiReferenceRemoved, remainingPhenotypeLines, outputFile);
    }

    static String fileNameFor(TermId pmid) {
        return "phenotype_" + pmid.getValue().replace(':', '_') + ".hpoa";
    }
}
