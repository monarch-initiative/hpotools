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
        double[] normalized = normalize(probabilities);
        double[] cumulativeProbabilities = new double[normalized.length];
        cumulativeProbabilities[0] = normalized[0];
        for (int i = 1; i < normalized.length; i++) {
            cumulativeProbabilities[i] = cumulativeProbabilities[i - 1] + normalized[i];
        }
        return cumulativeProbabilities;
    }

    public static double[] normalize(double[] probabilities) {
        double sum = 0;
        double[] normalized = new double[probabilities.length];
        for (double probability : probabilities) {
            sum += probability;
        }
        for (int i = 0; i < normalized.length; i++) {
            normalized[i] /= sum;
        }
        return normalized;
    }
}
