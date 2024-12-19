package org.monarchinitiative.hpotools.analysis.simhpo;

import java.util.List;
import java.util.Random;

public abstract class AbstractProportionalSampler<E>{
    protected List<E> elements;
    protected double[] probabilities;
    protected double[] cumulativeProbabilities;
    protected final Random random;

    public AbstractProportionalSampler(List<E> elements, double[] probabilities, Random random) {
        this.elements = elements;
        this.probabilities = probabilities;
        this.random = random;
        this.cumulativeProbabilities = cumulativeProbabilities();
    }

    protected double[] cumulativeProbabilities() {
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
            normalized[i] = probabilities[i] / sum;
        }
        return normalized;
    }

    /**
     * Returns a copy of the elements that can be sampled.
     * @return The elements that can be sampled.
     */
    public List<E> getElements() {
        return List.copyOf(elements);
    }

    /**
     * Returns a copy of the probabilities of sampling each element.
     * @return The probabilities of sampling each element.
     */
    public double[] getProbabilities() {
        return probabilities.clone();
    }

    /**
     * Returns a copy of the cumulative probabilities of sampling each element.
     * @return The cumulative probabilities of sampling each element.
     */
    public double[] getCumulativeProbabilities() {
        return cumulativeProbabilities.clone();
    }
}
