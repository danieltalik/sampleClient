package org.example.integration;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.net.ServerSocket;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** sampleServer isn't running: every call should be a clean 503, not a crash. */
@SpringBootTest
@AutoConfigureMockMvc
class ServerDownIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void petsUrl(DynamicPropertyRegistry registry) throws IOException {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        registry.add("pets.url", () -> "http://localhost:" + closedPort);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/pets", "/pets/1", "/pets/abc"})
    void returns503WhenServerIsDown(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("Pets API is unreachable"));
    }
}
