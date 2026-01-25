package com.fraguinha.ptisp.webhook.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DnsUtilsTest {

    @Test
    void removeTrailingDotRemovesDot() {
        Assertions.assertEquals("example.com", DnsUtils.removeTrailingDot("example.com."));
    }

    @Test
    void removeTrailingDotIgnoredIfMissing() {
        Assertions.assertEquals("example.com", DnsUtils.removeTrailingDot("example.com"));
    }

    @Test
    void removeTrailingDotHandlesNullAndEmpty() {
        Assertions.assertEquals("", DnsUtils.removeTrailingDot(null));
        Assertions.assertEquals("", DnsUtils.removeTrailingDot(""));
    }

    @Test
    void removeQuotesRemovesQuotes() {
        Assertions.assertEquals("text", DnsUtils.removeQuotes("\"text\""));
    }

    @Test
    void removeQuotesIgnoredIfMissing() {
        Assertions.assertEquals("text", DnsUtils.removeQuotes("text"));
        Assertions.assertEquals("\"text", DnsUtils.removeQuotes("\"text"));
        Assertions.assertEquals("text\"", DnsUtils.removeQuotes("text\""));
    }

    @Test
    void removeQuotesHandlesNullAndEmpty() {
        Assertions.assertEquals("", DnsUtils.removeQuotes(null));
        Assertions.assertEquals("", DnsUtils.removeQuotes(""));
    }

    @Test
    void ensureTrailingDotAddsDot() {
        Assertions.assertEquals("example.com.", DnsUtils.ensureTrailingDot("example.com"));
    }

    @Test
    void ensureTrailingDotIgnoredIfPresent() {
        Assertions.assertEquals("example.com.", DnsUtils.ensureTrailingDot("example.com."));
    }

    @Test
    void ensureTrailingDotHandlesNullAndEmpty() {
        Assertions.assertEquals("", DnsUtils.ensureTrailingDot(null));
        Assertions.assertEquals("", DnsUtils.ensureTrailingDot(""));
    }

    @Test
    void ensureQuotesAddsQuotes() {
        Assertions.assertEquals("\"text\"", DnsUtils.ensureQuotes("text"));
    }

    @Test
    void ensureQuotesIgnoredIfPresent() {
        Assertions.assertEquals("\"text\"", DnsUtils.ensureQuotes("\"text\""));
    }

    @Test
    void ensureQuotesHandlesNullAndEmpty() {
        Assertions.assertEquals("\"\"", DnsUtils.ensureQuotes(null));
        Assertions.assertEquals("\"\"", DnsUtils.ensureQuotes(""));
    }

    @Test
    void toPtispTxtNameLeavesNormalSubdomainUnchanged() {
        Assertions.assertEquals("sub.example.com", DnsUtils.toPtispTxtName("sub.example.com", "example.com"));
    }

    @Test
    void toPtispTxtNameLeavesApexDomainUnchanged() {
        Assertions.assertEquals("example.com", DnsUtils.toPtispTxtName("example.com", "example.com"));
    }

    @Test
    void toPtispTxtNameConvertsDashSuffixToDotSubdomain() {
        Assertions.assertEquals("_dkim.mail.example.com",
                DnsUtils.toPtispTxtName("_dkim.mail-example.com", "example.com"));
    }

    @Test
    void toPtispTxtNameLeavesUnrelatedNameUnchanged() {
        Assertions.assertEquals("other.net", DnsUtils.toPtispTxtName("other.net", "example.com"));
    }

    @Test
    void fromPtispTxtNameConvertsSubdomainToDash() {
        Assertions.assertEquals("_dkim.mail-example.com",
                DnsUtils.fromPtispTxtName("_dkim.mail.example.com", "example.com"));
    }

    @Test
    void fromPtispTxtNameLeavesNormalSubdomainUnchanged() {
        Assertions.assertEquals("sub.example.com", DnsUtils.fromPtispTxtName("sub.example.com", "example.com"));
    }

    @Test
    void fromPtispTxtNameLeavesAcmeChallengeUnchanged() {
        Assertions.assertEquals("_acme-challenge.example.com",
                DnsUtils.fromPtispTxtName("_acme-challenge.example.com", "example.com"));
    }

    @Test
    void fromPtispTxtNameLeavesNameWithoutDomainSuffixUnchanged() {
        Assertions.assertEquals("other.net", DnsUtils.fromPtispTxtName("other.net", "example.com"));
    }

    @Test
    void fromPtispTxtNameDecodesApexEdnsEncoding() {
        // PTisp stores _edns.a-{domain} as _edns.a.{domain}; normalise back to a-prefix form
        Assertions.assertEquals("_edns.a-example.com",
                DnsUtils.fromPtispTxtName("_edns.a.example.com", "example.com"));
    }

    @Test
    void fromPtispTxtNameDecodesSubdomainEdnsEncoding() {
        // _edns.a-{sub}.{domain} is already canonical a-prefix form; returned unchanged
        Assertions.assertEquals("_edns.a-www.example.com",
                DnsUtils.fromPtispTxtName("_edns.a-www.example.com", "example.com"));
    }

    @Test
    void fromPtispTxtNameDecodesHyphenatedSubdomainEdnsEncoding() {
        Assertions.assertEquals("_edns.a-my-site.example.com",
                DnsUtils.fromPtispTxtName("_edns.a-my-site.example.com", "example.com"));
    }

    @Test
    void fromPtispTxtNameMigratesLegacyApexEdns() {
        // Old external-dns stored _edns.{domain} directly; migrate to a-prefix canonical form
        Assertions.assertEquals("_edns.a-example.com",
                DnsUtils.fromPtispTxtName("_edns.example.com", "example.com"));
    }

    @Test
    void fromPtispTxtNameMigratesLegacySubdomainEdns() {
        // Old external-dns stored _edns.{sub}.{domain} directly; migrate to a-prefix canonical form
        Assertions.assertEquals("_edns.a-www.example.com",
                DnsUtils.fromPtispTxtName("_edns.www.example.com", "example.com"));
    }
}
