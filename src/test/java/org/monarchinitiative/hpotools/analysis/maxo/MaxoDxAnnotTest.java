package org.monarchinitiative.hpotools.analysis.maxo;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MaxoDxAnnotTest {

    private static List<String> fields = List.of(
            "HP:0030990",
            "Pleomorphic cholangitis",
            "is_observable_through",
            "MAXO:0000376",
            "biopsy of liver",
            "ORCID:0000-0002-0736-9199"
    );
    private final static String line = String.join("\t", fields);

    private final static MaxoDxAnnot dxAnnot = MaxoDxAnnot.fromLine(line);

    @Test
    public void testHpoId() {
        TermId expected = TermId.of(fields.get(0));
        assertEquals(expected, dxAnnot.hpoId());
    }

    @Test
    public void testHpoLabel() {
        String expected = fields.get(1);
        assertEquals(expected, dxAnnot.hpoLabel());
    }

    @Test
    public void testPredicate() {
        String expected = fields.get(2);
        assertEquals(expected, dxAnnot.predicate());
    }

    @Test
    public void testMaxoId() {
        TermId expected = TermId.of(fields.get(3));
        assertEquals(expected, dxAnnot.maxoId());
    }

    @Test
    public void testMaxoLabel() {
        String expected = fields.get(4);
        assertEquals(expected, dxAnnot.maxoLabel());
    }

    @Test
    public void testCreatorId() {
        String expected = fields.get(5);
        assertEquals(expected, dxAnnot.creatorId());
    }
}
