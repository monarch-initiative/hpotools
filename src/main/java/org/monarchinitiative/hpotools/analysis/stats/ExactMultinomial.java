package org.monarchinitiative.hpotools.analysis.stats;
import java.math.BigDecimal;
import org.apache.commons.math3.util.CombinatoricsUtils;

import java.math.RoundingMode;
import java.util.Arrays;

public class ExactMultinomial {

        public static BigDecimal multinomialProbability(long[] observed, double[] expectedProbabilities) {
            int total = 0;
            BigDecimal numerator = BigDecimal.ONE;

            // Calculate factorial(n) and x_i!
            for (int x=0; x< observed.length;x++) {
                numerator = numerator.multiply(factorial(x));
                total += x;
            }

            // Denominator: total!
            BigDecimal denominator = factorial(total);

            // Probability term
            BigDecimal probability = BigDecimal.ONE;
            for (int i = 0; i < observed.length; i++) {
                probability = probability.multiply(BigDecimal.valueOf(Math.pow(expectedProbabilities[i], observed[i])));
            }

            return probability.multiply(denominator).divide(numerator, RoundingMode.HALF_EVEN);
        }

        private static BigDecimal factorial(int n) {
            BigDecimal result = BigDecimal.ONE;
            for (int i = 1; i <= n; i++) {
                result = result.multiply(BigDecimal.valueOf(i));
            }
            return result;
        }
    /**
     * Computes the Exact Multinomial Test probability.
     * TODO check this
     * @param observed an array of observed counts (long integers)
     * @param expected an array of expected probabilities (double)
     * @return the Exact Multinomial Test probability
     * @throws IllegalArgumentException if arrays are of different lengths or invalid values
     */
    public static double exactMultinomialTest(long[] observed, double[] expected) {
        if (observed.length != expected.length) {
            throw new IllegalArgumentException("Observed and expected arrays must have the same length.");
        }

        long totalCount = 0;
        for (long count : observed) {
            totalCount += count;
        }

        // Step 1: Calculate the multinomial coefficient (log-space)
        double logCoefficient = CombinatoricsUtils.factorialLog((int)totalCount); // log(n!)
        for (long count : observed) {
            logCoefficient -= CombinatoricsUtils.factorialLog((int)count); // Subtract log(x_i!)
        }

        double [] expectedProbabilities = new double[expected.length];
        double expectedTotal = Arrays.stream(expected).sum();
        for (int i = 0; i < expected.length; i++) {
            expectedProbabilities[i] = expected[i]/expectedTotal;
        }

        // Step 2: Compute the probability (log-space)
        double logProbability = logCoefficient; // Start with log(multinomial coefficient)
        for (int i = 0; i < observed.length; i++) {
            if (expectedProbabilities[i] <= 0 || expectedProbabilities[i] > 1) {
                throw new IllegalArgumentException("Expected probabilities must be in the range (0, 1].");
            }
            logProbability += observed[i] * Math.log(expectedProbabilities[i]); // Add log(p_i^x_i)
        }

        // Step 3: Convert from log-space to actual probability
        return Math.exp(logProbability);
    }
    }
