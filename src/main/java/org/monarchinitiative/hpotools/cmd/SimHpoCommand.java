package org.monarchinitiative.hpotools.cmd;


import org.monarchinitiative.hpotools.analysis.simhpo.SimulatedHpoDiseaseGenerator;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoader;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaders;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.phenopackets.schema.v2.Phenopacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import com.google.protobuf.util.JsonFormat;
import org.phenopackets.schema.v2.Phenopacket;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "sim",
        mixinStandardHelpOptions = true,
        description = "Simulate phenopackets")
public class SimHpoCommand extends HPOCommand implements Callable<Integer> {
    private final static Logger LOGGER = LoggerFactory.getLogger(SimHpoCommand.class);

    /** default Noonan syndrome 1	163950 */
    @CommandLine.Option(names={"-c","--cases"}, description = "Number of cases to be simulated", required = false)
    private int nCases = 100;

    /** default simulate 5 HPO terms */
    @CommandLine.Option(names={"-n","--nterms"}, description = "number of HPO terms to be simulated", required = false)
    private int nterms = 5;

    /** output directory for simulated phenopackets */
    @CommandLine.Option(names={"--outdir"}, description = "Output directory", required = false)
    private File outdir = new File("sim_data");

    @Override
    public Integer call() throws Exception {
        if (hpopath==null) {
            throw new PhenolRuntimeException("Need to specify hp.json path");
        }
        File hpoFile = new File(hpopath);
        LOGGER.info("HPO file: {}", hpoFile.getAbsolutePath());
        if (annotpath==null) {
            throw new PhenolRuntimeException("Need to specify annotpath path");
        }
        File annotFile = new File(annotpath);
        if (!annotFile.exists()) {
            throw new PhenolRuntimeException("Did not find annotation file at " + annotpath);
        }
        LOGGER.info("Annotation file: {}", annotFile.getAbsolutePath());
        Ontology ontology = OntologyLoader.loadOntology(hpoFile);
        LOGGER.info("Loaded HPO file version: {}", ontology.version().orElse("n/a"));
        HpoDiseaseLoaderOptions options = HpoDiseaseLoaderOptions.of(Set.of(DiseaseDatabase.OMIM), false, 5);
        LOGGER.info("HPOA loader options: {}", options);
        HpoDiseaseLoader loader = HpoDiseaseLoaders.defaultLoader(ontology, options);
        Path annotpath = annotFile.toPath();
        HpoDiseases diseases = loader.load(annotpath);
        // make directory if needed
        if (outdir.exists()) {
            System.out.println("[WARN] Output directory already exists: " + outdir.getAbsolutePath());
        } else {
            boolean created = outdir.mkdirs();
            if (!created) {
                System.err.println("[ERROR] Could not create directory at: " + outdir.getAbsolutePath());
                return 1;
            }
        }
        SimulatedHpoDiseaseGenerator generator = new SimulatedHpoDiseaseGenerator(diseases, ontology);
        List<TermId> diseaseIds = new ArrayList<>(diseases.diseaseIds());
        int count = 0;
        while (count < nCases) {
            Random rand = new Random();
            int idx = rand.nextInt(diseaseIds.size());
            TermId randomOmimId = diseaseIds.get(idx);
            Optional<Phenopacket> opt = generator.generateSimulatedPhenopacket(randomOmimId);
            // When we get here, we want to output the simulated phenopackets to file
            // now, the generator is just a skeleton and it always returns Optional.empty()!!!
            if (opt.isPresent()) {
                Phenopacket ppkt = opt.get();
                String jsonString = JsonFormat.printer().print(ppkt);
                System.out.println(jsonString);
                String ppkt_id = ppkt.getId();
                String cleaned = ppkt_id.replaceAll("[^\\x00-\\x7F]", "_");
                String outname = String.format("%s.json", cleaned);
                try {
                    Path filePath = Paths.get(String.valueOf(outdir), outname);
                    Files.write(filePath, jsonString.getBytes());
                    System.out.println("File written successfully");
                } catch (IOException e) {
                    System.err.println("Error writing file: " + e.getMessage());
                }
            } else {
                System.err.println("[ERROR] Could not generate simulated phenopacket");
                System.exit(1);
            }
            count++;
        }



        return 0;
    }
}
