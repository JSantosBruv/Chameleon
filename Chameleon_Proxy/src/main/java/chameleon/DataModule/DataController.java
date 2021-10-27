package chameleon.DataModule;

import chameleon.ConfigModule.ConfigService;
import chameleon.EncryptionService.EncryptionService;
import chameleon.Exceptions.Exceptions;
import chameleon.Models.StructureSchema;
import chameleon.Utils.JSONMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@SuppressWarnings("unchecked")

@Component
public class DataController implements DataAPI {

    private final DataService dataManagementService;
    private final ConfigService configService;
    private final EncryptionService encryptionService;

    public DataController(DataService dataManagementService, EncryptionService homoService,
                          ConfigService configService) {
        this.dataManagementService = dataManagementService;
        this.encryptionService = homoService;
        this.configService = configService;
    }

    @Override
    public String uploadBatch(String json, String auth) {

        List<CompletableFuture<String>> asynchs = new ArrayList<>();

        StringBuilder builder = new StringBuilder();

        try {

            Map<String, StructureSchema> schemas = new ConcurrentHashMap<>();
            String[] batches = splitJson(json);

            for (String batch : batches)
                asynchs.add(dataManagementService.processByBatch(json, schemas, auth));

            CompletableFuture<String>[] asynchArray = asynchs.toArray(new CompletableFuture[0]);

            Stream.of(asynchArray).map(CompletableFuture::join).collect(Collectors.toList()).forEach(builder::append);


            return builder.toString();


        } catch (IOException e) {

            throw new Exceptions.ElasticException(e.getMessage());
        }


    }

    @Override
    public String index(String index, String json, String auth) {

        try {

            StructureSchema schema = configService.getEncryption(index, auth);


            return encryptionService.encryptDecryptJson(schema, json, true);

        } catch (IOException e) {
            e.printStackTrace();
            throw new Exceptions.ElasticException(e.getMessage());
        }

    }

    @Override
    public void deleteIndex(String index, String auth) {

        configService.deleteIndex(index, auth);
    }

    @Override
    public String getEncryptedDoc(String index, String responseBody, String auth) {

        try {

            JsonNode responseNode = JSONMapper.getJsonNode(responseBody);

            String jsonResult = JSONMapper.writeValueAsString(responseNode.get("_source"));

            StructureSchema schema = configService.getEncryption(index, auth);

            String decryptedSource = encryptionService.encryptDecryptJson(schema, jsonResult, false);

            ((ObjectNode) responseNode).replace("_source", JSONMapper.getJsonNode(decryptedSource));

            return JSONMapper.writeValueAsString(responseNode);

        } catch (JsonProcessingException e) {
            throw new Exceptions.ElasticException(e.getMessage());
        }

    }

    private String[] splitJson(String json) throws IOException {


        long totalLines = json.lines().count();

        int processors = Runtime.getRuntime().availableProcessors();

        long batchSize = totalLines / processors;

        batchSize = batchSize % 2 != 0 ? batchSize + 1 : batchSize;

        Spliterator<String> split = json.lines().spliterator();

        int batchNumber = 0;
        String[] batches = new String[processors];

        for (int currentChunk = 0; currentChunk < processors; currentChunk++) {

            StringBuilder batch = new StringBuilder();
            //deletes alone pls
            for (int i = 0; i < batchSize && split.tryAdvance(line -> {
                batch.append(line);
                batch.append(System.lineSeparator());
            }); i++) {}


            batches[batchNumber] = batch.toString();
            batchNumber++;

        }

        return batches;
    }
}


