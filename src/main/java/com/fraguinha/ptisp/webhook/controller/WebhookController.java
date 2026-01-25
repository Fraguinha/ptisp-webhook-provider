package com.fraguinha.ptisp.webhook.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fraguinha.ptisp.webhook.model.ExternalDnsDTO;
import com.fraguinha.ptisp.webhook.model.WebhookConstants;
import com.fraguinha.ptisp.webhook.service.DnsService;

@RestController
public class WebhookController {
    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final DnsService dnsService;

    public WebhookController(final DnsService dnsService) {
        this.dnsService = dnsService;
    }

    @GetMapping(value = "/", produces = WebhookConstants.CONTENT_TYPE)
    public ResponseEntity<Map<String, String>> health() {
        WebhookController.log.debug("Received ExternalDNS health check");
        return ResponseEntity.ok()
                .header(WebhookConstants.PROVIDER_NAME_HEADER, WebhookConstants.PROVIDER_NAME)
                .body(Map.of("status", "ok", "provider", WebhookConstants.PROVIDER_NAME));
    }

    @GetMapping(value = "/records", produces = WebhookConstants.CONTENT_TYPE)
    public ResponseEntity<List<ExternalDnsDTO>> getRecords() {
        WebhookController.log.info("ExternalDNS fetching records from PTISP");
        return ResponseEntity.ok()
                .header(WebhookConstants.PROVIDER_NAME_HEADER, WebhookConstants.PROVIDER_NAME)
                .body(this.dnsService.getAllRecords());
    }

    @PostMapping(value = "/records", consumes = WebhookConstants.CONTENT_TYPE, produces = WebhookConstants.CONTENT_TYPE)
    public ResponseEntity<Void> applyChanges(@RequestBody final ExternalDnsDTO.Changes changes) {
        WebhookController.log.info("Applying DNS changes: create={}, updateOld={}, updateNew={}, delete={}",
                changes.create().size(),
                changes.updateOld().size(),
                changes.updateNew().size(),
                changes.delete().size());

        changes.delete().forEach(this.dnsService::deleteRecord);

        IntStream.range(0, changes.updateOld().size())
                .forEach(i -> this.dnsService.updateRecord(changes.updateOld().get(i), changes.updateNew().get(i)));

        changes.create().forEach(this.dnsService::createRecord);

        return ResponseEntity.noContent()
                .header(WebhookConstants.PROVIDER_NAME_HEADER, WebhookConstants.PROVIDER_NAME)
                .build();
    }

    @PostMapping(value = "/adjustendpoints", consumes = WebhookConstants.CONTENT_TYPE, produces = WebhookConstants.CONTENT_TYPE)
    public ResponseEntity<List<ExternalDnsDTO>> adjustEndpoints(@RequestBody final List<ExternalDnsDTO> endpoints) {
        WebhookController.log.debug("ExternalDNS adjusting endpoints (no-op)");
        return ResponseEntity.ok()
                .header(WebhookConstants.PROVIDER_NAME_HEADER, WebhookConstants.PROVIDER_NAME)
                .body(endpoints);
    }
}
