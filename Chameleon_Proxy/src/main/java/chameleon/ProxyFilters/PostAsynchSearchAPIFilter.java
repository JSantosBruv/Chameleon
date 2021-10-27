package chameleon.ProxyFilters;

import chameleon.SearchModule.SearchController;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.NettyWriteResponseFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.RewriteFunction;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class PostAsynchSearchAPIFilter extends AbstractGatewayFilterFactory<PostAsynchSearchAPIFilter.Config> {

    final SearchController queryHandler;
    final FiltersChangeUtils filtersChangeUtils;

    public PostAsynchSearchAPIFilter(FiltersChangeUtils bodyChangeUtils, SearchController queryHandler) {
        super(Config.class);

        this.filtersChangeUtils = bodyChangeUtils;
        this.queryHandler = queryHandler;

    }

    private RewriteFunction<String, String> decryptSearchResults() {

        return (exchange, originalResponseBody) -> {

            ElasticsearchRequest request = new ElasticsearchRequest(exchange.getRequest());

            if(!request.responseIs2xx(exchange.getResponse()))
                    return  Mono.just(originalResponseBody);

            String index = request.getIndex();

            String authHeader = filtersChangeUtils.getAuthHeader(exchange);

            String decryptedResults = queryHandler.decryptAsynchSearchResults(index, originalResponseBody, authHeader);

            return Mono.just(decryptedResults);
        };
    }

    @Override
    public GatewayFilter apply(Config config) {


        return new OrderedGatewayFilter(
                (exchange, chain) -> filtersChangeUtils.changeResponseBody(exchange, chain, decryptSearchResults())

                , NettyWriteResponseFilter.WRITE_RESPONSE_FILTER_ORDER - 1);

    }

    public static class Config {
        // ...
    }
}
