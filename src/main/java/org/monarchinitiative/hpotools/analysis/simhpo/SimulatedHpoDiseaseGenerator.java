package org.monarchinitiative.hpotools.analysis.simhpo;

import com.google.protobuf.Timestamp;
import org.monarchinitiative.phenol.annotations.base.temporal.TemporalInterval;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseaseAnnotation;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.phenopackets.phenopackettools.builder.PhenopacketBuilder;
import org.phenopackets.phenopackettools.builder.constants.Onset;
import org.phenopackets.schema.v2.Phenopacket;

import org.phenopackets.schema.v2.core.Individual;
import org.phenopackets.schema.v2.core.MetaData;
import org.phenopackets.schema.v2.core.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class SimulatedHpoDiseaseGenerator {
    private final static Logger LOGGER = LoggerFactory.getLogger(SimulatedHpoDiseaseGenerator.class);
    private final static int DEFAULT_NUMBER_OF_TERMS = 5;
    private final static int DEFAULT_SEED = 42;

    private final HpoDiseases hpoDiseases;

    private final Ontology hpoOntology;
    private final Random random;

    public SimulatedHpoDiseaseGenerator(HpoDiseases hpoDiseases, Ontology hpoOntology) {
        this.hpoDiseases = hpoDiseases;
        this.hpoOntology = hpoOntology;
        this.random = new Random(DEFAULT_SEED);
    }


    public Optional<Phenopacket > generateSimulatedPhenopacket(TermId omimId) {
        return generateSimulatedPhenopacket(omimId, DEFAULT_NUMBER_OF_TERMS);
    }

    /**
     * Generates a simulated Phenopacket based on the specified OMIM ID and number of HPO terms.
     *
     * This method performs the following steps:
     * 1. Extracts the OMIM ID (e.g., OMIM:123456) from the provided "diseases" list. If the OMIM ID is not present,
     *      an error is thrown.
     * 2. Selects a specified number of HPO terms at random from the annotations of the disease.
     * 3. Chooses the annotations according to their frequencies by creating a normalized probability table.
     * 4. Uses PhenopacketTools Builder classes to construct the Phenopacket. For an example, see the phenopacket2prompt.
     *
     * @param omimId The OMIM ID representing the disease.
     * @param n_terms The number of HPO terms to select randomly from the disease annotations.
     * @return An Optional containing the generated Phenopacket if successful, otherwise an empty Optional.
     */
    public Optional<Phenopacket > generateSimulatedPhenopacket(TermId omimId, int n_terms, String identifier) {
        if (hpoDiseases.diseaseById().containsKey(omimId)) {
            HpoDisease disease = hpoDiseases.diseaseById().get(omimId);
            System.out.println(disease);

            // Add onset to the phenopacket if possible
            Optional<TemporalInterval> optOnsetRange = disease.diseaseOnset();
            Integer onset = null;
            if (optOnsetRange.isPresent()) {
                TemporalInterval onsetRange = optOnsetRange.get();
                System.out.println(onsetRange);
                // choose a random onset from the range
                int start = onsetRange.start().days();
                int end = onsetRange.end().days();
                onset = random.nextInt(start, end + 1); // TODO @pnrobinson: is end inclusive or exclusive?
                System.out.println("Randomly chosen onset (days): " + onset);
            } else {
                System.out.println("No onset information available");
            }

            // Add some age to the phenopacket by adding a few years to the onset
            if (onset != null) {
                int age = onset + random.nextInt(0, 10 * 365); // add up to 10 years
                System.out.println("Randomly chosen age (days): " + age);
            } else {
                System.out.println("No onset information available");
            }

            // Add random sex except for X-chromosomal recessive inheritance, in which case add male
           List<TermId> modeOfInheritance = disease.modesOfInheritance();
            // TODO @pnrobinson: is this the correct HPO term? Should it only be male if it is the only mode of inheritance, or also if there are other modes of inheritance?
            String xChromosomalRecessiveHPO = "HP:0001419";
            int male = 2;
            int sex;
            if (modeOfInheritance.contains(TermId.of(xChromosomalRecessiveHPO))) {
                System.out.println("X-chromosomal recessive inheritance");
                sex = male;
            } else {
                sex = random.nextInt(1, 3);
            }
            System.out.println("Randomly chosen sex: " + sex);

            double sum = disease.annotations().stream().mapToDouble(HpoDiseaseAnnotation::frequency).sum();

            double[] probabilities = disease.annotations().stream()
                    .mapToDouble(pf -> pf.frequency() / sum)
                    .toArray();

            System.out.println("Probabilities: " + Arrays.toString(probabilities));

            // Add annotations to the phenopacket
            List<HpoDiseaseAnnotation> annotations = (List<HpoDiseaseAnnotation>) disease.annotations();
            ProportionalRandomSelection<HpoDiseaseAnnotation> prs = new ProportionalRandomSelection<>(annotations, probabilities, random);
            List<HpoDiseaseAnnotation> selectedAnnotations = prs.sample(n_terms);
            System.out.println("Selected annotations: ");
            for (HpoDiseaseAnnotation annotation : selectedAnnotations) {
                System.out.println(annotation);
            }

        } else {
            LOGGER.error("Could not find OMIM identifier {}", omimId.getValue());
        }
        long currentSeconds = System.currentTimeMillis() / 1000;
        PhenopacketBuilder builder = PhenopacketBuilder.create(identifier, buildMetaData(currentSeconds));
        Individual individual = Individual.newBuilder()
        return Optional.empty(); // return the phenopacket unless there is an error
    }

    private MetaData buildMetaData(long currentSeconds) {
        return MetaData.newBuilder()
                .setCreated(Timestamp
                        .newBuilder()
                        .setSeconds(currentSeconds))
                .setCreatedBy("SimulatedHpoDiseaseGenerator")
                .setPhenopacketSchemaVersion("2.0")
                .setResources(0, Resource.newBuilder()
                        .setId("hp")
                        .setName("Human Phenotype Ontology")
                        .setNamespacePrefix("HP")
                        .setUrl("http://www.human-phenotype-ontology.org")
                        .setIriPrefix("http://purl.obolibrary.org/obo/HP_")
                        .build())
                .setResources(1, Resource.newBuilder()
                        .setId("omim")
                        .setName("Online Mendeian Inheritance in Man")
                        .setNamespacePrefix("OMIM")
                        .setUrl("https://omim.org/")
                        .setIriPrefix("https://omim.org/entry/") // TODO @pnrobinson: is this the correct IRI prefix?
                        .build())
                .build();
    }

}
