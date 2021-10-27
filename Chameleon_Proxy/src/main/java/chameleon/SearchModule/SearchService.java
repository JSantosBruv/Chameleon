package chameleon.SearchModule;

import chameleon.ConfigModule.ConfigService;
import chameleon.EncryptionService.EncryptionService;
import chameleon.EncryptionService.ScriptsAndMappings;
import chameleon.Exceptions.Exceptions;
import chameleon.HomoLib.HomoAdd.HomoAdd;
import chameleon.HomoLib.HomoSearch.HomoSearch;
import chameleon.Models.ElasticsearchQuery;
import chameleon.Models.StructureSchema;
import chameleon.Utils.CryptoUtils;
import chameleon.Utils.JSONMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Service
public class SearchService {

    private final EncryptionService encryptionService;
    private final ConfigService configService;
    private final CryptoUtils cryptoUtils;

    public SearchService(ConfigService configService, EncryptionService homoService, CryptoUtils cryptoUtils) {
        this.encryptionService = homoService;
        this.configService = configService;
        this.cryptoUtils = cryptoUtils;
    }

    @Async
    public CompletableFuture<JsonNode> decryptHit(JsonNode hitMap, StructureSchema schema)
            throws JsonProcessingException {


        String hitSource = JSONMapper.writeValueAsString(hitMap.get("_source"));

        String decryptedSource = encryptionService.encryptDecryptJson(schema, hitSource, false);

        ((ObjectNode) hitMap).set("_source", JSONMapper.getJsonNode(decryptedSource));

        return CompletableFuture.completedFuture(hitMap);
    }


    public JsonNode decryptAggregation(JsonNode aggResultNode, StructureSchema schema) throws JsonProcessingException {

        JsonNode aggResult = aggResultNode.get("value");

        if (aggResult.isArray()) {
            double decryptedSum = encryptionService
                    .decryptAdd(schema.getKeys().getAdd(), aggResult.get(0).bigIntegerValue());
            long count = aggResult.get(1).asLong();

            return ((ObjectNode) aggResultNode).set("value", JSONMapper.convertToJsonNode(decryptedSum / count));
        }


        String encryptedResult = aggResult.asText();

        try {

            long encryptedValue = Long.parseLong(encryptedResult);
            int decryptedSource = encryptionService.decryptOPE(schema.getKeys().getOpe(), encryptedValue);

            return ((ObjectNode) aggResultNode).set("value", JSONMapper.convertToJsonNode(decryptedSource));

        } catch (NumberFormatException e) {

            BigInteger encryptedValue = new BigInteger(encryptedResult);
            long decryptedSource = encryptionService.decryptAdd(schema.getKeys().getAdd(), encryptedValue);

            return ((ObjectNode) aggResultNode).set("value", JSONMapper.convertToJsonNode(decryptedSource));
        }

    }

    public JsonNode transformQuery(ElasticsearchQuery plainQuery, String index, String auth) {


        StructureSchema schema = configService.getEncryption(index, auth);

        if (plainQuery.hasQueryBody()) {
            JsonNode encryptedQueryBody = transformQueryByType(plainQuery, schema);
            plainQuery.setEncryptedQueryNode(encryptedQueryBody);
        }
        if (plainQuery.hasAggsBody()) {
            JsonNode encryptedAggsBody = transformAggsByType(plainQuery, schema);
            plainQuery.setEncryptedAggregationNode(encryptedAggsBody);
        }

        return plainQuery.getRootNode();
    }

    public JsonNode transformQueryByType(ElasticsearchQuery plainQuery, StructureSchema schema) {

        Map.Entry<String, JsonNode> queryBody = plainQuery.getQueryBody();

        String queryType = queryBody.getKey();

        if (!plainQuery.isValidQuery(queryType))
            throw new Exceptions.UnsupportedOperationsException("Query not supported.");

        JsonNode queryTypeBody = queryBody.getValue();

        if (plainQuery.isTypeMatchAll(queryType))
            return JSONMapper.createJsonNode(queryType, JSONMapper.createJsonNode());
        else if (plainQuery.isTypeMatches(queryType))
            queryTypeBody = transformMatchesAndTermsType(schema, queryTypeBody, true);
        else if (plainQuery.isTypeTerms(queryType))
            queryTypeBody = transformMatchesAndTermsType(schema, queryTypeBody, false);
        else if (plainQuery.isTypeCombinedFields(queryType) || plainQuery.isTypeMultiMatch(queryType))
            queryTypeBody = transformCombinedFieldsAndMultiMatchType(schema, queryTypeBody);
        else if (plainQuery.isTypeBool(queryType))
            queryTypeBody = transformBoolType(schema, queryTypeBody);
        else if (plainQuery.isTypeRange(queryType))
            queryTypeBody = transformRangeType(schema, queryTypeBody);

        return JSONMapper.createJsonNode(queryType, queryTypeBody);
    }

    public JsonNode transformAggsByType(ElasticsearchQuery plainQuery, StructureSchema schema) {

        Map.Entry<String, JsonNode> aggsBody = plainQuery.getAggsBody();

        String aggsName = aggsBody.getKey();

        JsonNode aggsNameBody = aggsBody.getValue();

        Iterator<Map.Entry<String, JsonNode>> aggIt = aggsNameBody.fields();

        if (!aggIt.hasNext())
            throw new Exceptions.UnsupportedOperationsException("Aggregation Malformed or not supported.");

        Map.Entry<String, JsonNode> aggNode = aggIt.next();

        String aggType = aggNode.getKey();

        if (!plainQuery.isValidAggregation(aggType))
            throw new Exceptions.UnsupportedOperationsException("Aggregation not supported.");

        JsonNode aggsTypeBody = aggNode.getValue();

        aggsTypeBody = transformAggs(plainQuery, schema, aggType, aggsTypeBody);

        return JSONMapper.createJsonNode(aggsName, aggsTypeBody);
    }

    public JsonNode transformAggs(ElasticsearchQuery plainQuery, StructureSchema schema, String aggType,
                                  JsonNode aggsTypeBody) {
        Iterator<Map.Entry<String, JsonNode>> fieldBody = aggsTypeBody.fields();

        if (!fieldBody.hasNext()) {
            throw new Exceptions.UnsupportedOperationsException("Aggregation Malformed.");
        }

        Map.Entry<String, JsonNode> fieldNode = fieldBody.next();

        JsonNode fieldValue = fieldNode.getValue();
        String field = fieldValue.asText();
        Set<String> algs = schema.getSchema().get(field);

        if (algs == null)
            throw new Exceptions.UnsupportedOperationsException(
                    String.format("Field [%s] isn't currently configured.", field));

        if (algs.contains("NONE"))
            return aggsTypeBody;

        String index = schema.getIndex();

        if (plainQuery.isTypeMinMax(aggType)) {
            return transformMinMaxType(aggType, index, field, algs);
        } else if (plainQuery.isTypeSum(aggType)) {
            return transformAvgSumType(schema, index, field, algs, "SUM");
        } else
            return transformAvgSumType(schema, index, field, algs, "AVG");

    }


    public JsonNode transformAvgSumType(StructureSchema schema, String index, String field, Set<String> algs,
                                        String type) {


        if (!algs.contains("ADDITION"))
            throw new Exceptions.UnsupportedOperationsException("Field isn't configured with any ADDITION alg");

        String encryptedField = hashField(index, "ADDITION", field);

        String addKey = schema.getKeys().getAdd();

        String nSquare = HomoAdd.keyFromString(addKey).getNsquare().toString();

        String params =
                "          \"field\": \"" + encryptedField + "\",           \n" + " \"nSquare\":\"" + nSquare + "\"";

        String aggScript = "";
        if (type.equals("AVG"))
            aggScript = String
                    .format(ScriptsAndMappings.AGG_BODY, ScriptsAndMappings.INIT_NAME, ScriptsAndMappings.MAP_NAME,
                            ScriptsAndMappings.COMBINE_AVG_NAME, ScriptsAndMappings.REDUCE_AVG_NAME, params);
        else
            aggScript = String
                    .format(ScriptsAndMappings.AGG_BODY, ScriptsAndMappings.INIT_NAME, ScriptsAndMappings.MAP_NAME,
                            ScriptsAndMappings.COMBINE_SUM_NAME, ScriptsAndMappings.REDUCE_SUM_NAME, params);

        return JSONMapper.getJsonNode(aggScript);
    }


    public JsonNode transformMinMaxType(String aggType, String index, String field, Set<String> algs) {


        if (!algs.contains("OPE"))
            throw new Exceptions.UnsupportedOperationsException("Field isn't configured with any OPE alg");

        String encryptedField = hashField(index, "OPE", field);

        long starterValue;
        String combineName, reduceName = "";

        if (aggType.equalsIgnoreCase("max")) {
            starterValue = 0;
            combineName = ScriptsAndMappings.COMBINE_MAX_NAME;
            reduceName = ScriptsAndMappings.REDUCE_MAX_NAME;
        } else {
            starterValue = Long.MAX_VALUE;
            combineName = ScriptsAndMappings.COMBINE_MIN_NAME;
            reduceName = ScriptsAndMappings.REDUCE_MIN_NAME;
        }

        String params =
                "          \"field\": \"" + encryptedField + "\",           \n" + " \"initialValue\":" + starterValue;

        String aggMinMaxScript = String
                .format(ScriptsAndMappings.AGG_BODY, ScriptsAndMappings.INIT_NAME, ScriptsAndMappings.MAP_NAME,
                        combineName, reduceName, params);

        return JSONMapper.getJsonNode(aggMinMaxScript);
    }

    public JsonNode transformBoolType(StructureSchema schema, JsonNode queryTypeBody) {

        Iterator<Map.Entry<String, JsonNode>> occurrences = queryTypeBody.fields();

        occurrences.forEachRemaining(occurrence -> {

            JsonNode occurrenceValue = occurrence.getValue();

            Iterator<Map.Entry<String, JsonNode>> queries = occurrenceValue.fields();

            queries.forEachRemaining(query -> {

                JsonNode encryptedQuery = transformQueryByType(ElasticsearchQuery.fromJsonNode(query), schema);

                ((ObjectNode) occurrenceValue).set(query.getKey(), encryptedQuery.get(query.getKey()));
            });

            ((ObjectNode) queryTypeBody).set(occurrence.getKey(), occurrenceValue);
        });

        return queryTypeBody;
    }

    public JsonNode transformMatchesAndTermsType(StructureSchema schema, JsonNode queryTypeBody,
                                                 boolean isMatchesType) {

        String requiredParameter = "query";

        if (!isMatchesType)
            requiredParameter = "value";

        Iterator<Map.Entry<String, JsonNode>> queryIterator = queryTypeBody.fields();

        if (!queryIterator.hasNext())
            throw new Exceptions.UnsupportedOperationsException("Query Malformed.");

        Map.Entry<String, JsonNode> fieldNode = queryIterator.next();

        JsonNode fieldValue = fieldNode.getValue();
        String field = fieldNode.getKey();

        Set<String> algs = schema.getSchema().get(field);

        if (algs == null)
            throw new Exceptions.UnsupportedOperationsException(
                    String.format("Field %s isn't currently configured.", field));

        if (algs.contains("NONE"))
            return queryTypeBody;

        String index = schema.getIndex();
        Object value;
        String alg = "";

        if (fieldValue.isObject()) {

            JsonNode requiredObjectField = fieldValue.get(requiredParameter);

            if (requiredObjectField == null)
                throw new Exceptions.UnsupportedOperationsException("Query Malformed.");

            if (!isMatchesType) {

                if (algs.contains("DET")) {
                    value = encryptionService
                            .encryptDet(schema.getKeys().getDet(), fieldValue.get(requiredParameter).asText());
                    alg = "DET";
                } else
                    throw new Exceptions.UnsupportedOperationsException("Field isn't configured with DET alg");
            } else {

                if (algs.contains("SEARCH") || algs.contains("SEARCH_STATIC")) {

                    value = encryptSeveralSearch(fieldValue.get(requiredParameter).asText(),
                                                 schema.getKeys().getSearch());
                    alg = "SEARCH";

                } else
                    throw new Exceptions.UnsupportedOperationsException("Field isn't configured with any SEARCH alg");

            }

            ((ObjectNode) fieldValue).set(requiredParameter, JSONMapper.convertToJsonNode(value));

            queryTypeBody = JSONMapper.createJsonNode(hashField(index, alg, field), fieldValue);

        } else if (fieldValue.isArray() && !isMatchesType) {

            if (!algs.contains("DET") && !algs.contains("NONE"))
                throw new Exceptions.UnsupportedOperationsException("Field isn't configured with DET alg");

            ArrayNode encryptedTerms = JSONMapper.createArrayNode();

            for (JsonNode term : fieldValue) {

                value = encryptionService.encryptDet(schema.getKeys().getDet(), term.asText());

                encryptedTerms.add(JSONMapper.convertToJsonNode(value));
            }

            queryTypeBody = JSONMapper.createJsonNode(hashField(index, alg, field), encryptedTerms);

        } else {

            if (algs.contains("SEARCH") || algs.contains("SEARCH_STATIC")) {

                value = encryptSeveralSearch(fieldValue.asText(), schema.getKeys().getSearch());
                alg = "SEARCH";

            } else
                throw new Exceptions.UnsupportedOperationsException("Field isn't configured with any SEARCH alg");

            queryTypeBody = JSONMapper.createJsonNode(hashField(index, alg, field), value);
        }

        return queryTypeBody;
    }

    public JsonNode transformCombinedFieldsAndMultiMatchType(StructureSchema schema, JsonNode queryTypeBody) {

        if (!queryTypeBody.isObject())
            throw new Exceptions.UnsupportedOperationsException("Query Malformed.");

        String query = queryTypeBody.get("query").asText();

        JsonNode fields = queryTypeBody.get("fields");

        if (fields == null || !fields.isArray())
            throw new Exceptions.UnsupportedOperationsException("Query Malformed.");

        String index = schema.getIndex();

        ArrayNode encryptedFields = JSONMapper.createArrayNode();

        for (JsonNode field : fields) {

            String fieldText = field.asText();
            Set<String> algs = schema.getSchema().get(fieldText);

            if (algs == null)
                throw new Exceptions.UnsupportedOperationsException(
                        String.format("Field %s isn't currently configured.", field));

            if (algs.contains("SEARCH"))
                encryptedFields.add(hashField(index, "SEARCH", fieldText));
            else if (algs.contains("SEARCH_STATIC"))
                encryptedFields.add(hashField(index, "SEARCH_STATIC", fieldText));
            else
                throw new Exceptions.UnsupportedOperationsException(
                        "Field " + fieldText + "is not configured with any SEARCH alg");
        }

        String encryptedQuery = encryptSeveralSearch(query, schema.getKeys().getSearch());

        ((ObjectNode) queryTypeBody).set("query", JSONMapper.convertToJsonNode(encryptedQuery));
        ((ObjectNode) queryTypeBody).set("fields", encryptedFields);

        return queryTypeBody;
    }


    public JsonNode transformRangeType(StructureSchema schema, JsonNode queryTypeBody) {

        Iterator<Map.Entry<String, JsonNode>> fieldRange = queryTypeBody.fields();

        if (!fieldRange.hasNext()) {
            throw new Exceptions.UnsupportedOperationsException("Query Malformed.");
        }

        Map.Entry<String, JsonNode> fieldNode = fieldRange.next();

        JsonNode fieldValue = fieldNode.getValue();
        String field = fieldNode.getKey();
        Set<String> algs = schema.getSchema().get(field);

        if (algs == null)
            throw new Exceptions.UnsupportedOperationsException(
                    String.format("Field [%s] isn't currently configured.", field));

        if (algs.contains("NONE"))
            return queryTypeBody;

        String index = schema.getIndex();

        Iterator<Map.Entry<String, JsonNode>> parameters = fieldValue.fields();

        if (!algs.contains("OPE"))
            throw new Exceptions.UnsupportedOperationsException("Field isn't configured with any OPE alg");

        parameters.forEachRemaining(entry -> {

            JsonNode encryptedValue = JSONMapper.convertToJsonNode(
                    encryptionService.encryptOPE(schema.getKeys().getOpe(), entry.getValue().asInt()));

            ((ObjectNode) fieldValue).set(entry.getKey(), encryptedValue);

        });

        queryTypeBody = JSONMapper.createJsonNode(hashField(index, "OPE", field), fieldValue);

        return queryTypeBody;
    }

    private String hashField(String index, String alg, String field) {

        StringBuilder builder = new StringBuilder();

        String[] nestedFields = field.split("[.]");

        Iterator<String> it = Arrays.stream(nestedFields).iterator();

        while (it.hasNext()) {

            String nextField = it.next();
            String hashField = cryptoUtils.hashValue(index + nextField);

            if (it.hasNext()) {

                builder.append(hashField);
                builder.append(".");
            } else {
                builder.append(alg).append(":").append(hashField);
            }

        }


        return builder.toString();
    }

    private String encryptSeveralSearch(String value, String keyString) {

        String[] terms = value.split(" ");
        StringBuilder encryptedTerms = new StringBuilder();

        SecretKey key = HomoSearch.keyFromString(keyString);

        for (String term : terms)
            encryptedTerms.append(HomoSearch.wordDigest64(key, term)).append(" ");

        return encryptedTerms.toString().trim();
    }
}
