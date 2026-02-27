package com.ammapickles.backend.repository;

import com.ammapickles.backend.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    
    List<Product> findByNameContainingIgnoreCase(String name);

   
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

   
    boolean existsByCategoryId(Long categoryId);
}