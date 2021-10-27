package chameleon.Utils;

import chameleon.Exceptions.Exceptions;
import chameleon.Models.ProxyUser;
import org.elasticsearch.client.*;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RequestBuilder {

    public static final String AUTH_HEADER = "Authorization";

    final RestClient lowLevelClient;

    public RequestBuilder(RestClient lowLevelClient) {
        this.lowLevelClient = lowLevelClient;
    }

    private RequestOptions.Builder addBasicAuth(String auth) {

        RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();

        if (auth == null)
            throw new Exceptions.NoAuthException("Auth Header Required");

        return builder.addHeader(AUTH_HEADER, auth);
    }

    public Response performRequest(Request request, Object body, String auth) {

        RequestOptions.Builder builder = addBasicAuth(auth);

        request.addParameter("pretty", "true");
        request.setOptions(builder);

        try {

            if (body != null) {

                if (body instanceof ProxyUser)
                    request.setJsonEntity(JSONMapper.writeObjectAsStringWithView(body));
                else if (body instanceof byte[])
                    request.setJsonEntity(new String((byte[]) body));
                else if (body instanceof String)
                    request.setJsonEntity((String)body);
                else
                    request.setJsonEntity(JSONMapper.writeValueAsString(body));

            }

            return lowLevelClient.performRequest(request);

        } catch (IOException e) {

            if (e instanceof ResponseException) {
                ResponseException resEx = (ResponseException) e;

                return resEx.getResponse();
            }
            e.printStackTrace();
            throw new Exceptions.ElasticException("Could not reach ES instance");

        }

    }

}

