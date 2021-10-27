package chameleon.DataModule;

import chameleon.ConfigModule.ConfigService;
import chameleon.EncryptionService.EncryptionService;
import chameleon.Exceptions.Exceptions;
import chameleon.Models.StructureSchema;
import chameleon.Utils.JSONMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class DataService {

    private final EncryptionService encryptionService;
    private final ConfigService configService;

    public DataService(EncryptionService homoService, ConfigService configService) {
        this.encryptionService = homoService;
        this.configService = configService;

    }

    @Async
    public CompletableFuture<String> processByBatch(String batch, Map<String, StructureSchema> schemas, String auth)
            throws IOException {

        BufferedReader reader = new BufferedReader(new StringReader(batch));
        String data;
        StringBuilder builder = new StringBuilder();

        StructureSchema schema = null;

        while ((data = reader.readLine()) != null && !data.isBlank()) {

            String action = data;


            if (!getActionType(action).equalsIgnoreCase("delete")) {

                String index = getActionIndex(action);

                schema = schemas.get(index);

                if (schema == null) {
                    schema = configService.getEncryption(index, auth);
                    schemas.put(index, schema);
                }
                data = reader.readLine();
            } else
                data = null;


            builder.append(encryptDataAndWriteAction(data, action, schema));

        }
        reader.close();
        return CompletableFuture.completedFuture(builder.toString());
    }


    public String encryptDataAndWriteAction(String data, String action, StructureSchema schema) throws IOException {


        StringBuilder builder = new StringBuilder();

        builder.append(action);
        builder.append("\n");

        if (data != null) {

            builder.append(encryptionService.encryptDecryptJson(schema, data, true));

            builder.append("\n");
        }

        return builder.toString();

    }


    private String getActionType(String action) {

        JsonNode actionNode = JSONMapper.getJsonNode(action);

        Iterator<String> actionField = actionNode.fieldNames();

        if (actionField.hasNext())
            return actionField.next();
        else
            throw new Exceptions.UnsupportedOperationsException("Bulk action is missing");
    }

    private String getActionIndex(String action) {
        JsonNode actionNode = JSONMapper.getJsonNode(action);

        Iterator<Map.Entry<String, JsonNode>> actionMetadata = actionNode.fields();

        if (actionMetadata.hasNext())
            return actionMetadata.next().getValue().get("_index").asText();
        else
            throw new Exceptions.UnsupportedOperationsException("Bulk action metadata's index is missing");
    }
}

