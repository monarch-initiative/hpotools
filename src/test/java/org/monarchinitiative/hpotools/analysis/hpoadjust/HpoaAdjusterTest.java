package org.monarchinitiative.hpotools.analysis.hpoadjust;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HpoaAdjusterTest {

    private static HpoaAdjuster adjuster;

    @BeforeAll
    public static void init() throws Exception {
        Path path = Path.of(HpoaAdjusterTest.class.getResource("/hpoadjust/small_phenotype.hpoa").toURI());
        adjuster = new HpoaAdjuster(HpoaFile.parse(path));
    }

    private static PhenopacketCase caseOf(String id, String diseaseId, String pmid) {
        return new PhenopacketCase(id, TermId.of(diseaseId), TermId.of(pmid), Path.of(id + ".json"));
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
        assertEquals(1, result.multiReferenceLinesRemoved());
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
    public void outputFileNameContainsPmid() {
        assertEquals("phenotype_PMID_24136356.hpoa", HpoaAdjuster.fileNameFor(TermId.of("PMID:24136356")));
    }
}
