package chameleon.ProxyFilters;

import chameleon.DataModule.DataController;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.NettyWriteResponseFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.RewriteFunction;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class PostGetAPIFilter extends AbstractGatewayFilterFactory<PostGetAPIFilter.Config> {

    final DataController dataHandler;
    final FiltersChangeUtils filtersChangeUtils;

    public PostGetAPIFilter(FiltersChangeUtils bodyChangeUtils, DataController dataHandler) {
        super(Config.class);

        this.filtersChangeUtils = bodyChangeUtils;
        this.dataHandler = dataHandler;
    }

    private RewriteFunction<String, String> getDocument() {

        return (exchange, originalRequestBody) -> {

            ElasticsearchRequest request = new ElasticsearchRequest(exchange.getRequest());

            String index = request.getIndex();

            String authHeader = filtersChangeUtils.getAuthHeader(exchange);

            String encryptedIndex = dataHandler.getEncryptedDoc(index, originalRequestBody, authHeader);

            return Mono.just(encryptedIndex);
        };
    }

    @Override
    public GatewayFilter apply(Config config) {

        return new OrderedGatewayFilter(
                (exchange, chain) -> filtersChangeUtils.changeResponseBody(exchange, chain, getDocument())

                , NettyWriteResponseFilter.WRITE_RESPONSE_FILTER_ORDER - 1);

    }

    public static class Config {
        // ...
    }
}
