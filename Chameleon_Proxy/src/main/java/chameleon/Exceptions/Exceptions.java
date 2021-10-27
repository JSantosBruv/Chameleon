package chameleon.Exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class Exceptions {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class UnsupportedOperationsException extends RuntimeException {

        public UnsupportedOperationsException(String message) {
            super(message);
        }
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public static class ElasticException extends RuntimeException {

        public ElasticException(String message) {
            super(message);
        }
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public static class NoAuthException extends RuntimeException {

        public NoAuthException(String message) {
            super(message);
        }
    }


}


