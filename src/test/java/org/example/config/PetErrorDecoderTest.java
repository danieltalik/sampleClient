package org.example.config;

import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Stream;

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

    /** Every status, with and without a body: the decoder must never throw and must keep the status. */
    static Stream<Arguments> statusesWithAndWithoutBody() {
        int[] statuses = {400, 401, 403, 404, 409, 422, 429, 500, 502, 503, 504};
        byte[][] bodies = {null, new byte[0], "{\"detail\":\"x\"}".getBytes(StandardCharsets.UTF_8)};
        return java.util.Arrays.stream(statuses).boxed()
                .flatMap(status -> Stream.of(bodies).map(body -> Arguments.of(status, body)));
    }

    @ParameterizedTest(name = "status {0}, body {1}")
    @MethodSource("statusesWithAndWithoutBody")
    void keepsTheOriginalStatus(int status, byte[] body) {
        Exception ex = decoder.decode("PetsApi#getPetById", response(status, body));

        assertThat(ex).isInstanceOf(FeignException.class);
        assertThat(((FeignException) ex).status()).isEqualTo(status);
    }

    static Stream<Arguments> specificExceptionTypes() {
        return Stream.of(
                Arguments.of(400, FeignException.BadRequest.class, "Bad request to Pets API"),
                Arguments.of(404, FeignException.NotFound.class, "Pet not found"));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("specificExceptionTypes")
    void mapsKnownStatusesToFriendlyExceptions(int status, Class<? extends FeignException> type, String message) {
        Exception ex = decoder.decode("PetsApi#getPetById", response(status, null));

        assertThat(ex).isInstanceOf(type).hasMessage(message);
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 404})
    void keepsTheResponseBody(int status) {
        byte[] body = "{\"detail\":\"Pet not found\"}".getBytes(StandardCharsets.UTF_8);

        FeignException ex = (FeignException) decoder.decode("PetsApi#getPetById", response(status, body));

        assertThat(ex.contentUTF8()).isEqualTo("{\"detail\":\"Pet not found\"}");
    }
}
