package org.monarchinitiative.hpotools.analysis.mondo;


import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Ingest the existing phenopackets for ClintLR
 * This is the file structure
 * phenopacket	mondo.id	omim.id	mondo.label	narrow.id	narrow.label	broad.id	broad.label	included	comment
 */
public class ClintLrItem {

    private final String phenopacketFileName;

    private final TermId mondoId;

    private final TermId omimId;

    private final String mondoLabel;

    private final TermId narrowId;

    private final String narrowLabel;

    private final TermId broadId;

    private final String broadLabel;

    public ClintLrItem(String phenopacketFileName, TermId mondoId, TermId omimId, String mondoLabel, TermId narrowId, String narrowLabel, TermId broadId, String broadLabel) {
        this.phenopacketFileName = phenopacketFileName;
        this.mondoId = mondoId;
        this.omimId = omimId;
        this.mondoLabel = mondoLabel;
        this.narrowId = narrowId;
        this.narrowLabel = narrowLabel;
        this.broadId = broadId;
        this.broadLabel = broadLabel;
    }

    public String getPhenopacketFileName() {
        return phenopacketFileName;
    }

    public TermId getMondoId() {
        return mondoId;
    }

    public TermId getOmimId() {
        return omimId;
    }

    public String getMondoLabel() {
        return mondoLabel;
    }

    public TermId getNarrowId() {
        return narrowId;
    }

    public String getNarrowLabel() {
        return narrowLabel;
    }

    public TermId getBroadId() {
        return broadId;
    }

    public String getBroadLabel() {
        return broadLabel;
    }

    public static List<ClintLrItem> fromFile(String fileName) {

        List<ClintLrItem> items = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line = br.readLine(); // discard header
            while ((line = br.readLine()) != null) {
                //System.out.println(line);
                Optional<ClintLrItem> opt = ClintLrItem.fromLine(line);
                if (opt.isPresent()) {
                    items.add(opt.get());
                } else {
                    System.err.println("Could not parse line: " + line);
                }
            }
        } catch (IOException e) {
            throw new PhenolRuntimeException("Could not open ClintLR file at " + fileName, e);
        }
        System.out.printf("Got %d items.\n", items.size());
        return items;
    }


    public static Optional<ClintLrItem> fromLine(String line) {
        String[] tokens = line.split("\t");
        try {
            String ppktId = tokens[0];
            TermId mondoId = TermId.of(tokens[1]);
            TermId omimId = TermId.of(tokens[2]);
            String mondoLabel = tokens[3];
            TermId narrowId = TermId.of(tokens[4]);
            String narrowLabel = tokens[5];
            TermId broadId = TermId.of(tokens[6]);
            String broadLabel = tokens[7];
            return Optional.of(new ClintLrItem(ppktId,
                    mondoId,
                    omimId,
                    mondoLabel,
                    narrowId,
                    narrowLabel,
                    broadId,
                    broadLabel));
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return Optional.empty();
        }
    }

}
