package org.monarchinitiative.hpotools.analysis;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Optional;

public record PpktStoreItem(
        String diseaseLabel,
        TermId disease_id,
        String patient_id,
        String gene,
        String PMID,
        String filename) {


    public static Optional<PpktStoreItem> fromLine(String line) {
        String [] tokens = line.split("\t");
        System.out.println(line);
        System.out.println(tokens);
        try {
            String diseaseLabel = tokens[0];
            TermId diseaseId = TermId.of(tokens[1]);
            String patientId = tokens[2];
            String gene = tokens[3];
            String PMID = tokens[4];
            String filename = tokens[5];
            return Optional.of(new PpktStoreItem(diseaseLabel, diseaseId, patientId, gene, PMID, filename));
        } catch (Exception e) {
            System.err.println("Could not parse " + line);
            System.exit(1);
        }
        return Optional.empty();
    }
}
