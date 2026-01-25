package com.fraguinha.ptisp.webhook.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
    private static final long CACHE_TTL_MS = 60_000;
    private static final List<String> ADDRESS_TYPES = List.of("A", "AAAA", "CNAME");
    private static final String HERITAGE_MARKER = "heritage=external-dns";

    private final List<ManagedAccount> accounts;
    private final Map<String, CacheEntry> recordsCache = new ConcurrentHashMap<>();

    private record CacheEntry(List<PtispDTO> records, long timestamp) {
    }

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
            final ExternalDnsDTO ptispEndpoint = this.toPtispEndpoint(endpoint, domain);
            final List<PtispDTO> existingRecords = this.fetchPtispRecords(acc.client(), domain);

            ptispEndpoint.targets().forEach(target -> {
                if (!this.findRecords(existingRecords, ptispEndpoint.dnsName(), ptispEndpoint.recordType(), target,
                        domain).isEmpty()) {
                    DnsService.log.debug("Skipped creating {} record: {} -> {} (already exists)",
                            ptispEndpoint.recordType(), ptispEndpoint.dnsName(), target);
                    return;
                }

                try {
                    acc.client().addRecord(domain, PtispDTO.fromExternalDns(ptispEndpoint, target));
                    this.invalidateCache(acc.client(), domain);
                    DnsService.log.info("Created {} record: {} -> {} (account: {})",
                            ptispEndpoint.recordType(), ptispEndpoint.dnsName(), target, acc.client().getEmail());
                } catch (final Exception e) {
                    DnsService.log.error("Failed to create {} record: {} -> {} (account: {}): {}",
                            ptispEndpoint.recordType(), ptispEndpoint.dnsName(), target, acc.client().getEmail(),
                            e.getMessage());
                }
            });
        }, () -> DnsService.log.warn("No account configured for {}, skipping create", endpoint.dnsName()));
    }

    public void updateRecord(final ExternalDnsDTO oldEndpoint, final ExternalDnsDTO newEndpoint) {
        final List<String> toDelete = oldEndpoint.targets().stream()
                .filter(t -> !newEndpoint.targets().contains(t))
                .toList();
        final List<String> toCreate = newEndpoint.targets().stream()
                .filter(t -> !oldEndpoint.targets().contains(t))
                .toList();

        if (!toDelete.isEmpty() || !toCreate.isEmpty()) {
            DnsService.log.info("Updating {} record: {} (removing: {}, adding: {})",
                    newEndpoint.recordType(), oldEndpoint.dnsName(), toDelete, toCreate);
        }

        if (!toDelete.isEmpty()) {
            this.deleteRecord(ExternalDnsDTO.of(oldEndpoint.dnsName(), toDelete, oldEndpoint.recordType(),
                    oldEndpoint.ttl()));
        }
        if (!toCreate.isEmpty()) {
            this.createRecord(ExternalDnsDTO.of(newEndpoint.dnsName(), toCreate, newEndpoint.recordType(),
                    newEndpoint.ttl()));
        }
    }

    public void deleteRecord(final ExternalDnsDTO endpoint) {
        this.findAccount(endpoint.dnsName()).ifPresentOrElse(acc -> {
            final String domain = acc.findBaseDomain(endpoint.dnsName()).orElseThrow();
            final ExternalDnsDTO ptispEndpoint = this.toPtispEndpoint(endpoint, domain);

            ptispEndpoint.targets().forEach(target -> {
                final List<PtispDTO> existingRecords = this.fetchPtispRecords(acc.client(), domain);
                final List<PtispDTO> matchedRecords = this.findRecords(existingRecords, ptispEndpoint.dnsName(),
                        ptispEndpoint.recordType(), target, domain);

                if (matchedRecords.isEmpty()) {
                    DnsService.log.warn("Skipped deleting {} record: {} -> {} (not found in PTISP)",
                            ptispEndpoint.recordType(), ptispEndpoint.dnsName(), target);
                } else {
                    matchedRecords.forEach(record -> {
                        try {
                            acc.client().deleteRecord(domain, record.line());
                            this.invalidateCache(acc.client(), domain);
                            DnsService.log.info("Deleted {} record: {} -> {} (account: {})",
                                    ptispEndpoint.recordType(), ptispEndpoint.dnsName(), target,
                                    acc.client().getEmail());
                        } catch (final Exception e) {
                            DnsService.log.error("Failed to delete {} record: {} -> {} (account: {}): {}",
                                    ptispEndpoint.recordType(), ptispEndpoint.dnsName(), target,
                                    acc.client().getEmail(), e.getMessage());
                        }
                    });
                }
            });
        }, () -> DnsService.log.warn("No account configured for {}, skipping delete",
                endpoint.dnsName()));
    }

    public int getAccountCount() {
        return this.accounts.size();
    }

    private ExternalDnsDTO toPtispEndpoint(final ExternalDnsDTO endpoint, final String domain) {
        if (!"TXT".equals(endpoint.recordType())) {
            return endpoint;
        }
        final String ptispName = DnsUtils.toPtispTxtName(endpoint.dnsName(), domain);
        return ptispName.equals(endpoint.dnsName())
                ? endpoint
                : ExternalDnsDTO.of(ptispName, endpoint.targets(), endpoint.recordType(), endpoint.ttl());
    }

    private PtispDTO fromPtispRecord(final PtispDTO record, final String domain) {
        if (!"TXT".equals(record.type())) {
            return record;
        }
        final String name = DnsUtils.removeTrailingDot(record.name());
        final String translated = DnsUtils.fromPtispTxtName(name, domain);
        return translated.equals(name)
                ? record
                : new PtispDTO(record.line(), DnsUtils.ensureTrailingDot(translated), record.type(),
                        record.recordClass(), record.ttl(), record.address(), record.cname(), record.txtdata());
    }

    private List<ExternalDnsDTO> fetchEndpoints(final PtispClient client, final String domain) {
        DnsService.log.debug("Fetching records for domain: {} (account: {})", domain, client.getEmail());
        final List<PtispDTO> records = this.fetchPtispRecords(client, domain);

        final Map<String, List<PtispDTO>> grouped = records.stream()
                .filter(r -> List.of("A", "CNAME", "TXT").contains(r.type()))
                .map(r -> this.fromPtispRecord(r, domain))
                .collect(Collectors
                        .groupingBy(r -> DnsUtils.removeTrailingDot(r.name()).toLowerCase() + ":" + r.type()));

        final List<ExternalDnsDTO> endpoints = grouped.values().stream()
                .map(this::mergeRecordsToEndpoint)
                .toList();

        final List<ExternalDnsDTO> visible = this.filterUnownedAddressRecords(endpoints);

        if (DnsService.log.isTraceEnabled()) {
            visible.forEach(ep -> DnsService.log.trace("Returning endpoint to ExternalDNS: {} [{}] with targets {}",
                    ep.dnsName(), ep.recordType(), ep.targets()));
        }

        return visible;
    }

    private List<ExternalDnsDTO> filterUnownedAddressRecords(final List<ExternalDnsDTO> endpoints) {
        final Set<String> ownedKeys = endpoints.stream()
                .filter(ep -> "TXT".equals(ep.recordType()))
                .filter(ep -> ep.targets().stream()
                        .anyMatch(t -> DnsUtils.removeQuotes(t).contains(DnsService.HERITAGE_MARKER)))
                .map(ep -> DnsService.ownershipKeyFromRegistryTxt(ep.dnsName()))
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());

        return endpoints.stream()
                .filter(ep -> !DnsService.ADDRESS_TYPES.contains(ep.recordType())
                        || ownedKeys.contains(ep.recordType() + ":"
                                + DnsUtils.removeTrailingDot(ep.dnsName()).toLowerCase()))
                .toList();
    }

    private static Optional<String> ownershipKeyFromRegistryTxt(final String txtName) {
        final String name = DnsUtils.removeTrailingDot(txtName).toLowerCase();
        if (!name.startsWith("_edns.")) {
            return Optional.empty();
        }
        final String rest = name.substring("_edns.".length());
        final int dash = rest.indexOf('-');
        if (dash <= 0 || dash >= rest.length() - 1) {
            return Optional.empty();
        }
        final String type = rest.substring(0, dash).toUpperCase();
        final String owned = rest.substring(dash + 1);
        return Optional.of(type + ":" + owned);
    }

    private List<PtispDTO> fetchPtispRecords(final PtispClient client, final String domain) {
        final String cacheKey = client.getEmail() + ":" + domain;
        final long now = System.currentTimeMillis();
        final CacheEntry cached = this.recordsCache.get(cacheKey);

        if (cached != null && (now - cached.timestamp()) < DnsService.CACHE_TTL_MS) {
            DnsService.log.debug("Cache hit for domain: {} (account: {})", domain, client.getEmail());
            return cached.records();
        }

        try {
            final List<PtispDTO> records = client.listRecords(domain)
                    .filter(response -> "ok".equals(response.result()))
                    .map(PtispDTO.ListResponse::records)
                    .stream()
                    .flatMap(List::stream)
                    .filter(Objects::nonNull)
                    .toList();

            this.recordsCache.put(cacheKey, new CacheEntry(records, now));

            if (DnsService.log.isTraceEnabled()) {
                DnsService.log.trace("PTISP raw records for {}: {}", domain, records.size());
            }

            return records;
        } catch (final Exception e) {
            DnsService.log.error("Failed to fetch records for {} (account: {}): {}",
                    domain, client.getEmail(), e.getMessage());
            if (cached != null) {
                DnsService.log.warn("Returning stale cache for {} (account: {})", domain, client.getEmail());
                return cached.records();
            }
            return List.of();
        }
    }

    private void invalidateCache(final PtispClient client, final String domain) {
        this.recordsCache.remove(client.getEmail() + ":" + domain);
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
                .distinct()
                .sorted()
                .toList();

        return ExternalDnsDTO.of(
                dnsName,
                targets,
                first.type(),
                0L);
    }

    private Optional<ManagedAccount> findAccount(final String dnsName) {
        return this.accounts.stream().filter(acc -> acc.matches(dnsName)).findFirst();
    }

    private List<PtispDTO> findRecords(final List<PtispDTO> records, final String dnsName, final String type,
            final String target, final String domain) {
        return records.stream()
                .filter(r -> {
                    final boolean typeMatch = type.equals(r.type());
                    final String ptispName = DnsUtils.removeTrailingDot(r.name());
                    // For TXT records, compare canonical forms so both PTisp-encoded names
                    // (e.g. _edns.a.domain) and legacy names (_edns.domain) match correctly.
                    final boolean nameMatch = dnsName.equalsIgnoreCase(ptispName)
                            || ("TXT".equals(type) && DnsUtils.fromPtispTxtName(dnsName, domain).equalsIgnoreCase(
                                    DnsUtils.fromPtispTxtName(ptispName, domain)));

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
