package chameleon.DataModule;

public interface DataAPI {


    String uploadBatch(String json, String auth);

    String index(String index, String json, String auth);

    void deleteIndex(String index, String auth);

    String getEncryptedDoc(String index, String id, String auth);

}

