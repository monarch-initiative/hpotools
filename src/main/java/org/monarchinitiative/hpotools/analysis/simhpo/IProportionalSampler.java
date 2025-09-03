package org.monarchinitiative.hpotools.analysis.simhpo;

import java.util.*;

public interface IProportionalSampler<E> {

    E sample();

    List<E> sample(int n);
}
