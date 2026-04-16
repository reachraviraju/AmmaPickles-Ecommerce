package com.ammapickles.backend.controller;

import com.ammapickles.backend.dto.common.ApiResponse;
import com.ammapickles.backend.dto.product.ProductGroupResponse;
import com.ammapickles.backend.dto.product.ProductRequest;
import com.ammapickles.backend.dto.product.ProductResponse;
import com.ammapickles.backend.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // GET /api/products?page=0&size=10&sort=price,asc
   
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort) {

        //  PageRequest.of() — builds Pageable from page, size, sort params
        Pageable pageable = buildPageable(page, size, sort);

        Page<ProductResponse> response = productService.getAllProducts(pageable);
        return ResponseEntity.ok(ApiResponse.success("Products fetched successfully", response));
    }

    private Pageable buildPageable(int page, int size, String[] sort) {
        Sort resolvedSort = Sort.by("id").ascending();

        if (sort != null && sort.length > 0 && sort[0] != null && !sort[0].isBlank()) {
            try {
                Sort.Direction direction = Sort.Direction.ASC;
                if (sort.length > 1 && sort[1] != null && !sort[1].isBlank()) {
                    direction = Sort.Direction.fromString(sort[1]);
                }
                resolvedSort = Sort.by(direction, sort[0]);
            } catch (IllegalArgumentException ignored) {
                resolvedSort = Sort.by("id").ascending();
            }
        }

        return PageRequest.of(page, size, resolvedSort);
    }

    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(
            @PathVariable Long id) {

        ProductResponse response = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success("Product fetched successfully", response));
    }

    
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getProductsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ProductResponse> response = productService.getProductsByCategory(categoryId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Products fetched successfully", response));
    }
    
    

    
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> searchProducts(
            @RequestParam String name) {

        List<ProductResponse> response = productService.searchProducts(name);
        return ResponseEntity.ok(ApiResponse.success("Search results", response));
    }
    
 
    @GetMapping("/grouped")
    public ResponseEntity<ApiResponse<List<ProductGroupResponse>>> getAllProductsGrouped() {
        List<ProductGroupResponse> response = productService.getAllProductsGrouped();
        return ResponseEntity.ok(ApiResponse.success("Grouped products fetched", response));
    }

    
    @GetMapping("/grouped/category/{categoryId}")
    public ResponseEntity<ApiResponse<List<ProductGroupResponse>>> getProductsGroupedByCategory(
            @PathVariable Long categoryId) {
        List<ProductGroupResponse> response = productService.getProductsGroupedByCategory(categoryId);
        return ResponseEntity.ok(ApiResponse.success("Grouped products by category fetched", response));
    }

 
    @GetMapping("/grouped/search")
    public ResponseEntity<ApiResponse<List<ProductGroupResponse>>> searchProductsGrouped(
            @RequestParam String keyword) {
        List<ProductGroupResponse> response = productService.searchProductsGrouped(keyword);
        return ResponseEntity.ok(ApiResponse.success("Search results", response));
    }
    
    
    
    

   
    
            
  // SecurityConfig + @PreAuthorize
    
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> addProduct(
            @Valid @RequestBody ProductRequest request) {

        ProductResponse response = productService.addProduct(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product added successfully", response));
    }

  
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {

        ProductResponse response = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", response));
    }

    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable Long id) {

        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully"));
    }
}
