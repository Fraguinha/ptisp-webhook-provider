package com.fraguinha.ptisp.webhook.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fraguinha.ptisp.webhook.util.DnsUtils;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ExternalDnsDTO(
        @JsonProperty("dnsName") String dnsName,
        @JsonProperty("targets") List<String> targets,
        @JsonProperty("recordType") String recordType,
        @JsonProperty("ttl") Long ttl,
        @JsonProperty("labels") Map<String, String> labels,
        @JsonProperty("providerSpecific") List<Property> providerSpecific) {

    public ExternalDnsDTO {
        dnsName = DnsUtils.removeTrailingDot(Objects.requireNonNullElse(dnsName, ""));
        final String type = Objects.requireNonNullElse(recordType, "");
        recordType = type;
        targets = Objects.requireNonNullElse(targets, List.<String>of()).stream()
                .map(target -> switch (type) {
                    case "CNAME" -> DnsUtils.removeTrailingDot(target);
                    default -> target;
                })
                .toList();
        ttl = Objects.requireNonNullElse(ttl, WebhookConstants.DEFAULT_TTL);
        labels = Objects.requireNonNullElse(labels, Map.of());
        providerSpecific = Objects.requireNonNullElse(providerSpecific, List.of());
    }

    public record Property(
            @JsonProperty("name") String name,
            @JsonProperty("value") String value) {
    }

    public record Changes(
            @JsonProperty("create") List<ExternalDnsDTO> create,
            @JsonProperty("updateOld") List<ExternalDnsDTO> updateOld,
            @JsonProperty("updateNew") List<ExternalDnsDTO> updateNew,
            @JsonProperty("delete") List<ExternalDnsDTO> delete) {

        public Changes {
            create = Objects.requireNonNullElse(create, List.of());
            updateOld = Objects.requireNonNullElse(updateOld, List.of());
            updateNew = Objects.requireNonNullElse(updateNew, List.of());
            delete = Objects.requireNonNullElse(delete, List.of());
        }
    }

    public static ExternalDnsDTO of(final String dnsName, final List<String> targets, final String recordType) {
        return new ExternalDnsDTO(dnsName, targets, recordType, WebhookConstants.DEFAULT_TTL, Map.of(), List.of());
    }

    public static ExternalDnsDTO of(final String dnsName, final List<String> targets, final String recordType,
            final Long ttl) {
        return new ExternalDnsDTO(dnsName, targets, recordType, ttl, Map.of(), List.of());
    }
}
