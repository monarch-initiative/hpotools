package org.monarchinitiative.hpotools.analysis.simhpo;

import org.monarchinitiative.phenol.annotations.base.temporal.TemporalInterval;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.phenopackets.phenopackettools.builder.constants.Onset;
import org.phenopackets.schema.v2.Phenopacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class SimulatedHpoDiseaseGenerator {
    private final static Logger LOGGER = LoggerFactory.getLogger(SimulatedHpoDiseaseGenerator.class);
    private final static int DEFAULT_NUMBER_OF_TERMS = 5;

    private final HpoDiseases hpoDiseases;

    private final Ontology hpoOntology;

    public SimulatedHpoDiseaseGenerator(HpoDiseases hpoDiseases, Ontology hpoOntology) {
        this.hpoDiseases = hpoDiseases;
        this.hpoOntology = hpoOntology;
    }


    public Optional<Phenopacket > generateSimulatedPhenopacket(TermId omimId) {
        return generateSimulatedPhenopacket(omimId, DEFAULT_NUMBER_OF_TERMS);
    }

    public Optional<Phenopacket > generateSimulatedPhenopacket(TermId omimId, int n_terms) {
        if (hpoDiseases.diseaseById().containsKey(omimId)) {
            HpoDisease disease = hpoDiseases.diseaseById().get(omimId);
            System.out.println(disease);
            Optional<TemporalInterval> opt = disease.diseaseOnset();
            // Add onset to the phenopacket if possible
            // Add some age to the phenopacket by adding a few years to the onset
            // Add random sex except for X-chromosomal recessive inheritance, in which case add male

            for (var pf: disease.annotations()) {
                System.out.printf("freqeuncy of the term %.2f", pf.frequency());
                System.out.println(" ");
            }/*
                 * TODO ..
                 * 1. Extract OMIM:123456 from "diseases", throw error if not present
                 * 2. choose nterms HPO terms at random from the annotations of the disease
                 * 3. choose the annotations according to the frequencies -- for instance, create a normalized probability table
                 * Something like this: https://stackoverflow.com/questions/43530244/how-to-choose-an-item-in-a-list-according-to-a-specific-probability
                 * 4. Use PhenopacketTools Builder classes to build phenopacket (See phenopacket2prompt for example)
                 * Just simulate HPO terms, do not simulate age and sex for now, but we could do htis in phenopacket2prompt
                 */

        } else {
            LOGGER.error("Could not find OMIM identifier {}", omimId.getValue());
        }
        return Optional.empty(); // return the phenopacket unless there is an error
    }



}
