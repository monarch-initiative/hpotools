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
