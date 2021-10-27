package chameleon.UserModule;

public interface UserAPI {

    String getEncryptedUsername(String username, String auth);

    String createUser(String user, String auth);

    String decryptUser(String encryptedUser, String auth);


}
