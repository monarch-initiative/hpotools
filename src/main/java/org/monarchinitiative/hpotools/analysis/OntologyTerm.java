package org.monarchinitiative.hpotools.analysis;

import org.monarchinitiative.phenol.ontology.data.TermId;

/**
 * POJO for dealing with HPO and MONDO terms
 * @param id
 * @param label
 */
public record OntologyTerm(
        TermId id,
        String label) {
}
