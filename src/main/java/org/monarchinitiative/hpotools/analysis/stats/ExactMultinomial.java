package org.monarchinitiative.hpotools.analysis.stats;

import java.math.BigDecimal;

import org.apache.commons.math3.util.CombinatoricsUtils;

import java.math.RoundingMode;
import java.util.Arrays;

public class ExactMultinomial {

    /**
     * Computes the Exact Multinomial Test probability.
     *
     * @param observed an array of observed counts (long integers)
     * @param expected an array of expected probabilities (double)
     * @return the Exact Multinomial Test probability
     * @throws IllegalArgumentException if arrays are of different lengths or invalid values
     */
    public static double exactMultinomialTest(int[] observed, double[] expected) {
        if (observed.length != expected.length) {
            throw new IllegalArgumentException("Observed and expected arrays must have the same length.");
        }

        int totalCount = 0;
        for (int count : observed) {
            totalCount += count;
        }

        // Step 1: Calculate the multinomial coefficient (log-space)
        double logCoefficient = CombinatoricsUtils.factorialLog(totalCount); // log(n!)
        for (int count : observed) {
            logCoefficient -= CombinatoricsUtils.factorialLog(count); // Subtract log(x_i!)
        }

        // Step 2: Compute the probability (log-space)
        double logProbability = logCoefficient; // Start with log(multinomial coefficient)
        for (int i = 0; i < observed.length; i++) {
            if (expected[i] > 1) {
                throw new IllegalArgumentException("Expected probabilities must be in the range (0, 1].");
            } else if (expected[i] <= 0) {
                continue; // we do have zero counts, so do not take the log
            } else {
                logProbability += observed[i] * Math.log(expected[i]); // Add log(p_i^x_i)
            }
        }

        // Step 3: Convert from log-space to actual probability
        return Math.exp(logProbability);
    }
}
