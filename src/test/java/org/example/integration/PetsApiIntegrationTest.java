package org.example.integration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.UncheckedIOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Runs the real generated Feign client, error decoder and controller against a stub sampleServer,
 * so nothing between our endpoint and the HTTP call is mocked.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PetsApiIntegrationTest {

    private static final StubPetsServer STUB = startStub();

    @Autowired
    private MockMvc mockMvc;

    private static StubPetsServer startStub() {
        try {
            return new StubPetsServer();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void petsUrl(DynamicPropertyRegistry registry) {
        registry.add("pets.url", STUB::url);
    }

    @AfterAll
    static void stopStub() {
        STUB.close();
    }

    @Test
    void listsPetsFromTheServer() throws Exception {
        STUB.respondWith(200, """
                [{"id":1,"name":"Alphonse","age":4,"type":"CAT"},
                 {"id":3,"name":"Sloopy","age":1,"type":"DOG"}]
                """);

        mockMvc.perform(get("/pets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].type").value("DOG"));
        assertThat(STUB.lastPath()).isEqualTo("/pets");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "42", "abc"})
    void callsTheServerWithTheSameId(String id) throws Exception {
        STUB.respondWith(200, "{\"id\":1,\"name\":\"Alphonse\",\"age\":4,\"type\":\"CAT\"}");

        mockMvc.perform(get("/pets/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alphonse"));
        assertThat(STUB.lastPath()).isEqualTo("/pets/" + id);
    }

    @ParameterizedTest(name = "server {0} -> client {1}")
    @CsvSource(delimiter = '|', value = {
            "400 | 400 | {\"detail\":\"bad\"}",
            "404 | 404 | {\"detail\":\"Pet not found\"}",
            "404 | 404 | ''",
            "409 | 409 | {}",
            "500 | 502 | {\"detail\":\"boom\"}",
            "500 | 502 | ''",
            "503 | 502 | ''"
    })
    void mapsServerErrors(int serverStatus, int expectedStatus, String body) throws Exception {
        STUB.respondWith(serverStatus, body);

        mockMvc.perform(get("/pets/1"))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.status").value(expectedStatus));
    }

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
