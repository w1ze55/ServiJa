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

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public List<AddressResponse> getUserAddresses(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        return addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(user.getId())
                .stream()
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
        address = addressRepository.save(address);

        return AddressResponse.fromEntity(address);
    }
}
