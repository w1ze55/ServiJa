package com.servija.servija.service;

import com.servija.servija.dto.AddressRequest;
import com.servija.servija.dto.AddressResponse;
import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.model.Address;
import com.servija.servija.model.User;
import com.servija.servija.repository.AddressRepository;
import com.servija.servija.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final GeocodingService geocodingService;

    @Transactional
    public List<AddressResponse> getUserAddresses(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        return addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(user.getId())
                .stream()
                .map(this::ensureCoordinates)
                .map(AddressResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public AddressResponse createAddress(String email, AddressRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        // Se for o primeiro endereço ou marcado como padrão, garantir que é o único padrão
        boolean isDefault = request.getIsDefault() != null && request.getIsDefault();
        if (addressRepository.countByUserId(user.getId()) == 0) {
            isDefault = true; // Primeiro endereço é sempre o padrão
        }

        Address address = Address.builder()
                .user(user)
                .label(request.getLabel())
                .street(request.getStreet())
                .number(request.getNumber())
                .complement(request.getComplement())
                .neighborhood(request.getNeighborhood())
                .city(request.getCity())
                .state(request.getState().toUpperCase())
                .zipCode(request.getZipCode())
                .isDefault(isDefault)
                .build();
        populateCoordinates(address, true);

        address = addressRepository.save(address);

        // Se marcado como padrão, remover padrão dos outros
        if (isDefault) {
            addressRepository.clearDefaultForUser(user.getId(), address.getId());
        }

        return AddressResponse.fromEntity(address);
    }

    @Transactional
    public AddressResponse updateAddress(String email, Long addressId, AddressRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        Address address = addressRepository.findByIdAndUserId(addressId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado"));

        address.setLabel(request.getLabel());
        address.setStreet(request.getStreet());
        address.setNumber(request.getNumber());
        address.setComplement(request.getComplement());
        address.setNeighborhood(request.getNeighborhood());
        address.setCity(request.getCity());
        address.setState(request.getState().toUpperCase());
        address.setZipCode(request.getZipCode());
        populateCoordinates(address, true);

        // Atualizar padrão se solicitado
        if (request.getIsDefault() != null && request.getIsDefault() && !address.getIsDefault()) {
            addressRepository.clearDefaultForUser(user.getId(), address.getId());
            address.setIsDefault(true);
        }

        address = addressRepository.save(address);
        return AddressResponse.fromEntity(address);
    }

    @Transactional
    public void deleteAddress(String email, Long addressId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        Address address = addressRepository.findByIdAndUserId(addressId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado"));

        boolean wasDefault = address.getIsDefault();
        addressRepository.delete(address);

        // Se era o padrão, definir outro como padrão
        if (wasDefault) {
            List<Address> remaining = addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(user.getId());
            if (!remaining.isEmpty()) {
                Address newDefault = remaining.get(0);
                newDefault.setIsDefault(true);
                addressRepository.save(newDefault);
            }
        }
    }

    @Transactional
    public AddressResponse setDefaultAddress(String email, Long addressId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        Address address = addressRepository.findByIdAndUserId(addressId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado"));

        addressRepository.clearDefaultForUser(user.getId(), address.getId());
        address.setIsDefault(true);
        address = ensureCoordinates(address);
        address = addressRepository.save(address);

        return AddressResponse.fromEntity(address);
    }
    private Address ensureCoordinates(Address address) {
        if (hasCoordinates(address)) {
            return address;
        }

        if (!populateCoordinates(address, false)) {
            return address;
        }

        return addressRepository.save(address);
    }

    private boolean populateCoordinates(Address address, boolean required) {
        Optional<GeocodingService.Coordinates> coordinates = geocodingService.geocodeAddress(
                        address.getStreet(),
                        address.getNumber(),
                        address.getNeighborhood(),
                        address.getCity(),
                        address.getState());

        if (coordinates.isEmpty()) {
            if (required) {
                throw new IllegalArgumentException(
                        "Nao foi possivel localizar o endereco informado no mapa.");
            }
            return false;
        }

        address.setLatitude(coordinates.get().latitude());
        address.setLongitude(coordinates.get().longitude());
        return true;
    }

    private boolean hasCoordinates(Address address) {
        return address.getLatitude() != null
                && address.getLongitude() != null
                && isCoordinateInRange(address.getLatitude(), -90, 90)
                && isCoordinateInRange(address.getLongitude(), -180, 180);
    }

    private boolean isCoordinateInRange(BigDecimal value, int min, int max) {
        return Objects.nonNull(value)
                && value.compareTo(BigDecimal.valueOf(min)) >= 0
                && value.compareTo(BigDecimal.valueOf(max)) <= 0
                && value.compareTo(BigDecimal.ZERO) != 0;
    }
}
