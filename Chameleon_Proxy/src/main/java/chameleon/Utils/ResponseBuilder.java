package chameleon.Utils;

import chameleon.Exceptions.Exceptions;
import org.apache.http.Header;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Response;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;


public class ResponseBuilder {


    public static ResponseEntity<?> buildResponseEntity(Response response) {

        try {
            String body = EntityUtils.toString(response.getEntity());

            int status = response.getStatusLine().getStatusCode();

            Header[] responseHeaders = response.getHeaders();

            HttpHeaders headers = new HttpHeaders();

            for (Header responseHeader : responseHeaders)
                headers.add(responseHeader.getName(), responseHeader.getValue());


            return new ResponseEntity<>(body, headers, HttpStatus.valueOf(status));

        } catch (IOException e) {

            return new ResponseEntity<>("Internal Error reading response.", HttpStatus.valueOf(500));
        }


    }

    public static String getResponseBody(Response response) {
        try {
            return EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            throw new Exceptions.ElasticException("Couldn't obtain response body from Response.");
        }

    }

    public static boolean documentExist(Response response) {

        int statusRequest = response.getStatusLine().getStatusCode();

        if (statusRequest == 401)
            throw new Exceptions.NoAuthException("Wrong Authentication. Unauthorized.");

        return statusRequest <= 204 && statusRequest >= 200;
    }

}
