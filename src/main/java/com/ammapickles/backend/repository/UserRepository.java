package com.ammapickles.backend.repository;

import com.ammapickles.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
	
	
	       Optional<User> findByEmail(String email);
	 
	    // for phone number login
	    Optional<User> findByPhoneNumber(String phoneNumber);
	 
	    boolean existsByEmail(String email);


	
	
	

    
}