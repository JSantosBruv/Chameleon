package chameleon.Models;

import chameleon.Exceptions.Exceptions;
import chameleon.Utils.JSONMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;


public final class ElasticsearchQuery {


    public enum QUERY_TYPES {
        MATCH, MATCH_PHRASE, MATCH_ALL, COMBINED_FIELDS, MULTI_MATCH, BOOL, TERM, TERMS, RANGE
    }

    public enum AGGS_TYPES {
        MAX, MIN, SUM, AVG
    }

    public enum MATCH_TYPES {
        MATCH, MATCH_PHRASE
    }

    private final JsonNode queryNode;
    private final JsonNode aggsNode;
    private final JsonNode rootNode;

    public ElasticsearchQuery(JsonNode rootNode) {

        this.rootNode = rootNode;

        JsonNode queryNode = rootNode.path("query");
        JsonNode aggsNode = rootNode.path("aggs");

        this.queryNode = queryNode.isMissingNode() ? null : queryNode;
        this.aggsNode = aggsNode.isMissingNode() ? null : aggsNode;

    }

    public boolean hasQueryBody() {
        return queryNode != null;
    }

    public boolean hasAggsBody() {
        return aggsNode != null;
    }

    public Map.Entry<String, JsonNode> getQueryBody() {

        return queryNode.fields().next();

    }

    public Map.Entry<String, JsonNode> getAggsBody() {

        return aggsNode.fields().next();

    }

    public JsonNode getRootNode() {
        return rootNode;
    }

    public void setEncryptedQueryNode(JsonNode queryNode) {

        ((ObjectNode) rootNode).set("query", queryNode);
    }

    public void setEncryptedAggregationNode(JsonNode aggsNode) {

        ((ObjectNode) rootNode).set("aggs", aggsNode);
    }

    public static ElasticsearchQuery fromJson(String json) {

        if (json == null || json.equals(""))
            throw new Exceptions.UnsupportedOperationsException("Empty Query.");

        JsonNode rootNode = JSONMapper.getJsonNode(json);

        return new ElasticsearchQuery(rootNode);
    }

    public static ElasticsearchQuery fromJsonNode(Map.Entry<String, JsonNode> query) {

        return new ElasticsearchQuery(JSONMapper.createJsonNode("query", query));
    }

    public boolean isMalformed() {

        if (queryNode != null && queryNode.fields().hasNext())
            return false;

        return aggsNode == null || !aggsNode.fields().hasNext();
    }

    public boolean isValidQuery(String queryType) {

        for (QUERY_TYPES type : QUERY_TYPES.values())
            if (type.name().equalsIgnoreCase(queryType))
                return true;

        return false;
    }

    public boolean isValidAggregation(String aggType) {

        for (AGGS_TYPES type : AGGS_TYPES.values())
            if (type.name().equalsIgnoreCase(aggType))
                return true;

        return false;
    }

    public boolean isTypeMatches(String queryType) {

        for (MATCH_TYPES type : MATCH_TYPES.values())
            if (type.name().equalsIgnoreCase(queryType))
                return true;

        return false;
    }

    public boolean isTypeCombinedFields(String queryType) {
        return QUERY_TYPES.COMBINED_FIELDS.name().equalsIgnoreCase(queryType);
    }

    public boolean isTypeMatchAll(String queryType) {
        return QUERY_TYPES.MATCH_ALL.name().equalsIgnoreCase(queryType);
    }

    public boolean isTypeMultiMatch(String queryType) {
        return QUERY_TYPES.MULTI_MATCH.name().equalsIgnoreCase(queryType);
    }

    public boolean isTypeBool(String queryType) {
        return QUERY_TYPES.BOOL.name().equalsIgnoreCase(queryType);
    }


    public boolean isTypeTerms(String queryType) {
        return QUERY_TYPES.TERM.name().equalsIgnoreCase(queryType) ||
                QUERY_TYPES.TERMS.name().equalsIgnoreCase(queryType);
    }

    public boolean isTypeRange(String queryType) {
        return QUERY_TYPES.RANGE.name().equalsIgnoreCase(queryType);
    }

    public boolean isTypeMinMax(String aggType) {
        return AGGS_TYPES.MAX.name().equalsIgnoreCase(aggType) || AGGS_TYPES.MIN.name().equalsIgnoreCase(aggType);
    }

    public boolean isTypeSum(String aggType) {
        return AGGS_TYPES.SUM.name().equalsIgnoreCase(aggType);
    }

    public boolean isTypeAvg(String aggType) {
        return AGGS_TYPES.AVG.name().equalsIgnoreCase(aggType);
    }
}

