package org.monarchinitiative.hpotools.analysis.stats;

import org.monarchinitiative.hpotools.analysis.maxo.MaxoDxAnnot;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * #description: MAxO/HPO curation file: diagnostic observability of HPO terms
 * #version: 2023-06-11
 */
public class MaxoDxAnnotStats extends TsvAnnotStats{
    private final List<MaxoDxAnnot> maxoDxAnnotList;
    private final int totalMaxoDxAnnots;
    private final int totalMaxoTermUsageCount;
    private final int totalHpoTermUsageCount;

    public MaxoDxAnnotStats(File maxoDxAnnots) {
        String line;
        this.maxoDxAnnotList = new ArrayList<>();
        try(BufferedReader br = new BufferedReader(new FileReader(maxoDxAnnots))) {
            // check and skip the four header lines
            checkStartsWith(br, "#description");
            checkStartsWith(br, "#version");
            checkStartsWith(br, "hpo_id");
            while ((line=br.readLine())!= null) {
                maxoDxAnnotList.add(MaxoDxAnnot.fromLine(line));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        totalMaxoDxAnnots = maxoDxAnnotList.size();
        totalMaxoTermUsageCount = maxoDxAnnotList
                .stream()
                .map(MaxoDxAnnot::maxoId)
                .collect(Collectors.toSet())
                .size();

        totalHpoTermUsageCount = maxoDxAnnotList
                .stream()
                .map(MaxoDxAnnot::hpoId)
                .collect(Collectors.toSet())
                .size();


    }

    public void printStats() {
        System.out.printf("Total number of MAxO Diagnostics annotations: %d\n", totalMaxoDxAnnots);
        System.out.printf("\t- total unique MAxO terms used for annotation: %d\n", totalMaxoTermUsageCount);
        System.out.printf("\t- total unique HPO terms used for annotation:s: %d\n", totalHpoTermUsageCount);

    }
}
