package org.monarchinitiative.hpotools.analysis.maxo;

import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;

/**
 * hpo_id	hpo_label	predicate_id	maxo_id	maxo_label	creator_id
 */
public class MaxoDxAnnot {

    private final TermId hpo_id;
    private final String hpo_label;
    private final String predicate;
    private final TermId maxo_id;
    private final String maxoLabel;
    private final String creator_id;

    public MaxoDxAnnot(TermId hpoId,
                       String hpoLabel,
                       String predicateId,
                       TermId maxoId,
                       String maxoLabel,
                       String creatorId) {
        this.hpo_id = hpoId;
        this.hpo_label = hpoLabel;
        this.predicate = predicateId;
        this.maxo_id = maxoId;
        this.maxoLabel = maxoLabel;
        this.creator_id = creatorId;
    }


    public static MaxoDxAnnot fromLine(String line) {
        String[] fields = line.split("\t");
        if (fields.length != 6) {
            String e = String.format("Invalid format with %d fields (expected 5): %s",
                    fields.length, line);
            throw new PhenolRuntimeException(e);
        }
        TermId hpoId = TermId.of(fields[0]);
        String hpoLabel = fields[1];
        String predicate = fields[2];
        TermId maxoId = TermId.of(fields[3]);
        String maxoLabel = fields[4];
        String creatorId = fields[5];
        return new MaxoDxAnnot(hpoId,
                hpoLabel,
                predicate,
                maxoId,
                maxoLabel,
                creatorId);
    }

    public TermId hpoId() {
        return hpo_id;
    }

    public String hpoLabel() {
        return hpo_label;
    }

    public String predicate() {
        return predicate;
    }

    public TermId maxoId() {
        return maxo_id;
    }

    public String maxoLabel() {
        return maxoLabel;
    }

    public String creatorId() {
        return creator_id;
    }
}
