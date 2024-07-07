package org.monarchinitiative.hpotools.analysis;

import org.monarchinitiative.phenol.ontology.data.TermId;

public record OntologyTerm(
        TermId id,
        String label) {
}
