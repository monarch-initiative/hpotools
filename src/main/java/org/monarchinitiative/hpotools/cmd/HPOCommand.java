package org.monarchinitiative.hpotools.cmd;


import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import picocli.CommandLine;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Super class for all commands, i.e. the classes implementing one HpoWorkbench execution step.
 *
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public abstract class HPOCommand {

    @CommandLine.Option(names={"-d","--download"},description = "directory to download HPO data")
    protected String downloadDirectory="data";
    @CommandLine.Option(names={"-a", "--annot"},description = "path to phenol.hpoa")
    protected String annotpath="data/phenotype.hpoa";
    @CommandLine.Option(names={"--hpo"}, description = "path to hp.json")
    protected String hpopath ="data/hp.json";

    protected Map<String,String> defaults=new HashMap<>();


    public HPOCommand setDefaultValue(String key,String value) {
        defaults.put(key,value);
        return this;
    }

    protected File getHpoJsonFile() {
        File f = new File(hpopath);
        if (! f.isFile()) {
            throw new PhenolRuntimeException("Could not find hp.json file at " + hpopath);
        }
        return f;
    }

    protected Ontology getHpOntology() {
        File f = getHpoJsonFile();
        return OntologyLoader.loadOntology(f);
    }

}