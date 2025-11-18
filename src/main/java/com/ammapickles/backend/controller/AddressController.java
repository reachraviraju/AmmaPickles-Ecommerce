package com.ammapickles.backend.controller;

import com.ammapickles.backend.dto.AddressDTO;
import com.ammapickles.backend.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    // Get all addresses for a user
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AddressDTO>> getAddressesByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(addressService.getAddressesByUser(userId));
   
    }

    // Get specific address by ID
    @GetMapping("/{addressId}")
    public ResponseEntity<AddressDTO> getAddressById(@PathVariable Long addressId) {
        return ResponseEntity.ok(addressService.getAddressById(addressId));
    }

    //Create new address for user
    @PostMapping("/user/{userId}")
    public ResponseEntity<AddressDTO> createAddress(
            @PathVariable Long userId,
            @RequestBody AddressDTO addressDTO) {
        return ResponseEntity.ok(addressService.createAddress(userId, addressDTO));
    }

    //  Update existing address
    @PutMapping("/{addressId}")
    public ResponseEntity<AddressDTO> updateAddress( @PathVariable Long addressId,  @RequestBody AddressDTO addressDTO) {
        return ResponseEntity.ok(addressService.updateAddress(addressId, addressDTO));
    }

    //  Delete specific address for a user
    @DeleteMapping("/user/{userId}/{addressId}")
    public ResponseEntity<Void> deleteAddress(  @PathVariable Long userId,   @PathVariable Long addressId) {
    	
        addressService.deleteAddress(userId, addressId);
        return ResponseEntity.noContent().build(); 
        
 
        
        
        
        
        
        
       
        
        
        
        
        
        
        
   
       
    }
}
