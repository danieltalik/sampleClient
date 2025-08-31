package org.example.config;


import feign.FeignException;
import feign.Response;
import feign.codec.ErrorDecoder;

import java.io.IOException;

public class PetErrorDecoder implements ErrorDecoder {
    @Override
    public Exception decode(String s, Response response) {
        byte[] responseBody;
        try {
            responseBody = response.body().asInputStream().readAllBytes();
        }
        catch (IOException ex){
            return new IOException("Error parsing Response from Pets API");
        }
        return switch (response.status()) {
            case 400 -> new FeignException.BadRequest("Bad Request to Pets API",response.request(), responseBody, response.headers());
            case 404 -> new FeignException.NotFound("Resource Not Found in Pets API",response.request(),responseBody,response.headers());
            default -> new FeignException.InternalServerError("Exception calling Pets API",response.request(),responseBody,response.headers());
        };
    }
}
