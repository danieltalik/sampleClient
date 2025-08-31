package org.example.controller;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.example.model.Pet;
import org.example.service.PetService;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
public class PetController {

    private final PetService petService;

    @GetMapping(value = "/pets")
    public ResponseEntity<List<Pet>> getAllPets(){
        return petService.getPets();
    }

    @GetMapping(value = "/pets/{petId}")
    public ResponseEntity<Pet> getPet(@PathVariable("petId") String pedId){
        return petService.getPet(pedId);
    }

    @ExceptionHandler
    public ResponseEntity<String> getException(FeignException ex){
        return new ResponseEntity<>(ex.getMessage(),HttpStatusCode.valueOf(ex.status()));
    }

}
