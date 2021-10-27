package chameleon.ProxyFilters;

import chameleon.SearchModule.SearchController;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.RewriteFunction;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class PreSearchAPIFilter extends AbstractGatewayFilterFactory<PreSearchAPIFilter.Config> {

    final SearchController queryHandler;
    final FiltersChangeUtils filtersChangeUtils;

    public PreSearchAPIFilter(FiltersChangeUtils bodyChangeUtils, SearchController queryHandler) {
        super(Config.class);

        this.filtersChangeUtils = bodyChangeUtils;
        this.queryHandler = queryHandler;
    }

    private RewriteFunction<String, String> encryptQuery() {

        return (exchange, originalRequestBody) -> {

            ElasticsearchRequest request = new ElasticsearchRequest(exchange.getRequest());

            String index = request.getIndex();

            String authHeader = filtersChangeUtils.getAuthHeader(exchange);

            String encryptedQuery = queryHandler.encryptQuery(index, originalRequestBody, authHeader);

            return Mono.just(encryptedQuery);
        };
    }

    @Override
    public GatewayFilter apply(Config config) {

        return (exchange, chain) -> filtersChangeUtils.changeRequestBody(exchange, chain, encryptQuery());

    }

    public static class Config {
        // ...
    }
}
