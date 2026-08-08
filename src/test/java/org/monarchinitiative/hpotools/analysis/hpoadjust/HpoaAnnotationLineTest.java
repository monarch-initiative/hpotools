package org.monarchinitiative.hpotools.analysis.hpoadjust;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HpoaAnnotationLineTest {

    private static final String SINGLE_REF_LINE =
            "OMIM:619340\tDevelopmental and epileptic encephalopathy 96\t\tHP:0011097\tPMID:31675180\tPCS\t\t1/2\t\t\tP\tHPO:probinson[2021-06-21]";
    private static final String MULTI_REF_LINE =
            "OMIM:607624\tGriscelli syndrome, type 2\t\tHP:0002220\tPMID:16517541;PMID:10835631\tPCS\t\t16/16\t\t\tP\tHPO:probinson[2020-11-06]";

    @Test
    public void parseSingleReferenceLine() {
        HpoaAnnotationLine line = HpoaAnnotationLine.of(SINGLE_REF_LINE);
        assertEquals(TermId.of("OMIM:619340"), line.diseaseId());
        assertEquals(TermId.of("HP:0011097"), line.hpoId());
        assertEquals(List.of(TermId.of("PMID:31675180")), line.references());
        assertTrue(line.isPhenotypeAnnotation());
        assertFalse(line.hasMultipleReferences());
        assertEquals(SINGLE_REF_LINE, line.raw());
    }

    @Test
    public void parseMultiReferenceLine() {
        HpoaAnnotationLine line = HpoaAnnotationLine.of(MULTI_REF_LINE);
        assertTrue(line.hasMultipleReferences());
        assertTrue(line.cites(TermId.of("PMID:16517541")));
        assertTrue(line.cites(TermId.of("PMID:10835631")));
        assertFalse(line.cites(TermId.of("PMID:16517")));
    }

    @Test
    public void nonPhenotypeAspect() {
        String inheritance = SINGLE_REF_LINE.replace("\tP\t", "\tI\t");
        HpoaAnnotationLine line = HpoaAnnotationLine.of(inheritance);
        assertFalse(line.isPhenotypeAnnotation());
    }

    @Test
    public void malformedLineThrows() {
        assertThrows(PhenolRuntimeException.class, () -> HpoaAnnotationLine.of("OMIM:619340\tonly\tfour\tfields"));
    }
}
