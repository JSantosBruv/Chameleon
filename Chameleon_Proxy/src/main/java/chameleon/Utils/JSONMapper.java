package chameleon.Utils;

import chameleon.Exceptions.Exceptions;
import chameleon.Models.Views;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.IOException;

public class JSONMapper {

    private final static ObjectMapper objectMapper = new ObjectMapper();

    public static String writeObjectAsStringWithView(Object body) throws JsonProcessingException {

        return objectMapper.writerWithView(Views.ElasticUserPOST.class).writeValueAsString(body);
    }

    public static String writeValueAsString(Object body) throws JsonProcessingException {

        return objectMapper.writeValueAsString(body);
    }


    public static Object readJSON(String body, Class<?> view) throws JsonProcessingException {

        return objectMapper.readValue(body, view);
    }

    public static Object writeStringAsObject(String body, Class<?> type) {

        try {
            return objectMapper.readValue(body, type);
        } catch (JsonProcessingException e) {
            throw new Exceptions.UnsupportedOperationsException("JSON Input Malformed.");
        }

    }

    public static String ignoreFirstKey(String body) {

        //Remove first key in order to obtain the json value
        String removeFirstKey = body.substring(body.indexOf(":") + 1);
        //Remove last bracket
        return removeFirstKey.substring(0, removeFirstKey.length() - 1);
    }

    public static JsonNode getJsonNode(String json) {

        try {
            return objectMapper.readTree(json);
        } catch (IOException e) {

            throw new Exceptions.UnsupportedOperationsException("JSON Input Malformed.");
        }
    }

    public static ArrayNode createArrayNode() {
        return objectMapper.createArrayNode();
    }

    public static JsonNode createJsonNode(String key, Object value) {

        return objectMapper.createObjectNode().set(key, convertToJsonNode(value));
    }

    public static JsonNode createJsonNode() {
        return objectMapper.createObjectNode();
    }

    public static JsonNode convertToJsonNode(Object value) {

        return objectMapper.convertValue(value, JsonNode.class);

    }
}
