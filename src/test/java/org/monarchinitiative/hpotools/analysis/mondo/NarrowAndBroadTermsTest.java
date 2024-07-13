package org.monarchinitiative.hpotools.analysis.mondo;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.hpotools.analysis.OntologyTerm;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.File;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class NarrowAndBroadTermsTest {


    @Test
    @Disabled("This test is disabled because it depends on mondo.json having been previously downloaded.")
    void testFindALS() {
        // This test will be skipped when running the test suite.
        String mondopath = "data/mondo.json";
        Ontology mondo = OntologyLoader.loadOntology(new File(mondopath));
        assertNotNull(mondo);
        NarrowAndBroadTerms nbterms = new NarrowAndBroadTerms(mondo);
        assertNotNull(nbterms);
        TermId als1 = TermId.of("MONDO:0007103");
        Optional<Term> opt = mondo.termForTermId(als1);
        assertEquals("amyotrophic lateral sclerosis type 1", opt.get().getName());
        // familial amyotrophic lateral sclerosis
        TermId familialAls = TermId.of("MONDO:0005144");
        OntologyTerm narrowOntologyTerm = nbterms.getNarrowTermId(als1);
        assertEquals(familialAls, narrowOntologyTerm.id());
        TermId broadTermId = nbterms.getBroadForNarrow(familialAls);
      //  System.out.println(broadTermId.getValue());

    }

}
