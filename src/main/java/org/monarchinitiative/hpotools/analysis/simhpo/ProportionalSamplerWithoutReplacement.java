package org.monarchinitiative.hpotools.analysis.simhpo;

import java.util.*;

public class ProportionalSamplerWithoutReplacement<E> extends ProportionalSamplerWithReplacement<E> implements IProportionalSampler<E> {

    public ProportionalSamplerWithoutReplacement(List<E> elements, double[] probabilities, Random random) {
        super(elements, probabilities, random);
    }

    public E sample() {
        if (elements.isEmpty()) {
            throw new IllegalStateException("The list of elements is empty.");
        } else if (elements.size() == 1) {
            return elements.get(0);
        }
        E sampled = super.sample();

        int selectedIndex = elements.indexOf(sampled);

        elements = new ArrayList<E>(elements);
        elements.remove(selectedIndex);

        double[] newProbabilities = new double[probabilities.length - 1];
        int j = 0;
        for (int i = 0; i < probabilities.length; i++) {
            if (i != selectedIndex) {
                newProbabilities[j] = probabilities[i];
                j++;
            }
        }

        probabilities = newProbabilities;

        cumulativeProbabilities = cumulativeProbabilities();

        return sampled;
    }

    public List<E> sample(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("The number of samples must be non-negative.");
        } else if (n >= elements.size()) {
            throw new IllegalArgumentException("The number of samples must be less than the number of elements.");
        }
        return super.sample(n);
    }

}

