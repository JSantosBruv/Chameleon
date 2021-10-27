package chameleon.Models;

import java.util.List;

public class LeafWrapper {


    private List<Object> encryptedValues;

    public LeafWrapper(List<Object> encryptedValues) {
        this.encryptedValues = encryptedValues;
    }

    public LeafWrapper(){

    }
    public List<Object> getEncryptedValues() {
        return encryptedValues;
    }

    public void setEncryptedValues(List<Object> encryptedValues) {
        this.encryptedValues = encryptedValues;
    }

}
