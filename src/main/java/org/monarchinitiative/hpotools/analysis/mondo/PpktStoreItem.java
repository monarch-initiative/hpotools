package org.monarchinitiative.hpotools.analysis.mondo;

import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Optional;

public record PpktStoreItem(
        String diseaseLabel,
        TermId disease_id,
        String patient_id,
        String gene,
        String PMID,
        String cohort,
        String filename) {


    public static Optional<PpktStoreItem> fromLine(String line) {
        String [] tokens = line.split("\t");
        if (tokens.length < 9) {
            throw new PhenolRuntimeException("Bad line in PPKTStoreItem: " + line);
        }
        try {
            String diseaseLabel = tokens[0];
            TermId diseaseId = TermId.of(tokens[1]);
            String patientId = tokens[2];
            String gene = tokens[3];
            String PMID = tokens[4];
            String cohort = tokens[7];
            String filename = tokens[8];
            if (! filename.endsWith(".json")) {
                throw new PhenolRuntimeException("Filename must end with .json but we got: " + filename);
            }
            return Optional.of(new PpktStoreItem(diseaseLabel, diseaseId, patientId, gene, PMID, cohort, filename));
        } catch (Exception e) {
            System.err.println("Could not parse " + line);
            System.exit(1);
        }
        return Optional.empty();
    }
}
