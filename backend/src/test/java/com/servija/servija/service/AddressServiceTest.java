package com.servija.servija.service;

import com.servija.servija.dto.AddressRequest;
import com.servija.servija.dto.AddressResponse;
import com.servija.servija.model.Address;
import com.servija.servija.model.User;
import com.servija.servija.repository.AddressRepository;
import com.servija.servija.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GeocodingService geocodingService;

    @InjectMocks
    private AddressService addressService;

    @Test
    @DisplayName("Deve persistir latitude e longitude ao criar endereco")
    void createAddress_persistsCoordinates() {
        User user = buildUser(1L);
        AddressRequest request = AddressRequest.builder()
                .label("Casa")
                .street("Rua Teste")
                .number("123")
                .neighborhood("Centro")
                .city("Palhoca")
                .state("sc")
                .zipCode("88130-000")
                .isDefault(true)
                .build();

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(addressRepository.countByUserId(1L)).thenReturn(0L);
        when(geocodingService.geocodeAddress("Rua Teste", "123", "Centro", "Palhoca", "SC"))
                .thenReturn(Optional.of(new GeocodingService.Coordinates(
                        BigDecimal.valueOf(-27.6455),
                        BigDecimal.valueOf(-48.6697))));
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> {
            Address saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        AddressResponse response = addressService.createAddress("user@test.com", request);

        assertThat(response.getLatitude()).isEqualByComparingTo("-27.6455");
        assertThat(response.getLongitude()).isEqualByComparingTo("-48.6697");
        verify(addressRepository).clearDefaultForUser(1L, 10L);
    }

    @Test
    @DisplayName("Deve preencher coordenadas ausentes ao listar enderecos antigos")
    void getUserAddresses_backfillsMissingCoordinates() {
        User user = buildUser(1L);
        Address address = Address.builder()
                .id(7L)
                .user(user)
                .label("Casa")
                .street("Rua Teste")
                .number("123")
                .neighborhood("Centro")
                .city("Florianopolis")
                .state("SC")
                .zipCode("88000-000")
                .isDefault(true)
                .build();

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(1L)).thenReturn(List.of(address));
        when(geocodingService.geocodeAddress("Rua Teste", "123", "Centro", "Florianopolis", "SC"))
                .thenReturn(Optional.of(new GeocodingService.Coordinates(
                        BigDecimal.valueOf(-27.5949),
                        BigDecimal.valueOf(-48.5482))));
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<AddressResponse> responses = addressService.getUserAddresses("user@test.com");

        assertThat(responses).singleElement().satisfies(response -> {
            assertThat(response.getLatitude()).isEqualByComparingTo("-27.5949");
            assertThat(response.getLongitude()).isEqualByComparingTo("-48.5482");
        });
    }

    @Test
    @DisplayName("Nao deve geocodificar novamente endereco que ja possui coordenadas")
    void getUserAddresses_reusesStoredCoordinates() {
        User user = buildUser(1L);
        Address address = Address.builder()
                .id(8L)
                .user(user)
                .label("Casa")
                .street("Rua Teste")
                .number("123")
                .neighborhood("Centro")
                .city("Florianopolis")
                .state("SC")
                .zipCode("88000-000")
                .latitude(BigDecimal.valueOf(-27.5949))
                .longitude(BigDecimal.valueOf(-48.5482))
                .isDefault(true)
                .build();

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(1L)).thenReturn(List.of(address));

        List<AddressResponse> responses = addressService.getUserAddresses("user@test.com");

        assertThat(responses).singleElement().satisfies(response -> {
            assertThat(response.getLatitude()).isEqualByComparingTo("-27.5949");
            assertThat(response.getLongitude()).isEqualByComparingTo("-48.5482");
        });
        verify(geocodingService, never()).geocodeAddress(any(), any(), any(), any(), any());
    }

    private User buildUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("user@test.com");
        user.setUserType(User.UserType.CONTRACTOR);
        return user;
    }
}
