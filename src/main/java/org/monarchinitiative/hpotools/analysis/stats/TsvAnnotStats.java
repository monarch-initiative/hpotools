package org.monarchinitiative.hpotools.analysis.stats;

import java.io.BufferedReader;
import java.io.IOException;

public abstract class TsvAnnotStats {

    abstract public void printStats();


    protected void checkStartsWith(BufferedReader br, String expectedPrefix) throws IOException {
        String line = br.readLine();
        if (!line.startsWith(expectedPrefix)) {
            throw new RuntimeException("Malformed line: "+ line);
        }
    }
}
