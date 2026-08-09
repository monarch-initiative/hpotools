package org.monarchinitiative.hpotools.analysis.hpoadjust;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CohortCountsTest {

    private static final TermId ROOT = TermId.of("HP:0000118");
    private static final TermId PARENT = TermId.of("HP:0000002");
    private static final TermId CHILD = TermId.of("HP:0000003");

    private static final Function<TermId, Set<TermId>> ANCESTORS = Map.of(
            ROOT, Set.of(ROOT),
            PARENT, Set.of(PARENT, ROOT),
            CHILD, Set.of(CHILD, PARENT, ROOT))::get;

    @Test
    public void ancestorCountingPropagatesToParents() {
        CohortCounts counts = CohortCounts.of(List.of(
                Set.of(CHILD),
                Set.of(PARENT),
                Set.of()), ANCESTORS);
        assertEquals(3, counts.cohortSize());
        assertEquals(1, counts.countOf(CHILD));
        assertEquals(2, counts.countOf(PARENT));
        assertEquals(2, counts.countOf(ROOT));
        assertEquals(0, counts.countOf(TermId.of("HP:9999999")));
    }

    @Test
    public void patientAnnotatedToParentAndChildCountsOnce() {
        CohortCounts counts = CohortCounts.of(List.of(Set.of(CHILD, PARENT)), ANCESTORS);
        assertEquals(1, counts.cohortSize());
        assertEquals(1, counts.countOf(PARENT));
        assertEquals(1, counts.countOf(ROOT));
    }
}
