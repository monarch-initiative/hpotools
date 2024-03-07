package org.monarchinitiative.hpotools.cmd;


import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

import org.monarchinitiative.hpotools.analysis.word.Hpo2Word;
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

    private final String DEFAULT_OUTPUTNAME="hpotest.word";
    private static String DEFAULT_START_TERM="HP:0002715";

    @CommandLine.Option(names={"--startterm"})
    private String startTerm=DEFAULT_START_TERM;

    @CommandLine.Option(names={"-o", "--out"})
    private String outfilename=DEFAULT_OUTPUTNAME;




    public WordCommand() {
    }


    @Override
    public Integer call() {
        File hpoJsonFile = getHpoJsonFile();
        LOGGER.info("running Word command from {}", startTerm, hpoJsonFile.getAbsolutePath());
        System.exit(1);
        try {
           // Hpo2Word hpo2Word = new Hpo2Word(outfilename, startTerm);
            File f = new File("sdf");
        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        }
        return 0;
    }

}
