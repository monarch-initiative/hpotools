package org.monarchinitiative.hpotools.analysis.hpoadjust;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Optional;

@FunctionalInterface
public interface CohortSource {

    Optional<CohortCounts> lookup(TermId diseaseId, TermId pmid);

    static CohortSource empty() {
        return (diseaseId, pmid) -> Optional.empty();
    }
}
