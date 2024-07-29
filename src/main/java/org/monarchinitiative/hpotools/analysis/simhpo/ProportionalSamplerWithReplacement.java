package org.monarchinitiative.hpotools.analysis.simhpo;

import java.util.*;

public class ProportionalSamplerWithReplacement<E> extends AbstractProportionalSampler<E> implements IProportionalSampler<E> {

    public ProportionalSamplerWithReplacement(List<E> elements, double[] probabilities, Random random) {
        super(elements, probabilities, random);
    }

    public E sample() {
        double r = random.nextDouble();
        int selectedIndex = 0;
        Iterator<Double> probIterator = Arrays.stream(cumulativeProbabilities).iterator();
        int i = 0;
        while (probIterator.hasNext()) {
            if (r <= probIterator.next()) {
                selectedIndex = i;
                break;
            }
            i++;
        }
        return elements.get(selectedIndex);
    }

    public List<E> sample(int n) {
        List<E> selectedElements = new ArrayList<E>();
        for (int i = 0; i < n; i++) {
            selectedElements.add(sample());
        }
        return selectedElements;
    }
}

