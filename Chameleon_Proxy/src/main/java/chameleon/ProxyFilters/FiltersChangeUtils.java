package chameleon.ProxyFilters;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.rewrite.*;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Set;

@Component
public class FiltersChangeUtils {

    public static final String AUTH_HEADER = "Authorization";

    private final Set<MessageBodyDecoder> messageBodyDecoders;
    private final Set<MessageBodyEncoder> messageBodyEncoders;
    private final CodecConfigurer codecConfigurer;

    public FiltersChangeUtils(Set<MessageBodyDecoder> messageBodyDecoders, Set<MessageBodyEncoder> messageBodyEncoders,
                              CodecConfigurer codecConfigurer) {
        this.messageBodyEncoders = messageBodyEncoders;
        this.messageBodyDecoders = messageBodyDecoders;
        this.codecConfigurer = codecConfigurer;
    }

    protected Mono<Void> changeRequestBody(ServerWebExchange exchange, GatewayFilterChain chain,
                                           RewriteFunction<String, String> applyChange) {


        ModifyRequestBodyGatewayFilterFactory.Config modifyRequestConfig =
                new ModifyRequestBodyGatewayFilterFactory.Config()
                .setContentType("application/json").setRewriteFunction(String.class, String.class, applyChange);

        return new ModifyRequestBodyGatewayFilterFactory(codecConfigurer.getReaders()).apply(modifyRequestConfig)
                                                                                      .filter(exchange, chain);

    }


    protected Mono<Void> changeResponseBody(ServerWebExchange exchange, GatewayFilterChain chain,
                                            RewriteFunction<String, String> applyChange) {


        ModifyResponseBodyGatewayFilterFactory.Config modifyResponseConfig =
                new ModifyResponseBodyGatewayFilterFactory.Config()
                .setNewContentType("application/json").setRewriteFunction(String.class, String.class, applyChange);

        return new ModifyResponseBodyGatewayFilterFactory(codecConfigurer.getReaders(), messageBodyDecoders,
                                                          messageBodyEncoders).apply(modifyResponseConfig)
                                                                              .filter(exchange, chain);

    }

    protected ServerHttpRequest changeRequestRoute(ServerWebExchange exchange, String[] usernames) {

        ServerHttpRequest req = exchange.getRequest();
        ServerWebExchangeUtils.addOriginalRequestUrl(exchange, req.getURI());
        String path = req.getURI().getRawPath();
        String newPath = path.replaceAll(usernames[0], usernames[1]);
        ServerHttpRequest request = req.mutate().path(newPath).build();
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, request.getURI());

        return request;
    }

    protected String getAuthHeader(ServerWebExchange exchange) {

        ServerHttpRequest req = exchange.getRequest();

        return req.getHeaders().getFirst(AUTH_HEADER);

    }

}
