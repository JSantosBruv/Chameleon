package chameleon.ProxyFilters;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

public class ElasticsearchRequest {

    private String index;
    private String resourceId;
    private int status;

    public ElasticsearchRequest(ServerHttpRequest request) {

        parseRequest(request);
    }

    private void parseRequest(ServerHttpRequest request) {

        String[] tokens = request.getURI().getPath().substring(1).split("/");

        parseIndex(tokens);
        parseDocId(tokens);
    }

    private void parseIndex(String[] tokens) {

        if (tokens.length > 0)
            index = tokens[0];

    }


    private void parseDocId(String[] tokens) {

        if (tokens.length > 2) {

            String possibleResourceId = tokens[2];

            if (!possibleResourceId.startsWith("_"))
                resourceId = possibleResourceId;

        }
    }

    protected String getIndex() {
        return index;
    }

    protected String getResourceId() {
        return resourceId;
    }

    protected boolean responseIs2xx(ServerHttpResponse response) {return response.getStatusCode().is2xxSuccessful();}

}
