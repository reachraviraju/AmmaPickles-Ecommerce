package com.ammapickles.backend.service;

import com.ammapickles.backend.dto.AddressDTO;
import java.util.List;

public interface AddressService {
	
	 List<AddressDTO> getAddressesByUser(Long userId);
	 AddressDTO getAddressById(Long addressId);
	    AddressDTO createAddress(Long userId, AddressDTO addressDTO);
	    AddressDTO updateAddress(Long addressId, AddressDTO addressDTO);
	    void deleteAddress(Long addressId);
	
	
	
   
}
