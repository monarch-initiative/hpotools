package org.monarchinitiative.hpotools.analysis.simhpo;

import java.util.*;

public class ProportionalSamplerWithoutReplacement<E> extends AbstractProportionalSampler<E> implements IProportionalSampler<E> {

    public ProportionalSamplerWithoutReplacement(List<E> elements, double[] probabilities, Random random) {
        super(elements, probabilities, random);
    }

    public E sample() {
        // TODO: Implement this method
        return null;
    }

    public List<E> sample(int n) {
        List<E> selectedElements = new ArrayList<E>();
        for (int i = 0; i < n; i++) {
            selectedElements.add(sample());
        }
        return selectedElements;
    }
}

