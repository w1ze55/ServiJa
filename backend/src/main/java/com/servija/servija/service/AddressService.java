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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final GeocodingService geocodingService;
    private final AuditLogger auditLogger;

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

        Address address = createAddressForUser(user, request);
        return AddressResponse.fromEntity(address);
    }

    @Transactional
    public Address createAddressForUser(User user, AddressRequest request) {
        validateAddressData(request);

        boolean isDefault = request.getIsDefault() != null && request.getIsDefault();
        if (addressRepository.countByUserId(user.getId()) == 0) {
            isDefault = true;
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

        if (isDefault) {
            addressRepository.clearDefaultForUser(user.getId(), address.getId());
        }

        return address;
    }

    public void validateAddressData(AddressRequest request) {
        Address address = Address.builder()
                .label(request.getLabel())
                .street(request.getStreet())
                .number(request.getNumber())
                .complement(request.getComplement())
                .neighborhood(request.getNeighborhood())
                .city(request.getCity())
                .state(request.getState().toUpperCase())
                .zipCode(request.getZipCode())
                .isDefault(Boolean.TRUE.equals(request.getIsDefault()))
                .build();

        populateCoordinates(address, true);
    }

    @Transactional
    public AddressResponse updateAddress(String email, Long addressId, AddressRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        Address address = addressRepository.findByIdAndUserId(addressId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado"));

        Map<String, Object> beforeState = buildAddressAuditState(address);

        address.setLabel(request.getLabel());
        address.setStreet(request.getStreet());
        address.setNumber(request.getNumber());
        address.setComplement(request.getComplement());
        address.setNeighborhood(request.getNeighborhood());
        address.setCity(request.getCity());
        address.setState(request.getState().toUpperCase());
        address.setZipCode(request.getZipCode());
        populateCoordinates(address, true);

        if (request.getIsDefault() != null && request.getIsDefault() && !address.getIsDefault()) {
            addressRepository.clearDefaultForUser(user.getId(), address.getId());
            address.setIsDefault(true);
        }

        address = addressRepository.save(address);
        recordAddressAudit("ADDRESS_UPDATED", address, beforeState);
        return AddressResponse.fromEntity(address);
    }

    @Transactional
    public void deleteAddress(String email, Long addressId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        Address address = addressRepository.findByIdAndUserId(addressId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado"));

        boolean wasDefault = address.getIsDefault();
        AddressResponse deletedAddress = AddressResponse.fromEntity(address);
        addressRepository.delete(address);

        if (wasDefault) {
            List<Address> remaining = addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(user.getId());
            if (!remaining.isEmpty()) {
                Address newDefault = remaining.get(0);
                newDefault.setIsDefault(true);
                addressRepository.save(newDefault);
            }
        }

        auditLogger.recordSemantic(
                "ADDRESS_DELETED",
                "ADDRESS",
                String.valueOf(addressId),
                address.getLabel(),
                com.servija.servija.model.AuditLog.Result.SUCCESS,
                null,
                deletedAddress,
                null,
                null);
    }

    @Transactional
    public AddressResponse setDefaultAddress(String email, Long addressId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        Address address = addressRepository.findByIdAndUserId(addressId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado"));

        Map<String, Object> beforeState = buildAddressAuditState(address);

        addressRepository.clearDefaultForUser(user.getId(), address.getId());
        address.setIsDefault(true);
        address = ensureCoordinates(address);
        address = addressRepository.save(address);
        recordAddressAudit("ADDRESS_DEFAULT_SET", address, beforeState);

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
                        "Não foi possível localizar o endereço informado no mapa.");
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

    private void recordAddressAudit(String action, Address address, Map<String, Object> beforeState) {
        Map<String, Object> afterState = buildAddressAuditState(address);
        List<String> changedFields = AuditStateDiffUtils.changedFields(beforeState, afterState);
        if (changedFields.isEmpty()) {
            return;
        }
        auditLogger.recordSemantic(
                action,
                "ADDRESS",
                String.valueOf(address.getId()),
                address.getLabel(),
                com.servija.servija.model.AuditLog.Result.SUCCESS,
                Map.of("fieldsChanged", changedFields),
                AuditStateDiffUtils.filterFields(beforeState, changedFields),
                AuditStateDiffUtils.filterFields(afterState, changedFields));
    }

    private Map<String, Object> buildAddressAuditState(Address address) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("label", address.getLabel());
        state.put("street", address.getStreet());
        state.put("number", address.getNumber());
        state.put("complement", address.getComplement());
        state.put("neighborhood", address.getNeighborhood());
        state.put("city", address.getCity());
        state.put("state", address.getState());
        state.put("zipCode", address.getZipCode());
        state.put("latitude", address.getLatitude());
        state.put("longitude", address.getLongitude());
        state.put("default", address.getIsDefault());
        return state;
    }
}
