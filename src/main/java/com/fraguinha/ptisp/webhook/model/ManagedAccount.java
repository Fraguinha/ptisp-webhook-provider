package com.fraguinha.ptisp.webhook.model;

import java.util.List;
import java.util.Optional;

import com.fraguinha.ptisp.webhook.client.PtispClient;

public record ManagedAccount(
        List<String> domains,
        PtispClient client) {
    public Optional<String> findBaseDomain(final String dnsName) {
        return Optional.ofNullable(dnsName)
                .flatMap(name -> this.domains.stream()
                        .filter(domain -> this.isMatch(name, domain))
                        .findFirst());
    }

    public boolean matches(final String dnsName) {
        return Optional.ofNullable(dnsName)
                .map(name -> this.domains.stream().anyMatch(domain -> this.isMatch(name, domain)))
                .orElse(false);
    }

    private boolean isMatch(final String dnsName, final String domain) {
        return dnsName.equals(domain) || dnsName.endsWith("." + domain);
    }
}
