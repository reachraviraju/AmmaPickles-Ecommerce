package com.ammapickles.backend.service.impl;

import com.ammapickles.backend.dto.AddressDTO;
import com.ammapickles.backend.entity.Address;
import com.ammapickles.backend.entity.User;
import com.ammapickles.backend.repository.AddressRepository;
import com.ammapickles.backend.repository.UserRepository;
import com.ammapickles.backend.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Override
    public List<AddressDTO> getAddressesByUser(Long userId) {
        List<Address> addresses = addressRepository.findByUserId(userId);
        return addresses.stream()
                .map(address -> modelMapper.map(address, AddressDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public AddressDTO addAddress(Long userId, AddressDTO addressDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Address address = modelMapper.map(addressDTO, Address.class);
        address.setUser(user);
        Address saved = addressRepository.save(address);
        return modelMapper.map(saved, AddressDTO.class);
    }

    @Override
    public AddressDTO updateAddress(Long addressId, AddressDTO addressDTO) {
        Address existing = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));
        modelMapper.map(addressDTO, existing);
        Address updated = addressRepository.save(existing);
        return modelMapper.map(updated, AddressDTO.class);
    }

    @Override
    public void deleteAddress(Long addressId) {
        if (!addressRepository.existsById(addressId)) {
            throw new RuntimeException("Address not found");
        }
        addressRepository.deleteById(addressId);
    }
}
