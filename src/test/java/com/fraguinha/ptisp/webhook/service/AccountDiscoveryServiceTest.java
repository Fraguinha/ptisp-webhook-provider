package com.fraguinha.ptisp.webhook.service;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;

import com.fraguinha.ptisp.webhook.model.ManagedAccount;

class AccountDiscoveryServiceTest {

    private final AccountDiscoveryService discoveryService = new AccountDiscoveryService();

    @Test
    void discoversMultipleAccounts() {
        final Map<String, String> env = Map.of(
                "PTISP_ACC1_DOMAINS", "example.com, example.org",
                "PTISP_ACC1_EMAIL", "acc1@test.com",
                "PTISP_ACC1_HASH", "hash1",
                "PTISP_ACC2_DOMAINS", "other.net",
                "PTISP_ACC2_EMAIL", "acc2@test.com",
                "PTISP_ACC2_HASH", "hash2");

        final List<ManagedAccount> accounts = this.discoveryService.discoverAccounts(env, RestClient.builder());

        Assertions.assertEquals(2, accounts.size());

        final ManagedAccount acc1 = accounts.stream()
                .filter(a -> "acc1@test.com".equals(a.client().getEmail()))
                .findFirst().orElseThrow();
        Assertions.assertEquals(List.of("example.com", "example.org"), acc1.domains());

        final ManagedAccount acc2 = accounts.stream()
                .filter(a -> "acc2@test.com".equals(a.client().getEmail()))
                .findFirst().orElseThrow();
        Assertions.assertEquals(List.of("other.net"), acc2.domains());
    }

    @Test
    void eachAccountHasUniqueAuthentication() {
        final Map<String, String> env = Map.of(
                "PTISP_ACC1_DOMAINS", "example.com",
                "PTISP_ACC1_EMAIL", "acc1@test.com",
                "PTISP_ACC1_HASH", "hash1",
                "PTISP_ACC2_DOMAINS", "other.net",
                "PTISP_ACC2_EMAIL", "acc2@test.com",
                "PTISP_ACC2_HASH", "hash2");

        final RestClient.Builder builder = RestClient.builder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        final List<ManagedAccount> accounts = this.discoveryService.discoverAccounts(env, builder);

        Assertions.assertEquals(2, accounts.size());

        final String jsonResponse = """
                {
                  "result": "ok",
                  "records": []
                }
                """;

        for (final ManagedAccount acc : accounts) {
            final String email = acc.client().getEmail();
            final String hash = "acc1@test.com".equals(email) ? "hash1" : "hash2";
            final String expectedAuth = "Basic " + Base64.getEncoder().encodeToString((email + ":" + hash).getBytes());

            server.expect(MockRestRequestMatchers.requestTo("https://api.ptisp.pt/parking/example.com/dns/list"))
                    .andExpect(MockRestRequestMatchers.header("Authorization", expectedAuth))
                    .andRespond(MockRestResponseCreators.withSuccess(jsonResponse, MediaType.APPLICATION_JSON));
        }

        for (final ManagedAccount acc : accounts) {
            acc.client().listRecords("example.com");
        }
        server.verify();
    }

    @Test
    void skipsIncompleteConfigurations() {
        final Map<String, String> env = Map.of(
                "PTISP_ACC1_DOMAINS", "example.com",
                "PTISP_ACC1_EMAIL", "acc1@test.com");

        final List<ManagedAccount> accounts = this.discoveryService.discoverAccounts(env, RestClient.builder());
        Assertions.assertEquals(0, accounts.size());
    }
}
