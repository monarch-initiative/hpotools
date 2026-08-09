package org.monarchinitiative.hpotools.cmd;

import org.monarchinitiative.hpotools.analysis.hpoadjust.AugmentedHpoa;
import org.monarchinitiative.hpotools.analysis.hpoadjust.CaseResult;
import org.monarchinitiative.hpotools.analysis.hpoadjust.CaseStatus;
import org.monarchinitiative.hpotools.analysis.hpoadjust.DiseaseAugmentation;
import org.monarchinitiative.hpotools.analysis.hpoadjust.HpoaAdjuster;
import org.monarchinitiative.hpotools.analysis.hpoadjust.HpoaAugmenter;
import org.monarchinitiative.hpotools.analysis.hpoadjust.HpoaFile;
import org.monarchinitiative.hpotools.analysis.hpoadjust.PhenopacketCollection;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "hpoadjust",
        mixinStandardHelpOptions = true,
        description = "Adjust the HPOA for phenopacket-based benchmarking: add the annotations of a curated "
                + "cohort and remove the contribution of the publication a benchmarked case comes from")
public class HpoaAdjustCommand extends HPOCommand implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HpoaAdjustCommand.class);

    @CommandLine.Option(names = {"-p", "--phenopackets"},
            required = true,
            description = "path to a phenopacket JSON file or a directory of phenopackets")
    private Path phenopacketPath;

    @CommandLine.Option(names = {"-o", "--outdir"},
            description = "directory for adjusted HPOA files (default: ${DEFAULT-VALUE})")
    private Path outputDirectory = Path.of("hpoa-adjusted");

    @CommandLine.Option(names = {"--augment"},
            description = "add the cohort annotations to the HPOA before removing the per-case publication")
    private boolean augment;

    @CommandLine.Option(names = {"--biocuration"},
            description = "biocuration tag written to generated annotations (default: ${DEFAULT-VALUE})")
    private String biocurationTag = "HPO:esid4hpo";

    @Override
    public Integer call() {
        Path hpoaPath = Path.of(annotpath);
        if (!Files.isRegularFile(hpoaPath)) {
            LOGGER.error("Could not find phenotype.hpoa at {}", hpoaPath);
            return 1;
        }
        Ontology ontology = getHpOntology();
        PhenopacketCollection cohort = PhenopacketCollection.load(phenopacketPath,
                termId -> ancestors(ontology, termId));
        if (cohort.isEmpty()) {
            LOGGER.error("No phenopackets found at {}", phenopacketPath);
            return 1;
        }
        HpoaFile hpoa = HpoaFile.parse(hpoaPath);
        LOGGER.info("Parsed {} annotation lines from {}", hpoa.annotationCount(), hpoaPath);

        try {
            Files.createDirectories(outputDirectory);
        } catch (IOException e) {
            throw new PhenolRuntimeException("Could not create output directory " + outputDirectory + ": " + e.getMessage());
        }
        if (augment) {
            AugmentedHpoa augmented = new HpoaAugmenter(cohort, biocuration()).augment(hpoa);
            hpoa = augmented.hpoa();
            writeAugmentationSummary(augmented.augmentations());
            hpoa.write(outputDirectory.resolve("phenotype_augmented.hpoa"));
            LOGGER.info("Augmented HPOA has {} annotation lines", hpoa.annotationCount());
        }

        List<CaseResult> results = new HpoaAdjuster(hpoa, cohort).adjustAll(cohort.cases(), outputDirectory);
        writeSummary(results);
        logStatusCounts(results);
        return 0;
    }

    private static Set<TermId> ancestors(Ontology ontology, TermId termId) {
        return ontology.containsTerm(termId)
                ? ontology.getAncestorTermIds(termId, true)
                : Set.of(termId);
    }

    private String biocuration() {
        return biocurationTag + "[" + LocalDate.now() + "]";
    }

    private void writeAugmentationSummary(List<DiseaseAugmentation> augmentations) {
        Path summaryPath = outputDirectory.resolve("augmentation_summary.tsv");
        try (BufferedWriter writer = Files.newBufferedWriter(summaryPath)) {
            writer.write(String.join("\t", "disease_id", "cohort_size", "cohort_pmids",
                    "annotations_added", "annotations_pooled", "annotations_superseded"));
            writer.newLine();
            for (DiseaseAugmentation augmentation : augmentations) {
                writer.write(String.join("\t",
                        augmentation.diseaseId().getValue(),
                        String.valueOf(augmentation.cohortSize()),
                        augmentation.cohortPmids().stream().map(TermId::getValue).reduce((a, b) -> a + ";" + b).orElse(""),
                        String.valueOf(augmentation.linesAdded()),
                        String.valueOf(augmentation.linesPooled()),
                        String.valueOf(augmentation.linesSuperseded())));
                writer.newLine();
            }
        } catch (IOException e) {
            throw new PhenolRuntimeException("Could not write summary to " + summaryPath + ": " + e.getMessage());
        }
        LOGGER.info("Wrote augmentation summary to {}", summaryPath);
    }

    private void writeSummary(List<CaseResult> results) {
        Path summaryPath = outputDirectory.resolve("adjustment_summary.tsv");
        try (BufferedWriter writer = Files.newBufferedWriter(summaryPath)) {
            writer.write(String.join("\t", "phenopacket_id", "pmid", "disease_id", "status",
                    "lines_removed_disease", "lines_subtracted_disease", "lines_removed_total",
                    "lines_subtracted_total", "undecomposable_lines_removed",
                    "remaining_phenotype_lines", "adjusted_hpoa"));
            writer.newLine();
            for (CaseResult result : results) {
                writer.write(String.join("\t",
                        result.phenopacketCase().phenopacketId(),
                        result.phenopacketCase().pmid().getValue(),
                        result.phenopacketCase().diseaseId().getValue(),
                        result.status().name(),
                        String.valueOf(result.linesRemovedForDisease()),
                        String.valueOf(result.linesSubtractedForDisease()),
                        String.valueOf(result.linesRemovedTotal()),
                        String.valueOf(result.linesSubtractedTotal()),
                        String.valueOf(result.undecomposableLinesRemoved()),
                        String.valueOf(result.remainingPhenotypeLines()),
                        result.adjustedHpoa().map(Path::toString).orElse("-")));
                writer.newLine();
            }
        } catch (IOException e) {
            throw new PhenolRuntimeException("Could not write summary to " + summaryPath + ": " + e.getMessage());
        }
        LOGGER.info("Wrote summary to {}", summaryPath);
    }

    private void logStatusCounts(List<CaseResult> results) {
        for (CaseStatus status : CaseStatus.values()) {
            long count = results.stream().filter(result -> result.status() == status).count();
            LOGGER.info("{}: {} case(s)", status, count);
        }
    }
}
