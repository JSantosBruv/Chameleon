package chameleon.UserModule;

import chameleon.ConfigModule.ConfigService;
import chameleon.EncryptionService.EncryptionService;
import chameleon.Exceptions.Exceptions;
import chameleon.Models.ProxyUser;
import chameleon.Models.StructureSchema;
import chameleon.Utils.JSONMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController implements UserAPI {

    private final UserService userService;
    private final EncryptionService encryptionService;
    private final ConfigService configService;

    public UserController(UserService userService, EncryptionService homoService, ConfigService configService) {
        this.userService = userService;
        this.encryptionService = homoService;
        this.configService = configService;
    }

    @Override
    public String decryptUser(String encryptedUsername, String auth) {

        try {
            String sanitizeBody = JSONMapper.ignoreFirstKey(encryptedUsername);

            ProxyUser user = (ProxyUser) JSONMapper.writeStringAsObject(sanitizeBody, ProxyUser.class);

            StructureSchema schema = configService.getEncryption(ProxyUser.METADATA_CONFIG, auth);

            userService.decryptUser(schema, user);

            return JSONMapper.writeValueAsString(user);

        } catch (JsonProcessingException e) {
            throw new Exceptions.ElasticException(e.getMessage());
        }

    }

    @Override
    public String createUser(String user, String auth) {

        try {

            ProxyUser userObject = (ProxyUser) JSONMapper.writeStringAsObject(user, ProxyUser.class);

            StructureSchema schema = configService.getEncryption(ProxyUser.METADATA_CONFIG, "");

            userService.encryptUser(userObject, schema, auth);

            return JSONMapper.writeValueAsString(userObject);

        } catch (JsonProcessingException e) {
            throw new Exceptions.ElasticException(e.getMessage());
        }

    }

    @Override
    public String getEncryptedUsername(String username, String auth) {

        StructureSchema schema = configService.getEncryption(ProxyUser.METADATA_CONFIG, auth);

        return encryptionService.encryptDet(schema.getKeys().getDet(), username);
    }
}

