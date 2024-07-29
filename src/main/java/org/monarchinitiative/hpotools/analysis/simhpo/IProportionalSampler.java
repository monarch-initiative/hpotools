package org.monarchinitiative.hpotools.analysis.simhpo;

import java.util.*;

public interface IProportionalSampler<E> {

    public E sample();

    public List<E> sample(int n);
}
