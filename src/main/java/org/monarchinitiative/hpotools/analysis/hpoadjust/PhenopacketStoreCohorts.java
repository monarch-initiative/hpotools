package org.monarchinitiative.hpotools.analysis.hpoadjust;

import com.google.protobuf.util.JsonFormat;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.phenopackets.schema.v2.Phenopacket;
import org.phenopackets.schema.v2.core.PhenotypicFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PhenopacketStoreCohorts implements CohortSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(PhenopacketStoreCohorts.class);

    private record CohortKey(TermId diseaseId, TermId pmid) {
    }

    private final Map<CohortKey, List<Set<TermId>>> patientsByCohort;
    private final Function<TermId, Set<TermId>> ancestorsWithSelf;
    private final Map<CohortKey, CohortCounts> countsCache = new HashMap<>();

    private PhenopacketStoreCohorts(Map<CohortKey, List<Set<TermId>>> patientsByCohort,
                                    Function<TermId, Set<TermId>> ancestorsWithSelf) {
        this.patientsByCohort = patientsByCohort;
        this.ancestorsWithSelf = ancestorsWithSelf;
    }

    public static PhenopacketStoreCohorts load(Path storeDirectory, Function<TermId, Set<TermId>> ancestorsWithSelf) {
        Map<CohortKey, List<Set<TermId>>> patients;
        try (Stream<Path> paths = Files.walk(storeDirectory)) {
            patients = paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .map(PhenopacketStoreCohorts::parseStorePhenopacket)
                    .flatMap(Optional::stream)
                    .collect(Collectors.groupingBy(StorePatient::key,
                            Collectors.mapping(StorePatient::observedTerms, Collectors.toList())));
        } catch (IOException e) {
            throw new PhenolRuntimeException("Could not read phenopacket store at " + storeDirectory + ": " + e.getMessage());
        }
        LOGGER.info("Indexed {} (disease, PMID) cohorts from {}", patients.size(), storeDirectory);
        return new PhenopacketStoreCohorts(patients, ancestorsWithSelf);
    }

    private record StorePatient(CohortKey key, Set<TermId> observedTerms) {
    }

    private static Optional<StorePatient> parseStorePhenopacket(Path path) {
        Phenopacket.Builder builder = Phenopacket.newBuilder();
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            JsonFormat.parser().ignoringUnknownFields().merge(reader, builder);
        } catch (IOException e) {
            LOGGER.debug("Skipping {}: {}", path, e.getMessage());
            return Optional.empty();
        }
        Phenopacket phenopacket = builder.build();
        Optional<TermId> pmid = PhenopacketCase.extractPmid(phenopacket);
        Optional<TermId> diseaseId = PhenopacketCase.extractDiseaseId(phenopacket);
        if (pmid.isEmpty() || diseaseId.isEmpty()) {
            return Optional.empty();
        }
        Set<TermId> observed = phenopacket.getPhenotypicFeaturesList().stream()
                .filter(feature -> !feature.getExcluded())
                .map(PhenotypicFeature::getType)
                .map(type -> TermId.of(type.getId()))
                .collect(Collectors.toSet());
        return Optional.of(new StorePatient(new CohortKey(diseaseId.get(), pmid.get()), observed));
    }

    @Override
    public Optional<CohortCounts> lookup(TermId diseaseId, TermId pmid) {
        CohortKey key = new CohortKey(diseaseId, pmid);
        List<Set<TermId>> patients = patientsByCohort.get(key);
        if (patients == null) {
            return Optional.empty();
        }
        return Optional.of(countsCache.computeIfAbsent(key,
                k -> CohortCounts.of(patients, ancestorsWithSelf)));
    }
}
