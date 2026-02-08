package org.monarchinitiative.hpotools.cmd;


import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.data.TermSynonym;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@CommandLine.Command(name = "compare",
        mixinStandardHelpOptions = true,
        description = "Compare two versions of HPO")
public class CompareCommand implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CompareCommand.class);

    @CommandLine.Option(names = {"-a", "--hpoA"}, description = "Version A of HPO data")
    String hpoA = "/Users/robin/data/hpo/human-phenotype-ontology-2020-02-27/hp.json";

    @CommandLine.Option(names = {"-b", "--hpoB"})
    String hpoB = "/Users/robin/data/hpo/hp.json";

    Ontology loadHpo(File hpoFile) {
        if (!hpoFile.exists()) {
            throw new PhenolRuntimeException("HPO file does not exist: " + hpoFile);
        }
        return OntologyLoader.loadOntology(hpoFile);
    }


    record Synonym(String label, String category) {
        @Override
        public String toString() {
            return String.format("%s (%s)", label, category.toLowerCase());
        }
    }

    static class DiffRow implements Comparable<DiffRow> {
        private final String termId;
        private final String label;
        private final String category;
        String definitionA = "-";
        String definitionB = "-";
        String synonymA = "-";
        String synonymB = "-";
        String commentA = "-";
        String commentB = "-";

        DiffRow(String termId, String label, String category) {
            this.termId = termId;
            this.label = label;
            this.category = category;
        }

        public static DiffRow newTerm(String termId, String label) {
            return new DiffRow(termId, label, "new term");
        }

        public void setDefinition(TermDifference td) {
            this.definitionA = td.termA.getDefinition();
            this.definitionB = td.termB.getDefinition();
        }
        public void setSynonym(String a, String b) {
            this.synonymA = a;
            this.synonymB = b;
        }
        public void setComment(TermDifference td) {
            this.commentA = td.termA.getComment().replace("\n", " ");
            this.commentB = td.termB.getComment().replace("\n", " ");
        }
        public String getRow() {
            return termId + "\t" + label + "\t" + category + "\t" + definitionA + "\t" + definitionB + "\t" + synonymA + "\t" + synonymB+ "\t" + commentA + "\t" + commentB;
        }
        public static String header() {
            return "TermId" + "\t" + "label" + "\t" + "category" + "\t" + "definitionA"+ "\t" + "definitionB" + "\t" + "synonymA"+ "\t" + "synonymB" + "\t" + "commentA" + "\t" + "commentB";
        }
        @Override
        public int compareTo(DiffRow other) {
            int c = this.category.compareTo(other.category);
            if (c != 0) {
                return c;
            }
            return label.compareTo(other.label);
        }
    }

    static class TermDifference {

        private final Term termA;
        private final Term termB;
        private final Set<Synonym> synonymsA;
        private final Set<Synonym> synonymsB;
        public TermDifference(Term termA, Term termB) {
            this.termA = termA;
            this.termB = termB;
            synonymsA = this.termA.getSynonyms().stream().
                    map(s -> new Synonym(s.getValue(), s.getScope().name())).
                    collect(Collectors.toSet());
            synonymsB = this.termB.getSynonyms().stream().
                    map(s -> new Synonym(s.getValue(), s.getScope().name())).
                    collect(Collectors.toSet());
        }

        public boolean definitionDiffers() {
            return ! termA.getDefinition().equals(termB.getDefinition());
        }

        public boolean synonymDiffers() {
            return ! synonymsA.equals(synonymsB);
        }

        public boolean commentDiffers() {
            return ! termA.getComment().equals(termB.getComment());
        }

        public boolean differs() {
            return definitionDiffers() || synonymDiffers() || commentDiffers();
        }

        public DiffRow difference() {
            DiffRow drow = new DiffRow(termB.id().getValue(), termA.getName(), "differs");

            if (definitionDiffers()) {
                String s = String.format("Term A: %s -- Term B: %s", termA.getDefinition(), termB.getDefinition());
                drow.setDefinition(this);
            }
            if (synonymDiffers()) {
                Set<Synonym> onlyAsynonyms = new HashSet<>(synonymsA);
                synonymsB.forEach(onlyAsynonyms::remove);
                Set<Synonym> onlyBsynonyms = new HashSet<>(synonymsB);
                synonymsA.forEach(onlyBsynonyms::remove);

                String aOnly = onlyAsynonyms.stream().map(Object::toString).collect(Collectors.joining(";"));
                String bOnly = onlyBsynonyms.stream().map(Object::toString).collect(Collectors.joining(";"));
                drow.setSynonym(aOnly, bOnly);
            }
            if (commentDiffers()) {
                drow.setComment(this);
            }
            return drow;
        }

    }


    @Override
    public Integer call() throws Exception {
        File fA = new File(hpoA);
        Ontology hpoA = loadHpo(fA);
        File fB = new File(hpoB);
        Ontology hpoB = loadHpo(fB);
        // Focus on Abnormality of the integument
        TermId targetTermId = TermId.of("HP:0001574");
        Set<TermId> hpoAtermIdSet = hpoA.graph().getDescendantSet(targetTermId);
        Set<TermId> hpoBtermIdSet = hpoB.graph().getDescendantSet(targetTermId);
        Term targetTerm = hpoB.termForTermId(targetTermId).orElseThrow();
        System.out.printf("HPO A relevant terms %d, HPO B relevant terms %d for target term %s\n",
                hpoAtermIdSet.size(), hpoBtermIdSet.size(),
                targetTerm.getName()
        );
        int nsame = 0;
        int ndiff = 0;
        int ndiffdef = 0;
        int ndiffsyn = 0;
        int ndiffcomment = 0;
        List<DiffRow> rows = new ArrayList<>();
            for (TermId termId : hpoBtermIdSet) {
                Term termB   = hpoB.termForTermId(termId).orElseThrow();
                if (hpoAtermIdSet.contains(termId)) {
                    Term termA = hpoA.termForTermId(termId).orElseThrow();
                    TermDifference td = new TermDifference(termA, termB);
                    if (td.differs()) {
                        rows.add(td.difference());
                        ndiff++;
                        if (td.definitionDiffers()) ndiffdef++;
                        if (td.commentDiffers()) ndiffcomment++;
                        if (td.synonymDiffers()) ndiffsyn++;
                    } else {
                        nsame++;
                    }
                } else {
                    DiffRow diffRow = DiffRow.newTerm(termB.id().getValue(), termB.getName());
                    rows.add(diffRow);
                }
            }
        Collections.sort(rows);
        try(BufferedWriter bw = new BufferedWriter(new FileWriter("hpodiff.txt"))){
            bw.write(DiffRow.header() + "\n");
            for (DiffRow row : rows) {
                bw.write(row.getRow()+ "\n");
            }
        } catch (IOException e) {
            throw new PhenolRuntimeException(e);
        }
        System.out.printf("Terms that stayed the same: %d\n", nsame);
        System.out.printf("Terms with changes: %d\n", ndiff);
        System.out.printf("Terms with changed definition: %d\n", ndiffdef);
        System.out.printf("Terms with changed comment: %d\n", ndiffcomment);
        System.out.printf("Terms with changed synonyms: %d\n", ndiffsyn);

        return 0;
    }
}
