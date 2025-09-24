package com.location.server;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class SystemControllerTest {

  @Autowired MockMvc mvc;

  @Test
  void ping_isEventStream() throws Exception {
    mvc.perform(get("/api/system/ping"))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", MediaType.TEXT_EVENT_STREAM_VALUE));
  }
}
