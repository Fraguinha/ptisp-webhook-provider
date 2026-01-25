package com.fraguinha.ptisp.webhook.model;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fraguinha.ptisp.webhook.util.DnsUtils;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record PtispDTO(
        String line,
        String name,
        String type,
        @JsonProperty("class") String recordClass,
        Long ttl,
        String address,
        String cname,
        String txtdata) {

    public PtispDTO {
        line = Objects.requireNonNullElse(line, "");
        name = Objects.requireNonNullElse(name, "");
        type = Objects.requireNonNullElse(type, "");
        recordClass = Objects.requireNonNullElse(recordClass, "");
        ttl = Objects.requireNonNullElse(ttl, 0L);
        address = Objects.requireNonNullElse(address, "");
        cname = Objects.requireNonNullElse(cname, "");
        txtdata = Objects.requireNonNullElse(txtdata, "");
    }

    public record ListResponse(
            String result,
            List<PtispDTO> records) {
        public ListResponse {
            records = Objects.requireNonNullElse(records, List.of());
        }
    }

    public String getDnsTarget() {
        return switch (this.type) {
            case "CNAME" -> DnsUtils.removeTrailingDot(this.cname);
            case "TXT" -> this.txtdata;
            default -> this.address;
        };
    }

    public static PtispDTO fromExternalDns(final ExternalDnsDTO endpoint, final String target) {
        final String name = DnsUtils.ensureTrailingDot(endpoint.dnsName());
        final Long ttl = Objects.requireNonNullElse(endpoint.ttl(), WebhookConstants.DEFAULT_TTL);

        return switch (endpoint.recordType()) {
            case "A" -> new PtispDTO("", name, "A", "IN", ttl, target, "", "");
            case "CNAME" -> new PtispDTO("", name, "CNAME", "IN", ttl, "", DnsUtils.ensureTrailingDot(target), "");
            case "TXT" -> new PtispDTO("", name, "TXT", "IN", ttl, "", "", DnsUtils.ensureQuotes(target));
            default -> throw new IllegalArgumentException("Unsupported record type: " + endpoint.recordType());
        };
    }

    public ExternalDnsDTO toExternalDns() {
        return ExternalDnsDTO.of(
                DnsUtils.removeTrailingDot(this.name),
                java.util.List.of(this.getDnsTarget()),
                this.type,
                Objects.requireNonNullElse(this.ttl, WebhookConstants.DEFAULT_TTL));
    }
}
