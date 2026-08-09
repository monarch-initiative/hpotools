package org.monarchinitiative.hpotools.analysis.hpoadjust;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;

public record DiseaseAugmentation(TermId diseaseId,
                                  int cohortSize,
                                  List<TermId> cohortPmids,
                                  int linesAdded,
                                  int linesPooled,
                                  int linesSuperseded) {
}
