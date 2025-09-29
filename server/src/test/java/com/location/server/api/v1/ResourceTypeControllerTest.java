package com.location.server.api.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class ResourceTypeControllerTest {

  @Autowired private MockMvc mvc;

  @Autowired private ObjectMapper om;

  @Test
  void crudAndAssignment() throws Exception {
    mvc.perform(get("/api/v1/resource-types")).andExpect(status().isOk());

    JsonNode created =
        om.readTree(
            mvc.perform(
                    post("/api/v1/resource-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test Camion\",\"iconName\":\"truck.svg\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());

    String typeId = created.path("id").asText();
    assertThat(typeId).isNotBlank();

    mvc.perform(
            put("/api/v1/resources/R1/type")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"resourceTypeId\":\"" + typeId + "\"}"))
        .andExpect(status().isOk());

    JsonNode assigned =
        om.readTree(
            mvc.perform(get("/api/v1/resources/R1/type")).andExpect(status().isOk()).andReturn()
                .getResponse()
                .getContentAsString());
    assertThat(assigned.path("resourceTypeId").asText()).isEqualTo(typeId);

    mvc.perform(delete("/api/v1/resources/R1/type")).andExpect(status().isNoContent());
    mvc.perform(delete("/api/v1/resource-types/" + typeId)).andExpect(status().isNoContent());
  }
}
