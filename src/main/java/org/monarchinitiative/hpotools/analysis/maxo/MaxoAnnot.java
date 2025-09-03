package org.monarchinitiative.hpotools.analysis.maxo;


import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;

/**
 * disease_id
 * disease_name
 * citation
 * maxo_id
 * maxo_label
 * hpo_id
 * maxo_relation
 * evidence_code
 * extension_id
 * extension_label
 * attribute
 * creator
 * last_update
 * created_on
 */
public class MaxoAnnot {
    private final TermId diseaseId;
    private final String diseaseLabel;
    private final String citation;
    private final TermId maxoId;
    private final String maxoLabel;
    private final TermId hpoId;
    private final String maxoRelation;
    private final String evidenceCode;
    private final TermId extensionId;
    private final String extensionLabel;
    private final String attribute;
    private final String creator;
    private final String last_update;
    private final String created_on;


    private MaxoAnnot(TermId diseaseId,
                      String diseaseLabel,
                      String citation,
                      TermId maxoId,
                      String maxoLabel,
                      TermId hpoId,
                      String maxoRelation, String evidenceCode, TermId extensionId, String extensionLabel, String attribute, String creator, String lastUpdate, String createdOn) {
        this.diseaseId = diseaseId;
        this.diseaseLabel = diseaseLabel;
        this.citation = citation;
        this.maxoId = maxoId;
        this.maxoLabel = maxoLabel;
        this.hpoId = hpoId;
        this.maxoRelation = maxoRelation;
        this.evidenceCode = evidenceCode;
        this.extensionId = extensionId;
        this.extensionLabel = extensionLabel;
        this.attribute = attribute;
        this.creator = creator;
        this.last_update = lastUpdate;
        this.created_on = createdOn;
    }


    public static MaxoAnnot fromLine(String line) {
        String[] tokens = line.split("\t");
        if (tokens.length != 14) {
            throw new PhenolRuntimeException("Malformed MaxoAnnot: " + line);
        }
        TermId diseaseId = TermId.of(tokens[0]);
        String label = tokens[1];
        String citation = tokens[2];
        TermId maxoId = TermId.of(tokens[3]);
        String maxoLabel = tokens[4];
        TermId hpooId = TermId.of(tokens[5]);
        String maxoRelation = tokens[6];
        String evidenceCode = tokens[7];
        TermId extensionId = null;
        String extensionLabel = null;
        if (tokens[8] != null && tokens[8].length() > 3) {
            extensionId = TermId.of(tokens[8]);
            extensionLabel = tokens[9];
        }
        String attribute = tokens[10];
        String creator = tokens[11];
        String lastUpdate = tokens[12];
        String createdOn = tokens[13];

        return new MaxoAnnot(diseaseId,
                label,
                citation,
                maxoId,
                maxoLabel,
                hpooId,
                maxoRelation,
                evidenceCode,
                extensionId,
                extensionLabel,
                attribute,
                creator,
                lastUpdate,
                createdOn);
    }

    public TermId diseaseId() {
        return diseaseId;
    }

    public String diseaseLabel() {
        return diseaseLabel;
    }

    public String citation() {
        return citation;
    }

    public TermId maxoId() {
        return maxoId;
    }

    public String maxoLabel() {
        return maxoLabel;
    }

    public TermId hpoId() {
        return hpoId;
    }

    public String maxoRelation() {
        return   maxoRelation;
    }


    public String evidenceCode() {
        return evidenceCode;
    }

    public TermId extensionId() {
        return extensionId;
    }

    public String extensionLabel() {
        return extensionLabel;
    }

    public String attribute() {
        return attribute;
    }

    public String creator() {
        return creator;
    }
    public String lastUpdate() {
        return last_update;
    }
    public String createdOn() {
        return created_on;
    }

}
