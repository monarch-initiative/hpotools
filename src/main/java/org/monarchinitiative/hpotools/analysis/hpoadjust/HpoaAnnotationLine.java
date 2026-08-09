package org.monarchinitiative.hpotools.analysis.hpoadjust;

import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public record HpoaAnnotationLine(String raw,
                                 TermId diseaseId,
                                 String diseaseName,
                                 TermId hpoId,
                                 List<TermId> references,
                                 String frequency,
                                 String aspect) {

    public static final String PHENOTYPE_ASPECT = "P";

    private static final int EXPECTED_FIELD_COUNT = 12;
    private static final int REFERENCE_FIELD = 4;
    private static final int FREQUENCY_FIELD = 7;

    public static HpoaAnnotationLine of(String line) {
        String[] fields = line.split("\t", -1);
        if (fields.length != EXPECTED_FIELD_COUNT) {
            throw new PhenolRuntimeException(String.format("Expected %d fields but got %d: %s",
                    EXPECTED_FIELD_COUNT, fields.length, line));
        }
        List<TermId> references = fields[REFERENCE_FIELD].isEmpty()
                ? List.of()
                : Arrays.stream(fields[REFERENCE_FIELD].split(";")).map(TermId::of).toList();
        return new HpoaAnnotationLine(line, TermId.of(fields[0]), fields[1], TermId.of(fields[3]),
                references, fields[FREQUENCY_FIELD], fields[10]);
    }

    public static HpoaAnnotationLine phenotypeAnnotation(TermId diseaseId,
                                                        String diseaseName,
                                                        TermId hpoId,
                                                        Collection<TermId> references,
                                                        Ratio frequency,
                                                        String biocuration) {
        String[] fields = new String[EXPECTED_FIELD_COUNT];
        Arrays.fill(fields, "");
        fields[0] = diseaseId.getValue();
        fields[1] = diseaseName;
        fields[3] = hpoId.getValue();
        fields[REFERENCE_FIELD] = joinReferences(references);
        fields[5] = "PCS";
        fields[FREQUENCY_FIELD] = frequency.format();
        fields[10] = PHENOTYPE_ASPECT;
        fields[11] = biocuration;
        return of(String.join("\t", fields));
    }

    public boolean cites(TermId reference) {
        return references.contains(reference);
    }

    public boolean hasMultipleReferences() {
        return references.size() > 1;
    }

    public boolean isPhenotypeAnnotation() {
        return PHENOTYPE_ASPECT.equals(aspect);
    }

    public Optional<Ratio> frequencyRatio() {
        return Ratio.parse(frequency);
    }

    public HpoaAnnotationLine with(Collection<TermId> newReferences, Ratio newFrequency) {
        String[] fields = raw.split("\t", -1);
        fields[REFERENCE_FIELD] = joinReferences(newReferences);
        fields[FREQUENCY_FIELD] = newFrequency.format();
        return of(String.join("\t", fields));
    }

    public HpoaAnnotationLine withCohortSubtracted(TermId pmid, Ratio adjustedFrequency) {
        List<TermId> remaining = references.stream()
                .filter(reference -> !reference.equals(pmid))
                .toList();
        return with(remaining, adjustedFrequency);
    }

    private static String joinReferences(Collection<TermId> references) {
        return references.stream()
                .map(TermId::getValue)
                .collect(Collectors.joining(";"));
    }
}
