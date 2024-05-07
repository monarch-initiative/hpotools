package org.monarchinitiative.hpotools.cmd;

import org.monarchinitiative.hpotools.analysis.EncodingCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.util.concurrent.Callable;

/**
 * A  class to help find text with non-standard encodings in hp-edit.owl (this leads to problems with the
 * HPO QC pipeline)
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 0.1.0
 */
@CommandLine.Command(name = "encoding",
        mixinStandardHelpOptions = true,
        description = "Find bad encodings in hp-edit.owl")
public class EncodingCommand  implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(WordCommand.class);

    @CommandLine.Option(names={"--owl"}, required = true, description = "path to hp.owl")
    private File hpoOwlFile;


    @Override
    public Integer call() throws Exception {
        if (! hpoOwlFile.isFile()) {
            System.err.printf("[ERROR] could not find hp-edit.owl file at %s\n.", hpoOwlFile.getAbsolutePath());
        }
        EncodingCheck check = new EncodingCheck(hpoOwlFile);
        check.checkEncoding();
        return 0;
    }
}
