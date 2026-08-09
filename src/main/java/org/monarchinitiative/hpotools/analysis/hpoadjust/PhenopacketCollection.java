package org.monarchinitiative.hpotools.analysis.hpoadjust;

import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PhenopacketCollection implements CohortSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(PhenopacketCollection.class);

    private final List<PhenopacketCase> cases;
    private final Function<TermId, Set<TermId>> ancestorsWithSelf;
    private final Map<TermId, CohortCounts> diseaseCounts = new HashMap<>();
    private final Map<TermId, Map<TermId, CohortCounts>> pmidCounts = new HashMap<>();

    private PhenopacketCollection(List<PhenopacketCase> cases, Function<TermId, Set<TermId>> ancestorsWithSelf) {
        this.cases = List.copyOf(cases);
        this.ancestorsWithSelf = ancestorsWithSelf;
    }

    public static PhenopacketCollection of(List<PhenopacketCase> cases, Function<TermId, Set<TermId>> ancestorsWithSelf) {
        return new PhenopacketCollection(cases, ancestorsWithSelf);
    }

    public static PhenopacketCollection load(Path path, Function<TermId, Set<TermId>> ancestorsWithSelf) {
        List<PhenopacketCase> cases = new ArrayList<>();
        for (Path file : jsonFiles(path)) {
            try {
                cases.add(PhenopacketCase.fromFile(file));
            } catch (PhenolRuntimeException e) {
                LOGGER.debug("Skipping {}: {}", file, e.getMessage());
            }
        }
        LOGGER.info("Loaded {} phenopackets from {}", cases.size(), path);
        return new PhenopacketCollection(cases, ancestorsWithSelf);
    }

    private static List<Path> jsonFiles(Path path) {
        if (Files.isRegularFile(path)) {
            return List.of(path);
        }
        if (!Files.isDirectory(path)) {
            throw new PhenolRuntimeException("Not a file or directory: " + path);
        }
        try (Stream<Path> paths = Files.walk(path)) {
            return paths.filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new PhenolRuntimeException("Could not read " + path + ": " + e.getMessage());
        }
    }

    public List<PhenopacketCase> cases() {
        return cases;
    }

    public boolean isEmpty() {
        return cases.isEmpty();
    }

    public List<TermId> diseases() {
        return cases.stream()
                .map(PhenopacketCase::diseaseId)
                .distinct()
                .sorted()
                .toList();
    }

    public List<PhenopacketCase> casesOf(TermId diseaseId) {
        return cases.stream()
                .filter(phenopacketCase -> phenopacketCase.diseaseId().equals(diseaseId))
                .toList();
    }

    public Set<TermId> pmidsOf(TermId diseaseId) {
        return casesOf(diseaseId).stream()
                .map(PhenopacketCase::pmid)
                .sorted()
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public String diseaseNameOf(TermId diseaseId) {
        return casesOf(diseaseId).stream()
                .map(PhenopacketCase::diseaseName)
                .filter(name -> !name.isEmpty())
                .findFirst()
                .orElse(diseaseId.getValue());
    }

    public CohortCounts countsOf(TermId diseaseId) {
        return diseaseCounts.computeIfAbsent(diseaseId,
                id -> CohortCounts.of(termSets(casesOf(id)), ancestorsWithSelf));
    }

    @Override
    public Optional<CohortCounts> lookup(TermId diseaseId, TermId pmid) {
        Map<TermId, CohortCounts> byPmid = pmidCounts.computeIfAbsent(diseaseId, id -> new HashMap<>());
        if (byPmid.containsKey(pmid)) {
            return Optional.ofNullable(byPmid.get(pmid));
        }
        List<PhenopacketCase> matching = casesOf(diseaseId).stream()
                .filter(phenopacketCase -> phenopacketCase.pmid().equals(pmid))
                .toList();
        CohortCounts counts = matching.isEmpty()
                ? null
                : CohortCounts.of(termSets(matching), ancestorsWithSelf);
        byPmid.put(pmid, counts);
        return Optional.ofNullable(counts);
    }

    private static List<Set<TermId>> termSets(List<PhenopacketCase> cases) {
        return cases.stream()
                .map(PhenopacketCase::observedTerms)
                .map(Set::copyOf)
                .collect(Collectors.toList());
    }
}
