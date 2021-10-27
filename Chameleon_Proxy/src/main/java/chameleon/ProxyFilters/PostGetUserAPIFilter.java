package chameleon.ProxyFilters;

import chameleon.UserModule.UserController;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.NettyWriteResponseFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.RewriteFunction;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class PostGetUserAPIFilter extends AbstractGatewayFilterFactory<PostGetUserAPIFilter.Config> {

    final UserController userHandler;
    final FiltersChangeUtils filtersChangeUtils;

    public PostGetUserAPIFilter(FiltersChangeUtils bodyChangeUtils, UserController userHandler) {
        super(Config.class);

        this.filtersChangeUtils = bodyChangeUtils;
        this.userHandler = userHandler;

    }

    private RewriteFunction<String, String> decryptUser() {

        return (exchange, originalResponseBody) -> {

            String authHeader = filtersChangeUtils.getAuthHeader(exchange);

            String decryptedUser = userHandler.decryptUser(originalResponseBody, authHeader);

            return Mono.just(decryptedUser);
        };
    }

    @Override
    public GatewayFilter apply(Config config) {


        return new OrderedGatewayFilter(
                (exchange, chain) -> filtersChangeUtils.changeResponseBody(exchange, chain, decryptUser())

                , NettyWriteResponseFilter.WRITE_RESPONSE_FILTER_ORDER - 1);

    }

    public static class Config {
        // ...
    }
}
