package org.monarchinitiative.hpotools.analysis.simhpo;

import org.apache.poi.sl.draw.geom.GuideIf;
import org.monarchinitiative.hpotools.cmd.SimHpoCommand;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.phenopackets.schema.v2.Phenopacket;

import java.util.Optional;

public class SimulatedHpoDiseaseGenerator {


    private final HpoDiseases hpoDiseases;

    private final Ontology hpoOntology;

    public SimulatedHpoDiseaseGenerator(HpoDiseases hpoDiseases, Ontology hpoOntology) {
        this.hpoDiseases = hpoDiseases;
        this.hpoOntology = hpoOntology;
    }

    public Optional<Phenopacket > generateSimulatedPhenopacket(TermId omimId) {
        if (hpoDiseases.diseaseById().containsKey(omimId)) {
            /*
             * TODO ..
             * 1. Extract OMIM:123456 from "diseases", throw error if not present
             * 2. choose nterms HPO terms at random from the annotations of the disease
             * 3. choose the annotationds according to the frequencies -- for instance, create a normalized probability table
             * Something like this: https://stackoverflow.com/questions/43530244/how-to-choose-an-item-in-a-list-according-to-a-specific-probability
             * 4. Use PhenopacketTools Builder classes to build phenopacket (See phenopacket2prompt for example)
             * Just simulate HPO terms, do not simulate age and sex for now, but we could do htis in phenopacket2prompt
             */
            return Optional.empty(); // return the phenopacket unless there is an error

        } else {
            return Optional.empty();
        }
    }



}
