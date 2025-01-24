package org.monarchinitiative.hpotools.cmd;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.monarchinitiative.hpotools.analysis.stats.ExactMultinomial;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.*;
import java.util.concurrent.Callable;


@CommandLine.Command(name = "dist",
        mixinStandardHelpOptions = true,
        description = "Output subontology as word file (experimental)")
public class HpoDistCommand extends HPOCommand implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HpoDistCommand.class);


    @Override
    public Integer call() {
        final int PSEUDOCOUNT = 5;
        Map<TermId, Integer> actualHpoCounts = new HashMap<>();
        Ontology hpOntology = getHpOntology();
        Set<TermId> children = hpOntology.graph().getChildren(TermId.of("HP:0000118"));
        for (TermId termId : children) {
            // get number of descendants for each top level term
            Set<TermId> descendants = hpOntology.graph().getDescendantSet(termId);
            actualHpoCounts.put(termId, descendants.size() + 1); // +1 to include term itself
        }
        Map<TermId, Integer> observedHpoCounts = observedHpoCounts();
        int total = observedHpoCounts.values().stream().mapToInt(Integer::intValue).sum();
        List<TermId> topLevelTermList = new ArrayList<>(actualHpoCounts.keySet());
        int n = topLevelTermList.size();
        double [] expected = new double[n];
        long [] observed = new long[n];
        int i = 0;
        for (TermId termId : topLevelTermList) {
            double proportion = (PSEUDOCOUNT +(double)observedHpoCounts.getOrDefault(termId, 0)) / actualHpoCounts.get(termId);
            double expectedProportion  = proportion * total;
            expected[i] = expectedProportion;
            observed[i] = PSEUDOCOUNT + observedHpoCounts.getOrDefault(termId, 0);
            System.out.println(expected[i] + "e  o" + observed[i]);
            i++;
        }
        ChiSquareTest chiSquareTest = new ChiSquareTest();
        double pValue = chiSquareTest.chiSquareTest(expected, observed);
        double chiSquareStatistic = chiSquareTest.chiSquare(expected, observed);

        // Output the results
        System.out.println("Chi-Square Statistic: " + chiSquareStatistic);
        System.out.println("P-Value: " + pValue);
        // Exact multinomal
        double probability = ExactMultinomial.exactMultinomialTest(observed, expected);
        System.out.println("Exact multinomial p value " + probability);

        i = 0;
        for (TermId termId : topLevelTermList) {
            int obs = actualHpoCounts.getOrDefault(termId, 0);
            double exp = PSEUDOCOUNT +(double)observedHpoCounts.getOrDefault(termId, 0);
            double percObserved = 100.0 * obs / total;
            double percExpected = 100.0 * exp / total;
            Optional<Term> opt = hpOntology.termForTermId(termId);
            if (opt.isPresent()) {
                Term term = opt.get();
                String label = term.getName();
                System.out.printf("%s (%s) observed: %.1f; expected: %.1f%n",
                        label, termId.getValue(), percObserved, percExpected);
            }
            i++;
        }

        return 0;
    }



    private Map<TermId, Integer> observedHpoCounts() {
        Map<TermId, Integer> hpoCounts = new HashMap<>();
        // Abnormality of the nervous system
        hpoCounts.put(TermId.of("HP:0000707"), 39);
        // Abnormality of the musculoskeletal system
        hpoCounts.put(TermId.of("HP:0033127"), 31);
        //Abnormality of limbs
        hpoCounts.put(TermId.of("HP:0040064"), 14);
        //Abnormality of the cardiovascular system
        hpoCounts.put(TermId.of("HP:0001626"), 11);
        //Neoplasm
        hpoCounts.put(TermId.of("HP:0002664"), 11);
        // Abnormality of head or neck
        hpoCounts.put(TermId.of("HP:0000152"), 10);
        // Abnormality of the eye
        hpoCounts.put(TermId.of("HP:0000478"), 7);
        // Abnormality of the integument
        hpoCounts.put(TermId.of("HP:0001574"), 5);
        // Growth abnormality
        hpoCounts.put(TermId.of("HP:0001507"), 5);
        // Abnormality of the immune system
        hpoCounts.put(TermId.of("HP:0002715"), 5);
        // Abnormality of blood and blood-forming tissues
        hpoCounts.put(TermId.of("HP:0001871"), 5);
        // Abnormality of the digestive system
        hpoCounts.put(TermId.of("HP:0025031"), 2);
        // Abnormality of the respiratory system
        hpoCounts.put(TermId.of("HP:0002086"), 2);
        // Abnormality of metabolism/homeostasis
        hpoCounts.put(TermId.of("HP:0001939"), 2);
        // Abnormality of the endocrine system
        hpoCounts.put(TermId.of("HP:0000818"), 1);
        //Abnormal cellular phenotype
        hpoCounts.put(TermId.of("HP:0025354"), 1);
        //  Abnormality of the genitourinary system
        hpoCounts.put(TermId.of("HP:0000119"), 1);
        return hpoCounts;
    }


}
