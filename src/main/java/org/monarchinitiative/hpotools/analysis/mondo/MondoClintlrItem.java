package org.monarchinitiative.hpotools.analysis.mondo;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.File;
import java.util.List;

public record MondoClintlrItem(TermId omimId,
                               TermId mondoId,
                                String mondoLabel,
                               TermId narrowId,
                               String narrowLabel,
                               TermId broadId,
                               String   broadLabel,
                                String cohort,
                                File ppktFile) {

    public String getTsvLine() {
        List<String> fields = List.of(omimId.getValue(),
                mondoId.getValue(),
                mondoLabel,
                narrowId.getValue(),
                narrowLabel,
                broadId.getValue(),
                broadLabel,
                cohort,
                ppktFile.getName());
        return String.join("\t", fields);
    }

    public static String header() {
        List<String> fields = List.of("OMIM",
                "MONDO.id",
                "MONDO.label",
                "narrow.id",
                "narrow.label",
                "broad.id",
                "broad.label",
                "cohort",
                "phenopacket.filename");
        return String.join("\t", fields);
    }


}
