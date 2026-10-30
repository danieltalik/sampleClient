package org.example.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "pets.url=http://localhost:1")
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {"/actuator/health", "/v3/api-docs", "/swagger-ui/index.html"})
    void publicPathsAreOpen(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/pets", "/pets/1"})
    void petEndpointsAreNotBlockedBySecurity(String path) throws Exception {
        // pets.url points nowhere, so these fail downstream (503), but must get past security.
        mockMvc.perform(get(path)).andExpect(status().is(not(403)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/actuator/env", "/actuator/beans", "/anything-else"})
    void everythingElseIsForbidden(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isForbidden());
    }
}
