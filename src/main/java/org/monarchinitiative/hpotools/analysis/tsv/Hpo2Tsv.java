package org.monarchinitiative.hpotools.analysis.tsv;

import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Hpo2Tsv {

    private final File outfile;
    private final Ontology ontology;
    private final TermId targetTermId;


    public Hpo2Tsv(File outfile, Ontology ontology, TermId targetTermId) {
        this.outfile = outfile;
        this.ontology = ontology;
        this.targetTermId = targetTermId;
    }

    public void createTsvFile() {
        List<String> lineList = new ArrayList<>();
        Optional<Term> targetOpt = ontology.termForTermId(targetTermId);
        if (targetOpt.isPresent()) {
            lineList.add(getTsvLine(targetOpt.get()));
        }
        Iterable<TermId> children = ontology.graph().getDescendants(targetTermId);
        for (TermId tid : children) {
            Optional<Term> opt = ontology.termForTermId(tid);
            if (opt.isPresent()) {
                lineList.add(getTsvLine(opt.get()));
            } else {
                throw new PhenolRuntimeException("Could not get term for " + tid.getValue() + " (should never happen)");
            }
        }
        List<String> titleItems = List.of("label", "id", "synonyms", "parents", "children", "definition", "comment", "pmids");
        String title = String.join("\t", titleItems);

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outfile))) {
            bw.write(title + "\n");
            for (String line : lineList) {
                bw.write(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * label	id	synonyms	parents	children	definition	comment	pmids
     * @param term
     * @return
     */
    private String getTsvLine(Term term) {
        String label = term.getName();
        String id = term.id().getValue();
        String synString = term.getSynonyms().stream().map(TermSynonym::getValue).collect(Collectors.joining(", "));
        Set<TermId> parent_id_set = ontology.graph().getParents(term.id());
        Set<String> parent_label_set = new HashSet<>();
        for (TermId parentId : parent_id_set) {
            Optional<String> opt = ontology.getTermLabel(parentId);
            if (opt.isPresent()) {
                parent_label_set.add(opt.get());
            } else {
                throw new PhenolRuntimeException("Could not get label for " + parentId.getValue() + " (should never happen)");
            }
        }
        String parentString = String.join(", ", parent_label_set);
        Set<TermId> child_id_set = ontology.graph().getChildren(term.id());
        Set<String> child_label_set = new HashSet<>();
        for (TermId childId : child_id_set) {
            Optional<String> opt = ontology.getTermLabel(childId);
            if (opt.isPresent()) {
                child_label_set.add(opt.get());
            } else {
                throw new PhenolRuntimeException("Could not get label for " + childId.getValue() + " (should never happen)");
            }
        }
        String childString = String.join(", ", child_label_set);
        String definition = term.getDefinition();
        String comment = term.getComment();
        List<SimpleXref> pmids = term.getPmidXrefs();
        String pmidString;
        if (pmids.isEmpty()) {
            pmidString = "";
        } else {
            pmidString = pmids.stream().map(SimpleXref::getId).collect(Collectors.joining(", "));
        }
        List<String> items = List.of(label, id, synString, parentString, childString, definition, comment, pmidString);
        return String.join("\t", items);
    }
}
