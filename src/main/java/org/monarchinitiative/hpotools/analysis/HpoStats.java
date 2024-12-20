package org.monarchinitiative.hpotools.analysis;

import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.data.TermSynonym;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class HpoStats {
    private final Ontology ontology;

    private final Ontology oldOntology;
    private final String HP_PREFIX = "HP";

    private final Set<Term> nonObsoleteTerms;

    private final Date cutoffDate;

    private final static String DEFAULT_DATE = "2020-01-01";



    public HpoStats(Ontology ontology, Ontology old_ontology, String dateString) {
        this.ontology = ontology;
        this.oldOntology = old_ontology;
        LocalDate ld = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE);
        this.cutoffDate = Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
        nonObsoleteTerms = new HashSet<>();
        for (TermId tid :ontology.nonObsoleteTermIds()) {
            Optional<Term> opt = ontology.termForTermId(tid);
            if (!opt.isPresent()) {
                throw new PhenolRuntimeException("Could not find term " + tid + " in ontology");
            }
            Term term = opt.get();
            if (!term.id().getPrefix().equals(HP_PREFIX))
                continue;
            nonObsoleteTerms.add(term);
        }
    }

    public HpoStats(Ontology ontology, Ontology old_ontology) {
        this(ontology, old_ontology, DEFAULT_DATE);
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

    private int termsSinceDate(TermId tid) {
        int n_since_date = 0;
        for (TermId t: ontology.graph().getDescendants(tid)) {
            if (! oldOntology.containsTermId(t)) {
                n_since_date++;
            }
        }
        return n_since_date;
    }

    private void termUpdates() {
        TermId phenotypeRoot = TermId.of("HP:0000118");
        Set<TermId> children = ontology.graph().getChildren(phenotypeRoot);
        int other_new = 0;
        for (TermId child: children) {
            int i = termsSinceDate(child);
            if (i>10) {
                Optional<Term> opt = ontology.termForTermId(child);
                if (opt.isPresent()) {
                    Term term = opt.get();
                    System.out.printf("%s: %d new terms.\n", term.getName(), i);
                }
            } else {
                other_new += i;
            }
        }
        System.out.printf("%d Other new terms\n", other_new);
    }

    public void test() {
        TermId phenotypeRoot = TermId.of("HP:0000002");
        Optional<Term> opt = ontology.termForTermId(phenotypeRoot);
        Term term = opt.orElse(null);
        System.out.println(term);


    }



    public void printStats() {
        System.out.printf("HPO version (current): %s\n",ontology.version().get());
        System.out.printf("HPO version (previous): %s\n",oldOntology.version().get());
        int n_non_obsolete = ontology.nonObsoleteTermIdCount();
        int n_with_def = countTermsWithDefinition();
        System.out.printf("Terms with definition: %d/%d (%.1f%%).\n",
                n_with_def, n_non_obsolete, (100.0*n_with_def/n_non_obsolete));
        int nWithSynonym = countTermsWithSynonym();
        System.out.printf("Terms with synonyms: %d/%d (%.1f%%).\n",
                nWithSynonym, n_non_obsolete, (100.0*nWithSynonym/n_non_obsolete));
        termUpdates();

    }


}
