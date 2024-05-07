package org.monarchinitiative.hpotools.analysis;


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class EncodingCheck {

    private final String pattern = "(HP_\\d+)";
    private final Pattern HPO_TERM_PATTERN = Pattern.compile(pattern);
    private final File hpoOwlFile;


    /**
     * @param hpoOwlFile file for hp-edit.owl. Presumed checked by client code
     */
    public EncodingCheck(File hpoOwlFile) {
        this.hpoOwlFile = hpoOwlFile;
    }

    public void checkEncoding() {

        int i = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(hpoOwlFile))){
            String line;
            String hp_term = "";
            while ((line = br.readLine()) != null) {
                Matcher m = HPO_TERM_PATTERN.matcher(line);
                if (m.find()) {
                    hp_term = m.group();
                }
                checkLine(line, ++i, hp_term);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void checkLine(String line, int lineno, String previous) throws UnsupportedEncodingException {
        byte[] bytes = line.getBytes(StandardCharsets.ISO_8859_1);
        String decodedLine = new String(bytes);
        if (! line.equals(decodedLine)) {
            for (int i=0; i< line.length();i++) {
                if (line.charAt(i) != decodedLine.charAt(i)) {
                    System.out.println(previous);
                    System.out.printf("L.%d:Pos:%d: ", lineno, i);
                    int b = Math.max(0, i-20);
                    int e = Math.min(line.length(), i+20);
                    String ss1 = line.substring(b, i);
                    String ss2 = line.substring(i+1, e);
                    System.out.printf("%s{%c}%sc\n\n", ss1, line.charAt(i), ss2);
                }
            }
        }
    }


}
