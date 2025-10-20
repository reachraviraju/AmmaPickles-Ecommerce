package com.ammapickles.backend.service.impl;

import com.ammapickles.backend.dto.ResetPasswordDTO;
import com.ammapickles.backend.dto.UserDTO;
import com.ammapickles.backend.entity.Role;
import com.ammapickles.backend.entity.User;
import com.ammapickles.backend.exception.BadRequestException;
import com.ammapickles.backend.exception.ResourceNotFoundException;
import com.ammapickles.backend.repository.RoleRepository;
import com.ammapickles.backend.repository.UserRepository;
import com.ammapickles.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

    @Override
    public UserDTO registerUser(UserDTO userDTO) {
        if (userRepository.existsByUsername(userDTO.getUsername())) {
            throw new BadRequestException("Username already exists");
        }
        if (userRepository.existsByEmail(userDTO.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        User user = modelMapper.map(userDTO, User.class);
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));

        // Assign default CUSTOMER role
        Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        Set<Role> roles = new HashSet<>();
        roles.add(customerRole);
        user.setRoles(roles);

        User savedUser = userRepository.save(user);
        return modelMapper.map(savedUser, UserDTO.class);
        
        
        
    } 
    
    
    public void resetPassword(String username, ResetPasswordDTO resetPasswordDTO)
    {
        User user = userRepository.findByUsername(username).orElseThrow(() -> 
                                         new ResourceNotFoundException("user not found"+username));
      
          user.setPassword(passwordEncoder.encode(resetPasswordDTO.getNewPassword()));
          userRepository.save(user);
    }
    
    

    @Override
    public UserDTO login(String username, String password) {
        User user = userRepository.findByUsername(username)
        		.orElseThrow(() -> new ResourceNotFoundException("User not found with username : " + username));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadRequestException("Invalid password");
        }

        return modelMapper.map(user, UserDTO.class);
    }

    @Override
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return modelMapper.map(user, UserDTO.class);
    }
    
    @Override
    public UserDTO updateUser(Long id, UserDTO userDTO) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setUsername(userDTO.getUsername());
        user.setEmail(userDTO.getEmail());
        user.setAddress(userDTO.getAddress());
        user.setPhoneNumber(userDTO.getPhoneNumber());

        if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        }

        User updatedUser = userRepository.save(user);
        return modelMapper.map(updatedUser, UserDTO.class);
    }
}
