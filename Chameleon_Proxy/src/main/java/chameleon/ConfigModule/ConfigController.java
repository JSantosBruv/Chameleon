package chameleon.ConfigModule;

import chameleon.Exceptions.Exceptions;
import chameleon.Models.StructureSchema;
import chameleon.Utils.ResponseBuilder;
import org.elasticsearch.client.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConfigController implements ConfigAPI {

    private final ConfigService configService;
    
    public ConfigController(ConfigService configService) {
        this.configService = configService;
    
    }

    @Override
    public ResponseEntity<?> configSchema(String auth, StructureSchema mappings) {

        mappings.setHashes(null);
        mappings.setKeys(null);

        if (!mappings.hasValidOperations())
            throw new Exceptions.UnsupportedOperationsException("One or more provided mappings are not supported..");

        Response response = configService.configStructureMappings(auth, mappings);

        return ResponseBuilder.buildResponseEntity(response);
    }



}
