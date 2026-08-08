package org.monarchinitiative.hpotools.analysis.hpoadjust;

import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class HpoaFile {

    private final List<String> headerLines;
    private final List<HpoaAnnotationLine> annotationLines;

    HpoaFile(List<String> headerLines, List<HpoaAnnotationLine> annotationLines) {
        this.headerLines = List.copyOf(headerLines);
        this.annotationLines = List.copyOf(annotationLines);
    }

    public static HpoaFile parse(Path path) {
        List<String> header = new ArrayList<>();
        List<HpoaAnnotationLine> annotations = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.startsWith("database_id")) {
                    header.add(line);
                } else if (!line.isBlank()) {
                    annotations.add(HpoaAnnotationLine.of(line));
                }
            }
        } catch (IOException e) {
            throw new PhenolRuntimeException("Could not read HPOA file at " + path + ": " + e.getMessage());
        }
        return new HpoaFile(header, annotations);
    }

    public boolean hasPmidEvidence(TermId diseaseId, TermId pmid) {
        return annotationLines.stream()
                .anyMatch(line -> line.diseaseId().equals(diseaseId) && line.cites(pmid));
    }

    public List<HpoaAnnotationLine> linesCiting(TermId pmid) {
        return annotationLines.stream()
                .filter(line -> line.cites(pmid))
                .toList();
    }

    public HpoaFile withoutPmid(TermId pmid) {
        List<HpoaAnnotationLine> retained = annotationLines.stream()
                .filter(line -> !line.cites(pmid))
                .toList();
        return new HpoaFile(headerLines, retained);
    }

    public long phenotypeLineCount(TermId diseaseId) {
        return annotationLines.stream()
                .filter(line -> line.diseaseId().equals(diseaseId))
                .filter(HpoaAnnotationLine::isPhenotypeAnnotation)
                .count();
    }

    public List<HpoaAnnotationLine> annotationLines() {
        return annotationLines;
    }

    public List<String> headerLines() {
        return headerLines;
    }

    public int annotationCount() {
        return annotationLines.size();
    }

    public void write(Path path) {
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            for (String headerLine : headerLines) {
                writer.write(headerLine);
                writer.newLine();
            }
            for (HpoaAnnotationLine line : annotationLines) {
                writer.write(line.raw());
                writer.newLine();
            }
        } catch (IOException e) {
            throw new PhenolRuntimeException("Could not write HPOA file to " + path + ": " + e.getMessage());
        }
    }
}
