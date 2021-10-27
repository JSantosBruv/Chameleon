package chameleon.ProxyFilters;

import chameleon.DataModule.DataController;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

@Component
public class PreDeleteIndexAPIFilter extends AbstractGatewayFilterFactory<PreDeleteIndexAPIFilter.Config> {

    final DataController dataHandler;
    final FiltersChangeUtils filtersChangeUtils;

    public PreDeleteIndexAPIFilter(FiltersChangeUtils bodyChangeUtils, DataController dataHandler) {
        super(Config.class);

        this.filtersChangeUtils = bodyChangeUtils;
        this.dataHandler = dataHandler;
    }

    @Override
    public GatewayFilter apply(Config config) {

        return (exchange, chain) -> {

            ElasticsearchRequest request = new ElasticsearchRequest(exchange.getRequest());
            String index = request.getIndex();
            String authHeader = filtersChangeUtils.getAuthHeader(exchange);
           
	   if(!index.equals("logsconfig"))
	     	dataHandler.deleteIndex(index, authHeader);

            return chain.filter(exchange);

        };

    }

    public static class Config {
        // ...
    }
}

