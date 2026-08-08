package org.monarchinitiative.hpotools.analysis.hpoadjust;

import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Arrays;
import java.util.List;

public record HpoaAnnotationLine(String raw,
                                 TermId diseaseId,
                                 String diseaseName,
                                 TermId hpoId,
                                 List<TermId> references,
                                 String aspect) {

    private static final int EXPECTED_FIELD_COUNT = 12;

    public static HpoaAnnotationLine of(String line) {
        String[] fields = line.split("\t", -1);
        if (fields.length != EXPECTED_FIELD_COUNT) {
            throw new PhenolRuntimeException(String.format("Expected %d fields but got %d: %s",
                    EXPECTED_FIELD_COUNT, fields.length, line));
        }
        List<TermId> references = fields[4].isEmpty()
                ? List.of()
                : Arrays.stream(fields[4].split(";")).map(TermId::of).toList();
        return new HpoaAnnotationLine(line, TermId.of(fields[0]), fields[1], TermId.of(fields[3]), references, fields[10]);
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
}
