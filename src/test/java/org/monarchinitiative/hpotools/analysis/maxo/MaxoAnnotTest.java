package org.monarchinitiative.hpotools.analysis.maxo;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MaxoAnnotTest {

    private static List<String> fields = List.of("MONDO:0100079",
            "Epileptic Encephalopathy, Early Infantile, 6 (dravet Syndrome)",
            "PMID:9596203",
            "MAXO:0000208",
            "sodium channel inhibitor therapy",
            "HP:0032794",
            "CONTRAINDICATED",
            "TAS",
            "CHEBI:6367",
            "lamotrigine",
            "comment: \"Seizures can worsen on withdrawing lamotrigine.\"",
            "ORCID:0000-0002-1735-8178",
            "2023-07-12",
            "2023-07-12");
    private static final String EIEE6_line = String.join("\t", fields);

    private static final MaxoAnnot maxoAnnot =  MaxoAnnot.fromLine(EIEE6_line);

    @Test
    public void testDiseaseId() {
        TermId diseaseId = TermId.of(fields.get(0));
        assertEquals(diseaseId, maxoAnnot.diseaseId());
    }

    @Test
    public void testDiseaseLabel() {
        String expected = fields.get(1);
        assertEquals(expected, maxoAnnot.diseaseLabel());
    }
    @Test
    public void testCitation() {
        String expected = fields.get(2);
        assertEquals(expected, maxoAnnot.citation());
    }

    @Test
    public void testMaxoId() {
        TermId expected = TermId.of(fields.get(3));
        assertEquals(expected, maxoAnnot.maxoId());
    }

    @Test
    public void testMaxoLabel() {
        String expected = fields.get(4);
        assertEquals(expected, maxoAnnot.maxoLabel());
    }

    @Test
    public void testHpoId() {
        TermId expected = TermId.of(fields.get(5));
        assertEquals(expected, maxoAnnot.hpoId());
    }

    @Test
    public void testMaxoRelation() {
        String expected = fields.get(6);
        assertEquals(expected, maxoAnnot.maxoRelation());
    }
    /*
    String evidenceCode = tokens[7];
        TermId extensionId = null;
        String extensionLabel = null;
        if (tokens[8] != null && tokens[8].length() > 3) {
            extensionId = TermId.of(tokens[8]);
            extensionLabel = tokens[9];
        }
        String attribute = tokens[10];
        String creator = tokens[11];
        String lastUpdate = tokens[12];
        String createdOn = tokens[13];
     */

    @Test
    public void testEvidence() {
        String expected = fields.get(7);
        assertEquals(expected, maxoAnnot.evidenceCode());
    }

    @Test
    public void testExtensionCode() {
        TermId expected = TermId.of(fields.get(8));
        assertEquals(expected, maxoAnnot.extensionId());
    }

    @Test
    public void testExtensionLabel() {
        String expected = fields.get(9);
        assertEquals(expected, maxoAnnot.extensionLabel());
    }

    @Test
    public void testAttribute() {
        String expected = fields.get(10);
        assertEquals(expected, maxoAnnot.attribute());
    }

    @Test
    public void testCreator() {
        String expected = fields.get(11);
        assertEquals(expected, maxoAnnot.creator());
    }

    @Test
    public void testLastUpdate() {
        String expected = fields.get(12);
        assertEquals(expected, maxoAnnot.lastUpdate());
    }

    @Test
    public void testCreatedOn() {
        String expected = fields.get(13);
        assertEquals(expected, maxoAnnot.createdOn());
    }

}
