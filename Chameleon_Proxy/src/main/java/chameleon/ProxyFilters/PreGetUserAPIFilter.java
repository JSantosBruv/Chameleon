package chameleon.ProxyFilters;

import chameleon.UserModule.UserController;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

@Component
public class PreGetUserAPIFilter extends AbstractGatewayFilterFactory<PreGetUserAPIFilter.Config> {

    final UserController userHandler;
    final FiltersChangeUtils filtersChangeUtils;

    public PreGetUserAPIFilter(FiltersChangeUtils bodyChangeUtils, UserController userHandler) {
        super(Config.class);

        this.filtersChangeUtils = bodyChangeUtils;
        this.userHandler = userHandler;
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

            return chain.filter(exchange.mutate().request(request).build());
        };

    }

    public static class Config {
        // ...
    }
}
