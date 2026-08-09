package org.monarchinitiative.hpotools.analysis.hpoadjust;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public record CohortCounts(Map<TermId, Integer> countsByTerm, int cohortSize) {

    public static CohortCounts of(List<Set<TermId>> patientTermSets, Function<TermId, Set<TermId>> ancestorsWithSelf) {
        Map<TermId, Integer> counts = new HashMap<>();
        for (Set<TermId> observedTerms : patientTermSets) {
            observedTerms.stream()
                    .flatMap(term -> ancestorsWithSelf.apply(term).stream())
                    .distinct()
                    .forEach(term -> counts.merge(term, 1, Integer::sum));
        }
        return new CohortCounts(Map.copyOf(counts), patientTermSets.size());
    }

    public int countOf(TermId termId) {
        return countsByTerm.getOrDefault(termId, 0);
    }
}
