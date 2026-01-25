package com.fraguinha.ptisp.webhook.model;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fraguinha.ptisp.webhook.client.PtispClient;

@ExtendWith(MockitoExtension.class)
class ManagedAccountTest {

    @Mock
    private PtispClient client;

    @Test
    void matchesExactDomain() {
        final ManagedAccount acc = new ManagedAccount(List.of("example.com"), this.client);
        Assertions.assertTrue(acc.matches("example.com"));
    }

    @Test
    void matchesSubdomain() {
        final ManagedAccount acc = new ManagedAccount(List.of("example.com"), this.client);
        Assertions.assertTrue(acc.matches("sub.example.com"));
        Assertions.assertTrue(acc.matches("deep.sub.example.com"));
    }

    @Test
    void doesNotMatchFalsePositive() {
        final ManagedAccount acc = new ManagedAccount(List.of("example.com"), this.client);
        Assertions.assertFalse(acc.matches("myexample.com"));
        Assertions.assertFalse(acc.matches("com"));
    }

    @Test
    void findBaseDomainReturnsCorrectDomain() {
        final ManagedAccount acc = new ManagedAccount(List.of("example.com", "other.org"), this.client);
        Assertions.assertEquals(Optional.of("example.com"), acc.findBaseDomain("sub.example.com"));
        Assertions.assertEquals(Optional.of("other.org"), acc.findBaseDomain("other.org"));
        Assertions.assertEquals(Optional.empty(), acc.findBaseDomain("unknown.net"));
    }

    @Test
    void handleNullDnsName() {
        final ManagedAccount acc = new ManagedAccount(List.of("example.com"), this.client);
        Assertions.assertFalse(acc.matches(null));
        Assertions.assertTrue(acc.findBaseDomain(null).isEmpty());
    }
}
