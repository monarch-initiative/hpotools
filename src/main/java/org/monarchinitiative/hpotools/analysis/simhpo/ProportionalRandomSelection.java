package org.monarchinitiative.hpotools.analysis.simhpo;

import java.util.Random;

public class ProportionalRandomSelection<E> {
    E[] elements;
    double[] probabilities;
    double[] cumulativeProbabilities;
    Random random;

    public ProportionalRandomSelection(E[] elements, double[] probabilities, Random random) {
        this.elements = elements;
        this.probabilities = probabilities;
        this.random = random;
        this.cumulativeProbabilities = cumulativeProbabilities();
    }

    private double[] cumulativeProbabilities() {
        double[] cumulativeProbabilities = new double[probabilities.length];
        cumulativeProbabilities[0] = probabilities[0];
        for (int i = 1; i < probabilities.length; i++) {
            cumulativeProbabilities[i] = cumulativeProbabilities[i - 1] + probabilities[i];
        }
        return cumulativeProbabilities;
    }
