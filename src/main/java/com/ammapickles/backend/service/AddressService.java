package com.ammapickles.backend.service;

import com.ammapickles.backend.dto.address.AddressRequest;
import com.ammapickles.backend.dto.address.AddressResponse;

import java.util.List;

public interface AddressService {

    List<AddressResponse> getAddressesByUser(Long userId);

    
    
    AddressResponse getAddressById(Long addressId, Long requestingUserId);
    
    AddressResponse updateAddress(Long addressId, AddressRequest request, Long requestingUserId);

    AddressResponse createAddress(Long userId, AddressRequest request);

   

    void deleteAddress(Long userId, Long addressId);
    
    
    
   

  

}