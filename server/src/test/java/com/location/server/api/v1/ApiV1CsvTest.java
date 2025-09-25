package com.location.server.api.v1;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiV1CsvTest {
  @Autowired MockMvc mockMvc;

  @Test
  void csv_ok_with_attachment_header() throws Exception {
    mockMvc
        .perform(get("/api/v1/interventions/csv"))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Disposition", containsString("attachment")));
  }
}
