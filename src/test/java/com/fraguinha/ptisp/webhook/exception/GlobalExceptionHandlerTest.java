package com.fraguinha.ptisp.webhook.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @RestController
    static class ExceptionTriggerController {
        @GetMapping("/trigger-error")
        public void trigger() {
            throw new RuntimeException("Test exception");
        }
    }

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(new ExceptionTriggerController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void handleExceptionReturns500WithPayload() throws Exception {
        this.mockMvc.perform(MockMvcRequestBuilders.get("/trigger-error"))
                .andExpect(MockMvcResultMatchers.status().isInternalServerError())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Test exception"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(500))
                .andExpect(MockMvcResultMatchers.jsonPath("$.timestamp").exists());
    }
}
