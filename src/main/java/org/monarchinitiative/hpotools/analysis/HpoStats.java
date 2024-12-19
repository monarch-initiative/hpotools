package org.monarchinitiative.hpotools.analysis;

import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.data.TermSynonym;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class HpoStats {
    private final Ontology ontology;

    private final String HP_PREFIX = "HP";

    private final Set<Term> nonObsoleteTerms;

    public HpoStats(Ontology ontology) {
        this.ontology = ontology;
        nonObsoleteTerms = new HashSet<>();
        for (TermId tid :ontology.nonObsoleteTermIds()) {
            Optional<Term> opt = ontology.termForTermId(tid);
            if (!opt.isPresent()) {
                throw new PhenolRuntimeException("Could not find term " + tid + " in ontology");
            }
            nonObsoleteTerms.add(opt.get());
        }
    }

    private int countTermsWithDefinition() {
        int n_with_def = 0;
        for (Term term: nonObsoleteTerms) {
            if (!term.id().getPrefix().equals(HP_PREFIX))
                continue;
            String def = term.getDefinition();
            if (!def.isEmpty()) n_with_def++;
        }
        return n_with_def;
    }

    private int countTermsWithSynonym() {
        int n_with_synonym = 0;
        for (Term term: nonObsoleteTerms) {
            if (!term.id().getPrefix().equals(HP_PREFIX))
                continue;
            List<TermSynonym> synonyms = term.getSynonyms();
            if (!synonyms.isEmpty()) n_with_synonym++;
        }
        return n_with_synonym;

    }

    public void printStats() {
        System.out.println(ontology);
        int n_non_obsolete = ontology.nonObsoleteTermIdCount();
        int n_with_def = countTermsWithDefinition();
        System.out.printf("Terms with definition: %d/%d (%.1f%%).\n",
                n_with_def, n_non_obsolete, (100.0*n_with_def/n_non_obsolete));
        int nWithSynonym = countTermsWithSynonym();
        System.out.printf("Terms with synonyms: %d/%d (%.1f%%).\n",
                nWithSynonym, n_non_obsolete, (100.0*nWithSynonym/n_non_obsolete));

    }


}
