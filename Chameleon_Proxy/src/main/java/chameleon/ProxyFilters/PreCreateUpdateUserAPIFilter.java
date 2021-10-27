package chameleon.ProxyFilters;

import chameleon.UserModule.UserController;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.RewriteFunction;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class PreCreateUpdateUserAPIFilter extends AbstractGatewayFilterFactory<PreCreateUpdateUserAPIFilter.Config> {

    final UserController userHandler;
    final FiltersChangeUtils filtersChangeUtils;

    public PreCreateUpdateUserAPIFilter(FiltersChangeUtils bodyChangeUtils, UserController userHandler) {
        super(Config.class);

        this.filtersChangeUtils = bodyChangeUtils;
        this.userHandler = userHandler;
    }

    private RewriteFunction<String, String> encryptUser() {

        return (exchange, originalRequestBody) -> {

            String encryptedUser = userHandler.createUser(originalRequestBody, "");

            return Mono.just(encryptedUser);
        };
    }

    private String[] getEncryptedUsername(ServerWebExchange exchange) {

        ElasticsearchRequest esRequest = new ElasticsearchRequest(exchange.getRequest());

        String username = esRequest.getResourceId();

        String authHeader = filtersChangeUtils.getAuthHeader(exchange);

        return new String[]{username, userHandler.getEncryptedUsername(username, authHeader)};
    }

    @Override
    public GatewayFilter apply(Config config) {

        return (exchange, chain) -> {

            String[] usernames = getEncryptedUsername(exchange);

            ServerHttpRequest request = filtersChangeUtils.changeRequestRoute(exchange, usernames);

            return filtersChangeUtils
                    .changeRequestBody(exchange.mutate().request(request).build(), chain, encryptUser());
        };

    }

    public static class Config {
        // ...
    }
}
