package org.monarchinitiative.hpotools;

import org.monarchinitiative.hpotools.cmd.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.concurrent.Callable;
@CommandLine.Command(name = "lcp", mixinStandardHelpOptions = true, version = "lcp 0.0.1",
        description = "long covid phenottype")
public class Main implements Callable<Integer> {
        private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

        public static void main(String[] args) {
            if (args.length == 0) {
                // if the user doesn't pass any command or option, add -h to show help
                args = new String[]{"-h"};
            }
            LOGGER.trace("Starting HPO tools");
            CommandLine cline = new CommandLine(new Main())
                    .addSubcommand("download", new DownloadCommand())
                    .addSubcommand("encoding", new EncodingCommand())
                    .addSubcommand("maxo", new MaxoCommand())
                    .addSubcommand("mondo", new MondoCommand())
                    .addSubcommand("onset", new OnsetCommand())
                    .addSubcommand("simhpo", new SimHpoCommand())
                    .addSubcommand("stats", new StatsCommand())
                    .addSubcommand("translate", new DiseaseTranslateCommand())
                    .addSubcommand("tsv", new Hpo2TsvCommand())
                    .addSubcommand("word", new WordCommand())
                   ;
            cline.setToggleBooleanFlags(false);
            int exitCode = cline.execute(args);
            System.exit(exitCode);
        }


        public static String getVersion() {
            String version = "0.0.0";// default, should be overwritten by the following.
            try {
                Package p = Main.class.getPackage();
                version = p.getImplementationVersion();
            } catch (Exception e) {
                // do nothing
            }
            return version;
        }


        @Override
        public Integer call() {
            // work done in subcommands
            return 0;
        }

    }
