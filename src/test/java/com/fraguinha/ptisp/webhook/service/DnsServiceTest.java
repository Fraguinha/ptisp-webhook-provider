package com.fraguinha.ptisp.webhook.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fraguinha.ptisp.webhook.client.PtispClient;
import com.fraguinha.ptisp.webhook.model.ExternalDnsDTO;
import com.fraguinha.ptisp.webhook.model.ManagedAccount;
import com.fraguinha.ptisp.webhook.model.PtispDTO;

@ExtendWith(MockitoExtension.class)
class DnsServiceTest {

    @Mock
    private PtispClient client;

    private DnsService dnsService;

    @BeforeEach
    void setUp() {
        final ManagedAccount account = new ManagedAccount(List.of("example.com"), this.client);
        this.dnsService = new DnsService(List.of(account));
    }

    @Test
    void getAllRecordsMergesTargets() {
        final PtispDTO r1 = new PtispDTO("1", "test.example.com.", "A", "IN", 300L, "1.1.1.1", "", "");
        final PtispDTO r2 = new PtispDTO("2", "test.example.com.", "A", "IN", 300L, "2.2.2.2", "", "");
        final PtispDTO owner = new PtispDTO("3", "_edns.a-test.example.com.", "TXT", "IN", 300L, null, null,
                "\"heritage=external-dns\"");
        Mockito.when(this.client.listRecords("example.com"))
                .thenReturn(Optional.of(new PtispDTO.ListResponse("ok", List.of(r1, r2, owner))));

        final List<ExternalDnsDTO> results = this.dnsService.getAllRecords();

        final ExternalDnsDTO ep = results.stream()
                .filter(e -> "A".equals(e.recordType()))
                .findFirst().orElseThrow();
        Assertions.assertEquals("test.example.com", ep.dnsName());
        Assertions.assertEquals(2, ep.targets().size());
        Assertions.assertTrue(ep.targets().contains("1.1.1.1"));
        Assertions.assertTrue(ep.targets().contains("2.2.2.2"));
    }

    @Test
    void getAllRecordsHidesUnownedAddressRecords() {
        final PtispDTO parking = new PtispDTO("1", "example.com.", "A", "IN", 300L, "109.71.47.21", "", "");
        Mockito.when(this.client.listRecords("example.com"))
                .thenReturn(Optional.of(new PtispDTO.ListResponse("ok", List.of(parking))));

        final List<ExternalDnsDTO> results = this.dnsService.getAllRecords();

        Assertions.assertTrue(results.stream().noneMatch(e -> "A".equals(e.recordType())));
    }

    @Test
    void getAllRecordsKeepsOwnedApexAddressRecord() {
        final PtispDTO a = new PtispDTO("1", "example.com.", "A", "IN", 300L, "1.1.1.1", "", "");
        final PtispDTO owner = new PtispDTO("2", "_edns.a.example.com.", "TXT", "IN", 300L, null, null,
                "\"heritage=external-dns,external-dns/owner=k8s-cluster\"");
        Mockito.when(this.client.listRecords("example.com"))
                .thenReturn(Optional.of(new PtispDTO.ListResponse("ok", List.of(a, owner))));

        final List<ExternalDnsDTO> results = this.dnsService.getAllRecords();

        Assertions.assertTrue(results.stream()
                .anyMatch(e -> "A".equals(e.recordType()) && e.targets().contains("1.1.1.1")));
    }

    @Test
    void getAllRecordsHandlesTxtWithQuotes() {
        final PtispDTO r1 = new PtispDTO("1", "test.example.com.", "TXT", "IN", 300L, null, null, "\"v=spf1\"");
        Mockito.when(this.client.listRecords("example.com"))
                .thenReturn(Optional.of(new PtispDTO.ListResponse("ok", List.of(r1))));

        final List<ExternalDnsDTO> results = this.dnsService.getAllRecords();

        Assertions.assertEquals(1, results.size());
        Assertions.assertEquals(List.of("\"v=spf1\""), results.get(0).targets());
    }

    @Test
    void deleteRecordMatchesTxtWithOrWithoutQuotes() {
        final PtispDTO existing = new PtispDTO("line123", "test.example.com.", "TXT", "IN", 300L, null, null, "\"v=spf1\"");
        Mockito.when(this.client.listRecords("example.com"))
                .thenReturn(Optional.of(new PtispDTO.ListResponse("ok", List.of(existing))));

        final ExternalDnsDTO ep = ExternalDnsDTO.of("test.example.com", List.of("v=spf1"), "TXT", 300L);
        this.dnsService.deleteRecord(ep);

        Mockito.verify(this.client).deleteRecord("example.com", "line123");
    }

    @Test
    void createRecordSkipsExisting() {
        final PtispDTO existing = new PtispDTO("1", "test.example.com.", "A", "IN", 300L, "1.1.1.1", "", "");
        Mockito.when(this.client.listRecords("example.com"))
                .thenReturn(Optional.of(new PtispDTO.ListResponse("ok", List.of(existing))));

        final ExternalDnsDTO ep = ExternalDnsDTO.of("test.example.com", List.of("1.1.1.1"), "A", 300L);
        this.dnsService.createRecord(ep);

        Mockito.verify(this.client, Mockito.never()).addRecord(ArgumentMatchers.eq("example.com"), ArgumentMatchers.any());
    }

    @Test
    void createRecordAddsNew() {
        Mockito.when(this.client.listRecords("example.com")).thenReturn(Optional.of(new PtispDTO.ListResponse("ok", List.of())));

        final ExternalDnsDTO ep = ExternalDnsDTO.of("new.example.com", List.of("1.1.1.1"), "A", 300L);
        this.dnsService.createRecord(ep);

        Mockito.verify(this.client).addRecord(ArgumentMatchers.eq("example.com"), ArgumentMatchers.any());
    }

    @Test
    void deleteRecordFindsCorrectLine() {
        final PtispDTO existing = new PtispDTO("line123", "test.example.com.", "A", "IN", 300L, "1.1.1.1", "", "");
        Mockito.when(this.client.listRecords("example.com"))
                .thenReturn(Optional.of(new PtispDTO.ListResponse("ok", List.of(existing))));

        final ExternalDnsDTO ep = ExternalDnsDTO.of("test.example.com", List.of("1.1.1.1"), "A", 300L);
        this.dnsService.deleteRecord(ep);

        Mockito.verify(this.client).deleteRecord("example.com", "line123");
    }

    @Test
    void updateRecordDeletesAndCreates() {
        final PtispDTO existing = new PtispDTO("line123", "test.example.com.", "A", "IN", 300L, "1.1.1.1", "", "");
        Mockito.when(this.client.listRecords("example.com"))
                .thenReturn(Optional.of(new PtispDTO.ListResponse("ok", List.of(existing))));

        final ExternalDnsDTO oldEp = ExternalDnsDTO.of("test.example.com", List.of("1.1.1.1"), "A", 300L);
        final ExternalDnsDTO newEp = ExternalDnsDTO.of("test.example.com", List.of("2.2.2.2"), "A", 300L);

        this.dnsService.updateRecord(oldEp, newEp);

        Mockito.verify(this.client).deleteRecord("example.com", "line123");
        Mockito.verify(this.client).addRecord(ArgumentMatchers.eq("example.com"), ArgumentMatchers.any());
    }

    @Test
    void updateRecordIsNoOpWhenTargetsUnchanged() {
        final ExternalDnsDTO ep = ExternalDnsDTO.of("test.example.com", List.of("1.1.1.1"), "A", 300L);
        this.dnsService.updateRecord(ep, ep);

        Mockito.verify(this.client, Mockito.never()).deleteRecord(ArgumentMatchers.any(), ArgumentMatchers.any());
        Mockito.verify(this.client, Mockito.never()).addRecord(ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void getAllRecordsFiltersMxRecords() {
        final PtispDTO mx = new PtispDTO("1", "test.example.com.", "MX", "IN", 300L, "mail.example.com", "", "");
        final PtispDTO a = new PtispDTO("2", "test.example.com.", "A", "IN", 300L, "1.1.1.1", "", "");
        final PtispDTO owner = new PtispDTO("3", "_edns.a-test.example.com.", "TXT", "IN", 300L, null, null,
                "\"heritage=external-dns\"");
        Mockito.when(this.client.listRecords("example.com"))
                .thenReturn(Optional.of(new PtispDTO.ListResponse("ok", List.of(mx, a, owner))));

        final List<ExternalDnsDTO> results = this.dnsService.getAllRecords();

        Assertions.assertTrue(results.stream().anyMatch(e -> "A".equals(e.recordType())));
        Assertions.assertTrue(results.stream().noneMatch(e -> "MX".equals(e.recordType())));
    }

    @Test
    void getAllRecordsReturnsEmptyOnApiFailure() {
        Mockito.when(this.client.listRecords("example.com")).thenThrow(new RuntimeException("connection refused"));

        final List<ExternalDnsDTO> results = this.dnsService.getAllRecords();

        Assertions.assertTrue(results.isEmpty());
    }

    @Test
    void getAllRecordsReturnsEmptyWhenResultNotOk() {
        Mockito.when(this.client.listRecords("example.com"))
                .thenReturn(Optional.of(new PtispDTO.ListResponse("error", List.of(
                        new PtispDTO("1", "test.example.com.", "A", "IN", 300L, "1.1.1.1", "", "")))));

        final List<ExternalDnsDTO> results = this.dnsService.getAllRecords();

        Assertions.assertTrue(results.isEmpty());
    }

    @Test
    void createRecordSkipsWhenNoAccountConfigured() {
        final ExternalDnsDTO ep = ExternalDnsDTO.of("unknown.net", List.of("1.1.1.1"), "A", 300L);
        this.dnsService.createRecord(ep);

        Mockito.verify(this.client, Mockito.never()).addRecord(ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void deleteRecordSkipsWhenNoAccountConfigured() {
        final ExternalDnsDTO ep = ExternalDnsDTO.of("unknown.net", List.of("1.1.1.1"), "A", 300L);
        this.dnsService.deleteRecord(ep);

        Mockito.verify(this.client, Mockito.never()).deleteRecord(ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void deleteRecordSkipsWhenNotFound() {
        Mockito.when(this.client.listRecords("example.com"))
                .thenReturn(Optional.of(new PtispDTO.ListResponse("ok", List.of())));

        final ExternalDnsDTO ep = ExternalDnsDTO.of("missing.example.com", List.of("1.1.1.1"), "A", 300L);
        this.dnsService.deleteRecord(ep);

        Mockito.verify(this.client, Mockito.never()).deleteRecord(ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void createRecordTranslatesTxtDashName() {
        Mockito.when(this.client.listRecords("example.com"))
                .thenReturn(Optional.of(new PtispDTO.ListResponse("ok", List.of())));

        final ExternalDnsDTO ep = ExternalDnsDTO.of("_dkim.mail-example.com", List.of("tokenvalue"), "TXT", 300L);
        this.dnsService.createRecord(ep);

        Mockito.verify(this.client).addRecord(
                ArgumentMatchers.eq("example.com"),
                ArgumentMatchers.argThat(dto -> "_dkim.mail.example.com.".equals(dto.name())));
    }

    @Test
    void deleteRecordFindsPtispEncodedApexTxt() {
        // external-dns v0.21 sends _edns.a-{domain}; PTisp stores it as _edns.a.{domain}
        final PtispDTO existing = new PtispDTO("line99", "_edns.a.example.com.", "TXT", "IN", 300L, null, null,
                "\"heritage=external-dns\"");
        Mockito.when(this.client.listRecords("example.com"))
                .thenReturn(Optional.of(new PtispDTO.ListResponse("ok", List.of(existing))));

        final ExternalDnsDTO ep = ExternalDnsDTO.of("_edns.a-example.com", List.of("heritage=external-dns"), "TXT",
                300L);
        this.dnsService.deleteRecord(ep);

        Mockito.verify(this.client).deleteRecord("example.com", "line99");
    }

    @Test
    void deleteRecordFindsPtispEncodedSubdomainTxt() {
        // external-dns v0.21 sends _edns.a-{sub}.{domain}; PTisp stores it unchanged
        final PtispDTO existing = new PtispDTO("line88", "_edns.a-www.example.com.", "TXT", "IN", 300L, null, null,
                "\"heritage=external-dns\"");
        Mockito.when(this.client.listRecords("example.com"))
                .thenReturn(Optional.of(new PtispDTO.ListResponse("ok", List.of(existing))));

        final ExternalDnsDTO ep = ExternalDnsDTO.of("_edns.a-www.example.com",
                List.of("heritage=external-dns"), "TXT", 300L);
        this.dnsService.deleteRecord(ep);

        Mockito.verify(this.client).deleteRecord("example.com", "line88");
    }

    @Test
    void createRecordSkipsPtispEncodedApexTxtDuplicate() {
        // PTisp stores _edns.a-{domain} as _edns.a.{domain}; skip-check must detect the duplicate
        final PtispDTO existing = new PtispDTO("line77", "_edns.a.example.com.", "TXT", "IN", 300L, null, null,
                "\"same-value\"");
        Mockito.when(this.client.listRecords("example.com"))
                .thenReturn(Optional.of(new PtispDTO.ListResponse("ok", List.of(existing))));

        final ExternalDnsDTO ep = ExternalDnsDTO.of("_edns.a-example.com", List.of("same-value"), "TXT", 300L);
        this.dnsService.createRecord(ep);

        Mockito.verify(this.client, Mockito.never()).addRecord(ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void getAllRecordsMergesCnameTargets() {
        final PtispDTO cname = new PtispDTO("1", "alias.example.com.", "CNAME", "IN", 300L, "", "target.example.com.", "");
        final PtispDTO owner = new PtispDTO("2", "_edns.cname-alias.example.com.", "TXT", "IN", 300L, null, null,
                "\"heritage=external-dns\"");
        Mockito.when(this.client.listRecords("example.com"))
                .thenReturn(Optional.of(new PtispDTO.ListResponse("ok", List.of(cname, owner))));

        final List<ExternalDnsDTO> results = this.dnsService.getAllRecords();

        final ExternalDnsDTO ep = results.stream()
                .filter(e -> "CNAME".equals(e.recordType()))
                .findFirst().orElseThrow();
        Assertions.assertEquals(List.of("target.example.com"), ep.targets());
    }

    @Test
    void getAllRecordsTranslatesTxtDashName() {
        final PtispDTO txt = new PtispDTO("1", "_dkim.mail.example.com.", "TXT", "IN", 300L, "", "", "\"token\"");
        Mockito.when(this.client.listRecords("example.com"))
                .thenReturn(Optional.of(new PtispDTO.ListResponse("ok", List.of(txt))));

        final List<ExternalDnsDTO> results = this.dnsService.getAllRecords();

        Assertions.assertEquals(1, results.size());
        Assertions.assertEquals("_dkim.mail-example.com", results.get(0).dnsName());
    }
}
