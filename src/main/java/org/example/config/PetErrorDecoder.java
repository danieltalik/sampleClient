package org.example.config;

import feign.FeignException;
import feign.Response;
import feign.codec.ErrorDecoder;

import java.io.IOException;
import java.io.InputStream;

/**
 * Turns error responses from the Pets API into FeignExceptions that keep the original status,
 * so the controller can pass it on.
 */
public class PetErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder fallback = new ErrorDecoder.Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        byte[] body = readBody(response);
        return switch (response.status()) {
            case 400 -> new FeignException.BadRequest("Bad request to Pets API", response.request(), body, response.headers());
            case 404 -> new FeignException.NotFound("Pet not found", response.request(), body, response.headers());
            default -> fallback.decode(methodKey, response);
        };
    }

    private static byte[] readBody(Response response) {
        if (response.body() == null) {
            return new byte[0];
        }
        try (InputStream in = response.body().asInputStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            return new byte[0];
        }
    }
}
