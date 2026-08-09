package org.monarchinitiative.hpotools.analysis.hpoadjust;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HpoaAdjusterTest {

    private static HpoaFile hpoa;
    private static HpoaAdjuster adjuster;

    @BeforeAll
    public static void init() throws Exception {
        Path path = Path.of(HpoaAdjusterTest.class.getResource("/hpoadjust/small_phenotype.hpoa").toURI());
        hpoa = HpoaFile.parse(path);
        adjuster = new HpoaAdjuster(hpoa);
    }

    private static PhenopacketCase caseOf(String id, String diseaseId, String pmid) {
        return new PhenopacketCase(id, TermId.of(diseaseId), "test disease", TermId.of(pmid),
                Set.of(), Path.of(id + ".json"));
    }

    @Test
    public void caseWithOtherEvidenceLeftIsAdjusted(@TempDir Path tempDir) {
        List<CaseResult> results = adjuster.adjustAll(
                List.of(caseOf("PMID_24136356_P10", "OMIM:615513", "PMID:24136356")), tempDir);
        assertEquals(1, results.size());
        CaseResult result = results.get(0);
        assertEquals(CaseStatus.ADJUSTED, result.status());
        assertEquals(3, result.linesRemovedForDisease());
        assertEquals(3, result.linesRemovedTotal());
        assertEquals(0, result.linesSubtractedTotal());
        assertEquals(1, result.undecomposableLinesRemoved());
        assertEquals(1, result.remainingPhenotypeLines());
        assertTrue(result.adjustedHpoa().isPresent());
        assertTrue(Files.isRegularFile(result.adjustedHpoa().get()));
    }

    @Test
    public void caseWithoutRemainingPhenotypeLinesIsInsufficient(@TempDir Path tempDir) {
        List<CaseResult> results = adjuster.adjustAll(
                List.of(caseOf("PMID_29403474_Fam089_I1", "OMIM:616576", "PMID:29403474")), tempDir);
        CaseResult result = results.get(0);
        assertEquals(CaseStatus.INSUFFICIENT_DATA, result.status());
        assertEquals(2, result.linesRemovedForDisease());
        assertEquals(0, result.remainingPhenotypeLines());
        assertTrue(result.adjustedHpoa().isPresent());
    }

    @Test
    public void caseWithUncitedPmidNeedsNoAdjustment(@TempDir Path tempDir) {
        List<CaseResult> results = adjuster.adjustAll(
                List.of(caseOf("PMID_16984281_P3", "OMIM:615513", "PMID:16984281")), tempDir);
        CaseResult result = results.get(0);
        assertEquals(CaseStatus.NO_PMID_EVIDENCE, result.status());
        assertEquals(0, result.linesRemovedTotal());
        assertTrue(result.adjustedHpoa().isEmpty());
        assertEquals(3, result.remainingPhenotypeLines());
    }

    @Test
    public void casesSharingPmidShareOneOutputFile(@TempDir Path tempDir) throws Exception {
        List<CaseResult> results = adjuster.adjustAll(List.of(
                caseOf("PMID_24136356_P10", "OMIM:615513", "PMID:24136356"),
                caseOf("PMID_24136356_P11", "OMIM:615513", "PMID:24136356")), tempDir);
        assertEquals(2, results.size());
        assertEquals(results.get(0).adjustedHpoa(), results.get(1).adjustedHpoa());
        try (var files = Files.list(tempDir)) {
            assertEquals(1, files.count());
        }
    }

    @Test
    public void adjustedFileNoLongerCitesPmid(@TempDir Path tempDir) {
        List<CaseResult> results = adjuster.adjustAll(
                List.of(caseOf("PMID_24136356_P10", "OMIM:615513", "PMID:24136356")), tempDir);
        HpoaFile reloaded = HpoaFile.parse(results.get(0).adjustedHpoa().get());
        assertTrue(reloaded.linesCiting(TermId.of("PMID:24136356")).isEmpty());
        assertEquals(5, reloaded.annotationCount());
    }

    @Test
    public void multiReferenceLineIsSubtractedWhenCohortIsKnown(@TempDir Path tempDir) {
        TermId disease = TermId.of("OMIM:615513");
        TermId pmid = TermId.of("PMID:24136356");
        TermId sharedTerm = TermId.of("HP:0002205");
        CohortSource source = (diseaseId, target) ->
                diseaseId.equals(disease) && target.equals(pmid)
                        ? Optional.of(new CohortCounts(Map.of(sharedTerm, 12), 14))
                        : Optional.empty();
        HpoaAdjuster subtracting = new HpoaAdjuster(hpoa, source);
        List<CaseResult> results = subtracting.adjustAll(
                List.of(caseOf("PMID_24136356_P10", "OMIM:615513", "PMID:24136356")), tempDir);
        CaseResult result = results.get(0);
        assertEquals(CaseStatus.ADJUSTED, result.status());
        assertEquals(2, result.linesRemovedForDisease());
        assertEquals(1, result.linesSubtractedForDisease());
        assertEquals(0, result.undecomposableLinesRemoved());
        assertEquals(2, result.remainingPhenotypeLines());

        HpoaFile reloaded = HpoaFile.parse(result.adjustedHpoa().get());
        assertTrue(reloaded.linesCiting(pmid).isEmpty());
        HpoaAnnotationLine subtractedLine = reloaded.annotationLines().stream()
                .filter(line -> line.hpoId().equals(sharedTerm))
                .findFirst()
                .orElseThrow();
        assertEquals(Optional.of(new Ratio(3, 3)), subtractedLine.frequencyRatio());
        assertEquals(List.of(TermId.of("PMID:24165795")), subtractedLine.references());
    }

    @Test
    public void subtractionThatZeroesTheLineRemovesIt(@TempDir Path tempDir) {
        TermId sharedTerm = TermId.of("HP:0002205");
        CohortSource source = (diseaseId, target) ->
                Optional.of(new CohortCounts(Map.of(sharedTerm, 15), 17));
        HpoaAdjuster subtracting = new HpoaAdjuster(hpoa, source);
        List<CaseResult> results = subtracting.adjustAll(
                List.of(caseOf("PMID_24136356_P10", "OMIM:615513", "PMID:24136356")), tempDir);
        CaseResult result = results.get(0);
        assertEquals(3, result.linesRemovedForDisease());
        assertEquals(0, result.linesSubtractedForDisease());
        assertEquals(1, result.remainingPhenotypeLines());
    }

    @Test
    public void outputFileNameContainsPmid() {
        assertEquals("phenotype_PMID_24136356.hpoa", HpoaAdjuster.fileNameFor(TermId.of("PMID:24136356")));
    }
}
