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
    void fromExternalDnsDefaultsTtlWhenZero() {
        final ExternalDnsDTO ep = ExternalDnsDTO.of("test.com", List.of("1.1.1.1"), "A", 0L);
        Assertions.assertEquals(300L, PtispDTO.fromExternalDns(ep, "1.1.1.1").ttl());
    }

    @Test
    void fromExternalDnsDefaultsTtlWhenNull() {
        final ExternalDnsDTO ep = ExternalDnsDTO.of("test.com", List.of("1.1.1.1"), "A", null);
        Assertions.assertEquals(300L, PtispDTO.fromExternalDns(ep, "1.1.1.1").ttl());
    }

    @Test
    void fromExternalDnsRespectsExplicitTtl() {
        final ExternalDnsDTO ep = ExternalDnsDTO.of("test.com", List.of("1.1.1.1"), "A", 60L);
        Assertions.assertEquals(60L, PtispDTO.fromExternalDns(ep, "1.1.1.1").ttl());
    }

    @Test
    void fromExternalDnsThrowsOnUnsupportedType() {
        final ExternalDnsDTO ep = ExternalDnsDTO.of("test.com", List.of("1.1.1.1"), "MX", 300L);

        Assertions.assertThrows(IllegalArgumentException.class, () -> PtispDTO.fromExternalDns(ep, "1.1.1.1"));
    }

    @Test
    void fromExternalDnsAlreadyQuotedTxtIsNotDoubleQuoted() {
        final ExternalDnsDTO ep = ExternalDnsDTO.of("test.com", List.of("\"v=spf1\""), "TXT", 300L);

        final PtispDTO dto = PtispDTO.fromExternalDns(ep, "\"v=spf1\"");

        Assertions.assertEquals("\"v=spf1\"", dto.txtdata());
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

    @Test
    void getDnsTargetReturnsAddressForARecord() {
        final PtispDTO dto = new PtispDTO("1", "test.com.", "A", "IN", 300L, "1.2.3.4", "", "");
        Assertions.assertEquals("1.2.3.4", dto.getDnsTarget());
    }

    @Test
    void getDnsTargetReturnsCnameWithoutTrailingDot() {
        final PtispDTO dto = new PtispDTO("1", "test.com.", "CNAME", "IN", 300L, "", "target.com.", "");
        Assertions.assertEquals("target.com", dto.getDnsTarget());
    }

    @Test
    void getDnsTargetReturnsTxtdata() {
        final PtispDTO dto = new PtispDTO("1", "test.com.", "TXT", "IN", 300L, "", "", "\"v=spf1\"");
        Assertions.assertEquals("\"v=spf1\"", dto.getDnsTarget());
    }

    @Test
    void getDnsTargetFallsBackToAddressForUnknownType() {
        final PtispDTO dto = new PtispDTO("1", "test.com.", "MX", "IN", 300L, "mail.example.com", "", "");
        Assertions.assertEquals("mail.example.com", dto.getDnsTarget());
    }
}
