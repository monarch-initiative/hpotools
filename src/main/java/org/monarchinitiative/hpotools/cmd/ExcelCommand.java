package org.monarchinitiative.hpotools.cmd;

import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

import java.io.*;

import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.data.TermSynonym;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;


/**
 * A command class to coordinate the production and output of an RTF file containing information about
 * a subhierarchy of the HPO.
 *
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 0.1.0
 */
@CommandLine.Command(name = "excel",
        mixinStandardHelpOptions = true,
        description = "Output subontology as excel file (experimental)")
public class ExcelCommand extends HPOCommand implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExcelCommand.class);

    private final String DEFAULT_OUTPUTNAME = "hpotest.xlsx";
    private final String[] headers = {
            "termLabel", "rootTerm", "Melvin Reviewed", "Include In Abnormal Survey",
            "Include In Survey Parasomnia", "Include in Survey Clinical Sleep", "PARENTS",
            "CHILDREN", "id", "SYNONYM", "def", "termComment", "PMID",
            "SUGGESTED CHILDREN", "parentId", "parentHpoLink", "childrenHpoLink",
            "hierarchyText", "hpoLink", "synAndTypeDoNotDelete", "CONTRIBUTORS",
            "COMPLETED BY", "numCurators", "numSynonyms", "SUGGESTED PARENT",
            "labelAndId", "feedback", "formSummary", "parentsDoNotDelete",
            "childrenDoNotDelete", "labelTimestamp", "defTimestamp", "synonymsTimestamp",
            "termCommentTimestamp", "allTermAttributesTimestamp", "formLink",
            "nextTerm", "nextTermLabel", "termRecordId", "grandparents",
            "parentLabelStringImport", "synonymStringImport", "pmidStrinImport",
            "CONTRIBUTIONS 2", "isFinalTerm", "Provenance"
    };

    private final String term_template_url = "https://hpo.jax.org/browse/term/%s";

    /**
     * The command will create tables for terms emanating from this term. Default: Abnormal social behavior HP:0012433
     */

    @CommandLine.Option(names = {"--startterm", "--st"})
    private String startTermId = "HP:0012433";

    @CommandLine.Option(names = {"-o", "--out"})
    private String outfilename = null;


    public ExcelCommand() {

    }


    public Term getTerm(Ontology hpOntology, String termId) {
        System.out.println("Getting term " + termId);
        TermId termOpt = TermId.of(termId);
        return hpOntology.termForTermId(termOpt)
                .orElseThrow(() -> new IllegalArgumentException("No HPO term found for " + termId));
    }

    @Override
    public Integer call() {

        Ontology hpOntology = getHpOntology();
        Term targetTerm = getTerm(hpOntology, startTermId);

        if (outfilename == null) {
            String name = targetTerm.getName().replaceAll(" ", "_");
            String id = targetTerm.id().getValue().replaceAll(" ", "_");
            outfilename = String.format("%s_%s.xlsx", name, id);
        }
        LOGGER.info("running EXCEL command from {}", startTermId, getHpoJsonFile().getAbsolutePath());
        try {
            try (OutputStream os = Files.newOutputStream(Paths.get(outfilename));

                 Workbook wb = new Workbook(os, "TermMaster", "1.0")) {
                Worksheet ws = wb.newWorksheet("Terms");
                System.out.println("Start!!!!");
                Iterable<TermId> childrenIter = hpOntology.graph().extendWithChildren(targetTerm.id(), false);
                List<TermId> termList = new ArrayList<>();
                childrenIter.forEach(termList::add);

                termList.addFirst(targetTerm.id());
                System.out.println("Iterating");
                System.out.println(termList.size());
                for (int i = 0; i < termList.size(); i++) {
                    TermId termId = termList.get(i);
                    System.out.println(termId);
                    write_row(i + 1, termId, hpOntology, ws);
                }

            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return 1;
        }
        return 0;
    }

    public void write_row(int row_idx, TermId term_id, Ontology ontology, Worksheet ws) {
        System.out.println("Getting term for " + term_id);
        Term targetTerm = getTerm(ontology, term_id.toString());
        System.out.println("Got term" + row_idx);
        Iterable<TermId> children = ontology.graph().extendWithChildren(targetTerm.id(), false);
        Set<TermId> parent_term_ids = ontology.getAncestorTermIds(targetTerm.id());
        System.out.println("Writing row: " + row_idx + " for term: " + term_id);

        for (int col = 0; col < headers.length; col++) {
            ws.value(0, col, headers[col]);
            ws.style(0, col).bold().fillColor("366092").fontColor("FFFFFF").set();
        }

        ws.value(row_idx, 0, targetTerm.getName());
        ws.value(row_idx, 1, "unchecked");
        ws.style(row_idx, 1).bold().fillColor("CC0000").fontColor("FFFFFF").set();
        ws.value(row_idx, 2, "unchecked");
        ws.style(row_idx, 2).bold().fillColor("CC0000").fontColor("FFFFFF").set();
        ws.value(row_idx, 3, "unchecked");
        ws.style(row_idx, 3).bold().fillColor("CC0000").fontColor("FFFFFF").set();
        ws.value(row_idx, 4, "unchecked");
        ws.style(row_idx, 4).bold().fillColor("CC0000").fontColor("FFFFFF").set();
        ws.value(row_idx, 5, "unchecked");
        ws.style(row_idx, 5).bold().fillColor("CC0000").fontColor("FFFFFF").set();


        StringJoiner parent_labels = new StringJoiner(", ");
        StringJoiner parent_ids = new StringJoiner(", ");
        StringJoiner parent_hpo_links = new StringJoiner(", ");
        for (TermId termId : parent_term_ids) {
            Optional<Term> parent_term_opt = ontology.termForTermId(termId);
            if (parent_term_opt.isPresent()) {
                Term parent_term = parent_term_opt.get();
                parent_labels.add(parent_term.getName());
                parent_ids.add(parent_term.id().getValue());
                parent_hpo_links.add(String.format(term_template_url, parent_term.id().getValue()));
            }

        }

        ws.value(row_idx, 6, parent_labels.toString());
        ws.value(row_idx, 28, parent_labels.toString());

        ws.value(row_idx, 14, parent_ids.toString());
        ws.value(row_idx, 15, parent_hpo_links.toString());


        StringJoiner child_labels = new StringJoiner(", ");
        StringJoiner child_links = new StringJoiner(", ");

        for (TermId termId : children) {
            Optional<Term> child_term_opt = ontology.termForTermId(termId);
            if (child_term_opt.isPresent()) {
                Term child_term = child_term_opt.get();
                child_labels.add(child_term.getName());
                child_links.add(String.format(term_template_url, child_term.id().getValue()));
            }
        }
        ws.value(row_idx, 7, child_labels.toString());
        ws.value(row_idx, 29, child_labels.toString());
        ws.value(row_idx, 16, child_links.toString());

        ws.value(row_idx, 8, targetTerm.id().getValue());


        StringJoiner synonyms_terms = new StringJoiner(", ");

        for (TermSynonym syn : targetTerm.getSynonyms()) {
            synonyms_terms.add(syn.getValue());
        }
        ws.value(row_idx, 9, synonyms_terms.toString());

        ws.value(row_idx, 10, targetTerm.getDefinition());
        ws.value(row_idx, 11, targetTerm.getComment());
        ws.value(row_idx, 18, String.format(term_template_url, targetTerm.id().getValue()));
        ws.value(row_idx, 25, targetTerm.getName() + " (" + targetTerm.id().getValue() + ")");
    }
}

