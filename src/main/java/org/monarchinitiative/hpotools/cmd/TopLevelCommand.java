package org.monarchinitiative.hpotools.cmd;


import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "onset",
        mixinStandardHelpOptions = true,
        description = "Calculate number of diseases with onset data")
public class TopLevelCommand extends HPOCommand implements Callable<Integer> {


    @Override
    public Integer call() {
        return 0;
    }
}
