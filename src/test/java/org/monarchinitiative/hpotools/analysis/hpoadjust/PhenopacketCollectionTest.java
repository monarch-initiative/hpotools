package org.monarchinitiative.hpotools.analysis.hpoadjust;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PhenopacketCollectionTest {

    private static PhenopacketCollection collection;

    @BeforeAll
    public static void init() throws Exception {
        Path directory = Path.of(PhenopacketCollectionTest.class.getResource("/hpoadjust").toURI());
        collection = PhenopacketCollection.load(directory, Set::of);
    }

    @Test
    public void loadsEveryPhenopacketInTheDirectory() {
        assertEquals(2, collection.cases().size());
        assertEquals(List.of(TermId.of("OMIM:615513"), TermId.of("OMIM:616576")), collection.diseases());
    }

    @Test
    public void groupsCasesByDisease() {
        assertEquals(1, collection.casesOf(TermId.of("OMIM:615513")).size());
        assertEquals(Set.of(TermId.of("PMID:24136356")), collection.pmidsOf(TermId.of("OMIM:615513")));
    }

    @Test
    public void countsAreLookedUpPerPublication() {
        Optional<CohortCounts> counts = collection.lookup(TermId.of("OMIM:615513"), TermId.of("PMID:24136356"));
        assertTrue(counts.isPresent());
        assertEquals(1, counts.get().cohortSize());
        assertEquals(1, counts.get().countOf(TermId.of("HP:0005403")));
    }

    @Test
    public void unknownCohortIsEmpty() {
        assertTrue(collection.lookup(TermId.of("OMIM:615513"), TermId.of("PMID:99999999")).isEmpty());
    }
}
