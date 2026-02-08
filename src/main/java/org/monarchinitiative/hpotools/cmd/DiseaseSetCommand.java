package org.monarchinitiative.hpotools.cmd;

import org.checkerframework.checker.units.qual.A;
import org.monarchinitiative.hpotools.analysis.OntologyTerm;
import org.monarchinitiative.hpotools.analysis.mondo.MondoClintlrItem;
import org.monarchinitiative.hpotools.analysis.mondo.NarrowAndBroadTerms;
import org.monarchinitiative.hpotools.analysis.mondo.PpktResolver;
import org.monarchinitiative.hpotools.analysis.mondo.PpktStoreItem;
import org.monarchinitiative.phenol.annotations.base.temporal.TemporalInterval;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseaseAnnotation;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoader;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaders;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@CommandLine.Command(name = "mondo",
        mixinStandardHelpOptions = true,
        description = "Output Mondo phenopackets with available narrow/broad Mondo parents")
public class DiseaseSetCommand extends HPOCommand implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiseaseSetCommand.class);



    @Override
    public Integer call() throws IOException {
        List<String> outputLines = new ArrayList<>();
        Ontology hpOntology = getHpOntology();
        HpoDiseaseLoaderOptions options = HpoDiseaseLoaderOptions.of(Set.of(DiseaseDatabase.ORPHANET), false, 5);
        LOGGER.info("HPOA loader options: {}", options);
        HpoDiseaseLoader loader = HpoDiseaseLoaders.defaultLoader(hpOntology, options);
        File annotFile = getAnnotFile();
        Path annotpath = annotFile.toPath();
        HpoDiseases diseases = loader.load(annotpath);
        LOGGER.info("GOT {} diseases", diseases.size());
        TermId hyperreflexia = TermId.of("HP:0001347"); // Hyperreflexia
        TermId axialHypotonia = TermId.of("HP:0008936"); // Axial hypotonia HP:0008936
        TermId spasticity = TermId.of("HP:0001257"); // Spasticity HP:0001257
        TermId ataxia = TermId.of("HP:0001251");  // Ataxia HP:0001251
        TermId abnNSphysiology = TermId.of("HP:0012638"); // Abnormal nervous system physiology HP:0012638
        TermId abnCentralMotor= TermId.of("HP:0011442"); // Abnormal central motor function HP:0011442        Set<TermId> cpRelatedTerms = Set.of( abnNSphysiology);
        Set<TermId> cpRelatedTerms = Set.of(spasticity);
        for (var entry: diseases.diseaseById().entrySet()) {
            HpoDisease disease = entry.getValue();
            List<String> relevantTerms = new ArrayList<>();
            for (var annot: disease.presentAnnotations()) {
                TermId tid = annot.id();
                for (TermId cpTid : cpRelatedTerms) {
                    if (tid.equals(cpTid) || hpOntology.graph().existsPath(tid, cpTid)) {
                        Optional<Term> term = hpOntology.termForTermId(tid);
                        if (term.isPresent()) {
                            String hpot = String.format("%s[%s]", term.get().getName(), tid.getValue());
                            relevantTerms.add(hpot);
                        }

                    }
                }
            }
            if (!relevantTerms.isEmpty()) {


                String msg = String.format("%s\t%s\t%s",
                        disease.diseaseName(),
                        disease.id().getValue(),
                        String.join(";", relevantTerms));
                System.out.println(msg);
                outputLines.add(msg);
            }

        }
        outputLines.sort(String::compareTo);
        File cpFile = new File("CpGenes.txt");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(cpFile))) {
            for (String line : outputLines) {
                bw.write(line +  "\n");
            }
        }
        System.out.printf("Output %d diseases with CP relevance", outputLines.size());
        return 0;
    }

}
