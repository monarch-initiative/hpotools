package org.monarchinitiative.hpotools.analysis.hpoadjust;

import com.google.protobuf.util.JsonFormat;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.phenopackets.schema.v2.Phenopacket;
import org.phenopackets.schema.v2.core.Disease;
import org.phenopackets.schema.v2.core.ExternalReference;
import org.phenopackets.schema.v2.core.Interpretation;
import org.phenopackets.schema.v2.core.OntologyClass;
import org.phenopackets.schema.v2.core.PhenotypicFeature;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public record PhenopacketCase(String phenopacketId,
                              TermId diseaseId,
                              String diseaseName,
                              TermId pmid,
                              Set<TermId> observedTerms,
                              Path source) {

    public static PhenopacketCase fromFile(Path path) {
        Phenopacket.Builder builder = Phenopacket.newBuilder();
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            JsonFormat.parser().ignoringUnknownFields().merge(reader, builder);
        } catch (IOException e) {
            throw new PhenolRuntimeException("Could not parse phenopacket at " + path + ": " + e.getMessage());
        }
        Phenopacket phenopacket = builder.build();
        TermId pmid = extractPmid(phenopacket)
                .orElseThrow(() -> new PhenolRuntimeException("No PMID external reference in " + path));
        OntologyClass disease = extractDisease(phenopacket)
                .orElseThrow(() -> new PhenolRuntimeException("No disease diagnosis in " + path));
        Set<TermId> observed = phenopacket.getPhenotypicFeaturesList().stream()
                .filter(feature -> !feature.getExcluded())
                .map(PhenotypicFeature::getType)
                .map(type -> TermId.of(type.getId()))
                .collect(Collectors.toSet());
        return new PhenopacketCase(phenopacket.getId(), TermId.of(disease.getId()), disease.getLabel(),
                pmid, observed, path);
    }

    private static Optional<TermId> extractPmid(Phenopacket phenopacket) {
        return phenopacket.getMetaData().getExternalReferencesList().stream()
                .map(ExternalReference::getId)
                .filter(id -> id.startsWith("PMID:"))
                .findFirst()
                .map(TermId::of);
    }

    private static Optional<OntologyClass> extractDisease(Phenopacket phenopacket) {
        Optional<OntologyClass> fromInterpretation = phenopacket.getInterpretationsList().stream()
                .map(Interpretation::getDiagnosis)
                .map(diagnosis -> diagnosis.getDisease())
                .filter(disease -> !disease.getId().isEmpty())
                .findFirst();
        if (fromInterpretation.isPresent()) {
            return fromInterpretation;
        }
        return phenopacket.getDiseasesList().stream()
                .filter(disease -> !disease.getExcluded())
                .map(Disease::getTerm)
                .filter(term -> !term.getId().isEmpty())
                .findFirst();
    }
}
