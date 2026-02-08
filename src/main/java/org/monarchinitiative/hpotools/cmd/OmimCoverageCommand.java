package org.monarchinitiative.hpotools.cmd;

import picocli.CommandLine;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@CommandLine.Command(name = "coverage",
        mixinStandardHelpOptions = true,
        description = "Check OMIM coverage.")
public class OmimCoverageCommand implements Callable<Integer> {

    @CommandLine.Option(names = {"--hpoa"}, required = true, description = "path to HPOA directory")
    private String hpoaDir;


    @Override
    public Integer call() throws Exception {
        System.out.println(hpoaDir);
        Path start = Paths.get(hpoaDir);
        Set<String> omimSet = new HashSet<>();
        try (Stream<Path> stream = Files.walk(start)) {
            omimSet = stream
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    // Ensure the file follows the "prefix-id.ext" pattern
                    .filter(name -> name.contains("-") && name.contains("."))
                    .map(name -> name.substring(0, name.lastIndexOf('.')).replace("-", ":"))
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Extracted " + omimSet.size() + " unique IDs.");
        Path omitted = Path.of(hpoaDir, "omit-list.txt");
        Set<String> omittedOmims = readOmitted(omitted);
        Set<String> medgen = readMedgen();
        int i = 0;
        for (String mg : medgen)
            outputCoverage( medgen, omimSet, omittedOmims);
        return 0;
    }
    void outputCoverage(Set<String> medgen, Set<String> hpoa, Set<String> omitted) {
        // 1. Filter the set using a stream (Medgen minus HPOA and Omitted)
        Set<String> forOutput = medgen.stream()
                .filter(mg -> !hpoa.contains(mg))
                .filter(mg -> !omitted.contains(mg))
                .collect(Collectors.toSet());

        String outfilename = "coverage.txt";

        // 2. Use try-with-resources to ensure the file closes properly
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(outfilename), StandardCharsets.UTF_8)) {
            for (String item : forOutput) {
                String[] fields = item.split(":");

                if (fields.length >= 2) {
                    String omimId = fields[1];
                    // 3. Structured string formatting for Markdown checkboxes
                    String line = String.format("- [ ] [%s](https://omim.org/entry/%s)%n", item, omimId);                    bw.write(line);
                }
            }
        } catch (IOException e) {
            // Log the actual error message for debugging
            throw new RuntimeException("Failed to write coverage file: " + e.getMessage(), e);
        }
    }


    Set<String> readOmitted(Path omitted) {
        Set<String> omittedOmims = new HashSet<>();
        try (BufferedReader br = Files.newBufferedReader(omitted)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("OMIM:")) {
                    String[] fields = line.split("\\s");
                    omittedOmims.add(fields[0]);
                }
            }
            System.out.println("Extracted " + omittedOmims.size() + " omitted IDs.");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return omittedOmims;
    }

    Set<String> readMedgen() {
        Set<String> omims = new HashSet<>();
        Path medgen = Path.of("data", "mim2gene_medgen");
        try (BufferedReader br = Files.newBufferedReader(medgen)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("susceptibility")) {continue;}
                if (line.contains("nondisease")) {continue;}
                String[] fields = line.split("\t");
                if (fields.length >= 3 && ! fields[1].equals("-") && fields[2].equals("phenotype")) {
                    String omimId = String.format("OMIM:%s", fields[0]);
                    omims.add(omimId);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Extracted " + omims.size() + " OMIM IDs.");
        return omims;

    }

}
