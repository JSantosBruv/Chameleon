package chameleon.Models;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public class StructureSchema {

    private String index;
    private Map<String, Set<String>> schema;
    private Map<String, String> hashes;
    private KeyValues keys;

    public KeyValues getKeys() {
        return keys;
    }

    public void setKeys(KeyValues keys) {
        this.keys = keys;
    }

    public Map<String, String> getHashes() {
        return hashes;
    }

    public void setHashes(Map<String, String> hashes) {
        this.hashes = hashes;
    }

    public Map<String, Set<String>> getSchema() {
        return schema;
    }

    public void setSchema(Map<String, Set<String>> schema) {
        this.schema = schema;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public boolean hasValidOperations() {

        List<String> EnumList = Arrays.stream(Operations.values()).map(Operations::name).collect(Collectors.toList());

        for (Set<String> algs : schema.values())
            if (!EnumList.containsAll(algs))
                return false;

        return true;

    }


    public enum Operations {
        ADDITION, SUBTRACTION, DET, RAND, NONE, MULT, SEARCH, SEARCH_STATIC, OPE;

    }
}
