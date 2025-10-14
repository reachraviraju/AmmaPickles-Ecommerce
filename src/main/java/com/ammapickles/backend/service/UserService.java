package com.ammapickles.backend.service;

import com.ammapickles.backend.dto.UserDTO;
import com.ammapickles.backend.entity.User;
import java.util.Optional;

public interface UserService {
	
	   UserDTO getUserById(Long id);
	    UserDTO registerUser(UserDTO userDTO);
	    UserDTO updateUser(Long id, UserDTO userDTO);
	    boolean existsByUsername(String username);
	    boolean existsByEmail(String email);
}
