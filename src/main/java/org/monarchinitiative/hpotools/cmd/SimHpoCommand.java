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
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "onset",
        mixinStandardHelpOptions = true,
        description = "Calculate number of diseases with onset data")
public class SimHpoCommand extends HPOCommand implements Callable<Integer> {


    /** default Noonan syndrome 1	163950 */
    @CommandLine.Option(names={"--disease"}, description = "OMIM identifer of disease to be simulated", required = true)
    private String omimIdentifier = "163950";

    /** default simulate 5 HPO terms */
    @CommandLine.Option(names={"-n","--nterms"}, description = "number of HPO terms to be simulated", required = false)
    private int nterms = 5;

    @Override
    public Integer call() throws Exception {
        if (hpopath==null) {
            throw new PhenolRuntimeException("Need to specify hp.json path");
        }
        if (annotpath==null) {
            throw new PhenolRuntimeException("Need to specify annotpath path");
        }

        Ontology ontology = OntologyLoader.loadOntology(new File(hpopath));

        HpoDiseaseLoaderOptions options = HpoDiseaseLoaderOptions.of(Set.of(DiseaseDatabase.OMIM), false, 5);
        HpoDiseaseLoader loader = HpoDiseaseLoaders.defaultLoader(ontology, options);
        HpoDiseases diseases = loader.load(Path.of(annotpath));

        SimulatedHpoDiseaseGenerator generator = new SimulatedHpoDiseaseGenerator(diseases, ontology);
        TermId diseaseId = TermId.of("OMIM", omimIdentifier);
        Optional<Phenopacket> opt = generator.generateSimulatedPhenopacket(diseaseId);
        if (opt.isPresent()) {
            System.out.println(opt.get());
        } else {
            System.out.println("Could not retrieve phenopacket for \"" + omimIdentifier + "\"");
        }

        return 0;
    }
}
