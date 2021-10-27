package chameleon.ProxyFilters;

import chameleon.DataModule.DataController;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.RewriteFunction;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class PreIndexAPIFilter extends AbstractGatewayFilterFactory<PreIndexAPIFilter.Config> {

    final DataController dataHandler;
    final FiltersChangeUtils filtersChangeUtils;

    public PreIndexAPIFilter(FiltersChangeUtils bodyChangeUtils, DataController dataHandler) {
        super(Config.class);

        this.filtersChangeUtils = bodyChangeUtils;
        this.dataHandler = dataHandler;
    }

    private RewriteFunction<String, String> encryptIndex() {

        return (exchange, originalRequestBody) -> {



            ElasticsearchRequest request = new ElasticsearchRequest(exchange.getRequest());
            String index = request.getIndex();

            String authHeader = filtersChangeUtils.getAuthHeader(exchange);
            String encryptedIndex = dataHandler.index(index, originalRequestBody, authHeader);

            return Mono.just(encryptedIndex);
        };
    }

    @Override
    public GatewayFilter apply(Config config) {

        return (exchange, chain) -> filtersChangeUtils.changeRequestBody(exchange, chain, encryptIndex());

    }

    public static class Config {
        // ...
    }
}

