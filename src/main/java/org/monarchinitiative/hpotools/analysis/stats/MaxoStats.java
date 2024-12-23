package org.monarchinitiative.hpotools.analysis.stats;

import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

public class MaxoStats extends JsonOntologyStats{

    private final static String MAXO_PREFIX = "MAXO";


    public MaxoStats(Ontology ontology) {
        super(ontology, MAXO_PREFIX);
    }

    @Override
    public void printStats() {
        System.out.printf("MAxO Version: %s\n", version());
        System.out.printf("Non-obsolete terms: %d\n", nonObsoleteTermCount());
        System.out.printf("Terms with definition: %d\n", countTermsWithDefinition());
        System.out.printf("Terms with synonyms: %d\n", countTermsWithSynonym());
        TermId maxoRoot = TermId.of("MAXO:0000001");
        Map<String, Integer> topLevelCounts = topLevelCounts(maxoRoot);
        // Sorting the map by values
        Map<String, Integer> sortedMap = topLevelCounts.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue()) // Sort by value
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, // Merge function (not needed here)
                        LinkedHashMap::new // Maintain insertion order
                ));
        System.out.println("Term counts in MAxO");
        for (Map.Entry<String, Integer> entry : sortedMap.entrySet()) {
            System.out.printf("- %s: %d\n", entry.getKey(), entry.getValue());
        }
    }
}
