package org.monarchinitiative.hpotools.cmd;

import org.monarchinitiative.hpotools.analysis.ClintLrItem;
import org.monarchinitiative.hpotools.analysis.OntologyTerm;
import org.monarchinitiative.hpotools.analysis.PpktStoreItem;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Dbxref;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import picocli.CommandLine;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@CommandLine.Command(name = "mondo",
        mixinStandardHelpOptions = true,
        description = "Output Mondo phenopackets with available narrow/broad Mondo parents")
public class MondoCommand extends HPOCommand implements Callable<Integer> {

    @CommandLine.Option(names={"--mondo"}, description = "path to mondo.json")
    private String mondopath ="data/mondo.json";


    @CommandLine.Option(names={"--clintlr"}, description = "path to mondo.json")
    private String clintltpath ="../ClintLR/scripts/DiseaseIntuitionGroupsTsv.tsv";

    @CommandLine.Option(names={"--allppkt"}, description = "path to mondo.json")
    private String all_phenopackets ="/Users/robin/data/allppkt0_1_14/all_phenopackets/all_phenopackets.tsv";



    @Override
    public Integer call() throws Exception {
        List<PpktStoreItem> ppktList = parseNewPhenopackets();
        Ontology mondo = OntologyLoader.loadOntology(new File(mondopath));
        List<ClintLrItem> items = ClintLrItem.fromFile(clintltpath);
        Set<TermId> clintLrTermIds = items.stream()
                .map(ClintLrItem::getMondoId)
                .collect(Collectors.toSet());
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
        System.out.printf("We got %d to narrow candidates.\n",  toNarrowMap.size());
        System.out.printf("We got %d OMIM to MONDO mappings.\n",  omimToMondoMap.size());
        // Now see how many of the phenopackets have narrow mappings
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("candidates.tsv"))) {
            for (PpktStoreItem item : ppktList) {
                if (omimToMondoMap.containsKey(item.disease_id())) {
                    TermId omimId = item.disease_id();
                    TermId mondoId = omimToMondoMap.get(omimId);
                    if (clintLrTermIds.contains(mondoId)) {
                        System.err.println("Skipping " + mondoId + " because it is already clintlr");
                        continue;
                    }
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
                                   broadLabel);
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

    private Optional<TermId> getOmimIdIfPossible(TermId mondoId, Ontology mondo) {
        if (! mondo.containsTerm(mondoId)) {
            System.out.println("Obsolte");
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
     * Get the parent of this term IF that parent is a Mondo term with a relevant OMIM PS
     * @param mondoId
     * @param mondo
     * @return
     */
    private Optional<Term> getNarrowParentWithOmimPs(TermId mondoId, Ontology mondo) {
        if (! mondo.containsTerm(mondoId)) {
            System.out.println("Obsolte");
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
            String line = br.readLine(); // discarrd header
            while( (line = br.readLine()) != null ) {
               Optional<PpktStoreItem> opt = PpktStoreItem.fromLine(line);
               opt.ifPresent(items::add);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.printf("Parsed %d new phenopackets.\n", items.size());
        return items;
    }



}
