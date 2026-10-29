package org.example.controller;

import feign.FeignException;
import feign.Request;
import feign.RetryableException;
import org.example.config.PetApiConfig;
import org.example.model.Pet;
import org.example.service.PetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PetController.class)
@Import(PetApiConfig.class)
class PetControllerTest {

    private static final Request REQUEST = Request.create(Request.HttpMethod.GET, "http://localhost:8085/pets/1",
            Map.of(), null, StandardCharsets.UTF_8, null);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PetService petService;

    @Test
    void listsPets() throws Exception {
        Pet pet = new Pet("Buddy", 3, Pet.TypeEnum.DOG);
        when(petService.getPets()).thenReturn(List.of(pet));

        mockMvc.perform(get("/pets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Buddy"));
    }

    @Test
    void passesThroughNotFound() throws Exception {
        when(petService.getPet("9")).thenThrow(
                new FeignException.NotFound("Pet not found", REQUEST, new byte[0], Map.of()));

        mockMvc.perform(get("/pets/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Pet not found"));
    }

    @Test
    void returns503WhenServerIsDown() throws Exception {
        // Feign reports connection failures with status -1.
        when(petService.getPet("1")).thenThrow(
                new RetryableException(-1, "Connection refused", Request.HttpMethod.GET, (Long) null, REQUEST));

        mockMvc.perform(get("/pets/1"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void returns502ForServerErrors() throws Exception {
        when(petService.getPets()).thenThrow(
                new FeignException.InternalServerError("boom", REQUEST, new byte[0], Map.of()));

        mockMvc.perform(get("/pets"))
                .andExpect(status().isBadGateway());
    }
}
