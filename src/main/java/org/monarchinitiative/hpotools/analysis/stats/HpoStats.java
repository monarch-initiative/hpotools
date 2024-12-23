package org.monarchinitiative.hpotools.analysis.stats;

import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.File;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class HpoStats extends JsonOntologyStats {

    private final Ontology oldOntology;
    private final static String HP_PREFIX = "HP";


    private final Date cutoffDate;

    private final static String DEFAULT_DATE = "2020-01-01";

    private final HpoaStats hpoaStats;

    public HpoStats(Ontology ontology,
                    Ontology old_ontology,
                    String phenotypeHpoa,
                    String dateString) {
        super(ontology, HP_PREFIX);
        this.oldOntology = old_ontology;
        LocalDate ld = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE);
        this.cutoffDate = Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
        hpoaStats = new HpoaStats(new File(phenotypeHpoa), ontology);
    }

    public HpoStats(Ontology ontology, Ontology old_ontology, String phenotypeHpoa) {
        this(ontology, old_ontology, phenotypeHpoa, DEFAULT_DATE);
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

    public void getTotalTermCounts() {
        int n_total = ontology.nonObsoleteTermIdCount();
        TermId rootTermId = ontology.getRootTermId();
        Set<TermId> children = ontology.graph().getChildren(rootTermId);
        System.out.println("*****************");
        System.out.println("Total terms: " + n_total);
        for (TermId child: children) {
            Set<TermId> terms = new HashSet<>();
            terms.add(child);
            for (var t : ontology.graph().getDescendants(child)) {
                terms.add(t);
            }
            Term term = ontology.termForTermId(child).get();
            System.out.printf("%s (%s): %d total terms\n.",
                    term.getName(), term.id().getValue(), terms.size());
        }

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
        getTotalTermCounts();
        hpoaStats.printStatistics();
    }


    public void showMultipleParentage() {
        for (Term term: nonObsoleteTerms) {
            if (!term.id().getPrefix().equals(HP_PREFIX))
                continue;
            Set<TermId> parents = ontology.graph().getParents(term.id());
            if (parents.size()>1) {
                System.out.printf("%s (%s):\n", term.getName(), term.id().getValue());
                for (TermId parentId: parents) {
                    Optional<Term> opt = ontology.termForTermId(parentId);
                    if (opt.isPresent()) {
                        Term parent = opt.get();
                        System.out.printf("\t%s (%s)\n", parent.getName(), parent.id().getValue());
                    }
                }
            }
        }
    }
}
