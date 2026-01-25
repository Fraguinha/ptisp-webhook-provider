package com.fraguinha.ptisp.webhook.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fraguinha.ptisp.webhook.client.PtispClient;
import com.fraguinha.ptisp.webhook.model.ManagedAccount;

@Service
public class AccountDiscoveryService {
    private static final Logger log = LoggerFactory.getLogger(AccountDiscoveryService.class);

    public List<ManagedAccount> discoverAccounts(final Map<String, String> env, final RestClient.Builder builder) {
        final RestClient baseClient = builder.baseUrl("https://api.ptisp.pt").build();

        return env.entrySet().stream()
                .filter(e -> e.getKey().startsWith("PTISP_") && e.getKey().endsWith("_DOMAINS"))
                .flatMap(e -> {
                    final String prefix = e.getKey().substring(0, e.getKey().length() - "_DOMAINS".length());
                    return Optional.ofNullable(env.get(prefix + "_EMAIL"))
                            .flatMap(email -> Optional.ofNullable(env.get(prefix + "_HASH"))
                                    .map(hash -> {
                                        final List<String> domains = Arrays.stream(e.getValue().split(","))
                                                .map(String::trim)
                                                .toList();

                                        final RestClient client = baseClient.mutate()
                                                .defaultHeaders(h -> h.setBasicAuth(email, hash))
                                                .build();

                                        AccountDiscoveryService.log.info("Registered PTISP account: {} for domains {}", email, domains);
                                        return new ManagedAccount(domains, new PtispClient(email, client));
                                    }))
                            .stream();
                })
                .toList();
    }
}
