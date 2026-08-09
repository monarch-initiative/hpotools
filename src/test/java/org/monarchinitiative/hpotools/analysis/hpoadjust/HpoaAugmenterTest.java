package org.monarchinitiative.hpotools.analysis.hpoadjust;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HpoaAugmenterTest {

    private static final TermId APDS = TermId.of("OMIM:615513");
    private static final TermId RECURRENT_INFECTIONS = TermId.of("HP:0005403");
    private static final TermId SHARED_TERM = TermId.of("HP:0002205");
    private static final TermId GROWTH_DELAY = TermId.of("HP:0001510");

    private static HpoaFile base;

    @BeforeAll
    public static void init() throws Exception {
        base = HpoaFile.parse(Path.of(HpoaAugmenterTest.class.getResource("/hpoadjust/small_phenotype.hpoa").toURI()));
    }

    private static PhenopacketCase patient(String id, String pmid, TermId... terms) {
        return new PhenopacketCase(id, APDS, "Immunodeficiency 14", TermId.of(pmid),
                Set.of(terms), Path.of(id + ".json"));
    }

    private static HpoaAugmenter augmenterOf(List<PhenopacketCase> cases) {
        return new HpoaAugmenter(PhenopacketCollection.of(cases, Set::of), "HPO:test[2026-08-09]");
    }

    private static Optional<HpoaAnnotationLine> lineFor(HpoaFile hpoa, TermId hpoId) {
        return hpoa.annotationLines().stream()
                .filter(line -> line.diseaseId().equals(APDS) && line.hpoId().equals(hpoId))
                .findFirst();
    }

    @Test
    public void cohortAnnotationsAreWrittenWithPooledFrequency() {
        AugmentedHpoa augmented = augmenterOf(List.of(
                patient("A", "PMID:16984281", GROWTH_DELAY),
                patient("B", "PMID:16984281"),
                patient("C", "PMID:27379089", GROWTH_DELAY))).augment(base);

        HpoaAnnotationLine line = lineFor(augmented.hpoa(), GROWTH_DELAY).orElseThrow();
        assertEquals(Optional.of(new Ratio(2, 3)), line.frequencyRatio());
        assertEquals(List.of(TermId.of("PMID:16984281"), TermId.of("PMID:27379089")), line.references());
        assertTrue(line.isPhenotypeAnnotation());
    }

    @Test
    public void annotationsFromCohortPublicationsAreSuperseded() {
        AugmentedHpoa augmented = augmenterOf(List.of(
                patient("A", "PMID:24136356", RECURRENT_INFECTIONS))).augment(base);

        DiseaseAugmentation report = augmented.augmentations().stream()
                .filter(entry -> entry.diseaseId().equals(APDS))
                .findFirst()
                .orElseThrow();
        assertEquals(2, report.linesSuperseded());
        assertEquals(1, report.linesAdded());
        assertEquals(0, report.linesPooled());

        HpoaAnnotationLine line = lineFor(augmented.hpoa(), RECURRENT_INFECTIONS).orElseThrow();
        assertEquals(List.of(TermId.of("PMID:24136356")), line.references());
        assertEquals(Optional.of(new Ratio(1, 1)), line.frequencyRatio());
    }

    @Test
    public void annotationsFromOtherPublicationsArePooled() {
        AugmentedHpoa augmented = augmenterOf(List.of(
                patient("A", "PMID:16984281", SHARED_TERM),
                patient("B", "PMID:16984281", SHARED_TERM))).augment(base);

        HpoaAnnotationLine line = lineFor(augmented.hpoa(), SHARED_TERM).orElseThrow();
        assertEquals(Optional.of(new Ratio(17, 19)), line.frequencyRatio());
        assertTrue(line.references().contains(TermId.of("PMID:16984281")));
        assertTrue(line.references().contains(TermId.of("PMID:24136356")));

        DiseaseAugmentation report = augmented.augmentations().get(0);
        assertEquals(1, report.linesPooled());
    }

    @Test
    public void nonPhenotypeAnnotationsAreKept() {
        AugmentedHpoa augmented = augmenterOf(List.of(
                patient("A", "PMID:16984281", RECURRENT_INFECTIONS))).augment(base);

        assertTrue(augmented.hpoa().annotationLines().stream()
                .anyMatch(line -> line.diseaseId().equals(APDS) && "I".equals(line.aspect())));
    }

    @Test
    public void diseasesOutsideTheCohortAreUntouched() {
        TermId other = TermId.of("OMIM:603597");
        AugmentedHpoa augmented = augmenterOf(List.of(
                patient("A", "PMID:16984281", RECURRENT_INFECTIONS))).augment(base);

        assertEquals(base.phenotypeLineCount(other), augmented.hpoa().phenotypeLineCount(other));
    }

    @Test
    public void headerCarriesProvenance() {
        AugmentedHpoa augmented = augmenterOf(List.of(
                patient("A", "PMID:16984281", RECURRENT_INFECTIONS))).augment(base);

        assertTrue(augmented.hpoa().headerLines().stream()
                .anyMatch(line -> line.startsWith("#cohort-augmented:")));
        assertFalse(augmented.hpoa().headerLines().isEmpty());
    }

    @Test
    public void leavingOutAPublicationRestoresTheOriginalCounts() {
        List<PhenopacketCase> cases = List.of(
                patient("A", "PMID:16984281", SHARED_TERM),
                patient("B", "PMID:16984281", SHARED_TERM));
        PhenopacketCollection cohort = PhenopacketCollection.of(cases, Set::of);
        HpoaFile augmented = new HpoaAugmenter(cohort, "HPO:test[2026-08-09]").augment(base).hpoa();

        HpoaFile adjusted = new HpoaAdjuster(augmented, cohort)
                .adjustFor(TermId.of("PMID:16984281"));
        HpoaAnnotationLine line = lineFor(adjusted, SHARED_TERM).orElseThrow();
        assertEquals(Optional.of(new Ratio(15, 17)), line.frequencyRatio());
    }
}
