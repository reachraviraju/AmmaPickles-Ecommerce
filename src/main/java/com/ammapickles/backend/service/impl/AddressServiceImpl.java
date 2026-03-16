package com.ammapickles.backend.service.impl;

import com.ammapickles.backend.dto.address.AddressRequest;
import com.ammapickles.backend.dto.address.AddressResponse;
import com.ammapickles.backend.entity.Address;
import com.ammapickles.backend.entity.User;
import com.ammapickles.backend.exception.ResourceNotFoundException;
import com.ammapickles.backend.repository.AddressRepository;
import com.ammapickles.backend.repository.UserRepository;
import com.ammapickles.backend.service.AddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;



    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getAddressesByUser(Long userId) {
        log.info("Fetching addresses for user: {}", userId);

        // Verify user exists first
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }

        return addressRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

   
    @Override
    @Transactional(readOnly = true)
    public AddressResponse getAddressById(Long addressId, Long requestingUserId) {
        log.info("Fetching address: {}", addressId);
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found: " + addressId));

        // Ownership check
        if (!address.getUser().getId().equals(requestingUserId)) {
            throw new SecurityException("Access denied to address: " + addressId);
        }
        return mapToResponse(address);
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(Long addressId, AddressRequest request, Long requestingUserId) {
        log.info("Updating address: {}", addressId);
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found: " + addressId));

        // Ownership check
        if (!address.getUser().getId().equals(requestingUserId)) {
            throw new SecurityException("Access denied to address: " + addressId);
        }

        if (request.getStreet() != null) address.setStreet(request.getStreet());
        if (request.getCity() != null) address.setCity(request.getCity());
        if (request.getDistrict() != null) address.setDistrict(request.getDistrict());
        if (request.getState() != null) address.setState(request.getState());
        if (request.getPincode() != null) address.setPincode(request.getPincode());
        if (request.getDistanceInKm() != null) address.setDistanceInKm(request.getDistanceInKm());

        log.info("Address updated: {}", addressId);
        return mapToResponse(address);
    }

    

    @Override
    @Transactional
    public AddressResponse createAddress(Long userId, AddressRequest request) {
        log.info("Creating address for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Address address = Address.builder()
                .street(request.getStreet())
                .city(request.getCity())
                .district(request.getDistrict())
                .state(request.getState())
                .pincode(request.getPincode())
                .distanceInKm(request.getDistanceInKm())
                .user(user)
                .build();

        Address saved = addressRepository.save(address);
        log.info("Address created with id: {}", saved.getId());

        return mapToResponse(saved);
    }

    

   

    

    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        log.info("Deleting address {} for user {}", addressId, userId);

        // Verify address exists AND belongs to this user 
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Address not found or doesn't belong to user"));

        addressRepository.delete(address);
        log.info("Address deleted: {}", addressId);
    }

    // PRIVATE HELPER 

    private AddressResponse mapToResponse(Address address) {
        AddressResponse response = new AddressResponse();
        response.setId(address.getId());
        response.setStreet(address.getStreet());
        response.setCity(address.getCity());
        response.setDistrict(address.getDistrict());
        response.setState(address.getState());
        response.setPincode(address.getPincode());
        response.setDistanceInKm(address.getDistanceInKm());

        // Formatted address — > useful for order confirmation display
        response.setFormattedAddress(
                address.getStreet() + ", " + address.getCity() + ", " +
                address.getDistrict() + " - " + address.getPincode()
        );

        return response;
    }
}