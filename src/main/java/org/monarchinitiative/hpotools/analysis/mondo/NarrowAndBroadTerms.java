package org.monarchinitiative.hpotools.analysis.mondo;

import org.monarchinitiative.hpotools.analysis.OntologyTerm;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.Dbxref;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class NarrowAndBroadTerms {
    private static final Logger LOGGER = LoggerFactory.getLogger(NarrowAndBroadTerms.class);
    private final Ontology mondo;

    private final Map<TermId, OntologyTerm> broadTerms ;
    private final Map<TermId, OntologyTerm> toNarrowMap = new HashMap<>();
    private final Map<TermId, TermId> omimToMondoMap = new HashMap<>();
    private final Map<TermId, TermId> narrowToBroadMap = new HashMap<>();

    public NarrowAndBroadTerms(Ontology mondo) {
        this.mondo = mondo;
        broadTerms = new HashMap<>();
        initBroadTerms();
        initNarrowTerms();
        initNarrowToBroadMap();
    }

    private void initNarrowToBroadMap() {
        for (TermId narrowTid : toNarrowMap.keySet()) {
            var ancestors = mondo.getAncestorTermIds(narrowTid);
            for (TermId ancestorTid : ancestors) {
                if (broadTerms.containsKey(ancestorTid)) {
                    narrowToBroadMap.put(narrowTid, ancestorTid);
                    break; // leave the inner for loop
                    // we have found an ancestor broiad term
                    // it should be very rare that there are more than one
                    // because of the way we construct the maps in this class
                    // if there are don't overworry, it is for testing and there is not one definition of what
                    // an ideal broad term is!
                }
            }
        }
        LOGGER.info("NarrowToBroadMap: {} items", narrowToBroadMap.size());
    }


    private void initNarrowTerms() {
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

    public void initBroadTerms() {
        /*genetic otorhinolaryngologic disease vs hereditary otorhinolaryngologic disease
        inborn errors of immunity vs inborn error of immunity
        genetic epidermal appendage anomaly vs hereditary epidermal appendage anomaly
        inborn errors of immunity vs inborn error of immunity
        genetic epidermal appendage anomaly vs hereditary epidermal appendage anomaly
        inborn errors of immunity vs inborn error of immunity
        inborn errors of immunity vs inborn error of immunity
        inborn errors of immunity vs inborn error of immunity
        broadTerms.put(TermId.of({k}), new OntologyTerm(TermId.of({k}), {v}) */
        broadTerms.put(TermId.of("MONDO:0024237"), new OntologyTerm(TermId.of("MONDO:0024237"), "inherited neurodegenerative disorder"));
        broadTerms.put(TermId.of("MONDO:0015469"), new OntologyTerm(TermId.of("MONDO:0015469"), "craniosynostosis"));
        broadTerms.put(TermId.of("MONDO:0018230"), new OntologyTerm(TermId.of("MONDO:0018230"), "skeletal dysplasia"));
        broadTerms.put(TermId.of("MONDO:0003778"), new OntologyTerm(TermId.of("MONDO:0003778"), "inborn error of immunity"));
        broadTerms.put(TermId.of("MONDO:0019507"), new OntologyTerm(TermId.of("MONDO:0019507"), "amelogenesis imperfecta"));
        broadTerms.put(TermId.of("MONDO:0018634"), new OntologyTerm(TermId.of("MONDO:0018634"), "hereditary amyloidosis"));
        broadTerms.put(TermId.of("MONDO:0001713"), new OntologyTerm(TermId.of("MONDO:0001713"), "inherited aplastic anemia"));
        broadTerms.put(TermId.of("MONDO:0018751"), new OntologyTerm(TermId.of("MONDO:0018751"), "hereditary otorhinolaryngologic disease"));
        broadTerms.put(TermId.of("MONDO:0021060"), new OntologyTerm(TermId.of("MONDO:0021060"), "RASopathy"));
        broadTerms.put(TermId.of("MONDO:0019118"), new OntologyTerm(TermId.of("MONDO:0019118"), "inherited retinal dystrophy"));
        broadTerms.put(TermId.of("MONDO:0023603"), new OntologyTerm(TermId.of("MONDO:0023603"), "hereditary disorder of connective tissue"));
        broadTerms.put(TermId.of("MONDO:0000508"), new OntologyTerm(TermId.of("MONDO:0000508"), "syndromic intellectual disability"));
        broadTerms.put(TermId.of("MONDO:0015356"), new OntologyTerm(TermId.of("MONDO:0015356"), "hereditary neoplastic syndrome"));
        broadTerms.put(TermId.of("MONDO:0019214"), new OntologyTerm(TermId.of("MONDO:0019214"), "inborn carbohydrate metabolic disorder"));
        broadTerms.put(TermId.of("MONDO:0100545"), new OntologyTerm(TermId.of("MONDO:0100545"), "hereditary neurological disease"));
        broadTerms.put(TermId.of("MONDO:0021026"), new OntologyTerm(TermId.of("MONDO:0021026"), "hereditary epidermal appendage anomaly"));
        broadTerms.put(TermId.of("MONDO:0005395"), new OntologyTerm(TermId.of("MONDO:0005395"), "movement disorder"));
        broadTerms.put(TermId.of("MONDO:0100310"), new OntologyTerm(TermId.of("MONDO:0100310"), "hereditary cerebellar ataxia"));
        broadTerms.put(TermId.of("MONDO:0005559"), new OntologyTerm(TermId.of("MONDO:0005559"), "neurodegenerative disease"));
        broadTerms.put(TermId.of("MONDO:0019952"), new OntologyTerm(TermId.of("MONDO:0019952"), "congenital myopathy"));
        broadTerms.put(TermId.of("MONDO:0100546"), new OntologyTerm(TermId.of("MONDO:0100546"), "hereditary neuromuscular disease"));
        broadTerms.put(TermId.of("MONDO:0100191"), new OntologyTerm(TermId.of("MONDO:0100191"), "inherited kidney disorder"));
        broadTerms.put(TermId.of("MONDO:0015358"), new OntologyTerm(TermId.of("MONDO:0015358"), "hereditary motor and sensory neuropathy"));
        broadTerms.put(TermId.of("MONDO:0018751"), new OntologyTerm(TermId.of("MONDO:0018751"), "hereditary otorhinolaryngologic disease"));
        broadTerms.put(TermId.of("MONDO:0019042"), new OntologyTerm(TermId.of("MONDO:0019042"), "multiple congenital anomalies/dysmorphic syndrome"));
        broadTerms.put(TermId.of("MONDO:0000009"), new OntologyTerm(TermId.of("MONDO:0000009"), "inherited bleeding disorder, platelet-type"));
        broadTerms.put(TermId.of("MONDO:0042983"), new OntologyTerm(TermId.of("MONDO:0042983"), "neurocutaneous syndrome"));
        broadTerms.put(TermId.of("MONDO:0019751"), new OntologyTerm(TermId.of("MONDO:0019751"), "autoinflammatory syndrome"));
        broadTerms.put(TermId.of("MONDO:0015225"), new OntologyTerm(TermId.of("MONDO:0015225"), "arthrogryposis syndrome"));
        broadTerms.put(TermId.of("MONDO:0015547"), new OntologyTerm(TermId.of("MONDO:0015547"), "hereditary dementia"));
        broadTerms.put(TermId.of("MONDO:0100309"), new OntologyTerm(TermId.of("MONDO:0100309"), "hereditary ataxia"));
        broadTerms.put(TermId.of("MONDO:0019052"), new OntologyTerm(TermId.of("MONDO:0019052"), "inborn errors of metabolism"));
        broadTerms.put(TermId.of("MONDO:0016165"), new OntologyTerm(TermId.of("MONDO:0016165"), "hereditary hypoparathyroidism"));
        broadTerms.put(TermId.of("MONDO:0015514"), new OntologyTerm(TermId.of("MONDO:0015514"), "hereditary endocrine growth disease"));
        broadTerms.put(TermId.of("MONDO:0015161"), new OntologyTerm(TermId.of("MONDO:0015161"), "multiple congenital anomalies/dysmorphic syndrome without intellectual disability"));
        broadTerms.put(TermId.of("MONDO:0100241"), new OntologyTerm(TermId.of("MONDO:0100241"), "inherited thrombocytopenia"));
        broadTerms.put(TermId.of("MONDO:0019303"), new OntologyTerm(TermId.of("MONDO:0019303"), "premature aging syndrome"));
        broadTerms.put(TermId.of("MONDO:0005497"), new OntologyTerm(TermId.of("MONDO:0005497"), "bone development disease"));
        broadTerms.put(TermId.of("MONDO:0044348"), new OntologyTerm(TermId.of("MONDO:0044348"), "hemoglobinopathy"));
        broadTerms.put(TermId.of("MONDO:0019287"), new OntologyTerm(TermId.of("MONDO:0019287"), "ectodermal dysplasia syndrome"));
        broadTerms.put(TermId.of("MONDO:0024239"), new OntologyTerm(TermId.of("MONDO:0024239"), "congenital anomaly of cardiovascular system"));
        broadTerms.put(TermId.of("MONDO:0037940"), new OntologyTerm(TermId.of("MONDO:0037940"), "inherited auditory system disease"));
        broadTerms.put(TermId.of("MONDO:0020127"), new OntologyTerm(TermId.of("MONDO:0020127"), "hereditary peripheral neuropathy"));
        broadTerms.put(TermId.of("MONDO:0700092"), new OntologyTerm(TermId.of("MONDO:0700092"), "neurodevelopmental disorder"));
        broadTerms.put(TermId.of("MONDO:0005308"), new OntologyTerm(TermId.of("MONDO:0005308"), "ciliopathy"));
        broadTerms.put(TermId.of("MONDO:0002320"), new OntologyTerm(TermId.of("MONDO:0002320"), "congenital nervous system disorder"));
        broadTerms.put(TermId.of("MONDO:0001149"), new OntologyTerm(TermId.of("MONDO:0001149"), "microcephaly"));
        broadTerms.put(TermId.of("MONDO:0015286"), new OntologyTerm(TermId.of("MONDO:0015286"), "congenital disorder of glycosylation"));
        broadTerms.put(TermId.of("MONDO:0001071"), new OntologyTerm(TermId.of("MONDO:0001071"), "intellectual disability"));
        broadTerms.put(TermId.of("MONDO:0015159"), new OntologyTerm(TermId.of("MONDO:0015159"), "multiple congenital anomalies/dysmorphic syndrome-intellectual disability"));
        broadTerms.put(TermId.of("MONDO:0003689"), new OntologyTerm(TermId.of("MONDO:0003689"), "familial hemolytic anemia"));
        broadTerms.put(TermId.of("MONDO:0100237"), new OntologyTerm(TermId.of("MONDO:0100237"), "inherited cutis laxa"));
        broadTerms.put(TermId.of("MONDO:0015364"), new OntologyTerm(TermId.of("MONDO:0015364"), "hereditary sensory and autonomic neuropathy"));
        broadTerms.put(TermId.of("MONDO:0018102"), new OntologyTerm(TermId.of("MONDO:0018102"), "corneal dystrophy"));
        broadTerms.put(TermId.of("MONDO:0023603"), new OntologyTerm(TermId.of("MONDO:0023603"), "hereditary disorder of connective tissue"));
        broadTerms.put(TermId.of("MONDO:0000509"), new OntologyTerm(TermId.of("MONDO:0000509"), "non-syndromic intellectual disability"));
        broadTerms.put(TermId.of("MONDO:0000688"), new OntologyTerm(TermId.of("MONDO:0000688"), "inborn organic aciduria"));
        broadTerms.put(TermId.of("MONDO:0020124"), new OntologyTerm(TermId.of("MONDO:0020124"), "neuromuscular junction disease"));
        broadTerms.put(TermId.of("MONDO:0002243"), new OntologyTerm(TermId.of("MONDO:0002243"), "hemorrhagic disease"));
        broadTerms.put(TermId.of("MONDO:0017755"), new OntologyTerm(TermId.of("MONDO:0017755"), "inborn disorder of bilirubin metabolism"));
        broadTerms.put(TermId.of("MONDO:0100547"), new OntologyTerm(TermId.of("MONDO:0100547"), "cardiogenetic disease"));
        broadTerms.put(TermId.of("MONDO:0021026"), new OntologyTerm(TermId.of("MONDO:0021026"), "hereditary epidermal appendage anomaly"));
        broadTerms.put(TermId.of("MONDO:0000824"), new OntologyTerm(TermId.of("MONDO:0000824"), "congenital diarrhea"));
        broadTerms.put(TermId.of("MONDO:0019064"), new OntologyTerm(TermId.of("MONDO:0019064"), "hereditary spastic paraplegia"));
        broadTerms.put(TermId.of("MONDO:0019054"), new OntologyTerm(TermId.of("MONDO:0019054"), "congenital limb malformation"));
        broadTerms.put(TermId.of("MONDO:0020121"), new OntologyTerm(TermId.of("MONDO:0020121"), "muscular dystrophy"));
        broadTerms.put(TermId.of("MONDO:0004868"), new OntologyTerm(TermId.of("MONDO:0004868"), "biliary tract disorder"));

        broadTerms.put(TermId.of("MONDO:0016624"), new OntologyTerm(TermId.of("MONDO:0016624"), "inherited deficiency anemia"));
        broadTerms.put(TermId.of("MONDO:0019356"), new OntologyTerm(TermId.of("MONDO:0019356"), "urogenital tract malformation"));
        int goodMapping = 0;
        for (var e: broadTerms.entrySet()) {
            TermId termId = e.getKey();
            String label = e.getValue().label();
            if (! mondo.containsTerm(termId)) {
                LOGGER.error("Could not find Mondo TermId " + termId.getValue() + " for label " + label);
                System.err.println("Could not find Mondo TermId " + termId.getValue() + " for label " + label);
                continue;
            }
            Optional<Term> opt = mondo.termForTermId(termId);
            if (opt.isEmpty()) {
                LOGGER.error("Could not find term for Mondo TermId " + termId.getValue());
                System.err.println("Could not find term for Mondo TermId " + termId.getValue());
                continue;
            }
            Term term = opt.get();
            if (! term.getName().equals(label)) {
                String errMsg = String.format("Label \"%s\" does not match term label \"%s\"", label, term.getName());
                LOGGER.error(errMsg);
                System.err.println(errMsg);
               continue;
            }
            goodMapping++;
        }
        LOGGER.info("Got {} mappings", goodMapping);
    }


    public boolean containsOmim(TermId omimId) {
        return omimToMondoMap.containsKey(omimId);
    }

    public TermId omimToMondoId(TermId omimId) {
        return omimToMondoMap.get(omimId);
    }

    public boolean containsNarrowTermId(TermId mondoId) {
        return narrowToBroadMap.containsKey(mondoId);
    }

    public TermId getNarrowTermId(TermId mondoId) {
        return narrowToBroadMap.get(mondoId);
    }

    /**
     * Convenience method to use if we know a mondo term is valid and just want its label
     */
    public String getMondoLabel(TermId mondoId) {
        Optional<Term> opt = mondo.termForTermId(mondoId);
        if (opt.isEmpty()) {
            throw new PhenolRuntimeException("Could not find Mondo TermId " + mondoId.getValue());
        }
        return opt.get().getName();
    }

    public boolean containsNarrowToBroad(TermId mondoId) {
        return narrowToBroadMap.containsKey(mondoId);
    }

    public TermId getBroadForNarrow(TermId mondoId) {
        return narrowToBroadMap.get(mondoId);
    }
}
