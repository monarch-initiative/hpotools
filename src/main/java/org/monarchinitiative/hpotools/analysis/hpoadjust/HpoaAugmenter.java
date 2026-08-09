package org.monarchinitiative.hpotools.analysis.hpoadjust;

import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Adds the phenotype annotations implied by a curated phenopacket cohort to an HPOA file.
 * <p>
 * For every disease covered by the cohort, one annotation line per observed term is written, with the
 * frequency pooled over all cohort publications (ancestor-aware counting) and every contributing PMID
 * listed in the reference field. Existing annotations of the same disease are superseded when they were
 * derived from publications that the cohort re-curates, and pooled into the cohort line when they come
 * from other publications.
 */
public class HpoaAugmenter {

    private static final Logger LOGGER = LoggerFactory.getLogger(HpoaAugmenter.class);

    private final PhenopacketCollection cohort;
    private final String biocuration;

    public HpoaAugmenter(PhenopacketCollection cohort, String biocuration) {
        this.cohort = cohort;
        this.biocuration = biocuration;
    }

    public AugmentedHpoa augment(HpoaFile base) {
        List<TermId> diseases = cohort.diseases();
        Set<TermId> augmentedDiseases = new LinkedHashSet<>(diseases);
        List<HpoaAnnotationLine> retained = new ArrayList<>();
        Map<TermId, List<HpoaAnnotationLine>> existingByDisease = new LinkedHashMap<>();
        for (HpoaAnnotationLine line : base.annotationLines()) {
            if (augmentedDiseases.contains(line.diseaseId()) && line.isPhenotypeAnnotation()) {
                existingByDisease.computeIfAbsent(line.diseaseId(), id -> new ArrayList<>()).add(line);
            } else {
                retained.add(line);
            }
        }

        List<DiseaseAugmentation> report = new ArrayList<>();
        List<HpoaAnnotationLine> generated = new ArrayList<>();
        for (TermId diseaseId : diseases) {
            List<HpoaAnnotationLine> existing = existingByDisease.getOrDefault(diseaseId, List.of());
            report.add(augmentDisease(diseaseId, existing, generated, retained));
        }

        List<HpoaAnnotationLine> lines = new ArrayList<>(retained);
        lines.addAll(generated);
        return new AugmentedHpoa(new HpoaFile(withProvenance(base.headerLines(), report), lines), report);
    }

    private DiseaseAugmentation augmentDisease(TermId diseaseId,
                                               List<HpoaAnnotationLine> existing,
                                               List<HpoaAnnotationLine> generated,
                                               List<HpoaAnnotationLine> retained) {
        Set<TermId> cohortPmids = cohort.pmidsOf(diseaseId);
        CohortCounts counts = cohort.countsOf(diseaseId);
        String diseaseName = diseaseName(diseaseId, existing);

        Map<TermId, HpoaAnnotationLine> poolable = new LinkedHashMap<>();
        int superseded = 0;
        for (HpoaAnnotationLine line : existing) {
            boolean disjoint = Collections.disjoint(line.references(), cohortPmids);
            if (!disjoint) {
                if (!cohortPmids.containsAll(line.references())) {
                    LOGGER.warn("{} {}: dropping annotation with mixed provenance {}",
                            diseaseId, line.hpoId(), line.references());
                }
                superseded++;
            } else if (line.frequencyRatio().isPresent() && !poolable.containsKey(line.hpoId())) {
                poolable.put(line.hpoId(), line);
            } else {
                retained.add(line);
            }
        }

        int added = 0;
        int pooled = 0;
        for (TermId hpoId : observedTerms(diseaseId)) {
            Ratio frequency = new Ratio(counts.countOf(hpoId), counts.cohortSize());
            Set<TermId> references = new LinkedHashSet<>(cohortPmids);
            HpoaAnnotationLine existingLine = poolable.remove(hpoId);
            if (existingLine != null) {
                frequency = frequency.plus(existingLine.frequencyRatio().orElseThrow());
                references.addAll(existingLine.references());
                pooled++;
            } else {
                added++;
            }
            generated.add(HpoaAnnotationLine.phenotypeAnnotation(diseaseId, diseaseName, hpoId,
                    references, frequency, biocuration));
        }
        retained.addAll(poolable.values());

        LOGGER.info("{}: {} cohort phenopackets from {} publications, {} annotations added, {} pooled, {} superseded",
                diseaseId, counts.cohortSize(), cohortPmids.size(), added, pooled, superseded);
        return new DiseaseAugmentation(diseaseId, counts.cohortSize(), List.copyOf(cohortPmids),
                added, pooled, superseded);
    }

    private Set<TermId> observedTerms(TermId diseaseId) {
        Set<TermId> terms = new TreeSet<>();
        cohort.casesOf(diseaseId).forEach(phenopacketCase -> terms.addAll(phenopacketCase.observedTerms()));
        return terms;
    }

    private String diseaseName(TermId diseaseId, List<HpoaAnnotationLine> existing) {
        Optional<String> fromHpoa = existing.stream()
                .map(HpoaAnnotationLine::diseaseName)
                .filter(name -> !name.isEmpty())
                .findFirst();
        return fromHpoa.orElseGet(() -> cohort.diseaseNameOf(diseaseId));
    }

    private static List<String> withProvenance(List<String> headerLines, List<DiseaseAugmentation> report) {
        List<String> header = new ArrayList<>(headerLines);
        String diseases = report.stream()
                .map(augmentation -> augmentation.diseaseId().getValue())
                .reduce((a, b) -> a + "," + b)
                .orElse("none");
        header.add(Math.min(1, header.size()), "#cohort-augmented: " + diseases);
        return header;
    }
}
