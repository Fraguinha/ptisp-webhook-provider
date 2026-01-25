package com.fraguinha.ptisp.webhook.monitoring;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import com.fraguinha.ptisp.webhook.service.DnsService;

@ExtendWith(MockitoExtension.class)
class PtispHealthIndicatorTest {

    @Mock
    private DnsService dnsService;

    private PtispHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        this.healthIndicator = new PtispHealthIndicator(this.dnsService);
    }

    @Test
    void healthUpWhenAccountsExist() {
        Mockito.when(this.dnsService.getAccountCount()).thenReturn(2);

        final Health health = this.healthIndicator.health();

        Assertions.assertEquals(Status.UP, health.getStatus());
        Assertions.assertEquals(2, health.getDetails().get("activeAccounts"));
    }

    @Test
    void healthDownWhenNoAccounts() {
        Mockito.when(this.dnsService.getAccountCount()).thenReturn(0);

        final Health health = this.healthIndicator.health();

        Assertions.assertEquals(Status.DOWN, health.getStatus());
    }
}
