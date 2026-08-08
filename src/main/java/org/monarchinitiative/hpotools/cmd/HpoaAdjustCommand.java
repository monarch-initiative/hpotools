package org.monarchinitiative.hpotools.cmd;

import org.monarchinitiative.hpotools.analysis.hpoadjust.CaseResult;
import org.monarchinitiative.hpotools.analysis.hpoadjust.CaseStatus;
import org.monarchinitiative.hpotools.analysis.hpoadjust.CohortSource;
import org.monarchinitiative.hpotools.analysis.hpoadjust.HpoaAdjuster;
import org.monarchinitiative.hpotools.analysis.hpoadjust.HpoaFile;
import org.monarchinitiative.hpotools.analysis.hpoadjust.PhenopacketCase;
import org.monarchinitiative.hpotools.analysis.hpoadjust.PhenopacketStoreCohorts;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

@CommandLine.Command(name = "hpoadjust",
        mixinStandardHelpOptions = true,
        description = "Remove PMID-derived annotations from the HPOA for phenopacket-based benchmarking")
public class HpoaAdjustCommand extends HPOCommand implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HpoaAdjustCommand.class);

    @CommandLine.Option(names = {"-p", "--phenopackets"},
            required = true,
            description = "path to a phenopacket JSON file or a directory of phenopackets")
    private Path phenopacketPath;

    @CommandLine.Option(names = {"-o", "--outdir"},
            description = "directory for adjusted HPOA files (default: ${DEFAULT-VALUE})")
    private Path outputDirectory = Path.of("hpoa-adjusted");

    @CommandLine.Option(names = {"-s", "--store"},
            description = "phenopacket-store directory; enables cohort-count subtraction for multi-reference lines (requires --hpo)")
    private Path phenopacketStorePath;

    @Override
    public Integer call() {
        Path hpoaPath = Path.of(annotpath);
        if (!Files.isRegularFile(hpoaPath)) {
            LOGGER.error("Could not find phenotype.hpoa at {}", hpoaPath);
            return 1;
        }
        List<PhenopacketCase> cases = collectCases();
        if (cases.isEmpty()) {
            LOGGER.error("No phenopackets found at {}", phenopacketPath);
            return 1;
        }
        HpoaFile hpoa = HpoaFile.parse(hpoaPath);
        LOGGER.info("Parsed {} annotation lines from {}", hpoa.annotationCount(), hpoaPath);
        HpoaAdjuster adjuster = new HpoaAdjuster(hpoa, cohortSource());
        List<CaseResult> results = adjuster.adjustAll(cases, outputDirectory);
        writeSummary(results);
        logStatusCounts(results);
        return 0;
    }

    private CohortSource cohortSource() {
        if (phenopacketStorePath == null) {
            LOGGER.info("No phenopacket store given, multi-reference lines citing a target PMID will be dropped");
            return CohortSource.empty();
        }
        if (!Files.isDirectory(phenopacketStorePath)) {
            throw new PhenolRuntimeException("Not a directory: " + phenopacketStorePath);
        }
        Ontology ontology = getHpOntology();
        return PhenopacketStoreCohorts.load(phenopacketStorePath,
                termId -> ontology.getAncestorTermIds(termId, true));
    }

    private List<PhenopacketCase> collectCases() {
        List<Path> files = phenopacketFiles();
        List<PhenopacketCase> cases = new ArrayList<>();
        for (Path file : files) {
            try {
                cases.add(PhenopacketCase.fromFile(file));
            } catch (PhenolRuntimeException e) {
                LOGGER.warn("Skipping {}: {}", file, e.getMessage());
            }
        }
        return cases;
    }

    private List<Path> phenopacketFiles() {
        if (Files.isRegularFile(phenopacketPath)) {
            return List.of(phenopacketPath);
        }
        if (!Files.isDirectory(phenopacketPath)) {
            throw new PhenolRuntimeException("Not a file or directory: " + phenopacketPath);
        }
        try (Stream<Path> paths = Files.walk(phenopacketPath)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new PhenolRuntimeException("Could not read " + phenopacketPath + ": " + e.getMessage());
        }
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
