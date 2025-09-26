package com.location.server.api.v1;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiV1EmailTest {
  @Autowired MockMvc mvc;

  @Test
  void emailing_returns_accepted() throws Exception {
    mvc.perform(
            post("/api/v1/documents/abc/email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"to\":\"demo@example.com\",\"subject\":\"Subj\",\"body\":\"Body\"}"))
        .andExpect(status().isAccepted());
  }

  @Test
  void emailing_requires_valid_email() throws Exception {
    mvc.perform(
            post("/api/v1/documents/abc/email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"to\":\"bad\"}"))
        .andExpect(status().isBadRequest());
  }
}
