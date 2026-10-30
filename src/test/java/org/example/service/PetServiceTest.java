package org.example.service;

import org.example.api.PetsApiClient;
import org.example.model.Pet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PetServiceTest {

    @Mock
    private PetsApiClient client;

    @InjectMocks
    private PetService service;

    @Test
    void returnsTheBodyOfTheListResponse() {
        List<Pet> pets = List.of(new Pet("Buddy", 3, Pet.TypeEnum.DOG));
        when(client.getPets()).thenReturn(ResponseEntity.ok(pets));

        assertThat(service.getPets()).isEqualTo(pets);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "42", "abc"})
    void passesTheIdThroughUnchanged(String id) {
        Pet pet = new Pet("Buddy", 3, Pet.TypeEnum.DOG);
        when(client.getPetById(id)).thenReturn(ResponseEntity.ok(pet));

        assertThat(service.getPet(id)).isSameAs(pet);
        verify(client).getPetById(id);
    }
}
