package chameleon.UserModule;

import chameleon.EncryptionService.EncryptionService;
import chameleon.Models.ProxyUser;
import chameleon.Models.StructureSchema;
import chameleon.Utils.CryptoUtils;
import chameleon.Utils.JSONMapper;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;


@Service
@SuppressWarnings("unchecked")
public class UserService {

    private final EncryptionService encryptionService;
    private final CryptoUtils cryptoUtils;

    public UserService(EncryptionService homoService, CryptoUtils cryptoUtils) {

        this.encryptionService = homoService;
        this.cryptoUtils = cryptoUtils;
    }


    public void encryptUser(ProxyUser user, StructureSchema schema, String auth) {

        user.setEmail(cryptoUtils.hashValue(ProxyUser.METADATA_CONFIG + user.getEmail()));
        user.setFull_name(cryptoUtils.hashValue(ProxyUser.METADATA_CONFIG + user.getFull_name()));
        user.setRoles(user.getRoles().stream().map(v -> cryptoUtils.hashValue(ProxyUser.METADATA_CONFIG + v))
                          .collect(Collectors.toSet()));


        try {
            String encryptedMetadata = encryptionService
                    .encryptDecryptJson(schema, JSONMapper.writeValueAsString(user.getMetadata()), true);

            Map<String, Object> encryptedMap = (Map<String, Object>) JSONMapper
                    .writeStringAsObject(encryptedMetadata, Map.class);

            user.setMetadata(encryptedMap);


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void decryptUser(StructureSchema schema, ProxyUser user) {


        Map<String, String> hashMappings = schema.getHashes();

        user.setUsername(encryptionService.decryptDet(schema.getKeys().getDet(), user.getUsername()));
        user.setEmail(hashMappings.get(user.getEmail()));
        user.setFull_name(hashMappings.get(user.getFull_name()));
        user.setRoles(user.getRoles().stream().map(hashMappings::get).collect(Collectors.toSet()));


        try {
            String decryptedMetadata = encryptionService
                    .encryptDecryptJson(schema, JSONMapper.writeValueAsString(user.getMetadata()), false);

            Map<String, Object> decryptedMap = (Map<String, Object>) JSONMapper
                    .writeStringAsObject(decryptedMetadata, Map.class);

            user.setMetadata(decryptedMap);


        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}



