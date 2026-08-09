package org.monarchinitiative.hpotools.analysis.hpoadjust;

import java.util.List;

public record AugmentedHpoa(HpoaFile hpoa, List<DiseaseAugmentation> augmentations) {
}
