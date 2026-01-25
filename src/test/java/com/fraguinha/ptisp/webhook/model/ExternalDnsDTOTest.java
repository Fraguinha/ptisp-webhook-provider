package com.fraguinha.ptisp.webhook.model;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ExternalDnsDTOTest {

    @Test
    void constructorNormalizesDnsName() {
        final ExternalDnsDTO dto = new ExternalDnsDTO("example.com.", null, null, null, null, null);
        Assertions.assertEquals("example.com", dto.dnsName());
    }

    @Test
    void constructorNormalizesCnameTargets() {
        final ExternalDnsDTO dto = new ExternalDnsDTO("example.com", List.of("target.com."), "CNAME", null, null, null);
        Assertions.assertEquals(List.of("target.com"), dto.targets());
    }

    @Test
    void constructorDoesNotNormalizeOtherTargets() {
        final ExternalDnsDTO dto = new ExternalDnsDTO("example.com", List.of("1.1.1.1"), "A", null, null, null);
        Assertions.assertEquals(List.of("1.1.1.1"), dto.targets());
    }

    @Test
    void constructorSetsDefaults() {
        final ExternalDnsDTO dto = new ExternalDnsDTO(null, null, null, null, null, null);
        Assertions.assertEquals("", dto.dnsName());
        Assertions.assertEquals("", dto.recordType());
        Assertions.assertTrue(dto.targets().isEmpty());
        Assertions.assertEquals(WebhookConstants.DEFAULT_TTL, dto.ttl());
        Assertions.assertTrue(dto.labels().isEmpty());
        Assertions.assertTrue(dto.providerSpecific().isEmpty());
    }

    @Test
    void staticOfCreatesWithDefaults() {
        final ExternalDnsDTO dto = ExternalDnsDTO.of("test.com", List.of("1.1.1.1"), "A");
        Assertions.assertEquals(WebhookConstants.DEFAULT_TTL, dto.ttl());
        Assertions.assertTrue(dto.labels().isEmpty());
    }
}
