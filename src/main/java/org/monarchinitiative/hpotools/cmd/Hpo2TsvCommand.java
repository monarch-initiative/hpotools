package org.monarchinitiative.hpotools.cmd;

import org.monarchinitiative.hpotools.analysis.tsv.Hpo2Tsv;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.Callable;


@CommandLine.Command(name = "word",
        mixinStandardHelpOptions = true,
        description = "Output subontology as word file (experimental)")
public class Hpo2TsvCommand extends HPOCommand implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(org.monarchinitiative.hpotools.cmd.WordCommand.class);


    /**
     * The command will create tables for terms emanating from this term. Default: Abnormal social behavior HP:0012433
     */
    private static final String DEFAULT_START_TERM = "HP:0012433";

    @CommandLine.Option(names = {"--startterm"})
    private String startTermId = DEFAULT_START_TERM;


    @Override
    public Integer call() {
        Ontology hpOntology = getHpOntology();
        TermId hpoId = TermId.of(startTermId);
        Optional<Term> opt = hpOntology.termForTermId(hpoId);
        if (opt.isEmpty()) {
            System.err.printf("[ERROR] No HPO term found for %s.\n", startTermId);
        }
        if (opt.isEmpty()) {
            LOGGER.error("[ERROR] No term found for {}.", startTermId);
            return 1;
        }
        Term targetTerm = opt.get();

        String name = targetTerm.getName().replaceAll(" ", "_");
        String id = targetTerm.id().getValue().replaceAll(":", "_");
        String outfilename = String.format("%s_%s.tsv", name, id);
        Hpo2Tsv tsv = new Hpo2Tsv(new File(outfilename), hpOntology, targetTerm.id());
        tsv.createTsvFile();
        return 0;
    }
}
