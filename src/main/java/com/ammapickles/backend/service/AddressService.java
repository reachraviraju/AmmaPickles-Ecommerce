package com.ammapickles.backend.service;

import com.ammapickles.backend.dto.address.AddressRequest;
import com.ammapickles.backend.dto.address.AddressResponse;

import java.util.List;

public interface AddressService {

    List<AddressResponse> getAddressesByUser(Long userId);

    AddressResponse getAddressById(Long addressId);

    AddressResponse createAddress(Long userId, AddressRequest request);

    AddressResponse updateAddress(Long addressId, AddressRequest request);

    void deleteAddress(Long userId, Long addressId);
}