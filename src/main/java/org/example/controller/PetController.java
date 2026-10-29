package org.example.controller;

import feign.FeignException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.Pet;
import org.example.service.PetService;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Tag(name = "pets", description = "Pet lookups proxied to sampleServer")
@RequiredArgsConstructor
@RestController
public class PetController {

    private final PetService petService;

    @Operation(summary = "Retrieve a list of pets")
    @GetMapping("/pets")
    public List<Pet> getAllPets() {
        return petService.getPets();
    }

    @Operation(summary = "Get a pet by its ID")
    @GetMapping("/pets/{petId}")
    public Pet getPet(@PathVariable("petId") String petId) {
        return petService.getPet(petId);
    }

    /**
     * Passes 4xx errors from the Pets API through. Anything else (5xx, or no response at all,
     * which Feign reports as status -1) becomes a 502/503 so callers can tell it's a downstream problem.
     */
    @ExceptionHandler
    public ProblemDetail handleFeignException(FeignException ex) {
        HttpStatusCode status;
        String detail;
        if (ex.status() >= 400 && ex.status() < 500) {
            status = HttpStatusCode.valueOf(ex.status());
            detail = ex.status() == 404 ? "Pet not found" : "Pets API rejected the request";
        } else if (ex.status() <= 0) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            detail = "Pets API is unreachable";
        } else {
            status = HttpStatus.BAD_GATEWAY;
            detail = "Pets API returned an error";
        }
        log.warn("Pets API call failed with status {}: {}", ex.status(), ex.getMessage());
        return ProblemDetail.forStatusAndDetail(status, detail);
    }
}
