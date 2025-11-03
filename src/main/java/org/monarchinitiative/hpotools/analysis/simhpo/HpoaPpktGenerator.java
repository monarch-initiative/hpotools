package org.monarchinitiative.hpotools.analysis.simhpo;

import com.google.protobuf.Timestamp;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.boqa.core.DiseaseData;
import org.p2gx.boqa.core.diseases.DiseaseDataPhenolIngest;
import org.phenopackets.phenopackettools.builder.PhenopacketBuilder;
import org.phenopackets.schema.v2.Phenopacket;
import org.phenopackets.schema.v2.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class HpoaPpktGenerator {
    private final static Logger LOGGER = LoggerFactory.getLogger(SimulatedHpoDiseaseGenerator.class);
    private DiseaseData diseaseData;
    private Ontology hpo;

    public HpoaPpktGenerator(Path annotpath, Path ontopath) {
        try {
            this.diseaseData = DiseaseDataPhenolIngest.fromPaths(annotpath, ontopath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.hpo = OntologyLoader.loadOntology(ontopath.toFile());
    }

    public int getNumOfDiseases(){
        return this.diseaseData.getDiseaseIds().size();
    }

    public Set<String> getDiseaseIds() {
        return this.diseaseData.getDiseaseIds();
    }

    public Optional<Phenopacket> generatePhenopacketForHpoaDisease(String diseaseId, String ppktId) {
        long age = 0;
        int sex = 0;
        long onset = 0;
        Set<String> annotations;
        if(this.diseaseData.getDiseaseIds().contains(diseaseId)) {

            // Get terms observed for this disease
            annotations = this.diseaseData.getObservedDiseaseFeatures(diseaseId);
        } else {
            LOGGER.error("Could not find disease identifier {}", diseaseId);
            return Optional.empty();
        }

        // Create individual
        long currentSeconds = System.currentTimeMillis() / 1000;
        Individual subject = Individual.newBuilder()
                .setId(ppktId)
                .setDateOfBirth(Timestamp.newBuilder().setSeconds(currentSeconds - (age / 24 / 60 / 60)))
                .setSex(Sex.forNumber(sex))
                .setTaxonomy(OntologyClass.newBuilder()
                        .setId("NCBITaxon:9606")
                        .setLabel("Homo Sapiens Sapiens")
                        .build())
                .build();

        // Create disease
        Disease disease;
        if (onset > 0) {
            disease = Disease.newBuilder()
                    .setTerm(OntologyClass.newBuilder()
                            .setId(diseaseId)
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
                            .setId(diseaseId)
                            .build())
                    .build();
        }

        // Get list of phenotypic features
        List<PhenotypicFeature> phenotypicFeatures = new ArrayList<>();
        for (String tid : annotations) {
            Optional<Term> opt = this.hpo.termForTermId(TermId.of(tid));
            //this.hpo.getAncestorTermIds(TermId.of("HP:0000118")).contains(opt);
            if (opt.isEmpty()) {
                throw new PhenolRuntimeException("Could not find term " + tid);
            }
            if (!this.hpo.getAncestorTermIds(opt.get().id()).contains(TermId.of("HP:0000118"))) {
                //System.out.println(diseaseId);
                System.out.println(opt.get().id());
                continue;
            }
            Term hpoTerm = opt.get();
            OntologyClass type = OntologyClass.newBuilder()
                    .setId(hpoTerm.id().getValue())
                    .setLabel(hpoTerm.getName())
                    .build();

            phenotypicFeatures.add(PhenotypicFeature.newBuilder()
                    .setType(type)
                    .build());
        }

        // Create phenopacket from individual, disease and list of features
        PhenopacketBuilder builder = PhenopacketBuilder.create(ppktId, buildMetaData(currentSeconds))
                .individual(subject)
                .addDisease(disease)
                .addPhenotypicFeatures(phenotypicFeatures);
        Phenopacket phenopacket = builder.build();

        return Optional.of(phenopacket);
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
