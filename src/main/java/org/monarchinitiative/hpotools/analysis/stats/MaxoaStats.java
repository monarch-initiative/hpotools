package org.monarchinitiative.hpotools.analysis.stats;

import org.monarchinitiative.hpotools.analysis.maxo.MaxoAnnot;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MaxoaStats extends TsvAnnotStats {

    private final List<MaxoAnnot> maxoAnnotList;
    private final int totalMaxoAnnots;
    private final int totalMaxoAnnotDiseaseCount;
    private final int totalMaxoTermUsageCount;
    private final int totalHpoTermUsageCount;




    public MaxoaStats(File maxoAnnots) {
        String line;
        this.maxoAnnotList = new ArrayList<>();
        try(BufferedReader br = new BufferedReader(new FileReader(maxoAnnots))) {
            // check and skip the four header lines
            checkStartsWith(br, "#description");
            checkStartsWith(br, "#date");
            checkStartsWith(br, "#tracker");
            checkStartsWith(br, "disease_id");
            while ((line=br.readLine())!= null) {
                maxoAnnotList.add(MaxoAnnot.fromLine(line));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        totalMaxoAnnots = maxoAnnotList.size();
        totalMaxoAnnotDiseaseCount = maxoAnnotList
                .stream()
                .map(MaxoAnnot::diseaseId)
                .collect(Collectors.toSet())
                .size();
        totalMaxoTermUsageCount = maxoAnnotList
                .stream()
                .map(MaxoAnnot::maxoId)
                .collect(Collectors.toSet())
                .size();
        totalHpoTermUsageCount = maxoAnnotList
                .stream()
                .map(MaxoAnnot::hpoId)
                .collect(Collectors.toSet())
                .size();
    }

    public void printStats() {
        System.out.printf("Total number of MAxOannotations: %d\n", totalMaxoAnnots);
        System.out.printf("\t- annotated diseases: %d\n", totalMaxoAnnotDiseaseCount);
        System.out.printf("\t- total unique MAxO terms used for annotation: %d\n", totalMaxoTermUsageCount);
        System.out.printf("\t- total unique HPO terms used for annotation:s: %d\n", totalHpoTermUsageCount);


    }





}
