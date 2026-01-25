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
    private ManagedAccount account;

    @BeforeEach
    void setUp() {
        this.account = new ManagedAccount(List.of("example.com"), this.client);
        this.dnsService = new DnsService(List.of(this.account));
    }

    @Test
    void getAllRecordsMergesTargets() {
        final PtispDTO r1 = new PtispDTO("1", "test.example.com.", "A", "IN", 300L, "1.1.1.1", "", "");
        final PtispDTO r2 = new PtispDTO("2", "test.example.com.", "A", "IN", 300L, "2.2.2.2", "", "");
        Mockito.when(this.client.listRecords("example.com"))
                .thenReturn(Optional.of(new PtispDTO.ListResponse("ok", List.of(r1, r2))));

        final List<ExternalDnsDTO> results = this.dnsService.getAllRecords();

        Assertions.assertEquals(1, results.size());
        final ExternalDnsDTO ep = results.get(0);
        Assertions.assertEquals("test.example.com", ep.dnsName());
        Assertions.assertEquals(2, ep.targets().size());
        Assertions.assertTrue(ep.targets().contains("1.1.1.1"));
        Assertions.assertTrue(ep.targets().contains("2.2.2.2"));
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
}
