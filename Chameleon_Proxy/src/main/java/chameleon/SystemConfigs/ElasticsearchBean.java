package chameleon.SystemConfigs;

import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

@Configuration
public class ElasticsearchBean extends AbstractElasticsearchConfiguration {


    @Override
    @Primary
    @Bean
    public RestHighLevelClient elasticsearchClient() {

        try {
            KeyStore truststore = KeyStore.getInstance("pkcs12");
            try (InputStream is = new ClassPathResource("Keys/chameleonTS").getInputStream()) {
                truststore.load(is, "chameleon".toCharArray());
            }

            SSLContextBuilder sslBuilder = SSLContexts.custom().loadTrustMaterial(truststore, null);


            final ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                                                                               .connectedTo("chameleon-es-http" +
                                                                                                    ":9200")
                                                                               .usingSsl(sslBuilder.build()).build();
            return RestClients.create(clientConfiguration).rest();

        } catch (NoSuchAlgorithmException | KeyStoreException | IOException | CertificateException | KeyManagementException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Bean
    public RestClient ESLowLevelClient() {

        return elasticsearchClient().getLowLevelClient();
    }

}
