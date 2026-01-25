package com.fraguinha.ptisp.webhook.client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;

import com.fraguinha.ptisp.webhook.model.PtispDTO;

class PtispClientTest {

    private PtispClient ptispClient;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        final RestClient.Builder builder = RestClient.builder();
        this.mockServer = MockRestServiceServer.bindTo(builder).build();
        this.ptispClient = new PtispClient("test@email.com", builder.build());
    }

    @Test
    void listRecordsCallsCorrectUri() {
        final String jsonResponse = """
                {
                  "result": "ok",
                  "records": [
                    { "line": "1", "name": "test.com", "type": "A", "address": "1.2.3.4" }
                  ]
                }
                """;

        this.mockServer.expect(MockRestRequestMatchers.requestTo("/parking/example.com/dns/list"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(MockRestResponseCreators.withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        final PtispDTO.ListResponse response = this.ptispClient.listRecords("example.com").orElseThrow();

        Assertions.assertEquals("ok", response.result());
        Assertions.assertEquals(1, response.records().size());
        Assertions.assertEquals("1.2.3.4", response.records().get(0).address());
    }

    @Test
    void addRecordCallsCorrectUri() {
        this.mockServer.expect(MockRestRequestMatchers.requestTo("/parking/example.com/dns/add"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withSuccess());

        final PtispDTO dto = new PtispDTO(null, "test.com.", "A", "IN", 300L, "1.2.3.4", null, null);
        this.ptispClient.addRecord("example.com", dto);
        this.mockServer.verify();
    }

    @Test
    void deleteRecordCallsCorrectUri() {
        this.mockServer.expect(MockRestRequestMatchers.requestTo("/parking/example.com/dns/line123/delete"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withSuccess());

        this.ptispClient.deleteRecord("example.com", "line123");
        this.mockServer.verify();
    }
}
