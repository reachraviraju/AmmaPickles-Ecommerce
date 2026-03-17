package com.ammapickles.backend.repository;

import com.ammapickles.backend.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    // Get all addresses for a user
    List<Address> findByUserId(Long userId);

    // Get address AND verify it belongs to user it prevents one user accessing another's address
    Optional<Address> findByIdAndUserId(Long addressId, Long userId);
    
    long countByUserId(Long userId);
}