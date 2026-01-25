package com.fraguinha.ptisp.webhook.client;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import com.fraguinha.ptisp.webhook.exception.PtispApiException;
import com.fraguinha.ptisp.webhook.model.PtispDTO;

public class PtispClient {

    private static final Logger log = LoggerFactory.getLogger(PtispClient.class);
    private static final int MAX_READ_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MS = 250L;

    private final String email;
    private final RestClient restClient;

    public PtispClient(final String email, final RestClient restClient) {
        this.email = email;
        this.restClient = restClient;
    }

    public String getEmail() {
        return this.email;
    }

    public RestClient getRestClient() {
        return this.restClient;
    }

    public Optional<PtispDTO.ListResponse> listRecords(final String domain) {
        return this.withRetry("list records for " + domain, () -> {
            final PtispDTO.ListResponse response = this.restClient.get()
                    .uri("/parking/{domain}/dns/list", domain)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(PtispDTO.ListResponse.class);

            if (response != null && !response.isOk()) {
                throw new PtispApiException("PTISP rejected list for " + domain + ": " + response.describe());
            }
            return Optional.ofNullable(response);
        });
    }

    public void addRecord(final String domain, final PtispDTO payload) {
        final PtispDTO.MutationResponse response = this.restClient.post()
                .uri("/parking/{domain}/dns/add", domain)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(PtispDTO.MutationResponse.class);

        PtispClient.requireOk(response, "add " + payload.type() + " record " + payload.name() + " in " + domain);
    }

    public void deleteRecord(final String domain, final String lineId) {
        final PtispDTO.MutationResponse response = this.restClient.post()
                .uri("/parking/{domain}/dns/{line}/delete", domain, lineId)
                .retrieve()
                .body(PtispDTO.MutationResponse.class);

        PtispClient.requireOk(response, "delete record line " + lineId + " in " + domain);
    }

    private static void requireOk(final PtispDTO.MutationResponse response, final String action) {
        if (response != null && !response.isOk()) {
            throw new PtispApiException("PTISP refused to " + action + ": " + response.describe());
        }
    }

    private <T> T withRetry(final String action, final Supplier<T> operation) {
        RuntimeException last = null;

        for (int attempt = 1; attempt <= PtispClient.MAX_READ_ATTEMPTS; attempt++) {
            try {
                return operation.get();
            } catch (final RuntimeException e) {
                last = e;
                if (attempt < PtispClient.MAX_READ_ATTEMPTS) {
                    PtispClient.log.warn("Attempt {}/{} failed to {} (account: {}): {}",
                            attempt, PtispClient.MAX_READ_ATTEMPTS, action, this.email, e.getMessage());
                    this.backOff(attempt);
                }
            }
        }

        throw new PtispApiException("Failed to " + action + " after " + PtispClient.MAX_READ_ATTEMPTS
                + " attempts (account: " + this.email + ")", last);
    }

    private void backOff(final int attempt) {
        final long base = PtispClient.INITIAL_BACKOFF_MS << (attempt - 1);
        final long jitter = ThreadLocalRandom.current().nextLong(base / 2 + 1);
        try {
            Thread.sleep(base + jitter);
        } catch (final InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new PtispApiException("Interrupted while retrying PTISP request", ie);
        }
    }
}

