package org.monarchinitiative.hpotools.cmd;
import org.monarchinitiative.hpotools.analysis.stats.ExactMultinomial;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;


@CommandLine.Command(name = "dist",
        mixinStandardHelpOptions = true,
        description = "Output subontology as word file (experimental)")
public class HpoDistCommand extends HPOCommand implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HpoDistCommand.class);


    @Override
    public Integer call() {
        Ontology hpOntology = getHpOntology();
        // Key - ID of a top level term, value: counts of descendents in the HPO
        List<Map.Entry<TermId, Integer>> topLevelHpoTermCounts = getTopLevelTermList(hpOntology);
        Map<TermId, String> topLevelHpoLabels = getLabels(topLevelHpoTermCounts, hpOntology);
        Map<TermId, Integer> observedHpoCounts = observedHpoCounts();
        int [] orderedObservedCounts = getOrderedObservedCounts(topLevelHpoTermCounts, observedHpoCounts);
        double [] expectedProportions = getExpectedProportions(topLevelHpoTermCounts);
        double probability = ExactMultinomial.exactMultinomialTest(orderedObservedCounts, expectedProportions);
        System.out.println("Exact multinomial p value " + probability);
        outputProprotions(topLevelHpoTermCounts, orderedObservedCounts, expectedProportions, topLevelHpoLabels);
        /*
        ChiSquareTest chiSquareTest = new ChiSquareTest();
        double pValue = chiSquareTest.chiSquareTest(expected, observedCounts);
        double chiSquareStatistic = chiSquareTest.chiSquare(expected, observedCounts);
    */

        return 0;
    }

    private void outputProprotions(List<Map.Entry<TermId, Integer>> topLevelHpoTermCounts,
                                   int[] orderedObservedCounts,
                                   double[] expectedProportions,
                                   Map<TermId, String> topLevelHpoLabels)  {
        double [] observedProportions = new double[orderedObservedCounts.length];
        int totalObservedCounts = Arrays.stream(orderedObservedCounts).sum();
        int N = orderedObservedCounts.length;
        for (int i = 0; i < N; i++) {
            observedProportions[i] = (double) orderedObservedCounts[i] / totalObservedCounts;
        }
        String header = String.join("\t", "HPO", "ID","Count", "Observed", "Expected");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("distribution-hpo.txt"))) {
            bw.write(header + "\n");
            for (int i = 0; i < N; i++) {
                TermId termId = topLevelHpoTermCounts.get(i).getKey();
                String label = topLevelHpoLabels.get(termId);
                String observedCount = String.valueOf(orderedObservedCounts[i]);
                String observed = String.format("%.1f%%", 100 * observedProportions[i]);
                String expected = String.format("%.1f%%", 100 * expectedProportions[i]);
                String line = String.join("\t", label,  termId.getValue(), observedCount, observed, expected);
                bw.write(line + "\n");
            }
        } catch ( IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Create an array with observed counts and arrange it in the same order
     * @param topLevelHpoTermCounts List with top-level HPO terms in order
     * @param observedHpoCounts map with counts observed in an experiment
     * @return
     */
    private int[] getOrderedObservedCounts(List<Map.Entry<TermId, Integer>> topLevelHpoTermCounts, Map<TermId, Integer> observedHpoCounts) {
        int[] observed_counts = new int[topLevelHpoTermCounts.size()];
        int n_identified = 0;
        int i=0;
        for (var e: topLevelHpoTermCounts) {
            TermId termId = e.getKey();
            if (observedHpoCounts.containsKey(termId)) {
                observed_counts[i++] = observedHpoCounts.get(termId);
                n_identified++;
            } else {
                observed_counts[i++] = 0;
            }
        }
        if (n_identified != observedHpoCounts.size()) {
            throw new PhenolRuntimeException("Did not find all observed HPO term counts (needs to be checked)");
        }
        return observed_counts;
    }

    private double[] getExpectedProportions(List<Map.Entry<TermId, Integer>> topLevelHpoTermCounts) {
        // The following gets the total number of terms underneath PhenotypicAbnormality
        int total = topLevelHpoTermCounts.stream().map(Map.Entry::getValue).mapToInt(Integer::intValue).sum();
        int N = topLevelHpoTermCounts.size();
        double [] expected = new double[N];
        for (int i = 0; i < N; i++) {
            expected[i] = (double) topLevelHpoTermCounts.get(i).getValue() / total;
        }
        return expected;
    }

    /**
     * Get a list of the top level terms, ordered by total number of children
     * @param hpo
     * @return A list of Map Entries, with key the TermId and value the count
     */
    private List<Map.Entry<TermId, Integer>> getTopLevelTermList(Ontology hpo) {
        Map<TermId, Integer> actualHpoCounts = new HashMap<>();
        Set<TermId> children = hpo.graph().getChildren(TermId.of("HP:0000118"));
        for (TermId termId : children) {
            // get number of descendants for each top level term
            Set<TermId> descendants = hpo.graph().getDescendantSet(termId);
            actualHpoCounts.put(termId, descendants.size() + 1); // +1 to include term itself
        }
        // Sort the map by value in descending order
        List<Map.Entry<TermId, Integer>> sortedEntries = new ArrayList<>(actualHpoCounts.entrySet());
        sortedEntries.sort((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));
        return sortedEntries;
    }

    private Map<TermId, String> getLabels(List<Map.Entry<TermId, Integer>> entries,
                                          Ontology hpo) {
        Map<TermId, String> id2labelMap = new HashMap<>();
        for (var e: entries) {
            Optional<String> opt = hpo.getTermLabel(e.getKey());
            if (opt.isPresent()) {
                id2labelMap.put(e.getKey(), opt.get());
            } else {
                throw new PhenolRuntimeException("Could not get label for " + e.getKey().getValue());
            }
        }
        return id2labelMap;
    }



    private void printPercentages(Map<TermId, Integer> observedHpoCounts) {

    }





    private Map<TermId, Integer> observedHpoCounts() {
        Map<TermId, Integer> hpoCounts = new HashMap<>();
        // Abnormality of the musculoskeletal system
        hpoCounts.put(TermId.of("HP:0033127"), 38);
        // Abnormality of the nervous system
        hpoCounts.put(TermId.of("HP:0000707"), 37);
        //Abnormality of limbs
        hpoCounts.put(TermId.of("HP:0040064"), 19);
        //Abnormality of the cardiovascular system
        hpoCounts.put(TermId.of("HP:0001626"), 11);
        //Neoplasm
        hpoCounts.put(TermId.of("HP:0002664"), 13);
        // Abnormality of head or neck
        hpoCounts.put(TermId.of("HP:0000152"), 8);
        // Abnormality of the eye
        hpoCounts.put(TermId.of("HP:0000478"), 6);
        // Abnormality of the integument
        hpoCounts.put(TermId.of("HP:0001574"), 8);
        // Growth abnormality
        hpoCounts.put(TermId.of("HP:0001507"), 6);
        // Abnormality of blood and blood-forming tissues
        hpoCounts.put(TermId.of("HP:0001871"), 6);
        // Abnormality of the immune system
        hpoCounts.put(TermId.of("HP:0002715"), 5);
        // Abnormality of the digestive system
        hpoCounts.put(TermId.of("HP:0025031"), 3);
        // Abnormality of the ear HP:0000598
        hpoCounts.put(TermId.of("HP:0000598"), 2);
        // Abnormality of metabolism/homeostasis
        hpoCounts.put(TermId.of("HP:0001939"), 2);
        // Abnormality of the endocrine system
        hpoCounts.put(TermId.of("HP:0000818"), 1);
        //Abnormal cellular phenotype
        hpoCounts.put(TermId.of("HP:0025354"), 1);
        // Abnormality of the respiratory system
        hpoCounts.put(TermId.of("HP:0002086"), 1);
        return hpoCounts;
    }


}
