package org.monarchinitiative.hpotools.analysis.mondo;

import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PpktResolver {
    private final static Logger LOGGER = LoggerFactory.getLogger(PpktResolver.class);
    private final File ppktDir;
    private final Map<String, File> fileNameToFileMap;


    public PpktResolver(File ppktDir, List<PpktStoreItem> ppktStoreItemList) {
        this.ppktDir = ppktDir;
        this.fileNameToFileMap = new HashMap<>();
        for (PpktStoreItem ppktStoreItem : ppktStoreItemList) {
            String cohort = ppktStoreItem.cohort();
            String fname = ppktStoreItem.filename();
            File f = new File(ppktDir + File.separator + cohort + File.separator + fname);
            System.out.println(f.getAbsolutePath());
            if (!f.exists()) {
                // should never happen
                String errMsg = String.format("Could not find phenopacket file at %s",
                        f.getAbsolutePath());
                LOGGER.error(errMsg);
                throw new PhenolRuntimeException(errMsg);
            }
            fileNameToFileMap.put(fname, f);
        }
    }

    public File getFile(String filename) {
        if (! fileNameToFileMap.containsKey(filename)) {
            throw new PhenolRuntimeException("could not find file in fileNameToFileMap: " + filename);
        }
        return fileNameToFileMap.get(filename);
    }


    public void outputFiles(String dirName, List<MondoClintlrItem> mcItemList) {
        if (!Files.exists(Paths.get(dirName))) {
            boolean success = new File(dirName).mkdir();
            if (!success) {
                throw new PhenolRuntimeException("could not create directory: " + dirName);
            }
        }
        // Summary file
        File summary = new File(dirName + File.separator +  "candidates.tsv");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(summary))) {
           bw.write(MondoClintlrItem.header() + "\n");
            for (MondoClintlrItem mcItem : mcItemList) {
               bw.write(mcItem.getTsvLine() + "\n");
           }
        } catch (IOException e) {
            LOGGER.error("Couldn't write to file: " + summary.getAbsolutePath(), e);
        }
        // Copy each of the phenopackets to this directory
        for (MondoClintlrItem mcItem : mcItemList) {
            File ppkt = mcItem.ppktFile();
            Path sourcepath = Paths.get(ppkt.getAbsolutePath());
            File destination = new File(dirName + File.separator + mcItem.ppktFile().getName());
            copyFile(ppkt, destination);
        }

    }

    public static void copyFile(File source, File target) {
            try {
                InputStream in = new FileInputStream(source);
                OutputStream out = new FileOutputStream(target);

                // Copy the bits from input stream to output stream
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
            } catch (IOException e) {
                throw new PhenolRuntimeException("Could not find file: " + source.getAbsolutePath(), e);
            }
    }


}
