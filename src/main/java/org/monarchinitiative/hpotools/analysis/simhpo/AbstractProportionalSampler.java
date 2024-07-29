package org.monarchinitiative.hpotools.analysis.simhpo;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public abstract class AbstractProportionalSampler<E>{
    List<E> elements;
    double[] probabilities;
    double[] cumulativeProbabilities;
    Random random;

    public AbstractProportionalSampler(List<E> elements, double[] probabilities, Random random) {
        this.elements = elements;
        this.probabilities = probabilities;
        this.random = random;
        this.cumulativeProbabilities = cumulativeProbabilities();
        System.out.println("cumulativeProbabilities = " + Arrays.toString(cumulativeProbabilities));
    }

    private double[] cumulativeProbabilities() {
        // TODO: normalize probabilities
        return;
//        double[] cumulativeProbabilities = new double[probabilities.length];
//        cumulativeProbabilities[0] = probabilities[0];
//        for (int i = 1; i < probabilities.length; i++) {
//            cumulativeProbabilities[i] = cumulativeProbabilities[i - 1] + probabilities[i];
//        }
//        return cumulativeProbabilities;
    }
}
