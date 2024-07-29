package org.monarchinitiative.hpotools.analysis.simhpo;

import java.util.*;

public class ProportionalSamplerWithoutReplacement<E> extends ProportionalSamplerWithReplacement<E> implements IProportionalSampler<E> {

    public ProportionalSamplerWithoutReplacement(List<E> elements, double[] probabilities, Random random) {
        super(elements, probabilities, random);
    }

    public E sample() {
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

}

