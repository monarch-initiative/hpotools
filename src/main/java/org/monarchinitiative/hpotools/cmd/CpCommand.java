package org.monarchinitiative.hpotools.cmd;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import org.monarchinitiative.phenol.annotations.base.temporal.TemporalInterval;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoOnset;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoader;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaders;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.TermId;
import picocli.CommandLine;
import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;


import java.util.stream.Collectors;




@CommandLine.Command(name = "cp",
        mixinStandardHelpOptions = true,
        description = "Enumerator candidate CP genes")
public class CpCommand  extends HPOCommand implements Callable<Integer> {
    /** Cerebral palsy */
    TermId cerebralPalsy = TermId.of("HP:0100021");
    /** Athetoid cerebral palsy */
    TermId athethoidCerebralPalsy = TermId.of("HP:0011445");

    TermId hyperreflexia = TermId.of("HP:0001347");
    /** Spasticity HP:0001257 */
    TermId spasticity = TermId.of("HP:0001257");

    /** Hypertonia HP:0001276 */
    TermId hypertonia = TermId.of("HP:0001348");
    /** Dystonia HP:0001332 */
    TermId dystonia = TermId.of("HP:0001332");
    /** Ataxia HP:0001251 */
    TermId ataxia = TermId.of("HP:0001251");
    /**Neurodevelopmental delay HP:0012758 */
    TermId neurodevDelay = TermId.of("HP:0012758");

    @Override
    public Integer call() throws Exception {
        if (hpopath==null) {
            throw new PhenolRuntimeException("Need to specify hp.json path");
        }
        if (annotpath==null) {
            throw new PhenolRuntimeException("Need to specify annotpath path");
        }


        Ontology ontology = OntologyLoader.loadOntology(new File(hpopath));
        String hpoVersion = ontology.version().orElse("n/a");
        System.out.println("[INFO] HPO version: " + hpoVersion);
        HpoDiseaseLoaderOptions options = HpoDiseaseLoaderOptions.defaultOmim();
        HpoDiseaseLoader loader = HpoDiseaseLoaders.defaultLoader(ontology, options);
        HpoDiseases diseases = loader.load(Path.of(annotpath));
        System.out.printf("[INFO] Total disease models: %d\n", diseases.size());
        Map<TermId, String>  omim2symbol = gene2disase();
        System.out.printf("[INFO] Total OMIM to gene symbol mappings: %d\n", omim2symbol.size());
        /// Get labels for output
        Map<TermId, String> diseaseMap = diseases.hpoDiseases()
                .collect(Collectors.toMap(
                        HpoDisease::id,
                        HpoDisease::diseaseName
                ));
        Set<TermId> cpDiseases = getHpoDiseasesForTerm(diseases, cerebralPalsy, "Cerebral palsy");
        int n_cp = cpDiseases.size();
        Set<TermId> earlyOnsetHyperreflexia = getEarlyOnsetHpoDiseasesForTermSet(diseases, hyperreflexia, "Hyperreflexia", ontology);
        int n_hyperreflexia = earlyOnsetHyperreflexia.size();
        Set<TermId> earlyOnsetSpasticity = getEarlyOnsetHpoDiseasesForTermSet(diseases, spasticity, "Spasticity", ontology);
        int n_spasticity = earlyOnsetSpasticity.size();
        Set<TermId> earlyOnsetHypertonia = getEarlyOnsetHpoDiseasesForTermSet(diseases, hypertonia, "Hypertonia", ontology);
        int n_hypertonia = earlyOnsetHypertonia.size();
        Set<TermId> earlyOnsetDystonia = getEarlyOnsetHpoDiseasesForTermSet(diseases, dystonia, "Dystonia", ontology);
        int n_dystonia = earlyOnsetDystonia.size();
        Set<TermId> earlyOnsetAtaxia = getEarlyOnsetHpoDiseasesForTermSet(diseases, ataxia, "Ataxia", ontology);
        int n_ataxia = earlyOnsetAtaxia.size();
        Set<TermId> neurodevDiseases = getEarlyOnsetHpoDiseasesForTermSet(diseases, neurodevDelay, "GDD", ontology);
        int n_neurodev = neurodevDiseases.size();
        Set<TermId> allEarlyOnsetCandidateDiseases = new HashSet<>();
        allEarlyOnsetCandidateDiseases.addAll(earlyOnsetHyperreflexia);
        allEarlyOnsetCandidateDiseases.addAll(earlyOnsetSpasticity);
        allEarlyOnsetCandidateDiseases.addAll(earlyOnsetHypertonia);
        allEarlyOnsetCandidateDiseases.addAll(earlyOnsetDystonia);
        allEarlyOnsetCandidateDiseases.addAll(earlyOnsetAtaxia);
        System.out.printf("Got a total of %d candidate diseases\n", allEarlyOnsetCandidateDiseases.size());
        System.out.printf("All neurodev diseases: %d\n", neurodevDiseases.size());
        neurodevDiseases.addAll(allEarlyOnsetCandidateDiseases);
        System.out.printf("All neurodev diseases intersect with candidates: %d\n", neurodevDiseases.size());
        System.out.printf("All diseases with annotations to Cerebral palsy with candidates: %d\n", cpDiseases.size());
        neurodevDiseases.addAll(cpDiseases);
        System.out.printf("All neurodev diseases and CP diseases: %d\n", neurodevDiseases.size());
        String breakdown = String.format("""
                 We defined a null model based on a set of potentially CP-associated genes, which we defined as all genes associated with diseases with infantile or childhood onset that are annotated to
                the HPO term Cerebral palsy (%s; n=%d), Spasticity (%s; n=%d), Hypertonia (%s; n=%d), Dystonia (%s; %d), Ataxia (%s; n=%d), or Neurodevelopmental delay (%s; n=%d). In each case, diseases annotated to either one of these terms or to
                a more specific descendent of the terms were included. This corresponded to a total of %d genes (the total is less than the sum of the genes associated with each HPO terms because of overlaps).
                """, cerebralPalsy.getValue(), n_cp,
                    spasticity.getValue(), n_spasticity,
                hypertonia.getValue(), n_hypertonia,
                dystonia.getValue(), n_dystonia,
                ataxia.getValue(), n_ataxia,
                neurodevDelay.getValue(), n_neurodev,
                neurodevDiseases.size());
        System.out.println(breakdown);

        String outpath = "CpCandidateDiseasesAndGenes.txt";
        int diseasesWithoutGenes = 0; // expected that not all diseases have genes, but let's count them
        Set<String> allGeneSymbolSet = new HashSet<>();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outpath))) {
            bw.write("Name\tidentifier\tsymbol\n");
            for (TermId tid : neurodevDiseases) {
                String label = diseaseMap.get(tid);
                if (omim2symbol.containsKey(tid)) {
                    String symbol = omim2symbol.get(tid);
                    allGeneSymbolSet.add(symbol);
                    bw.write(String.format("%s\t%s\t%s\n", label, tid.getValue(), symbol));
                } else {
                    diseasesWithoutGenes++;
                }
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        System.out.printf("Could not retrieve gene symbol for %d diseases.\n", diseasesWithoutGenes);
        System.out.printf("Total unique genes: %d.\n", allGeneSymbolSet.size());

        return 0;
    }

    /* Capture lines like this: NCBIGene:51025	PAM16	MENDELIAN	OMIM:613320 */
    private Map<TermId, String>  gene2disase() {
        String g2gpath = this.downloadDirectory + File.separator + "genes_to_disease.txt";
        Map<TermId, String> omim2symbol = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(g2gpath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String [] fields = line.split("\t");
                String symbol = fields[1];
                String diseaseId = fields[3];
                if (diseaseId.startsWith("OMIM:")) {
                    TermId tid = TermId.of(diseaseId);
                    omim2symbol.put(tid, symbol);
                }
            }
        } catch (IOException e) {
            throw new PhenolRuntimeException("Could not read gene symbols from " + g2gpath);
        }
        return omim2symbol;
    }

    private boolean hasHpo(HpoDisease disease, TermId hpoId) {
        return disease.presentAnnotationsStream().anyMatch(annot -> annot.id().equals(hpoId));
    }

    private Set<TermId> getDescendents(TermId hpoId, Ontology ontology) {
        Set<TermId> result = new HashSet<>();
        result.add(hpoId);
        ontology.graph().getDescendants(hpoId).forEach(result::add);
        return result;
    }

    private boolean hasTermInHpoSet(HpoDisease disease, Set<TermId> termIdSet) {
        return disease.presentAnnotationsStream().anyMatch(annot -> termIdSet.contains(annot.id()));
    }

    private boolean hasInfantileOrChildhoodOnset(HpoDisease disease) {
        HpoOnset infantileOnset = HpoOnset.INFANTILE_ONSET;
        HpoOnset childhoodOnset = HpoOnset.CHILDHOOD_ONSET;
        TemporalInterval neoAndChildInterval = TemporalInterval.of(infantileOnset.start(), childhoodOnset.end());
        Optional<TemporalInterval> opt = disease.diseaseOnset();
        if (opt.isPresent()) {
            TemporalInterval interval = opt.get();
            return interval.overlapsWith(neoAndChildInterval);
        } else {
            return false;
        }
    }

    private Set<TermId> getHpoDiseasesForTerm(HpoDiseases diseases, TermId hpoId, String label) {
        Set<TermId> targetDiseases = diseases.stream().
                filter(d -> hasHpo(d, cerebralPalsy)).
                map(HpoDisease::id).
                collect(Collectors.toSet());
        System.out.printf("Found %d diseases associated with %s (%s).\n", targetDiseases.size(), label, hpoId.getValue());
        return targetDiseases;
    }

    private Set<TermId> getEarlyOnsetHpoDiseasesForTermSet(HpoDiseases diseases, TermId targetTermId, String label, Ontology ontology) {
        Set<TermId> targetSet = getDescendents(targetTermId, ontology);
        Set<TermId> targetDiseases = diseases.stream().
                filter(d -> hasTermInHpoSet(d, targetSet)).
                filter(this::hasInfantileOrChildhoodOnset).
                map(HpoDisease::id).
                collect(Collectors.toSet());
        System.out.printf("Found %d diseases associated with %s or descendents.\n", targetDiseases.size(), label);
        return targetDiseases;
    }




}





