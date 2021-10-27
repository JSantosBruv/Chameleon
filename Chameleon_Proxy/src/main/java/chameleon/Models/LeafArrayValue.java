package chameleon.Models;

import java.util.List;

public class LeafArrayValue {

    private String alg;
    private List<Object> encryptedValues;

    public LeafArrayValue(String alg, List<Object> encryptedValues) {
        this.alg = alg;
        this.encryptedValues = encryptedValues;
    }

    public String getAlg() {
        return alg;
    }

    public void setAlg(String alg) {
        this.alg = alg;
    }

    public List<Object> getEncryptedValues() {
        return encryptedValues;
    }

    public void setEncryptedValues(List<Object> encryptedValues) {
        this.encryptedValues = encryptedValues;
    }
}
