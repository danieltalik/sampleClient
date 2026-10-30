package org.example.controller;

import feign.FeignException;
import feign.Request;
import feign.RetryableException;
import org.example.config.PetApiConfig;
import org.example.model.Pet;
import org.example.service.PetService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
        when(petService.getPets()).thenReturn(List.of(
                new Pet("Buddy", 3, Pet.TypeEnum.DOG),
                new Pet("Nemo", 1, Pet.TypeEnum.FISH)));

        mockMvc.perform(get("/pets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Buddy"))
                .andExpect(jsonPath("$[1].type").value("FISH"));
    }

    @Test
    void listsNoPets() throws Exception {
        when(petService.getPets()).thenReturn(List.of());

        mockMvc.perform(get("/pets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @ParameterizedTest
    @EnumSource(Pet.TypeEnum.class)
    void returnsPetOfEveryType(Pet.TypeEnum type) throws Exception {
        when(petService.getPet("1")).thenReturn(new Pet("Any", 2, type));

        mockMvc.perform(get("/pets/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value(type.getValue()));
    }

    /**
     * How a failed call to sampleServer is reported to our caller:
     * 4xx passes through, 5xx becomes 502, and "no response" (status <= 0) becomes 503.
     */
    @ParameterizedTest(name = "Pets API {0} -> {1}")
    @CsvSource({
            "400, 400, Pets API rejected the request",
            "401, 401, Pets API rejected the request",
            "403, 403, Pets API rejected the request",
            "404, 404, Pet not found",
            "409, 409, Pets API rejected the request",
            "429, 429, Pets API rejected the request",
            "499, 499, Pets API rejected the request",
            "500, 502, Pets API returned an error",
            "502, 502, Pets API returned an error",
            "503, 502, Pets API returned an error",
            "504, 502, Pets API returned an error",
            "-1,  503, Pets API is unreachable",
            "0,   503, Pets API is unreachable"
    })
    void mapsDownstreamFailures(int upstreamStatus, int expectedStatus, String expectedDetail) throws Exception {
        when(petService.getPet("1")).thenThrow(feignException(upstreamStatus));

        mockMvc.perform(get("/pets/1"))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.detail").value(expectedDetail));
    }

    @Test
    void connectionFailureFromListingIsA503() throws Exception {
        when(petService.getPets()).thenThrow(
                new RetryableException(-1, "Connection refused", Request.HttpMethod.GET, (Long) null, REQUEST));

        mockMvc.perform(get("/pets")).andExpect(status().isServiceUnavailable());
    }

    @Test
    void doesNotLeakDownstreamDetails() throws Exception {
        when(petService.getPets()).thenThrow(feignException(500));

        mockMvc.perform(get("/pets"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("localhost:8085"))));
    }

    private static FeignException feignException(int status) {
        if (status <= 0) {
            return new RetryableException(status, "Connection refused", Request.HttpMethod.GET, (Long) null, REQUEST);
        }
        return FeignException.errorStatus("PetsApi#getPetById", feign.Response.builder()
                .status(status).request(REQUEST).headers(Map.of())
                .body("internal details from http://localhost:8085", StandardCharsets.UTF_8).build());
    }
}
