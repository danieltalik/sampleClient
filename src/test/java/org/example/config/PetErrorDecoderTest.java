package org.example.config;

import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PetErrorDecoderTest {

    private final PetErrorDecoder decoder = new PetErrorDecoder();

    private static Response response(int status, byte[] body) {
        Request request = Request.create(Request.HttpMethod.GET, "http://localhost/pets/1",
                Map.of(), null, StandardCharsets.UTF_8, null);
        Response.Builder builder = Response.builder().status(status).request(request).headers(Map.of());
        if (body != null) {
            builder.body(body);
        }
        return builder.build();
    }

    @Test
    void mapsNotFound() {
        Exception ex = decoder.decode("PetsApi#getPetById", response(404, "{}".getBytes()));
        assertThat(ex).isInstanceOf(FeignException.NotFound.class);
    }

    @Test
    void handlesMissingBody() {
        Exception ex = decoder.decode("PetsApi#getPetById", response(404, null));
        assertThat(ex).isInstanceOf(FeignException.NotFound.class);
    }

    @Test
    void keepsOriginalStatusForOtherErrors() {
        Exception ex = decoder.decode("PetsApi#getPets", response(503, null));
        assertThat(ex).isInstanceOf(FeignException.class);
        assertThat(((FeignException) ex).status()).isEqualTo(503);
    }
}
