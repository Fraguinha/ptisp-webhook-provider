package com.fraguinha.ptisp.webhook.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fraguinha.ptisp.webhook.client.PtispClient;
import com.fraguinha.ptisp.webhook.model.ExternalDnsDTO;
import com.fraguinha.ptisp.webhook.model.ManagedAccount;
import com.fraguinha.ptisp.webhook.model.PtispDTO;
import com.fraguinha.ptisp.webhook.util.DnsUtils;

@Service
public class DnsService {

    private static final Logger log = LoggerFactory.getLogger(DnsService.class);
    private final List<ManagedAccount> accounts;

    public DnsService(final List<ManagedAccount> accounts) {
        this.accounts = accounts;
    }

    public List<ExternalDnsDTO> getAllRecords() {
        DnsService.log.debug("Fetching all records across all configured accounts");
        return this.accounts.stream()
                .flatMap(acc -> acc.domains().stream()
                        .flatMap(domain -> this.fetchEndpoints(acc.client(), domain).stream()))
                .toList();
    }

    public void createRecord(final ExternalDnsDTO endpoint) {
        this.findAccount(endpoint.dnsName()).ifPresentOrElse(acc -> {
            final String domain = acc.findBaseDomain(endpoint.dnsName()).orElseThrow();
            final List<PtispDTO> existingRecords = this.fetchPtispRecords(acc.client(), domain);

            endpoint.targets().forEach(target -> {
                if (!this.findRecords(existingRecords, endpoint.dnsName(), endpoint.recordType(), target).isEmpty()) {
                    DnsService.log.info("Record already exists, skipping creation: {} [{}] -> {}",
                            endpoint.dnsName(), endpoint.recordType(), target);
                    return;
                }

                try {
                    final PtispDTO request = PtispDTO.fromExternalDns(endpoint, target);

                    acc.client().addRecord(domain, request);
                    DnsService.log.info("Successfully created {} record: {} -> {}",
                            endpoint.recordType(), endpoint.dnsName(), target);
                } catch (final Exception e) {
                    DnsService.log.error("Failed to create {} record for {} in account {}: {}",
                            endpoint.recordType(), endpoint.dnsName(), acc.client().getEmail(), e.getMessage());
                }
            });
        }, () -> DnsService.log.warn("No managed account found for DNS name: {}", endpoint.dnsName()));
    }

    public void updateRecord(final ExternalDnsDTO oldEndpoint, final ExternalDnsDTO newEndpoint) {
        DnsService.log.info("Updating {} record: {} (targets: {} -> {})",
                newEndpoint.recordType(), oldEndpoint.dnsName(), oldEndpoint.targets(), newEndpoint.targets());
        this.deleteRecord(oldEndpoint);
        this.createRecord(newEndpoint);
    }

    public void deleteRecord(final ExternalDnsDTO endpoint) {
        this.findAccount(endpoint.dnsName()).ifPresentOrElse(acc -> {
            final String domain = acc.findBaseDomain(endpoint.dnsName()).orElseThrow();
            final List<PtispDTO> existingRecords = this.fetchPtispRecords(acc.client(), domain);

            endpoint.targets().forEach(target -> {
                final List<PtispDTO> matchedRecords = this.findRecords(existingRecords, endpoint.dnsName(),
                        endpoint.recordType(), target);

                if (matchedRecords.isEmpty()) {
                    DnsService.log.warn("Deletion skipped: No matching record found in PTISP for {} record: {} -> {}",
                            endpoint.recordType(), endpoint.dnsName(), target);
                } else {
                    matchedRecords.forEach(record -> {
                        final String id = record.line();
                        try {
                            acc.client().deleteRecord(domain, id);
                            DnsService.log.info("Successfully deleted {} record: {} -> {} (line: {})",
                                    endpoint.recordType(), endpoint.dnsName(), target, id);
                        } catch (final Exception e) {
                            DnsService.log.error("Failed to delete {} record {} in account {}: {}",
                                    endpoint.recordType(), endpoint.dnsName(), acc.client().getEmail(), e.getMessage());
                        }
                    });
                }
            });
        }, () -> DnsService.log.warn("Deletion skipped: No managed account found for DNS name: {}",
                endpoint.dnsName()));
    }

    public int getAccountCount() {
        return this.accounts.size();
    }

    private List<ExternalDnsDTO> fetchEndpoints(final PtispClient client, final String domain) {
        DnsService.log.debug("Fetching records for domain: {} (account: {})", domain, client.getEmail());
        final List<PtispDTO> records = this.fetchPtispRecords(client, domain);

        final Map<String, List<PtispDTO>> grouped = records.stream()
                .filter(r -> List.of("A", "CNAME", "TXT").contains(r.type()))
                .collect(Collectors
                        .groupingBy(r -> DnsUtils.removeTrailingDot(r.name()).toLowerCase() + ":" + r.type()));

        final List<ExternalDnsDTO> endpoints = grouped.values().stream()
                .map(this::mergeRecordsToEndpoint)
                .toList();

        if (DnsService.log.isTraceEnabled()) {
            endpoints.forEach(ep -> DnsService.log.trace("Returning endpoint to ExternalDNS: {} [{}] with targets {}",
                    ep.dnsName(), ep.recordType(), ep.targets()));
        }

        return endpoints;
    }

    private List<PtispDTO> fetchPtispRecords(final PtispClient client, final String domain) {
        try {
            final List<PtispDTO> records = client.listRecords(domain)
                    .filter(response -> "ok".equals(response.result()))
                    .map(PtispDTO.ListResponse::records)
                    .stream()
                    .flatMap(List::stream)
                    .filter(Objects::nonNull)
                    .toList();

            if (DnsService.log.isTraceEnabled()) {
                DnsService.log.trace("PTISP raw records for {}: {}", domain, records.size());
            }

            return records;
        } catch (final Exception e) {
            DnsService.log.error("Error fetching records for domain {} (account: {}): {}",
                    domain, client.getEmail(), e.getMessage());
            return List.of();
        }
    }

    private ExternalDnsDTO mergeRecordsToEndpoint(final List<PtispDTO> records) {
        final PtispDTO first = records.get(0);
        final String dnsName = DnsUtils.removeTrailingDot(first.name());

        if (DnsService.log.isTraceEnabled()) {
            DnsService.log.trace("Merging {} records for {}", records.size(), dnsName);
            records.forEach(r -> DnsService.log.trace("  Record: type={}, target={}", r.type(), r.getDnsTarget()));
        }

        final List<String> targets = records.stream()
                .map(PtispDTO::getDnsTarget)
                .sorted()
                .toList();

        return ExternalDnsDTO.of(
                dnsName,
                targets,
                first.type(),
                first.ttl());
    }

    private Optional<ManagedAccount> findAccount(final String dnsName) {
        return this.accounts.stream().filter(acc -> acc.matches(dnsName)).findFirst();
    }

    private List<PtispDTO> findRecords(final List<PtispDTO> records, final String dnsName, final String type,
            final String target) {
        return records.stream()
                .filter(r -> {
                    final boolean typeMatch = type.equals(r.type());
                    final String ptispName = DnsUtils.removeTrailingDot(r.name());
                    final boolean nameMatch = dnsName.equalsIgnoreCase(ptispName);

                    String normalizedTarget = target;
                    if ("TXT".equals(type)) {
                        normalizedTarget = DnsUtils.removeQuotes(target);
                    }

                    String ptispTarget = r.getDnsTarget();
                    if ("TXT".equals(r.type())) {
                        ptispTarget = DnsUtils.removeQuotes(r.getDnsTarget());
                    }

                    final boolean targetMatch = Objects.equals(normalizedTarget, ptispTarget);

                    if (DnsService.log.isTraceEnabled() && nameMatch && typeMatch) {
                        DnsService.log.trace("Comparing: name({}=={})={}, type({}=={})={}, target({}=={})={}",
                                dnsName, ptispName, nameMatch,
                                type, r.type(), typeMatch,
                                normalizedTarget, ptispTarget, targetMatch);
                    }

                    return typeMatch && nameMatch && targetMatch;
                })
                .toList();
    }
}
