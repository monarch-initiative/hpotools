package org.monarchinitiative.hpotools.analysis.stats;

import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


/**
 * Generate stats for a release of
 * phenotype.hpoa
 * <ol>
 *     <li>database_id</li>
 *     <li>disease_name</li>
 *     <li>qualifier</li>
 *     <li>hpo_id</li>
 *     <li>reference</li>
 *     <li>evidence</li>
 *     <li>onset</li>
 *     <li>frequency</li>
 *     <li>sex</li>
 *     <li>modifier</li>
 *     <li>aspect</li>
 *     <li>biocuration</li>
 * </ol>

 */
public class HpoaStats {

    private String hpoaVersion;

    private Set<TermId> utilizedHpoTermIds;
    private Map<TermId, Set<TermId>> diseaseToHpoMap;
    private Set<String> citationSet;
    private Map<String, Integer> lineByDb;
    private Map<String, Integer> lineWithOnsetByDb;
    private Map<String, Integer> lineWithFrequncyByDb;
    private Map<String, Integer> lineWithModifierByDb;
    private Map<String, Integer> lineWithSexInfoByDb;

    private Set<TermId> allDiseases;
    private HashMap<String, Integer> diseaseCountByDb;
    private Set<TermId> diseasesWithOnset;
    private int n_annots = 0;
    // Identifier for term "Onset"
    private final static TermId ONSET_ID = TermId.of("HP:0003674");

    public HpoaStats(File phenotypeHpoaFile, Ontology ontology) {
        utilizedHpoTermIds = new HashSet<>();
        citationSet  = new HashSet<>();
        diseaseToHpoMap = new HashMap<>();
        lineWithOnsetByDb = new HashMap<>();
        lineWithFrequncyByDb = new HashMap<>();
        lineWithModifierByDb = new HashMap<>();
        lineWithSexInfoByDb = new HashMap<>();
        lineByDb = new HashMap<>();
        diseasesWithOnset = new HashSet<>();
        allDiseases = new HashSet<>();
        diseaseCountByDb = new HashMap<>();
        try(BufferedReader br = new BufferedReader(new FileReader(phenotypeHpoaFile))) {
            String line = br.readLine(); // general header
            line = br.readLine();
            if (line.startsWith("#version:")) {
                hpoaVersion = line.substring(10).trim();
            } else {
                hpoaVersion = "?";
            }
            line = br.readLine(); // tracker
            line = br.readLine(); // HPO version
            line = br.readLine(); // header
            while ((line=br.readLine()) != null) {
                String[] tokens = line.split("\t");
                TermId diseaseId = TermId.of(tokens[0]);
                TermId hpoID = TermId.of(tokens[3]);
                String citation = tokens[4];
                String onset = tokens[6];
                String frequency = tokens[7];
                String sex = tokens[8];
                String modifiers = tokens[9];
                allDiseases.add(diseaseId);
                diseaseToHpoMap.computeIfAbsent(diseaseId, k -> new HashSet<>()).add(hpoID);
                utilizedHpoTermIds.add(hpoID);
                citationSet.add(citation);
                lineByDb.merge(diseaseId.getPrefix(),1,Integer::sum);
                if (onset != null && onset.contains("HP") ) {
                    lineWithOnsetByDb.merge(diseaseId.getPrefix(), 1, Integer::sum);
                    diseasesWithOnset.add(diseaseId);
                }
                if (frequency != null && (frequency.contains("HP") || frequency.contains("/"))) {
                    lineWithFrequncyByDb.merge(diseaseId.getPrefix(), 1, Integer::sum);
                }
                if (sex != null && (sex.equalsIgnoreCase("MALE") || sex.equalsIgnoreCase("FEMALE"))) {
                    lineWithSexInfoByDb.merge(diseaseId.getPrefix(), 1, Integer::sum);
                }
                if (modifiers != null && modifiers.trim().length() > 0) {
                    lineWithModifierByDb.merge(diseaseId.getPrefix(), 1, Integer::sum);
                }
                if (ontology.graph().isDescendantOf(hpoID, ONSET_ID)) {
                    diseasesWithOnset.add(diseaseId);
                }
                n_annots++;
        }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (TermId dxId : allDiseases) {
            diseaseCountByDb.merge(dxId.getPrefix(), 1, Integer::sum);
        }
    }

    private void sanityCheck() {
        if (diseaseToHpoMap.size() > 3) {
            throw new PhenolRuntimeException("Too many disease prefixes");
        }
    }


    public void printTotalInformation() {
        System.out.println("################");
        System.out.printf("Total annotations: %d\n.", n_annots);
    }


    public void printDiseasesWithOnset() {
        int omim = 0;
        int orpha = 0;
        int decipher = 0;
        for (TermId dxId : diseasesWithOnset) {
            String prefix = dxId.getPrefix();
            switch (prefix) {
                case "OMIM" -> omim++;
                case "ORPHA" -> orpha++;
                case "DECIPHER" -> decipher++;
            }
        }
        int total_omim = diseaseCountByDb.getOrDefault("OMIM", 0);
        int total_orpha = diseaseCountByDb.getOrDefault("ORPHA", 0);
        int total_decipher = diseaseCountByDb.getOrDefault("DECIPHER", 0);;
        System.out.println("Diseases with onset information");
        double omimPerc = 100d*omim/total_omim;
        double orphaPerc = 100d*orpha/total_orpha;
        double decipherPerc = 100d*decipher/total_decipher;
        System.out.printf("OMIM: %d/%d (%.1f%%)%n", omim, total_omim, omimPerc);
        System.out.printf("ORPHA: %d/%d (%.1f%%)%n", orpha, total_orpha, orphaPerc);
        System.out.printf("DECIPHER: %d/%d (%.1f%%)%n", decipher, total_decipher, decipherPerc);
    }

    public void printStatistics() {
        List<String> diseaseDb = List.of("OMIM", "ORPHA", "DECIPHER");
        System.out.printf("HPOA version: %s\n", hpoaVersion);
        for (var db : diseaseDb) {
            int total_lines = lineByDb.get(db);
            int lines_with_onset = lineWithOnsetByDb.getOrDefault(db, 0);
            int lines_with_frequncy = lineWithFrequncyByDb.getOrDefault(db, 0);
            int lines_with_modifier = lineWithModifierByDb.getOrDefault(db, 0);
            int lines_with_sex_info = lineWithSexInfoByDb.getOrDefault(db, 0);
            String onset = String.format("Onset: %d (%.1f%%)",
                    lines_with_onset,
                    (lines_with_onset*100d)/total_lines);
            String frequency = String.format("Frequency: %d (%.1f%%)",
                    lines_with_frequncy,
                    (lines_with_frequncy*100d)/total_lines);
            String modifiers = String.format("Modifiers: %d (%.1f%%)",
                    lines_with_modifier,
                    (lines_with_modifier*100d)/total_lines);
            String sex = String.format("Sex: %d (%.1f%%)",
                    lines_with_sex_info,
                    (lines_with_sex_info*100d/total_lines));
            System.out.printf("%s: %d lines. %s. %s. %s. %s\n", db, total_lines,
                    onset, frequency, modifiers, sex);
        }
        for (var db : diseaseDb) {
            System.out.printf("%s: %d lines\n", db, lineByDb.get(db));
        }
        printDiseasesWithOnset();
        printTotalInformation();
    }


}
