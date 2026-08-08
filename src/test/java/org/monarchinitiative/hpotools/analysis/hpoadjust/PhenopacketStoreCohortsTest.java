package org.monarchinitiative.hpotools.analysis.hpoadjust;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PhenopacketStoreCohortsTest {

    @Test
    public void loadsCohortsFromDirectoryTree() throws Exception {
        Path storeDir = Path.of(PhenopacketStoreCohortsTest.class.getResource("/hpoadjust").toURI());
        PhenopacketStoreCohorts cohorts = PhenopacketStoreCohorts.load(storeDir, termId -> Set.of(termId));

        Optional<CohortCounts> counts = cohorts.lookup(TermId.of("OMIM:615513"), TermId.of("PMID:24136356"));
        assertTrue(counts.isPresent());
        assertEquals(1, counts.get().cohortSize());
        assertEquals(1, counts.get().countOf(TermId.of("HP:0005403")));

        assertTrue(cohorts.lookup(TermId.of("OMIM:615513"), TermId.of("PMID:99999999")).isEmpty());
    }
}
