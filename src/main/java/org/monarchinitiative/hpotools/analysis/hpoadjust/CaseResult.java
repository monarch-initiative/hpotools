package org.monarchinitiative.hpotools.analysis.hpoadjust;

import java.nio.file.Path;
import java.util.Optional;

public record CaseResult(PhenopacketCase phenopacketCase,
                         CaseStatus status,
                         int linesRemovedForDisease,
                         int linesRemovedTotal,
                         int multiReferenceLinesRemoved,
                         long remainingPhenotypeLines,
                         Optional<Path> adjustedHpoa) {
}
