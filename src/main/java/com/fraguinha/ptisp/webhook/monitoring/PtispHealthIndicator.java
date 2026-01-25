package com.fraguinha.ptisp.webhook.monitoring;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import com.fraguinha.ptisp.webhook.service.DnsService;

@Component
public class PtispHealthIndicator implements HealthIndicator {

    private final DnsService dnsService;

    public PtispHealthIndicator(final DnsService dnsService) {
        this.dnsService = dnsService;
    }

    @Override
    public Health health() {
        final int accountCount = this.dnsService.getAccountCount();

        if (accountCount == 0) {
            return Health.down()
                    .withDetail("reason", "No PTISP accounts configured")
                    .build();
        }

        return Health.up()
                .withDetail("activeAccounts", accountCount)
                .build();
    }
}
