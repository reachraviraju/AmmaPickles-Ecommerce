package com.ammapickles.backend.service;

import java.util.List;

import com.ammapickles.backend.dto.AddressDTO;

public interface AddressService {
	
	 List<AddressDTO> getAddressesByUser(Long userId);
	    AddressDTO addAddress(Long userId, AddressDTO addressDTO);
	    AddressDTO updateAddress(Long addressId, AddressDTO addressDTO);
	    void deleteAddress(Long addressId);

}
