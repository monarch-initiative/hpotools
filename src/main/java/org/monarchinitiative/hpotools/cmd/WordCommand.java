package org.monarchinitiative.hpotools.cmd;


import java.util.Optional;
import java.util.concurrent.Callable;

import org.monarchinitiative.hpotools.analysis.word.Hpo2Word;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/**
 * A command class to coordinate the production and output of an RTF file containing information about
 * a subhierarchy of the HPO.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 0.1.0
 */
@CommandLine.Command(name = "word",
        mixinStandardHelpOptions = true,
        description = "Output subontology as word file (experimental)")
public class WordCommand extends HPOCommand implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(WordCommand.class);

    private final String DEFAULT_OUTPUTNAME="hpotest.doc";
    /** The command will create tables for terms emanating from this term. Default: Abnormal social behavior HP:0012433 */
    private static String DEFAULT_START_TERM="HP:0012433";

    @CommandLine.Option(names={"--startterm"})
    private String startTermId =DEFAULT_START_TERM;

    @CommandLine.Option(names={"-o", "--out"})
    private String outfilename = null;




    public WordCommand() {
    }


    @Override
    public Integer call() {
        Ontology hpOntology = getHpOntology();
        TermId hpoId = TermId.of(startTermId);
        Optional<Term> opt = hpOntology.termForTermId(hpoId);
        if (opt.isEmpty()) {
            System.err.printf("[ERROR] No HPO term found for %s.\n", startTermId);
        }
        Term targetTerm = opt.get();
        if (outfilename == null) {
            String name = targetTerm.getName().replaceAll(" ", "_");
            String id = targetTerm.id().getValue().replaceAll(" ", "_");
            outfilename = String.format("%s_%s.docx", name, id);
        }
        LOGGER.info("running Word command from {}", startTermId, getHpoJsonFile().getAbsolutePath());
        try {
           Hpo2Word hpo2Word = new Hpo2Word(outfilename, targetTerm, hpOntology);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return 1;
        }
        return 0;
    }

}
