package com.ammapickles.backend.service.impl;

import com.ammapickles.backend.dto.AddressDTO;
import com.ammapickles.backend.entity.Address;
import com.ammapickles.backend.entity.User;
import com.ammapickles.backend.repository.AddressRepository;
import com.ammapickles.backend.repository.UserRepository;
import com.ammapickles.backend.service.AddressService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AddressServiceImpl implements AddressService {

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public List<AddressDTO> getAddressesByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return addressRepository.findAll()
                .stream()
                .filter(address -> user.equals(address.getUser()))
                .map(address -> modelMapper.map(address, AddressDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public AddressDTO addAddress(Long userId, AddressDTO addressDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Address address = modelMapper.map(addressDTO, Address.class);

        // Link address to user
        address.setUser(user); 

        Address savedAddress = addressRepository.save(address);
        return modelMapper.map(savedAddress, AddressDTO.class);
    }

    @Override
    public AddressDTO updateAddress(Long addressId, AddressDTO addressDTO) {
    	
        Address existingAddress = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        // Update fields
        existingAddress.setStreet(addressDTO.getStreet());
        existingAddress.setCity(addressDTO.getCity());
        existingAddress.setDistrict(addressDTO.getDistrict());
        existingAddress.setState(addressDTO.getState());
        existingAddress.setPincode(addressDTO.getPincode());
        existingAddress.setDistanceInKm(addressDTO.getDistanceInKm());

        Address updatedAddress = addressRepository.save(existingAddress);
        return modelMapper.map(updatedAddress, AddressDTO.class);
    }

    @Override
    public void deleteAddress(Long addressId) {
        Address existingAddress = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));
        addressRepository.delete(existingAddress);
    }
}
