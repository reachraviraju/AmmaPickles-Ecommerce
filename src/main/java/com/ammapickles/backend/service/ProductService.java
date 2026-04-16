package com.ammapickles.backend.service;

import com.ammapickles.backend.dto.product.ProductGroupResponse;
import com.ammapickles.backend.dto.product.ProductRequest;
import com.ammapickles.backend.dto.product.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {
	
	
	List<ProductGroupResponse> getAllProductsGrouped();
	List<ProductGroupResponse> getProductsGroupedByCategory(Long categoryId);
	List<ProductGroupResponse> searchProductsGrouped(String name);
    ProductGroupResponse getProductGroupByVariantId(Long variantId);

    //  READ
    
    
     // Pageable carries: page number, page size, sort direction
    Page<ProductResponse> getAllProducts(Pageable pageable);

    // Single product
    ProductResponse getProductById(Long id);
    
    

    //  FILTER 

   
    Page<ProductResponse> getProductsByCategory(Long categoryId, Pageable pageable);

    // List is acceptable for search results
    List<ProductResponse> searchProducts(String name);
    
    

    // ADMIN OPERATIONS

    //  takes ProductRequest (input) returns ProductResponse (output)
    // These are different objects —> request has categoryId, response has categoryName
    ProductResponse addProduct(ProductRequest request);

    //   takes ProductRequest, returns updated ProductResponse
    ProductResponse updateProduct(Long id, ProductRequest request);

    // Delete returns void
    void deleteProduct(Long id);
}
