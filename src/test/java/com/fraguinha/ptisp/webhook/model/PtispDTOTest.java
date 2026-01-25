package com.fraguinha.ptisp.webhook.model;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PtispDTOTest {

    @Test
    void fromExternalDnsMapsA() {
        final ExternalDnsDTO ep = ExternalDnsDTO.of("test.com", List.of("1.1.1.1"), "A", 300L);
        final PtispDTO dto = PtispDTO.fromExternalDns(ep, "1.1.1.1");
        Assertions.assertEquals("test.com.", dto.name());
        Assertions.assertEquals("1.1.1.1", dto.address());
    }

    @Test
    void fromExternalDnsMapsCnameWithDot() {
        final ExternalDnsDTO ep = ExternalDnsDTO.of("test.com", List.of("target.com"), "CNAME", 300L);
        final PtispDTO dto = PtispDTO.fromExternalDns(ep, "target.com");
        Assertions.assertEquals("target.com.", dto.cname());
    }

    @Test
    void fromExternalDnsMapsTxtWithQuotes() {
        final ExternalDnsDTO ep = ExternalDnsDTO.of("test.com", List.of("hello"), "TXT", 300L);
        final PtispDTO dto = PtispDTO.fromExternalDns(ep, "hello");
        Assertions.assertEquals("\"hello\"", dto.txtdata());
    }

    @Test
    void fromExternalDnsThrowsOnUnsupportedType() {
        final ExternalDnsDTO ep = ExternalDnsDTO.of("test.com", List.of("1.1.1.1"), "MX", 300L);
        Assertions.assertThrows(IllegalArgumentException.class, () -> PtispDTO.fromExternalDns(ep, "1.1.1.1"));
    }

    @Test
    void toExternalDnsMapsBack() {
        final PtispDTO dto = new PtispDTO("1", "test.com.", "A", "IN", 300L, "1.1.1.1", null, null);
        final ExternalDnsDTO ep = dto.toExternalDns();
        Assertions.assertEquals("test.com", ep.dnsName());
        Assertions.assertEquals("A", ep.recordType());
        Assertions.assertEquals(List.of("1.1.1.1"), ep.targets());
        Assertions.assertEquals(300L, ep.ttl());
    }
}
