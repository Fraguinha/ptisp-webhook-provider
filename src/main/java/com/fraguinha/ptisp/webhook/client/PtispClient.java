package com.fraguinha.ptisp.webhook.client;

import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import com.fraguinha.ptisp.webhook.model.PtispDTO;

public class PtispClient {
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
        return Optional.ofNullable(this.restClient.get()
                .uri("/parking/{domain}/dns/list", domain)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(PtispDTO.ListResponse.class));
    }

    public void addRecord(final String domain, final PtispDTO payload) {
        this.restClient.post()
                .uri("/parking/{domain}/dns/add", domain)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }

    public void deleteRecord(final String domain, final String lineId) {
        this.restClient.post()
                .uri("/parking/{domain}/dns/{line}/delete", domain, lineId)
                .retrieve()
                .toBodilessEntity();
    }
}
