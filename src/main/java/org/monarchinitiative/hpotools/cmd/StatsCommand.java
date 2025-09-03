package org.monarchinitiative.hpotools.cmd;

import org.monarchinitiative.hpotools.analysis.stats.HpoStats;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.util.Set;
import java.util.concurrent.Callable;


    @CommandLine.Command(name = "stats",
            mixinStandardHelpOptions = true,
            description = "Calculate statistics for HPO attributes")
    public class StatsCommand extends HPOCommand implements Callable<Integer> {
        private static final Logger LOGGER = LoggerFactory.getLogger(StatsCommand.class);
        /** Terms such as Polydactyly that have a certain assignment to an age of onset (Congenital is taken
         * here to comprise also antenatal). The map is derived from the file {@code term2onset.txt} in the
         * resources section.
         */
        private Set<TermId> termIdToCongenitalOnsetSet;

        @CommandLine.Option(names = {"--previous"}, required = true)
        private String previousHpoVersion;

        @CommandLine.Option(names = {"--mp"}, required = false)
        private boolean showMultipleParentage = false;

        @Override
        public Integer call() {
            Ontology ontology = OntologyLoader.loadOntology(new File(hpopath));
            Ontology old_ontology = OntologyLoader.loadOntology(new File(previousHpoVersion));
            HpoStats stats = new HpoStats(ontology, old_ontology, annotpath);
            if (showMultipleParentage) {
                stats.showMultipleParentage();
                return 0;
            }
            stats.printStats();
            return 0;
        }
    }
