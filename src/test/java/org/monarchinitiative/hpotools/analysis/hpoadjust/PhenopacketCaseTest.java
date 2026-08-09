package org.monarchinitiative.hpotools.analysis.hpoadjust;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PhenopacketCaseTest {

    private static Path resource(String name) throws Exception {
        return Path.of(PhenopacketCaseTest.class.getResource("/hpoadjust/" + name).toURI());
    }

    @Test
    public void diseaseIdIsTakenFromInterpretation() throws Exception {
        PhenopacketCase ppktCase = PhenopacketCase.fromFile(resource("PMID_24136356_P10.json"));
        assertEquals("PMID_24136356_P10", ppktCase.phenopacketId());
        assertEquals(TermId.of("OMIM:615513"), ppktCase.diseaseId());
        assertEquals("Immunodeficiency 14", ppktCase.diseaseName());
        assertEquals(TermId.of("PMID:24136356"), ppktCase.pmid());
        assertEquals(Set.of(TermId.of("HP:0005403")), ppktCase.observedTerms());
    }

    @Test
    public void diseaseIdFallsBackToDiseasesList() throws Exception {
        PhenopacketCase ppktCase = PhenopacketCase.fromFile(resource("PMID_29403474_no_interpretation.json"));
        assertEquals(TermId.of("OMIM:616576"), ppktCase.diseaseId());
        assertEquals(TermId.of("PMID:29403474"), ppktCase.pmid());
        assertEquals(Set.of(TermId.of("HP:0004313")), ppktCase.observedTerms());
    }
}
