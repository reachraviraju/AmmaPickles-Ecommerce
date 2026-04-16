package com.ammapickles.backend.repository;

import com.ammapickles.backend.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    
    List<Product> findByNameIgnoreCase(String name);
    List<Product> findByNameContainingIgnoreCase(String name);
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);
    boolean existsByCategoryId(Long categoryId);

   
    @Query("SELECT DISTINCT p.name FROM Product p")
    List<String> findDistinctProductNames();

    @Query("SELECT DISTINCT p.name FROM Product p WHERE p.category.id = :categoryId")
    List<String> findDistinctProductNamesByCategory(@Param("categoryId") Long categoryId);

    List<Product> findByNameOrderBySizeAsc(String name);

    List<Product> findByNameAndCategoryIdOrderBySizeAsc(String name, Long categoryId);

    List<Product> findAllByOrderByNameAscSizeAsc();

    List<Product> findByCategoryIdOrderByNameAscSizeAsc(Long categoryId);

    List<Product> findByNameContainingIgnoreCaseOrderByNameAscSizeAsc(String keyword);
}
