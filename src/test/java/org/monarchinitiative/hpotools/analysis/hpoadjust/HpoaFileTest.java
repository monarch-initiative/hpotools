package org.monarchinitiative.hpotools.analysis.hpoadjust;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HpoaFileTest {

    private static HpoaFile hpoa;

    @BeforeAll
    public static void init() throws Exception {
        Path path = Path.of(HpoaFileTest.class.getResource("/hpoadjust/small_phenotype.hpoa").toURI());
        hpoa = HpoaFile.parse(path);
    }

    @Test
    public void parseRetainsAllAnnotationLines() {
        assertEquals(8, hpoa.annotationCount());
    }

    @Test
    public void parseRetainsHeader() {
        assertEquals(5, hpoa.headerLines().size());
        assertTrue(hpoa.headerLines().get(0).startsWith("#description"));
        assertTrue(hpoa.headerLines().get(4).startsWith("database_id"));
    }

    @Test
    public void hasPmidEvidenceIsTrueForAnnotatedDisease() {
        assertTrue(hpoa.hasPmidEvidence(TermId.of("OMIM:615513"), TermId.of("PMID:24136356")));
    }

    @Test
    public void hasPmidEvidenceIsFalseForOtherDisease() {
        assertFalse(hpoa.hasPmidEvidence(TermId.of("OMIM:616576"), TermId.of("PMID:24136356")));
    }

    @Test
    public void hasPmidEvidenceIsFalseForUnknownPmid() {
        assertFalse(hpoa.hasPmidEvidence(TermId.of("OMIM:615513"), TermId.of("PMID:99999999")));
    }

    @Test
    public void withoutPmidRemovesSingleAndMultiReferenceLines() {
        HpoaFile adjusted = hpoa.withoutPmid(TermId.of("PMID:24136356"));
        assertEquals(5, adjusted.annotationCount());
        assertFalse(adjusted.hasPmidEvidence(TermId.of("OMIM:615513"), TermId.of("PMID:24136356")));
        assertEquals(1, adjusted.phenotypeLineCount(TermId.of("OMIM:615513")));
    }

    @Test
    public void withoutPmidLeavesOtherDiseasesUntouched() {
        HpoaFile adjusted = hpoa.withoutPmid(TermId.of("PMID:24136356"));
        assertEquals(1, adjusted.phenotypeLineCount(TermId.of("OMIM:616576")));
        assertEquals(2, adjusted.phenotypeLineCount(TermId.of("OMIM:603597")));
    }

    @Test
    public void linesCitingFindsMultiReferenceLines() {
        List<HpoaAnnotationLine> lines = hpoa.linesCiting(TermId.of("PMID:24165795"));
        assertEquals(2, lines.size());
        assertEquals(1, lines.stream().filter(HpoaAnnotationLine::hasMultipleReferences).count());
    }

    @Test
    public void writtenFileEqualsOriginalMinusRemovedLines(@TempDir Path tempDir) throws Exception {
        Path original = Path.of(HpoaFileTest.class.getResource("/hpoadjust/small_phenotype.hpoa").toURI());
        Path out = tempDir.resolve("adjusted.hpoa");
        hpoa.withoutPmid(TermId.of("PMID:29403474")).write(out);
        List<String> expected = Files.readAllLines(original).stream()
                .filter(line -> !line.contains("PMID:29403474"))
                .toList();
        assertEquals(expected, Files.readAllLines(out));
    }

    @Test
    public void writeReparseRoundTrip(@TempDir Path tempDir) {
        Path out = tempDir.resolve("copy.hpoa");
        hpoa.write(out);
        HpoaFile reparsed = HpoaFile.parse(out);
        assertEquals(hpoa.annotationCount(), reparsed.annotationCount());
        assertEquals(hpoa.headerLines(), reparsed.headerLines());
    }
}
