package org.monarchinitiative.hpotools.cmd;


import org.monarchinitiative.biodownload.BioDownloader;
import org.monarchinitiative.biodownload.FileDownloadException;
import org.monarchinitiative.hpotools.analysis.stats.MaxoDxAnnotStats;
import org.monarchinitiative.hpotools.analysis.stats.MaxoStats;
import org.monarchinitiative.hpotools.analysis.stats.MaxoaStats;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.Callable;


@CommandLine.Command(name = "maxo",
        mixinStandardHelpOptions = true,
        description = "Output maxo summary")
public class MaxoCommand extends HPOCommand implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MaxoCommand.class.getName());

    private String maxopath ="data/maxo.json";

    private String maxoAnnots = "data/maxo-annotations.tsv";

    private String maxoDxAnnots = "data/maxo_diagnostic_annotations.tsv";

    @CommandLine.Option(names={"--download-maxo"}, description = "download maxo.json")
    private boolean download = false;


    @Override
    public Integer call() throws Exception {
        if (download) {
            return downloadMaxo();
        }
        File maxoFile =checkAndCreateFile(maxopath);
        Ontology maxo = OntologyLoader.loadOntology(maxoFile);
        MaxoStats maxoStats = new MaxoStats(maxo);
        maxoStats.printStats();
        File maxoAnnotFile = checkAndCreateFile(maxoAnnots);
        File maxoDxAnnotFile = checkAndCreateFile(maxoDxAnnots);
        MaxoaStats maxoaStats = new MaxoaStats(maxoAnnotFile);
        maxoaStats.printStats();
        MaxoDxAnnotStats dxStats = new MaxoDxAnnotStats(maxoDxAnnotFile);
        dxStats.printStats();
        return 0;
    }

    private File checkAndCreateFile(String path) {
        File f = new File(path);
        if (!f.isFile()) {
            throw new PhenolRuntimeException("File not found: " + path);
        }
        return f;
    }



    private int downloadMaxo() throws MalformedURLException {
        String baseURL="https://raw.githubusercontent.com/monarch-initiative/maxo-annotations/refs/heads/master/annotations/";
        String annotPath = baseURL + "maxo-annotations.tsv";
        String dxPath = baseURL + "maxo_diagnostic_annotations.tsv";
        URL maxoAnnots = URI.create(annotPath).toURL();
        URL maxoDxAnnots = URI.create(dxPath).toURL();
        try {
            BioDownloader downloader = BioDownloader.builder(Path.of(downloadDirectory))
                    .overwrite(true)
                    .maxoJson()
                    .custom(maxoAnnots)
                    .custom(maxoDxAnnots)
                    .build();
            downloader.download();
            LOGGER.info("Done!");
            return 0;
        } catch (FileDownloadException e) {
            LOGGER.error("Error: {}", e.getMessage(), e);
            return 1;
        }
    }
}
