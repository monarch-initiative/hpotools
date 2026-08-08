package org.monarchinitiative.hpotools.analysis.hpoadjust;

import com.google.protobuf.util.JsonFormat;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.phenopackets.schema.v2.Phenopacket;
import org.phenopackets.schema.v2.core.ExternalReference;
import org.phenopackets.schema.v2.core.Interpretation;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public record PhenopacketCase(String phenopacketId, TermId diseaseId, TermId pmid, Path source) {

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
        TermId diseaseId = extractDiseaseId(phenopacket)
                .orElseThrow(() -> new PhenolRuntimeException("No disease diagnosis in " + path));
        return new PhenopacketCase(phenopacket.getId(), diseaseId, pmid, path);
    }

    static Optional<TermId> extractPmid(Phenopacket phenopacket) {
        return phenopacket.getMetaData().getExternalReferencesList().stream()
                .map(ExternalReference::getId)
                .filter(id -> id.startsWith("PMID:"))
                .findFirst()
                .map(TermId::of);
    }

    static Optional<TermId> extractDiseaseId(Phenopacket phenopacket) {
        Optional<String> fromInterpretation = phenopacket.getInterpretationsList().stream()
                .map(Interpretation::getDiagnosis)
                .map(diagnosis -> diagnosis.getDisease().getId())
                .filter(id -> !id.isEmpty())
                .findFirst();
        if (fromInterpretation.isPresent()) {
            return fromInterpretation.map(TermId::of);
        }
        return phenopacket.getDiseasesList().stream()
                .filter(disease -> !disease.getExcluded())
                .map(disease -> disease.getTerm().getId())
                .filter(id -> !id.isEmpty())
                .findFirst()
                .map(TermId::of);
    }
}
