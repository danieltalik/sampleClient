package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.api.PetsApiClient;
import org.example.model.Pet;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PetService {
    private final PetsApiClient petsApiClient;

    public PetService(PetsApiClient petsApiClient){
        this.petsApiClient = petsApiClient;
    }

    public ResponseEntity<List<Pet>> getPets(){
        return petsApiClient.getPets();
    }
    public ResponseEntity<Pet> getPet(String id){
        return petsApiClient.getPetById(id);
    }
}
