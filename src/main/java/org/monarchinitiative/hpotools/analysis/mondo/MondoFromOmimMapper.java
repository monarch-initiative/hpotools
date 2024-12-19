package org.monarchinitiative.hpotools.analysis.mondo;

import org.monarchinitiative.phenol.ontology.data.Dbxref;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

public class MondoFromOmimMapper {


    private final Ontology ontology;

    private final Map<String, TermId> omimToMondoMap;

    public MondoFromOmimMapper(Ontology mondo) {
        ontology = mondo;
        omimToMondoMap = fromOmimIds();
    }


    /**
     * @param mondoId Mondo Term

     * @return Omim term id corresponding to the MONDO id (if possible)
     */
    private Optional<TermId> getOmimIdIfPossible(TermId mondoId) {
        if (! ontology.containsTerm(mondoId)) {
            System.out.println("Obsolete");
            return Optional.empty();
        }
        Optional<Term> opt = ontology.termForTermId(mondoId);
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

    private Map<String, TermId> fromOmimIds() {

        Map<String, TermId> omimToMondoMap = new HashMap<>();
        for (TermId mondoId : ontology.allTermIds()) {
            if (mondoId.getPrefix().equals("MONDO")) {
                Optional<TermId> opt = getOmimIdIfPossible(mondoId);
                opt.ifPresent(termId -> omimToMondoMap.put(termId.getValue(), mondoId));
            }

        }
        return omimToMondoMap;
    }


   public List<String> getMappings(String geneSymbol, String omimId) {
        List<String> items = new ArrayList<>();
        if (omimToMondoMap.containsKey(omimId)) {
            TermId mondoId = omimToMondoMap.get(omimId);
            Optional<Term> opt = ontology.termForTermId(mondoId);
            if (opt.isPresent()) {
                Term term = opt.get();
                String label = term.getName();
                items.add(geneSymbol);
                items.add(omimId);
                items.add(mondoId.getValue());
                items.add(label);
                // TODO extract ORDO -- get Ordo if possible
            }
        } else {
            System.err.println("Warning could not find "+ omimId);
        }

        return items;
    }
}
