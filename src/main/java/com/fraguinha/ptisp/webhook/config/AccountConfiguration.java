package com.fraguinha.ptisp.webhook.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.fraguinha.ptisp.webhook.model.ManagedAccount;
import com.fraguinha.ptisp.webhook.service.AccountDiscoveryService;

@Configuration
public class AccountConfiguration {

    private static final int API_TIMEOUT_MS = 15000;

    @Bean
    protected List<ManagedAccount> managedAccounts(final RestClient.Builder builder,
            final AccountDiscoveryService discoveryService) {
        final SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(AccountConfiguration.API_TIMEOUT_MS);
        factory.setReadTimeout(AccountConfiguration.API_TIMEOUT_MS);

        return discoveryService.discoverAccounts(System.getenv(), builder.requestFactory(factory));
    }
}
