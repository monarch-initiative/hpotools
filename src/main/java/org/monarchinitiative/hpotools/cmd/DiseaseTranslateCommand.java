package org.monarchinitiative.hpotools.cmd;


import org.monarchinitiative.hpotools.analysis.mondo.MondoFromOmimMapper;
import org.monarchinitiative.hpotools.analysis.mondo.NarrowAndBroadTerms;
import org.monarchinitiative.hpotools.analysis.mondo.PpktResolver;
import org.monarchinitiative.hpotools.analysis.mondo.PpktStoreItem;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoader;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaders;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "onset",
        mixinStandardHelpOptions = true,
        description = "Calculate number of diseases with onset data")
public class DiseaseTranslateCommand extends HPOCommand implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiseaseTranslateCommand.class);
    /** Terms such as Polydactyly that have a certain assignment to an age of onset (Congenital is taken
     * here to comprise also antenatal). The map is derived from the file {@code term2onset.txt} in the
     * resources section.
     */
    @CommandLine.Option(names = {"--mondo"}, description = "path to mondo.json")
    private String mondopath = "data/mondo.json";
    @CommandLine.Option(names={"-i", "--infile"}, description = "path to infile")
    private String infilePath;
    @CommandLine.Option(names={"--outfile"}, description = "path to outfile")
    private String outfilePath = "disease_mappings.tsv";

    @Override
    public Integer call() throws Exception {

        Ontology mondo = OntologyLoader.loadOntology(new File(mondopath));
        MondoFromOmimMapper mapper = new MondoFromOmimMapper(mondo);
        List<String> rowList = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(infilePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String [] fields = line.split("\t");
                if (fields.length != 2) {
                    System.err.println("malformed line " + line);
                } else {
                    List<String> items = mapper.getMappings(fields[0], fields[1]);
                    String row = String.join("\t", items);
                    rowList.add(row);
                }
            }
        } catch (IOException e) {
            throw new PhenolRuntimeException(e);
        }
        for (String line : rowList) {
            System.out.println(line);
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outfilePath))) {
            for (String line : rowList) {
                bw.write(line + "\n");
            }
        }
        return 0;
    }


}

