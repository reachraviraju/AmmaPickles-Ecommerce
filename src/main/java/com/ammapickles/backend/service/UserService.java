package com.ammapickles.backend.service;

import com.ammapickles.backend.dto.ResetPasswordDTO;
import com.ammapickles.backend.dto.UserDTO;

public interface UserService {



	    UserDTO registerUser(UserDTO userDTO);
	    
	    UserDTO login(String email, String password);  
	    
	    UserDTO getUserById(Long id);
	    
	    UserDTO getUserByEmail(String email);
	    
	    UserDTO updateUser(Long id, UserDTO userDTO);
	    void resetPassword(String email, ResetPasswordDTO resetPasswordDTO); 

	      
   

    
   
}
