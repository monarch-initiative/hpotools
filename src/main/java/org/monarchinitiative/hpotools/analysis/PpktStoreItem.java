package org.monarchinitiative.hpotools.analysis;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Optional;

public record PpktStoreItem(
        String diseaseLabel,
        TermId disease_id,
        String patient_id,
        String gene,
        String PMID) {


    public static Optional<PpktStoreItem> fromLine(String line) {
        String [] tokens = line.split("\t");
        try {
            String diseaseLabel = tokens[0];
            TermId diseaseId = TermId.of(tokens[1]);
            String patientId = tokens[2];
            String gene = tokens[3];
            String PMID = tokens[4];
            return Optional.of(new PpktStoreItem(diseaseLabel, diseaseId, patientId, gene, PMID));
        } catch (Exception e) {
            System.err.println("Could not parse " + line);
        }
        return Optional.empty();
    }
}
