package org.monarchinitiative.hpotools.cmd;

import org.monarchinitiative.hpotools.analysis.ClintLrItem;
import org.monarchinitiative.hpotools.analysis.OntologyTerm;
import org.monarchinitiative.hpotools.analysis.PpktStoreItem;
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

    @CommandLine.Option(names={"--mondo"}, description = "path to mondo.json")
    private String mondopath ="data/mondo.json";

    @CommandLine.Option(names={"--clintlr"}, description = "path to mondo.json")
    private String clintltpath ="../ClintLR/scripts/DiseaseIntuitionGroupsTsv.tsv";

    @CommandLine.Option(names={"--allppkt"}, description = "path to mondo.json")
    private String all_phenopackets ="/Users/robin/data/allppkt0_1_15/all_phenopackets.tsv";



    @Override
    public Integer call() throws Exception {
        //api
        System.out.println(org.apache.logging.log4j.Logger.class.getResource("/org/apache/logging/log4j/Logger.class"));
//core
        System.out.println(org.apache.logging.log4j.Logger.class.getResource("/org/apache/logging/log4j/core/Appender.class"));
//config
        System.out.println(org.apache.logging.log4j.Logger.class.getResource("/log4j2.xml"));
        if (1+1==2) {
            LOGGER.error("crap");
            System.exit(1);
        }



        List<PpktStoreItem> ppktList = parseNewPhenopackets();
        Ontology mondo = OntologyLoader.loadOntology(new File(mondopath));
        LOGGER.info("Mondo version {}", mondo.version().orElse("n/a"));
        List<ClintLrItem> items = ClintLrItem.fromFile(clintltpath);
        Set<TermId> clintLrTermIds = items.stream()
                .map(ClintLrItem::getMondoId)
                .collect(Collectors.toSet());
        LOGGER.info("Parse a total of {} ClintLR entries", clintLrTermIds.size());
        // Set of all PMIDs
        Map<TermId, OntologyTerm> toNarrowMap = new HashMap<>();
      //  Map<TermId, OntologyTerm> toBroadMap = new HashMap<>();
        Map<TermId, TermId> omimToMondoMap = new HashMap<>();
        Map<TermId, TermId> narrowToBroadMap = new HashMap<>();
        for (ClintLrItem item : items) {
            OntologyTerm narrow = new OntologyTerm(item.getNarrowId(), item.getNarrowLabel());
            OntologyTerm broad = new OntologyTerm(item.getBroadId(), item.getBroadLabel());
            TermId mondoId = item.getMondoId();
            toNarrowMap.put(mondoId, narrow);
          //  toBroadMap.put(mondoId, broad);
            narrowToBroadMap.put(item.getNarrowId(), item.getBroadId());
        }
        LOGGER.info("toNarrowMap: ClintLR to narrow entries {}", toNarrowMap.size());
        LOGGER.info("NarrowToBroadMap: entries derived from ClintLR {}", narrowToBroadMap.size());
        for (TermId mondoId: mondo.nonObsoleteTermIds()) {
            if (! mondoId.getPrefix().equals("MONDO")) continue; // skip other ontology terms
            Optional<Term> narrowParent = getNarrowParentWithOmimPs(mondoId, mondo);
            if (narrowParent.isPresent()) {
                Term narrow = narrowParent.get();
                TermId nrwId = narrow.id();
                String nrwLabel = narrow.getName();
                toNarrowMap.put(mondoId, new OntologyTerm(nrwId, nrwLabel));
            }
            Optional<TermId> optOmim = getOmimIdIfPossible(mondoId, mondo);
            if (optOmim.isPresent()) {
                TermId omim = optOmim.get();
                omimToMondoMap.put(omim, mondoId);
            }
        }
        LOGGER.info("We got {} to narrow candidates.",  toNarrowMap.size());
        LOGGER.info("We got {} OMIM to MONDO mappings.",  omimToMondoMap.size());
        // Now see how many of the phenopackets have narrow mappings
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("candidates.tsv"))) {
            for (PpktStoreItem item : ppktList) {
                if (omimToMondoMap.containsKey(item.disease_id())) {
                    TermId omimId = item.disease_id();
                    TermId mondoId = omimToMondoMap.get(omimId);
                    Optional<Term> optt = mondo.termForTermId(mondoId);
                    String mondoLabel = "n/a";
                    if (optt.isPresent()) {
                        mondoLabel = optt.get().getName();
                    }
                    String narrowId = "n/a";
                    String narrowLabel = "n/a";
                    String broadId = "b/a";
                    String broadLabel = "b/a";
                    if (toNarrowMap.containsKey(mondoId)) {
                        narrowId = toNarrowMap.get(mondoId).id().getValue();
                        narrowLabel = toNarrowMap.get(mondoId).label();
                        if (narrowToBroadMap.containsKey(toNarrowMap.get(mondoId).id())) {
                            TermId broadTid = narrowToBroadMap.get(toNarrowMap.get(mondoId).id());
                            Optional<Term> optTerm = mondo.termForTermId(broadTid);
                            broadId = broadTid.getValue();
                            if (optTerm.isPresent()) {
                                broadLabel = optTerm.get().getName();
                            }
                           List<String> fields = List.of(omimId.getValue(),
                                   mondoId.getValue(),
                                   mondoLabel,
                                   narrowId,
                                   narrowLabel,
                                   broadId,
                                   broadLabel,
                                   item.filename());
                            bw.write(String.join("\t", fields) + "\n");
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * @param mondoId Mondo Term
     * @param mondo ontology
     * @return Omim term id corresponding to the MONDO id (if possible)
     */
    private Optional<TermId> getOmimIdIfPossible(TermId mondoId, Ontology mondo) {
        if (! mondo.containsTerm(mondoId)) {
            System.out.println("Obsolete");
            return Optional.empty();
        }
        Optional<Term> opt = mondo.termForTermId(mondoId);
        if (opt.isPresent()) {
            Term term = opt.get();
            Optional<Dbxref> dbxopt = term.getXrefs().stream()
                    .filter(xr -> xr.getName().startsWith("OMIM:"))
                    .findFirst();
            if (dbxopt.isPresent()) {
                Dbxref dbxref = dbxopt.get();
                String name = dbxref.getName();
                if (name.startsWith("OMIM:")) {
                    return Optional.of(TermId.of(name));
                }
            }

        }
       return Optional.empty();
    }

    /**
     * Get the parent of this term IF that parent is a Mondo term with a relevant
     * OMIM PS
     * @param mondoId
     * @param mondo
     * @return Mondo term that is the narrow parent of the argument if we can find an OMIMPS in the parent
     */
    private Optional<Term> getNarrowParentWithOmimPs(TermId mondoId, Ontology mondo) {
        if (! mondo.containsTerm(mondoId)) {
            System.out.println("Obsolete term");
            return Optional.empty();
        }
        for (TermId parentId: mondo.graph().getParents(mondoId)) {
            Optional<Term> opt = mondo.termForTermId(parentId);
            if (opt.isPresent()) {
                Term parentTerm = opt.get();
                if (! parentTerm.id().getPrefix().equals("MONDO")) continue;
                Optional<Dbxref> dbxopt = parentTerm.getXrefs().stream().filter(xr -> xr.getName().startsWith("OMIMPS")).findFirst();
                if (dbxopt.isPresent()) {
                    // The parent term is an OMIMPS
                    return Optional.of(parentTerm);
                }
            }
        }
        return Optional.empty();
    }

    /** Parse the file from phenopacket-store to get a list of diseases and
     * pehnopackets
     * disease - disease_id -- patient_id -- gene -- allele1 -- allele2 -- PMID
     */
    public List<PpktStoreItem> parseNewPhenopackets() {
        List<PpktStoreItem> items = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(all_phenopackets))){
            String line = br.readLine(); // discard header
            while( (line = br.readLine()) != null ) {
               Optional<PpktStoreItem> opt = PpktStoreItem.fromLine(line);
               opt.ifPresent(items::add);
            }
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error("Could not find file at {}", all_phenopackets);
            System.exit(1);
        }
        System.out.printf("Parsed %d new phenopackets.\n", items.size());
        return items;
    }

}
