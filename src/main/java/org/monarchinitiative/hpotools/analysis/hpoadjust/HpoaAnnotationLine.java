package org.monarchinitiative.hpotools.analysis.hpoadjust;

import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Arrays;
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

    public boolean cites(TermId reference) {
        return references.contains(reference);
    }

    public boolean hasMultipleReferences() {
        return references.size() > 1;
    }

    public boolean isPhenotypeAnnotation() {
        return "P".equals(aspect);
    }

    public Optional<Ratio> frequencyRatio() {
        return Ratio.parse(frequency);
    }

    public HpoaAnnotationLine withCohortSubtracted(TermId pmid, Ratio adjustedFrequency) {
        String[] fields = raw.split("\t", -1);
        fields[REFERENCE_FIELD] = references.stream()
                .filter(reference -> !reference.equals(pmid))
                .map(TermId::getValue)
                .collect(Collectors.joining(";"));
        fields[FREQUENCY_FIELD] = adjustedFrequency.format();
        return of(String.join("\t", fields));
    }
}
