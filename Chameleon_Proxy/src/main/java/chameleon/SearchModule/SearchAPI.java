package chameleon.SearchModule;

public interface SearchAPI {

    String encryptQuery(String index, String query, String auth);

    String decryptSearchResults(String index, String encryptedResults, String auth);

    String decryptAsynchSearchResults(String index, String originalResponseBody, String authHeader);
}
