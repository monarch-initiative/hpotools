package org.monarchinitiative.hpotools.analysis.word;



import org.apache.poi.xwpf.usermodel.*;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm;
import org.monarchinitiative.phenol.ontology.data.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;



/**
 * This class coordinates the production and output of an RTF file that contains a table with all the terms
 * that emanate from a certain term in the HPO, for instance, abnormal immune physiology. It is intended to produce
 * an RTF file that can be easily distributed as a Word file to collaborators who will enter corrections and additions
 * to a set of HPO terms in a specific area of medicine.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 0.1.2
 */
public class Hpo2Word {
    private static final Logger LOGGER = LoggerFactory.getLogger(Hpo2Word.class);
    /** Number of unique terms we have output in this file. */
    private int n_terms_output;
    /** HPO Ontology object. */
    private final Ontology hpoOntology;
    private static String DEFAULT_START_TERM="HP:0000118";
    /** Term at the top of the subhierarchy to be displayed. */
    private Term startTerm;

    private static final int TABLE_CELL_FONT_SIZE = 10;


    public Hpo2Word(String outfileName, Term term, Ontology hpo) throws  IOException {
        startTerm=term;
        this.hpoOntology = hpo;
        n_terms_output = 0;
        LOGGER.info("HPO version {}", hpo.version().orElse("n/a"));
        LOGGER.info("Create WORD file at {}", outfileName);
        LOGGER.info("Show descendants of {} ({})", term.getName(), term.id());
        XWPFDocument document = new XWPFDocument();
        introductoryParagraph(document);
        LOGGER.info("We will create table");
        writeTablesFromTerm(document, startTerm);
        writeWordFile(document,outfileName);
        LOGGER.info("Output {} HPO terms.", n_terms_output);
    }



    private void introductoryParagraph(XWPFDocument document) {
        XWPFParagraph title = document.createParagraph();
        title.setAlignment(ParagraphAlignment.CENTER);
       // The content of a paragraph needs to be wrapped in an XWPFRun object.
        XWPFRun titleRun = title.createRun();
        titleRun.setText("Human Phenotype Ontology: ");
        titleRun.setColor("009933");
        titleRun.setBold(true);
        titleRun.setFontFamily("Courier");
        titleRun.setFontSize(20);

        XWPFParagraph subTitle = document.createParagraph();
        subTitle.setAlignment(ParagraphAlignment.CENTER);

        XWPFRun subTitleRun = subTitle.createRun();
        String intro = String.format("This document shows terms and definitions for HPO terms that descend from %s (%s)",
                startTerm.getName(), startTerm.id().getValue());
        subTitleRun.setText(intro);
        subTitleRun.setColor("00CC44");
        subTitleRun.setFontFamily("Courier");
        subTitleRun.setFontSize(14);
        subTitleRun.setTextPosition(10);
        subTitleRun.setUnderline(UnderlinePatterns.DOT_DOT_DASH);

        XWPFParagraph para1 = document.createParagraph();
        para1.setAlignment(ParagraphAlignment.BOTH);
        String string1 = "This document is intended to be used to check definitions of HPO terms. " +
                "Note that a term is shown only once in the Table (even if it has multiple parents), " +
                "therefore, please use the HPO website or another browser to check the full hierarchical structure" +
                " of the HPO. Note that the document shows one table for each of the direct children of " +
                " the target term " + startTerm.getName() + " (" + startTerm.id().getValue() + ").";
        XWPFRun para1Run = para1.createRun();
        para1Run.setText(string1);
        XWPFParagraph para2 = document.createParagraph();
        para2.setAlignment(ParagraphAlignment.BOTH);
        String string2 = "Please use Word's track changes feature to show your work. Add comments or write directly " +
                "into the current text.";
        XWPFRun para2Run = para1.createRun();
        para2Run.setText(string2);
    }


    private void writeTablesFromTerm(XWPFDocument document, Term startTerm) {

        Iterable<TermId> children = hpoOntology.graph().getChildren(startTerm.id(), false);
        List<TermId> childTermidList = new ArrayList<>();
        for (TermId tid : children) {
            childTermidList.add(tid);
        }
        XWPFParagraph myParagraph = document.createParagraph();
        myParagraph.setAlignment(ParagraphAlignment.CENTER);

        XWPFRun myParagraphRun = myParagraph.createRun();
        String intro = String.format("%s (%s)\n\n",
                startTerm.getName(), startTerm.id().getValue());
        myParagraphRun.setText(intro);
        myParagraphRun.setColor("00CC44");
        myParagraphRun.setFontFamily("Courier");
        myParagraphRun.setFontSize(12);
        myParagraphRun.setTextPosition(20);
        XWPFRun run2 = myParagraph.createRun();
        run2.setTextPosition(10);
        addTermParagraph(document, startTerm);
        XWPFParagraph myParagraph2 = document.createParagraph();
        XWPFRun runChildren = myParagraph2.createRun();
        runChildren.setText(String.format("%s: %d child terms.\n\n", startTerm.getName(), childTermidList.size()));

        for (TermId childId : childTermidList) {
            Optional<Term> opt = hpoOntology.termForTermId(childId);
            if (opt.isPresent()) {
                writeTableFromTerm(document, opt.get());
            } else {
                throw new PhenolRuntimeException("Could not get term for " + childId.getValue() + " (should never happen)");
            }
        }
    }


    private void addTermParagraph(XWPFDocument document, Term hpoTerm) {
        XWPFParagraph para1 = document.createParagraph();
        para1.setAlignment(ParagraphAlignment.BOTH);
        String header = String.format("\n\n%s (%s)\n", hpoTerm.getName(), hpoTerm.id().getValue());
        XWPFRun para1Run = para1.createRun();
        para1Run.setText(header);
        para1Run.setBold(true);
        XWPFParagraph para2 = document.createParagraph();
        para2.setAlignment(ParagraphAlignment.BOTH);
        XWPFRun para2Run = para2.createRun();
        List<SimpleXref> pmids = hpoTerm.getPmidXrefs();
        if (pmids.isEmpty()) {
            para1Run.setText(String.format("def: %s", hpoTerm.getDefinition()));
        } else {
            String pmidds = pmids.stream().map(SimpleXref::getId).collect(Collectors.joining("; "));
            para2Run.setText(String.format("def: %s (%s)", hpoTerm.getDefinition(), pmidds));
        }
        XWPFParagraph para3 = document.createParagraph();
        para3.setAlignment(ParagraphAlignment.BOTH);
        XWPFRun para3Run = para3.createRun();
        para3Run.setText(String.format("comment: %s\n", hpoTerm.getComment()));
        List<TermSynonym> syns = hpoTerm.getSynonyms();
        if (syns.size()>0) {
            XWPFParagraph para4 = document.createParagraph();
            para4.setAlignment(ParagraphAlignment.BOTH);
            XWPFRun para4Run = para4.createRun();
            String synString = syns.stream().map(TermSynonym::getValue).collect(Collectors.joining("; "));
            para4Run.setText(String.format("synonyms: %s\n", synString));
        }
        XWPFParagraph para5 = document.createParagraph();
        para5.setAlignment(ParagraphAlignment.BOTH);
        XWPFRun para5Run = para3.createRun();
        para5Run.setText("\n\n");
    }


    private void writeWordFile(XWPFDocument document,String outPath) throws IOException {
        FileOutputStream out = new FileOutputStream(outPath);
        document.write(out);
        out.close();
        document.close();
    }







    private List<Term> getChildren(TermId tid) {
        Iterable<TermId> iter = hpoOntology.graph().getChildren(tid, false);
        List<Term> termlist = new ArrayList<>();
        for (TermId childId : iter) {
            Optional<Term> opt = hpoOntology.termForTermId(childId);
            if (opt.isPresent()) {
                termlist.add(opt.get());
            } else {
                // Should never happen
                System.err.printf("[ERROR] Could not identify term for %s.\n", childId.getValue());
            }
        }
        return termlist;
    }


    private void writeTableFromTerm(XWPFDocument document, Term hpoTerm) {
        startTerm=hpoTerm;
        XWPFParagraph para1 = document.createParagraph();
        para1.setAlignment(ParagraphAlignment.BOTH);
        String header = String.format("\n\n%s (%s)\n", hpoTerm.getName(), hpoTerm.id().getValue());
        XWPFRun para1Run = para1.createRun();
        para1Run.setText(header);
        para1Run.setBold(true);
        para1Run.setFontSize(16);
        //addTermParagraph(document, startTerm);
        XWPFTable table = document.createTable();
        //create first row
        XWPFTableRow tableRowOne = table.getRow(0);
        tableRowOne.getCell(0).setText("Level");
        tableRowOne.addNewTableCell().setText("Term");
        tableRowOne.addNewTableCell().setText("Definition");
        tableRowOne.addNewTableCell().setText("Comment");
        tableRowOne.addNewTableCell().setText("Synonyms");
        // the following set keeps us from duplicating branches.
        Set<TermId> previouslyseen=new HashSet<>();
        Stack<Pair<TermId,Integer>> stack = new Stack<>();
        if (hpoTerm==null) {
            LOGGER.error("Attempt to create pretty format HPO Term with null id");
            return;
        }
        stack.push(new Pair<>(hpoTerm.id(),1));
        while (! stack.empty() ) {
            Pair<TermId,Integer> pair = stack.pop();
            n_terms_output++;
            TermId termId=pair.first;
            Integer level=pair.second;
            Optional<Term> opt = hpoOntology.termForTermId(termId);
            if (opt.isEmpty()) {
                System.err.printf("[ERROR] Could not find term for %s (should never happen).", termId.getValue());
                continue;
            }
            Term hterm = opt.get();
            if (! previouslyseen.contains(termId)) {
                // we have not yet output this term!
                XWPFTableRow tableRow = table.createRow();
                XWPFRun run = tableRow.getCell(0).addParagraph().createRun();
                run.setFontSize(TABLE_CELL_FONT_SIZE);
                run.setText(String.valueOf(level));
                String termString = String.format("%s (%s)", hterm.getName(), hterm.id().getValue());
                XWPFRun run1 = tableRow.getCell(1).addParagraph().createRun();
                run1.setFontSize(TABLE_CELL_FONT_SIZE);
                run1.setText(termString);
                String pmids = hterm.getPmidXrefs().stream().map(SimpleXref::toString).collect(Collectors.joining("; "));
                String def;
                if (pmids.length()>2) {
                    def = String.format("%s (%s)", hterm.getDefinition(), pmids);
                } else {
                    def = hterm.getDefinition();
                }
                XWPFRun run2 = tableRow.getCell(2).addParagraph().createRun();
                run2.setFontSize(TABLE_CELL_FONT_SIZE);
                run2.setText(def);
                XWPFRun run3 = tableRow.getCell(3).addParagraph().createRun();
                run3.setFontSize(TABLE_CELL_FONT_SIZE);
                run3.setText(hterm.getComment());
                String synonyms = hterm.getSynonyms().
                        stream().
                        map(TermSynonym::getValue).
                        collect(Collectors.joining("; "));
                XWPFRun run4 = tableRow.getCell(4).addParagraph().createRun();
                run4.setFontSize(TABLE_CELL_FONT_SIZE);
                run4.setText(synonyms);
                previouslyseen.add(termId);
                Iterable<TermId> children = hpoOntology.graph().getChildren(hterm.id(), false);
                for (TermId t:children) {
                    stack.push(new Pair<>(t,level+1));
                }
            } else {
                XWPFTableRow tableRow = table.createRow();
                tableRow.getCell(0).setText(String.valueOf(level));
                tableRow.getCell(1).setText(hterm.getName());
                tableRow.getCell(2).setText("Term previously shown (dependent on another parent)");
                tableRow.getCell(3).setText("");
                tableRow.getCell(4).setText("");
            }

        }

        //run.setText("End of table");
        int[] cols = {2000, 2000, 2000, 2000, 2000};

        for (int i = 0; i < table.getNumberOfRows(); i++) {
            XWPFTableRow row = table.getRow(i);
            int numCells = row.getTableCells().size();
            for (int j = 0; j < numCells; j++) {
                XWPFTableCell cell = row.getCell(j);
                cell.getCTTc().addNewTcPr().addNewTcW().setW(BigInteger.valueOf(2000));
            }
        }
    }



}
