package org.monarchinitiative.hpotools.analysis.simhpo;

import com.google.protobuf.Timestamp;
import org.monarchinitiative.phenol.annotations.base.temporal.TemporalInterval;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseaseAnnotation;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.phenopackets.phenopackettools.builder.PhenopacketBuilder;
import org.phenopackets.schema.v2.Phenopacket;

import org.phenopackets.schema.v2.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class SimulatedHpoDiseaseGenerator {
    private final static Logger LOGGER = LoggerFactory.getLogger(SimulatedHpoDiseaseGenerator.class);
    private final static int DEFAULT_NUMBER_OF_TERMS = 5;
    private final static int DEFAULT_SEED = 43;

    private final HpoDiseases hpoDiseases;

    private final Ontology hpoOntology;
    private final Random random;

    private static int idCounter = 0;

    private final List<TermId> allPhenotypetermIds;

    public SimulatedHpoDiseaseGenerator(HpoDiseases hpoDiseases, Ontology hpoOntology) {
        this.hpoDiseases = hpoDiseases;
        this.hpoOntology = hpoOntology;
        this.random = new Random(DEFAULT_SEED);
        allPhenotypetermIds = new ArrayList<>();
        TermId phenotypicAbn = TermId.of("HP:0000118");
        this.hpoOntology.graph().getDescendants(phenotypicAbn).forEach(t -> allPhenotypetermIds.add(t));
    }


    public Optional<Phenopacket > generateSimulatedPhenopacket(TermId omimId, int terms_to_simulate, int nNoiseTerms, int nImpreciseTerms) {
        return generateSimulatedPhenopacket(omimId, terms_to_simulate, nNoiseTerms, nImpreciseTerms, "SIM-" + SimulatedHpoDiseaseGenerator.idCounter++);
    }

    public Optional<Phenopacket > generateSimulatedPhenopacket(TermId omimId) {
        return generateSimulatedPhenopacket(omimId, DEFAULT_NUMBER_OF_TERMS, 0,0, "SIM-" + SimulatedHpoDiseaseGenerator.idCounter++);
    }

    /**
     * Generates a Phenopacket for a disease from HPOA.
     * <p>
     * This method performs the following steps:
     * 1. Extracts the OMIM ID (e.g., OMIM:123456) from the provided "diseases" list. If the OMIM ID is not present,
     *      an error is thrown.
     * 2. Selects a specified number of HPO terms at random from the annotations of the disease.
     * 3. Chooses the annotations according to their frequencies by creating a normalized probability table.
     * 4. Uses PhenopacketTools Builder classes to construct the Phenopacket. For an example, see the phenopacket2prompt.
     *
     * @param diseaseId The OMIM ID representing the disease.
     * @param identifier The number of HPO terms to select randomly from the disease annotations.
     * @return An Optional containing the generated Phenopacket if successful, otherwise an empty Optional.
     */
    public Optional<Phenopacket > generatePhenopacketForHpoaDisease(TermId diseaseId, String identifier) {
        long age = 0;
        int sex = 0;
        long onset = 0;
        List<HpoDiseaseAnnotation> annotations;
        if (hpoDiseases.diseaseById().containsKey(diseaseId)) {

            // Get Phenol disease object
            HpoDisease disease = hpoDiseases.diseaseById().get(diseaseId);

            // Add annotations to the phenopacket
            annotations = (List<HpoDiseaseAnnotation>) disease.annotations();
        } else {
            LOGGER.error("Could not find OMIM identifier {}", diseaseId.getValue());
            return Optional.empty();
        }
        List<TermId> hpoTermIds = annotations.stream().map(HpoDiseaseAnnotation::id).toList();

        long currentSeconds = System.currentTimeMillis() / 1000;
        Individual subject = Individual.newBuilder()
                .setId(identifier)
                .setDateOfBirth(Timestamp.newBuilder().setSeconds(currentSeconds - (age / 24 / 60 / 60)))
                .setSex(Sex.forNumber(sex))
                .setTaxonomy(OntologyClass.newBuilder()
                        .setId("NCBITaxon:9606")
                        .setLabel("Homo Sapiens Sapiens")
                        .build())
                .build();
        Disease disease;
        if (onset > 0) {
            disease = Disease.newBuilder()
                    .setTerm(OntologyClass.newBuilder()
                            .setId(diseaseId.getValue())
                            .build())
                    .setOnset(TimeElement.newBuilder()
                            .setTimestamp(Timestamp.newBuilder()
                                    .setSeconds(currentSeconds - onset / 24 / 60 / 60)  // days to seconds
                                    .build())
                            .build())
                    .build();
        } else {
            disease = Disease.newBuilder()
                    .setTerm(OntologyClass.newBuilder()
                            .setId(diseaseId.getValue())
                            .build())
                    .build();
        }
        List<PhenotypicFeature> phenotypicFeatures = new ArrayList<>();
        for (TermId tid : hpoTermIds) {
            Optional<Term> opt = this.hpoOntology.termForTermId(tid);
            if (opt.isEmpty() ) {
                throw new PhenolRuntimeException("Could not find term " + tid.getValue());
            }
            Term hpoTerm = opt.get();
            OntologyClass type = OntologyClass.newBuilder()
                    .setId(hpoTerm.id().getValue())
                    .setLabel(hpoTerm.getName())
                    .build();

            // TODO: could still add simulated onset and resolution in the future, is present in HpoDiseaseAnnotation
            phenotypicFeatures.add(PhenotypicFeature.newBuilder()
                    .setType(type)
                    .build());
        }
        PhenopacketBuilder builder = PhenopacketBuilder.create(identifier, buildMetaData(currentSeconds))
                .individual(subject) // TODO: @pnrobinson for all other fields it's add... for individual it isn't?
                .addDisease(disease)
                .addPhenotypicFeatures(phenotypicFeatures);
        Phenopacket phenopacket = builder.build();
        return Optional.of(phenopacket); // return the phenopacket unless there is an error
    }

    /**
     * Generates a simulated Phenopacket based on the specified OMIM ID and number of HPO terms.
     * <p>
     * This method performs the following steps:
     * 1. Extracts the OMIM ID (e.g., OMIM:123456) from the provided "diseases" list. If the OMIM ID is not present,
     *      an error is thrown.
     * 2. Selects a specified number of HPO terms at random from the annotations of the disease.
     * 3. Chooses the annotations according to their frequencies by creating a normalized probability table.
     * 4. Uses PhenopacketTools Builder classes to construct the Phenopacket. For an example, see the phenopacket2prompt.
     *
     * @param omimId The OMIM ID representing the disease.
     * @param nTerms The number of HPO terms to select randomly from the disease annotations.
     * @return An Optional containing the generated Phenopacket if successful, otherwise an empty Optional.
     */
    public Optional<Phenopacket > generateSimulatedPhenopacket(TermId omimId, int nTerms, int nNoiseTerms, int nImpreciseTerms, String identifier) {
        long age = 0;
        int sex = 0;
        long onset = 0;
        List<HpoDiseaseAnnotation> annotations;
        if (hpoDiseases.diseaseById().containsKey(omimId)) {
            HpoDisease disease = hpoDiseases.diseaseById().get(omimId);

            // Add onset to the phenopacket if possible
            Optional<TemporalInterval> optOnsetRange = disease.diseaseOnset();
            if (optOnsetRange.isPresent()) {
                TemporalInterval onsetRange = optOnsetRange.get();
                // choose a random onset from the range
                int start = onsetRange.start().days();
                int end = onsetRange.end().days();
                onset = (int)(start+end)/2;
            } else {
                LOGGER.debug("No onset information available for disease {}", omimId.getValue());
            }

            // Add some age to the phenopacket by adding a few years to the onset
            if (onset > 0) {
                age = onset + random.nextInt(0, 10 * 365); // add up to 10 years
            }

            // Add random sex except for X-chromosomal recessive inheritance, in which case add male
            List<TermId> modeOfInheritance = disease.modesOfInheritance();
            String xChromosomalRecessiveHPO = "HP:0001419";
            int male = 2;
            if (modeOfInheritance.contains(TermId.of(xChromosomalRecessiveHPO))) {
                sex = male;
            } else {
                sex = random.nextInt(1, 3);
            }

            double sum = disease.annotations().stream().mapToDouble(HpoDiseaseAnnotation::frequency).sum();

            double[] probabilities = disease.annotations().stream()
                    .mapToDouble(pf -> pf.frequency() / sum)
                    .toArray();

            // Add annotations to the phenopacket
            List<HpoDiseaseAnnotation> allAnnotations = (List<HpoDiseaseAnnotation>) disease.annotations();
            ProportionalSamplerWithoutReplacement<HpoDiseaseAnnotation> prs = new ProportionalSamplerWithoutReplacement<>(allAnnotations, probabilities, random);

            if (nTerms > allAnnotations.size()) {
                LOGGER.warn("Requested number of terms ({}) is greater than the number of annotations ({}) for disease {}",
                        nTerms, allAnnotations.size(), omimId.getValue());
                nTerms = allAnnotations.size();
            }

            annotations = prs.sample(nTerms);
        } else {
            LOGGER.error("Could not find OMIM identifier {}", omimId.getValue());
            return Optional.empty();
        }
        List<TermId> hpoTermIds = annotations.stream().map(HpoDiseaseAnnotation::id).toList();
        if (nImpreciseTerms > 0) {
            hpoTermIds = addImprecision(hpoTermIds, nImpreciseTerms);
        }
        if (nNoiseTerms > 0) {
            hpoTermIds = addNoise(hpoTermIds, nNoiseTerms);
        }

        long currentSeconds = System.currentTimeMillis() / 1000;
        Individual subject = Individual.newBuilder()
                .setId(identifier)
                .setDateOfBirth(Timestamp.newBuilder().setSeconds(currentSeconds - (age / 24 / 60 / 60)))
                .setSex(Sex.forNumber(sex))
                .setTaxonomy(OntologyClass.newBuilder()
                        .setId("NCBITaxon:9606")
                        .setLabel("Homo Sapiens Sapiens")
                        .build())
                .build();
        Disease disease;
        if (onset > 0) {
            disease = Disease.newBuilder()
                    .setTerm(OntologyClass.newBuilder()
                            .setId(omimId.getValue())
                            .build())
                    .setOnset(TimeElement.newBuilder()
                            .setTimestamp(Timestamp.newBuilder()
                                    .setSeconds(currentSeconds - onset / 24 / 60 / 60)  // days to seconds
                                    .build())
                            .build())
                    .build();
        } else {
            disease = Disease.newBuilder()
                    .setTerm(OntologyClass.newBuilder()
                            .setId(omimId.getValue())
                            .build())
                    .build();
        }
        List<PhenotypicFeature> phenotypicFeatures = new ArrayList<>();
        for (TermId tid : hpoTermIds) {
            Optional<Term> opt = this.hpoOntology.termForTermId(tid);
            if (opt.isEmpty() ) {
                throw new PhenolRuntimeException("Could not find term " + tid.getValue());
            }
            Term hpoTerm = opt.get();
            OntologyClass type = OntologyClass.newBuilder()
                    .setId(hpoTerm.id().getValue())
                    .setLabel(hpoTerm.getName())
                    .build();


            // TODO: could still add simulated onset and resolution in the future, is present in HpoDiseaseAnnotation
            phenotypicFeatures.add(PhenotypicFeature.newBuilder()
                    .setType(type)
                    .build());
        }
        PhenopacketBuilder builder = PhenopacketBuilder.create(identifier, buildMetaData(currentSeconds))
                .individual(subject) // TODO: @pnrobinson for all other fields it's add... for individual it isn't?
                .addDisease(disease)
                .addPhenotypicFeatures(phenotypicFeatures);
        Phenopacket phenopacket = builder.build();
        return Optional.of(phenopacket); // return the phenopacket unless there is an error
    }


    List<TermId> addImprecision(List<TermId> tidList , int nImpreciseTerms) {
        int c = 0;
        while (c < nImpreciseTerms && c < tidList.size()) {
            TermId termId = tidList.get(c);
            Set<TermId> parents = this.hpoOntology.graph().getParents(termId);
            TermId first = parents.iterator().next();
            tidList.set(c, first);
        }
        return tidList;
    }

    List<TermId> addNoise(List<TermId> tidList , int nNoiseTerms) {
        int N = this.allPhenotypetermIds.size();
        for (int i = 0; i < nNoiseTerms; i++) {
            int i1 = this.random.nextInt(N);
            TermId randomTermId = this.allPhenotypetermIds.get(i1);
            int i2 = this.random.nextInt(tidList.size());
            tidList.set(i2, randomTermId);
        }
        return tidList;
    }

    private MetaData buildMetaData(long currentSeconds) {
        return MetaData.newBuilder()
                .setCreated(Timestamp
                        .newBuilder()
                        .setSeconds(currentSeconds))
                .setCreatedBy("SimulatedHpoDiseaseGenerator")
                .setPhenopacketSchemaVersion("2.0")
                .addResources(Resource.newBuilder()
                        .setId("hp")
                        .setName("Human Phenotype Ontology")
                        .setNamespacePrefix("HP")
                        .setUrl("http://www.human-phenotype-ontology.org")
                        .setIriPrefix("http://purl.obolibrary.org/obo/HP_")
                        .build())
                .addResources(1, Resource.newBuilder()
                        .setId("omim")
                        .setName("Online Mendeian Inheritance in Man")
                        .setNamespacePrefix("OMIM")
                        .setUrl("https://omim.org/")
                        .setIriPrefix("https://omim.org/entry/") // TODO @pnrobinson: is this the correct IRI prefix?
                        .build())
                .addResources(2, Resource.newBuilder()
                        .setId("ncbitaxon")
                        .setName("NCBI Taxonomy")
                        .setNamespacePrefix("NCBITaxon")
                        .setUrl("https://www.ncbi.nlm.nih.gov/taxonomy")
                        .setIriPrefix("https://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id=")
                        .build())
                .build();
    }

}
