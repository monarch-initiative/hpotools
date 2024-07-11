package org.monarchinitiative.hpotools.cmd;

import org.monarchinitiative.hpotools.analysis.mondo.*;
import org.monarchinitiative.hpotools.analysis.OntologyTerm;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Dbxref;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@CommandLine.Command(name = "mondo",
        mixinStandardHelpOptions = true,
        description = "Output Mondo phenopackets with available narrow/broad Mondo parents")
public class MondoCommand extends HPOCommand implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MondoCommand.class);

    @CommandLine.Option(names = {"--mondo"}, description = "path to mondo.json")
    private String mondopath = "data/mondo.json";

    @CommandLine.Option(names = {"--clintlr"},
            required = true,
            description = "path to mondo.json")
    private String clintltpath;

    /**
     * Use this argument for the directory. We expect to find a file called
     * all_phenopackets.tsv at the top level of this directory.
     */
    @CommandLine.Option(names = {"--allppkt"},
            required = true,
            description = "path to directory of phenopackets")
    private File all_phenopackets;


    @Override
    public Integer call() throws Exception {
        File all_ppkt_tsv = new File(all_phenopackets + File.separator + "all_phenopackets.tsv");
        if (!all_ppkt_tsv.exists()) {
            throw new PhenolRuntimeException("Could not find all_phenopackets.tsv");
        }
        List<PpktStoreItem> ppktList = parseNewPhenopackets(all_ppkt_tsv);
        PpktResolver resolver = new PpktResolver(all_phenopackets, ppktList);
        Ontology mondo = OntologyLoader.loadOntology(new File(mondopath));
        NarrowAndBroadTerms nbterms = new NarrowAndBroadTerms(mondo);
        LOGGER.info("Mondo version {}", mondo.version().orElse("n/a"));
        List<MondoClintlrItem> mcItemList = new ArrayList<>();
        for (PpktStoreItem item : ppktList) {
            System.out.println(item.disease_id());
            if (nbterms.containsOmim(item.disease_id())) {
                TermId omimId = item.disease_id();
                TermId mondoId = nbterms.omimToMondoId(omimId);
                Optional<Term> optt = mondo.termForTermId(mondoId);
                String mondoLabel = "n/a";
                if (optt.isPresent()) {
                    mondoLabel = optt.get().getName();
                }
                if (nbterms.containsNarrowTermId(mondoId)) {
                    TermId narrowTid =  nbterms.getNarrowTermId(mondoId);
                    String narrowLabel = nbterms.getMondoLabel(narrowTid);
                    if (nbterms.containsNarrowToBroad(mondoId)) {
                        TermId broadTid = nbterms.getBroadForNarrow(mondoId);
                        String broadLabel = nbterms.getMondoLabel(broadTid);
                        File ppktFile = resolver.getFile(item.filename());
                        MondoClintlrItem mcitem = new MondoClintlrItem(omimId,
                                mondoId,
                                mondoLabel,
                                narrowTid,
                                narrowLabel,
                                broadTid,
                                broadLabel,
                                item.cohort(),
                                ppktFile);
                        mcItemList.add(mcitem);
                    }
                }
            }
        }
        resolver.outputFiles("candidates", mcItemList);

        return 0;
    }


    /**
     * Parse the file from phenopacket-store to get a list of diseases and
     * pehnopackets
     * disease - disease_id -- patient_id -- gene -- allele1 -- allele2 -- PMID
     */
    public List<PpktStoreItem> parseNewPhenopackets(File all_ppkt_tsv) {
        List<PpktStoreItem> items = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(all_ppkt_tsv))) {
            String line = br.readLine(); // discard header
            while ((line = br.readLine()) != null) {
                Optional<PpktStoreItem> opt = PpktStoreItem.fromLine(line);
                opt.ifPresent(items::add);
            }
        } catch (IOException e) {
            LOGGER.error("Could not find file at {}", all_phenopackets);
            System.exit(1);
        }
        System.out.printf("Parsed %d new phenopackets.\n", items.size());
        return items;
    }

}
