package org.monarchinitiative.hpotools.analysis.hpoadjust;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record Ratio(int numerator, int denominator) {

    private static final Pattern COUNT_PATTERN = Pattern.compile("(\\d+)/(\\d+)");

    public static Optional<Ratio> parse(String frequencyField) {
        Matcher matcher = COUNT_PATTERN.matcher(frequencyField);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(new Ratio(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))));
    }

    public Ratio plus(Ratio other) {
        return new Ratio(numerator + other.numerator, denominator + other.denominator);
    }

    public Ratio minus(int numerator, int denominator) {
        return new Ratio(this.numerator - numerator, this.denominator - denominator);
    }

    public boolean isInformative() {
        return numerator > 0 && denominator > 0 && numerator <= denominator;
    }

    public String format() {
        return numerator + "/" + denominator;
    }
}
