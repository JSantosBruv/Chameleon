package chameleon.Models;

public class LeafSingleValue {

    private String alg;
    private Object encryptedValue;


    public LeafSingleValue(String alg, Object encryptedValue) {
        this.alg = alg;
        this.encryptedValue = encryptedValue;
    }


    public String getAlg() {
        return alg;
    }

    public void setAlg(String alg) {
        this.alg = alg;
    }

    public Object getEncryptedValue() {
        return encryptedValue;
    }

    public void setEncryptedValue(Object encryptedValue) {
        this.encryptedValue = encryptedValue;
    }
}
