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
}
