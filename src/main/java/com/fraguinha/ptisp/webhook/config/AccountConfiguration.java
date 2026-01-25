package com.fraguinha.ptisp.webhook.config;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.fraguinha.ptisp.webhook.model.ManagedAccount;
import com.fraguinha.ptisp.webhook.service.AccountDiscoveryService;

@Configuration
public class AccountConfiguration {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(15);

    @Bean
    protected List<ManagedAccount> managedAccounts(final RestClient.Builder builder,
            final AccountDiscoveryService discoveryService) {
        final HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(AccountConfiguration.CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        final JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(AccountConfiguration.READ_TIMEOUT);

        return discoveryService.discoverAccounts(System.getenv(), builder.requestFactory(factory));
    }
}
