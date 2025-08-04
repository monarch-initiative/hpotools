package org.monarchinitiative.hpotools.cmd;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseaseAnnotation;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoader;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaders;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "onset",
        mixinStandardHelpOptions = true,
        description = "Calculate number of diseases with onset data")

public class OnsetCommand extends HPOCommand implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(OnsetCommand.class);
    /** Terms such as Polydactyly that have a certain assignment to an age of onset (Congenital is taken
     * here to comprise also antenatal). The map is derived from the file {@code term2onset.txt} in the
     * resources section.
     */
    private Set<TermId> termIdToCongenitalOnsetSet;

    /** Congenital onset HP:0003577 */
    private final String CONGENITAL_ONSET = "HP:0003577";
    private final String EMPTY_STRING = "";
    private final String INFERRED_FROM_ELECTRONIC_ANNOTATION = "IEA";
    private final String C_ASPECT = "C";
    private  Ontology ontology = null;
    /**
     * Köhler S, et al. The Human Phenotype Ontology in 2021. Nucleic Acids Res. 2021;49(D1):D1207-D1217.
     * doi: 10.1093/nar/gkaa1043. PMID: 33264411; PMCID: PMC7778952.
     */
    private final String HPO_PMID = "PMID:33264411";

    @CommandLine.Option(names={"--outfile"}, description = "path to outfile")
    private String outfilePath = "predictedCongenital.hpoa";

    public OnsetCommand() {

    }


    @Override
    public Integer call() throws Exception {
        if (hpopath==null) {
            throw new PhenolRuntimeException("Need to specify hp.json path");
        }
        if (annotpath==null) {
            throw new PhenolRuntimeException("Need to specify annotpath path");
        }

        // Load everything
        this.ontology = OntologyLoader.loadOntology(new File(hpopath));
        String hpoVersion = ontology.version().orElse("n/a");
        System.out.println("[INFO] HPO version: " + hpoVersion);
        // Parse Congenital terms from the text file and get the descendants of these HPO terms
        termIdToCongenitalOnsetSet = parseHpoTermToHpoOnsetMap(ontology);
        System.out.printf("[INFO] Congenital onset HPO terms: %d.\n", termIdToCongenitalOnsetSet.size());
        HpoDiseaseLoaderOptions options =
                HpoDiseaseLoaderOptions.of(Set.of(DiseaseDatabase.OMIM), false, 5);
        HpoDiseaseLoader loader = HpoDiseaseLoaders.defaultLoader(ontology, options);
        HpoDiseases diseases = loader.load(Path.of(annotpath));
        System.out.printf("[INFIO] Total disease models: %d\n", diseases.size());
        // Count current diseases with onset annotation in the phenotype.hpoa file and output
        int diseasesWithOnsetInformation = (int) countDiseasesWithOnset(diseases);
        System.out.println("[INFO] Current number of diseases with onset information: " + diseasesWithOnsetInformation);

        // Count current diseases without onset annotation in the phenotype.hpoa file and output
        int diseasesWithoutOnsetInformation = (int) countDiseasesWithoutOnset(diseases);
        System.out.println("[INFO] Current number of diseases without onset information: " +
                diseasesWithoutOnsetInformation);


        writeInferredOnsetTerms(termIdToCongenitalOnsetSet);
        System.out.printf("[INFO] Inferred %d congenital onset terms.%n", termIdToCongenitalOnsetSet.size());
        // Update diseases that have any of these HPO terms with congenital age of onset
        Map<HpoDisease, List<TermId>>  congenitalDiseaseMap = inferCongenitalDiseases(diseases, termIdToCongenitalOnsetSet);
        System.out.printf("[INFO] Inferred %d congenital onset diseases.%n", congenitalDiseaseMap.size());

        // Infer diseases to be congenital based on terms and write to file
        System.out.println("[INFO] Writing inferred congenital diseases to: " + outfilePath);
        writeCongenitalDiseasesToFile(congenitalDiseaseMap, outfilePath);

        // Update and inform user of new total number of diseases with onset information
        diseasesWithOnsetInformation = diseasesWithOnsetInformation + congenitalDiseaseMap.size();
        diseasesWithoutOnsetInformation = diseasesWithoutOnsetInformation - congenitalDiseaseMap.size();

        System.out.println("[INFO] New number of diseases with onset information " + diseasesWithOnsetInformation);
        System.out.println("[INFO] New number of diseases without onset information " +
                diseasesWithoutOnsetInformation);
        return 0;
    }



    private Set<TermId> parseHpoTermToHpoOnsetMap(Ontology ontology) {
        URL url = OnsetCommand.class.getResource("congenitalTerms.txt");
        if (url == null) {
            System.err.println("Could not read term2onset file");
            return Set.of();
        }

        Set<TermId> termSet = new HashSet<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] fields = line.split(",");
                if (fields.length != 2) {
                    throw new PhenolRuntimeException("Malformed line with " + fields.length + " fields");
                }
                TermId hpoId = TermId.of(fields[1]);
                termSet.add(hpoId);
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
        TermId  HFB = TermId.of("HP:0002692"); //         Hypoplastic facial bones
        System.err.printf("[INFO] Congenital onset terms from file: %d\n", termSet.size());// Get all agenesis terms
        for (Term term : ontology.getTerms()) {
            Set<String> labels = new HashSet<>();
            String termLabel = term.getName().toLowerCase(Locale.ROOT);
            labels.add(termLabel);

            for (var lbl : labels) {
                if (lbl.contains("aplasia/hypoplasia") || lbl.contains("hypoplasia/aplasia")) {
                    continue; // Not guaranteed to be congenital, but might be picked up by
                    // the following heuristic
                    // e.g., Aplasia/Hypoplasia of facial bones HP:0034261
                }
                if (lbl.contains("agenesis") || lbl.contains("aplasia") || lbl.contains("supernumerary")
                        || lbl.contains("situs inversus") || lbl.contains("situs ambiguous")) {
                    termSet.add(term.id());
                }
            }
        }
        // Get descendants of congenital terms, as these are also congenital
        Set<TermId> TermSetWithDescendants = new HashSet<>();
        for (TermId tid : termSet) {
            TermSetWithDescendants.add(tid);
            for (var hpoId: ontology.graph().getDescendants(tid)) {
                /// leave out descendents of terms with Hypoplasia/Aplasia
                TermSetWithDescendants.add(hpoId);
            }
        }
        return TermSetWithDescendants;
    }

    /**
     * Counts the number of diseases within the HpoDiseases collection that have a present onset annotation.
     *
     * @param diseases The HpoDiseases collection to analyse.
     * @return The count of diseases with onset annotations.
     */
    public long countDiseasesWithOnset(HpoDiseases diseases) {

        return diseases.stream()
                .filter(disease -> disease.diseaseOnset().isPresent())
                .count();
    }

    /**
     * Counts the number of diseases within the HpoDiseases collection that do not have a present onset annotation.
     *
     * @param diseases The HpoDiseases collection to analyse.
     * @return The count of diseases without onset annotations.
     */
    public long countDiseasesWithoutOnset(HpoDiseases diseases){

        return diseases.stream()
                .filter(disease -> disease.diseaseOnset().isEmpty())
                .count();
    }


    /**
     * Identifies and returns diseases inferred to have a congenital onset based on their HPO annotations.
     *
     * @param diseases The HpoDiseases collection to analyze.
     * @param congenitalOnsetTermIds A set of TermIds representing known congenital terms.
     * @return A set of HpoDisease objects inferred to have congenital onset.
     */
    private  Map<HpoDisease, List<TermId>> inferCongenitalDiseases(HpoDiseases diseases, Set<TermId> congenitalOnsetTermIds) {
        Map<HpoDisease, List<TermId>> congenitalDiseaseMap = new HashMap<>();
        for (HpoDisease disease : diseases) {
            if (disease.diseaseOnset().isEmpty()) {
                List<TermId> termIds = getCongenitalAnnotationList(disease, congenitalOnsetTermIds);
                if (!termIds.isEmpty()) {
                    congenitalDiseaseMap.put(disease, termIds);
                }
            }
        }
        return congenitalDiseaseMap;
    }

    /**
     * Checks if a disease has an HPO annotation that indicates congenital onset.
     *
     * @param disease The HpoDisease to check.
     * @param congenitalOnsetTermIds A set of TermIds representing known congenital terms.
     * @return Potentially empty list of congenital HPO term annotations for a disease.
     */
    private List<TermId> getCongenitalAnnotationList(HpoDisease disease, Set<TermId> congenitalOnsetTermIds) {
        return disease.annotations().stream()
                .filter(annotation -> annotation.frequency() > 0)
                .map(HpoDiseaseAnnotation::id)
                .filter(congenitalOnsetTermIds::contains)
                .toList();
    }

    /**
     * Writes the provided congenital diseases to a tab-separated file.
     *
     * @param diseases The set of HpoDisease objects to write.
     * @param outFilePath The path to the output file.
     */
    private void writeCongenitalDiseasesToFile(Map<HpoDisease, List<TermId>> diseases, String outFilePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outfilePath))) {
            for (var e: diseases.entrySet()) {
                HpoDisease disease = e.getKey();
                List<TermId> termIds = e.getValue();
                String formated= formatDiseaseData(disease,termIds );
                writer.write(formated + "\n");

            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }



    private String formatTerms(List<TermId> termIds) {
        List<String> terms = new ArrayList<>();
        for (TermId termId : termIds) {
            Optional<Term> term = this.ontology.termForTermId(termId);
            if (term.isPresent()) {
                terms.add(String.format("%s[%s]", term.get().getName(), term.get().id().getValue()));
            } else {
                throw new PhenolRuntimeException("Could not find term with id " + termId);
            }
        }
        return String.join(";", terms);
    }


    /**
     * Formats the data of an HpoDisease object into a tab-separated line.
     *
     * @param disease The HpoDisease object.
     * @return A tab-separated string representing the formatted disease data.
     */
    private String formatDiseaseData(HpoDisease disease, List<TermId> termIds ) {
        List<String> fields = Arrays.asList(
                disease.id().getValue(),
                disease.diseaseName(),
                EMPTY_STRING,
                CONGENITAL_ONSET,
                HPO_PMID,
                INFERRED_FROM_ELECTRONIC_ANNOTATION,
                formatTerms(termIds),
                EMPTY_STRING,
                EMPTY_STRING,
                EMPTY_STRING,
                C_ASPECT,
                "HPO:probinson[2022-05-21]"
        );
        return String.join("\t", fields);
    }

    private void writeInferredOnsetTerms(Set<TermId> congenitalTerms) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("inferredOnsetterms.txt"))){
            for (TermId tid : congenitalTerms) {
                Optional<Term> term = this.ontology.termForTermId(tid);
                if (term.isPresent()) {
                    bw.write(term.get().getName()+ "\t" + tid.getValue()  + "\n");
                } else {
                    throw new PhenolRuntimeException("Could not find term with id " + tid);
                }

            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }

}
