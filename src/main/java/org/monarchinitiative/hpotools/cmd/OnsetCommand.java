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
        Ontology ontology = OntologyLoader.loadOntology(new File(hpopath));
        HpoDiseaseLoaderOptions options =
                HpoDiseaseLoaderOptions.of(Set.of(DiseaseDatabase.OMIM), false, 5);
        HpoDiseaseLoader loader = HpoDiseaseLoaders.defaultLoader(ontology, options);
        HpoDiseases diseases = loader.load(Path.of(annotpath));


        // Count current diseases with onset annotation in the phenotype.hpoa file and output
        int diseasesWithOnsetInformation = (int) countDiseasesWithOnset(diseases);
        System.out.println("[INFO] Current number of diseases with onset information: " + diseasesWithOnsetInformation);

        // Count current diseases without onset annotation in the phenotype.hpoa file and output
        int diseasesWithoutOnsetInformation = (int) countDiseasesWithoutOnset(diseases);
        System.out.println("[INFO] Current number of diseases without onset information: " +
                diseasesWithoutOnsetInformation);

        // Parse Congenital terms from the text file and get the descendants of these HPO terms
        termIdToCongenitalOnsetSet = parseHpoTermToHpoOnsetMap(ontology);

        // Update diseases that have any of these HPO terms with congenital age of onset
        Set<HpoDisease> congenitalDiseaseSet = inferCongenitalDiseases(diseases, termIdToCongenitalOnsetSet);
        System.out.println(String.format("[INFO] Inferred %d congenital onsets.", congenitalDiseaseSet.size()));

        // Infer diseases to be congenital based on terms and write to file
        System.out.println("[INFO] Writing inferred congenital diseases to: " + outfilePath);
        writeCongenitalDiseasesToFile(congenitalDiseaseSet, outfilePath);

        // Update and inform user of new total number of diseases with onset information
        diseasesWithOnsetInformation = diseasesWithOnsetInformation + congenitalDiseaseSet.size();
        diseasesWithoutOnsetInformation = diseasesWithoutOnsetInformation - congenitalDiseaseSet.size();

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
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                String[] fields = line.split(",");
                if (fields.length != 2) {
                    throw new PhenolRuntimeException("Malformed line with " + fields.length + " fields");
                }
                TermId hpoId = TermId.of(fields[1]);
                termSet.add(hpoId);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Get all agenesis terms
        for (Term term : ontology.getTerms()) {
            Set<String> labels = new HashSet<>();
            labels.add(term.getName().toLowerCase(Locale.ROOT));
            for (var syn : term.getSynonyms()) {
                labels.add(syn.getValue().toLowerCase(Locale.ROOT));
            }
            for (var lbl : labels) {
                if (lbl.contains("agenesis") || lbl.contains("aplasia") || lbl.contains("supernumerary")
                        || lbl.contains("situs inversus") || lbl.contains("situs ambiguous")) {
                    termSet.add(term.id());
                }
            }
        }


        // Get descendants of congenital terms, as these are also congenital
        Set<TermId> TermSetWithDescendants = new HashSet<>();
        for (TermId tid : termSet) {
            for (var hpoId: ontology.graph().getDescendants(tid, true)) {
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
    private Set<HpoDisease> inferCongenitalDiseases(HpoDiseases diseases, Set<TermId> congenitalOnsetTermIds) {
        Set<HpoDisease> congenitalDiseaseSet = new HashSet<>();
        for (HpoDisease disease : diseases) {
            if (disease.diseaseOnset().isEmpty() && hasCongenitalAnnotation(disease, congenitalOnsetTermIds)) {
                congenitalDiseaseSet.add(disease);
            }
        }

        return congenitalDiseaseSet;
    }

    /**
     * Checks if a disease has an HPO annotation that indicates congenital onset.
     *
     * @param disease The HpoDisease to check.
     * @param congenitalOnsetTermIds A set of TermIds representing known congenital terms.
     * @return True if the disease has a congenital annotation, false otherwise.
     */
    private boolean hasCongenitalAnnotation(HpoDisease disease, Set<TermId> congenitalOnsetTermIds) {
        return disease.annotations().stream()
                .filter(annotation -> annotation.frequency() > 0)
                .map(HpoDiseaseAnnotation::id)
                .anyMatch(congenitalOnsetTermIds::contains);
    }

    /**
     * Writes the provided congenital diseases to a tab-separated file.
     *
     * @param diseases The set of HpoDisease objects to write.
     * @param outFilePath The path to the output file.
     */
    private void writeCongenitalDiseasesToFile(Set<HpoDisease> diseases, String outFilePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outfilePath))) {
            diseases.stream()
                    .map(this::formatDiseaseData)
                    .forEachOrdered(line -> {
                        try {
                            writer.write(line + "\n");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Formats the data of an HpoDisease object into a tab-separated line.
     *
     * @param disease The HpoDisease object.
     * @return A tab-separated string representing the formatted disease data.
     */
    private String formatDiseaseData(HpoDisease disease) {
        List<String> fields = Arrays.asList(
                disease.id().getValue(),
                disease.diseaseName(),
                EMPTY_STRING,
                CONGENITAL_ONSET,
                HPO_PMID,
                INFERRED_FROM_ELECTRONIC_ANNOTATION,
                EMPTY_STRING,
                EMPTY_STRING,
                EMPTY_STRING,
                EMPTY_STRING,
                C_ASPECT,
                "HPO:probinson[2022-05-21]"
        );
        return String.join("\t", fields);
    }



}
