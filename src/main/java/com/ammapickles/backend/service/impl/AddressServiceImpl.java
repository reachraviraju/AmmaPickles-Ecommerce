package com.ammapickles.backend.service.impl;

import com.ammapickles.backend.dto.address.AddressRequest;

import com.ammapickles.backend.dto.address.AddressResponse;
import com.ammapickles.backend.entity.Address;
import com.ammapickles.backend.entity.User;
import com.ammapickles.backend.exception.ResourceNotFoundException;
import com.ammapickles.backend.repository.AddressRepository;
import com.ammapickles.backend.repository.OrderRepository;
import com.ammapickles.backend.repository.UserRepository;
import com.ammapickles.backend.service.AddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;



@Slf4j
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getAddressesByUser(Long userId) {
        log.info("Fetching addresses for user: {}", userId);
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
        if (!address.getUser().getId().equals(requestingUserId)) {
            throw new AccessDeniedException("Access denied to address: " + addressId);
        }
        return mapToResponse(address);
    }

    @Override
    @Transactional
    public AddressResponse createAddress(Long userId, AddressRequest request) {
        log.info("Creating address for user: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        
        if (addressRepository.countByUserId(userId) >= 5) {
            throw new IllegalStateException("Maximum 5 addresses allowed per account.");
        }

        Address address = Address.builder()
        		.name(request.getName())
                .street(request.getStreet())
                .city(request.getCity())
                .district(request.getDistrict())
                .state(request.getState())
                .pincode(request.getPincode())
                .mobileNumber(request.getMobileNumber())
                .user(user)
                .build();

        Address saved = addressRepository.save(address);
        log.info("Address created with id: {}", saved.getId());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(Long addressId, AddressRequest request, Long requestingUserId) {
        log.info("Updating address: {}", addressId);
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found: " + addressId));
        if (!address.getUser().getId().equals(requestingUserId)) {
            throw new AccessDeniedException("Access denied to address: " + addressId);
        }
        if (request.getName()         != null) address.setName(request.getName());
        if (request.getStreet()       != null) address.setStreet(request.getStreet());
        if (request.getCity()         != null) address.setCity(request.getCity());
        if (request.getDistrict()     != null) address.setDistrict(request.getDistrict());
        if (request.getState()        != null) address.setState(request.getState());
        if (request.getPincode()      != null) address.setPincode(request.getPincode());
        if (request.getMobileNumber() != null) address.setMobileNumber(request.getMobileNumber());
        
        addressRepository.save(address);

        log.info("Address updated: {}", addressId);
        return mapToResponse(address);
    }

    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        log.info("Deleting address {} for user {}", addressId, userId);
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Address not found or doesn't belong to user"));
        
        if(orderRepository.existsByDeliveryAddressId(addressId))
        {
        	throw new IllegalStateException("Cannot delete this address as it is linked to an existing order.");
        }
        addressRepository.delete(address);
        log.info("Address deleted: {}", addressId);
    }

    // PRIVATE HELPER
    private AddressResponse mapToResponse(Address address) {
        AddressResponse response = new AddressResponse();
        response.setId(address.getId());
        response.setName(address.getName());
        response.setStreet(address.getStreet());
        response.setCity(address.getCity());
        response.setDistrict(address.getDistrict());
        response.setState(address.getState());
        response.setPincode(address.getPincode());
        response.setMobileNumber(address.getMobileNumber());
        response.setFormattedAddress(
        		address.getName() + ", " +   address.getStreet() + ", " + address.getCity() + ", " +
                address.getDistrict() + " - " + address.getPincode()
        );
        return response;
    }
}
