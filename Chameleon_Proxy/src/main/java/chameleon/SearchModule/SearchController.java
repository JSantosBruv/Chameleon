package chameleon.SearchModule;

import chameleon.ConfigModule.ConfigService;
import chameleon.Exceptions.Exceptions;
import chameleon.Models.ElasticsearchQuery;
import chameleon.Models.StructureSchema;
import chameleon.Utils.JSONMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("unchecked")
@RestController
public class SearchController implements SearchAPI {

    private final SearchService queryHandlerService;
    private final ConfigService configService;

    public SearchController(SearchService queryHandlerService, ConfigService configService) {
        this.queryHandlerService = queryHandlerService;
        this.configService = configService;

    }

    @Override
    public String encryptQuery(String index, String query, String auth) {

        try {

            return parseQueryFancy(query, index, auth);

        } catch (IOException e) {
            throw new Exceptions.ElasticException("Unable to parse/encrypt provided Query");
        }
    }

    @Override
    public String decryptAsynchSearchResults(String index, String encryptedResults, String authHeader) {
        try {
            JsonNode responseNode = JSONMapper.getJsonNode(encryptedResults);
            JsonNode responseObject = responseNode.get("response");

            String decryptedResponse = decryptSearchResults(index, JSONMapper.writeValueAsString(responseObject),
                                                            authHeader);

            ((ObjectNode) responseNode).set("response", JSONMapper.getJsonNode(decryptedResponse));

            return JSONMapper.writeValueAsString(responseNode);

        } catch (IOException e) {
            throw new Exceptions.ElasticException("Unable to decrypt search results.");
        }
    }

    @Override
    public String decryptSearchResults(String index, String encryptedResults, String auth) {

        try {
            JsonNode responseNode = JSONMapper.getJsonNode(encryptedResults);

            JsonNode hits = responseNode.get("hits");

            if (hits.get("total").get("value").asInt() > 0) {

                ArrayNode hitsInside = (ArrayNode) hits.get("hits");

                StructureSchema schema = configService.getEncryption(index, auth);

                List<CompletableFuture<JsonNode>> asynchs = new ArrayList<>();

                for (JsonNode hitMap : hitsInside) {

                    asynchs.add(queryHandlerService.decryptHit(hitMap, schema));

                }

                CompletableFuture<JsonNode>[] asynchArray = asynchs.toArray(new CompletableFuture[0]);

                ArrayNode unencryptedHits = JSONMapper.createArrayNode();

                Stream.of(asynchArray).map(CompletableFuture::join).collect(Collectors.toList())
                      .forEach(unencryptedHits::add);

                ((ObjectNode) hits).replace("hits", unencryptedHits);
                ((ObjectNode) responseNode).replace("hits", hits);
            }

            JsonNode aggs = responseNode.get("aggregations");

            if (aggs != null) {

                StructureSchema schema = configService.getEncryption(index, auth);
                Map.Entry<String, JsonNode> aggResult = aggs.fields().next();

                String aggName = aggResult.getKey();

                JsonNode encryptedValueNode = queryHandlerService.decryptAggregation(aggResult.getValue(), schema);

                ((ObjectNode) aggs).set(aggName, encryptedValueNode);

                ((ObjectNode) responseNode).replace("aggregations", aggs);
            }

            return JSONMapper.writeValueAsString(responseNode);

        } catch (IOException e) {
            throw new Exceptions.ElasticException("Unable to decrypt search results.");
        }

    }


    private String parseQueryFancy(String query, String index, String auth) throws IOException {

        ElasticsearchQuery elasticQuery = ElasticsearchQuery.fromJson(query);

        if (elasticQuery.isMalformed())
            throw new Exceptions.UnsupportedOperationsException("Query DSL Malformed.");

        JsonNode encryptedQuery = queryHandlerService.transformQuery(elasticQuery, index, auth);

        return JSONMapper.writeValueAsString(encryptedQuery);

    }


}

