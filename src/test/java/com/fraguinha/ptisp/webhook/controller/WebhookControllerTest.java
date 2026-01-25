package com.fraguinha.ptisp.webhook.controller;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fraguinha.ptisp.webhook.model.ExternalDnsDTO;
import com.fraguinha.ptisp.webhook.model.WebhookConstants;
import com.fraguinha.ptisp.webhook.service.DnsService;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class WebhookControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private DnsService dnsService;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(new WebhookController(this.dnsService)).build();
    }

    @Test
    void healthCheckReturnsCorrectHeader() throws Exception {
        this.mockMvc.perform(MockMvcRequestBuilders.get("/"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.header().string(WebhookConstants.PROVIDER_NAME_HEADER, WebhookConstants.PROVIDER_NAME))
                .andExpect(MockMvcResultMatchers.content().contentType(WebhookConstants.CONTENT_TYPE));
    }

    @Test
    void getRecordsReturnsData() throws Exception {
        final ExternalDnsDTO ep = ExternalDnsDTO.of("test.com", List.of("1.2.3.4"), "A", 300L);
        Mockito.when(this.dnsService.getAllRecords()).thenReturn(List.of(ep));

        this.mockMvc.perform(MockMvcRequestBuilders.get("/records"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(WebhookConstants.CONTENT_TYPE))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].dnsName").value("test.com"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].targets[0]").value("1.2.3.4"));
    }

    @Test
    void applyChangesHandlesMultipleOperations() throws Exception {
        final ExternalDnsDTO toCreate = ExternalDnsDTO.of("create.com", List.of("1.1.1.1"), "A", 300L);
        final ExternalDnsDTO toDelete = ExternalDnsDTO.of("delete.com", List.of("2.2.2.2"), "A", 300L);
        final ExternalDnsDTO oldUpd = ExternalDnsDTO.of("update.com", List.of("3.3.3.3"), "A", 300L);
        final ExternalDnsDTO newUpd = ExternalDnsDTO.of("update.com", List.of("4.4.4.4"), "A", 300L);

        final ExternalDnsDTO.Changes changes = new ExternalDnsDTO.Changes(
                List.of(toCreate),
                List.of(oldUpd),
                List.of(newUpd),
                List.of(toDelete));

        this.mockMvc.perform(MockMvcRequestBuilders.post("/records")
                .contentType(WebhookConstants.CONTENT_TYPE)
                .content(this.objectMapper.writeValueAsString(changes)))
                .andExpect(MockMvcResultMatchers.status().isNoContent());

        Mockito.verify(this.dnsService).createRecord(toCreate);
        Mockito.verify(this.dnsService).deleteRecord(toDelete);
        Mockito.verify(this.dnsService).updateRecord(oldUpd, newUpd);
    }

    @Test
    void applyChangesHandlesNullCollections() throws Exception {
        final ExternalDnsDTO.Changes changes = new ExternalDnsDTO.Changes(null, null, null, null);

        this.mockMvc.perform(MockMvcRequestBuilders.post("/records")
                .contentType(WebhookConstants.CONTENT_TYPE)
                .content(this.objectMapper.writeValueAsString(changes)))
                .andExpect(MockMvcResultMatchers.status().isNoContent());

        Mockito.verify(this.dnsService, Mockito.never()).createRecord(ArgumentMatchers.any());
        Mockito.verify(this.dnsService, Mockito.never()).deleteRecord(ArgumentMatchers.any());
        Mockito.verify(this.dnsService, Mockito.never()).updateRecord(ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void adjustEndpointsReturnsSameList() throws Exception {
        final ExternalDnsDTO ep = ExternalDnsDTO.of("adjust.com", List.of("9.9.9.9"), "A", 300L);
        final List<ExternalDnsDTO> endpoints = List.of(ep);

        this.mockMvc.perform(MockMvcRequestBuilders.post("/adjustendpoints")
                .contentType(WebhookConstants.CONTENT_TYPE)
                .content(this.objectMapper.writeValueAsString(endpoints)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].dnsName").value("adjust.com"));
    }
}
