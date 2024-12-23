package org.monarchinitiative.hpotools.analysis.stats;

import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.data.TermSynonym;

import java.util.*;

public abstract class JsonOntologyStats {

    protected final Ontology ontology;
    protected final Set<Term> nonObsoleteTerms;
    private final String ONTOLOGY_PREFIX;


    public JsonOntologyStats(Ontology ontology, String ontologyPrefix) {
        this.ontology = ontology;
        this.ONTOLOGY_PREFIX = ontologyPrefix;
        nonObsoleteTerms = new HashSet<>();
        for (TermId tid :ontology.nonObsoleteTermIds()) {
            Optional<Term> opt = ontology.termForTermId(tid);
            if (!opt.isPresent()) {
                throw new PhenolRuntimeException("Could not find term " + tid + " in ontology");
            }
            Term term = opt.get();
            if (!term.id().getPrefix().equals(ontologyPrefix))
                continue;
            nonObsoleteTerms.add(term);
        }
        System.out.printf("Extracted %d non-obsolete %s terms\n",
                nonObsoleteTerms.size(),
                ONTOLOGY_PREFIX);
    }

    protected int countTermsWithDefinition() {
        int n_with_def = 0;
        for (Term term: nonObsoleteTerms) {
            String def = term.getDefinition();
            if (!def.isEmpty()) n_with_def++;
        }
        return n_with_def;
    }

    protected int countTermsWithSynonym() {
        int n_with_synonym = 0;
        for (Term term: nonObsoleteTerms) {
            List<TermSynonym> synonyms = term.getSynonyms();
            if (!synonyms.isEmpty()) n_with_synonym++;
        }
        return n_with_synonym;
    }

    protected String version() {
        return ontology.version().get();
    }

    protected int nonObsoleteTermCount() {
        return nonObsoleteTerms.size();
    }

    protected Map<String, Integer> topLevelCounts(TermId rootId) {
        Map<String, Integer> topLevelCounts = new HashMap<>();
        Set<TermId> firstLevelChildren = ontology.graph().getChildren(rootId);
        for (TermId childId: firstLevelChildren ) {
            if (! childId.getPrefix().equals(ONTOLOGY_PREFIX)) {
                continue;
            }
            Optional<Term> opt = ontology.termForTermId(childId);
            if (!opt.isPresent()) {
                throw new PhenolRuntimeException("Could not find term " + childId + " in ontology");
            } else {
                Term childTerm = opt.get();
                String label = String.format("%s (%s)",childTerm.getName(), childId.getValue());
                Set<TermId> descSet = new HashSet<>();
                for (var tid: ontology.graph().extendWithDescendants(childId, true)) {
                    if (tid.getPrefix().equals(ONTOLOGY_PREFIX)) {
                        descSet.add(tid);
                    }
                }
                int size = descSet.size();
                topLevelCounts.put(label, size);
            }
        }
        return topLevelCounts;
    }



    abstract public void printStats();

}
