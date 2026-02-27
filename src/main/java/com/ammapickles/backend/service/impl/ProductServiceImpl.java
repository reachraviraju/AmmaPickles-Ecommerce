package com.ammapickles.backend.service.impl;

import com.ammapickles.backend.dto.product.ProductRequest;
import com.ammapickles.backend.dto.product.ProductResponse;
import com.ammapickles.backend.entity.Category;
import com.ammapickles.backend.entity.Product;
import com.ammapickles.backend.exception.ResourceNotFoundException;
import com.ammapickles.backend.repository.CategoryRepository;
import com.ammapickles.backend.repository.ProductRepository;
import com.ammapickles.backend.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
                                                           // Read methods should use readOnly=true, write methods use full @Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    

    

              // @Transactional(readOnly = true)
              // Tells DB: only reading, no writes
              // DB skips write locks -> faster performance (20-30% improvement on large datasets)
    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        log.info("Fetching all products - page: {}, size: {}", 
                pageable.getPageNumber(), pageable.getPageSize());

       
                  // Returns Page<Product> with pagination info —> total pages, total elements
        return productRepository.findAll(pageable)
                .map(this::mapToResponse);  // map each Product -> ProductResponse
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        log.info("Fetching product with id: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        return mapToResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsByCategory(Long categoryId, Pageable pageable) {
        log.info("Fetching products for category id: {}", categoryId);

        // Verify category exists first
        categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));

        return productRepository.findByCategoryId(categoryId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> searchProducts(String name) {
        log.info("Searching products with name containing: {}", name);

     
        return productRepository.findByNameContainingIgnoreCase(name)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

   

    @Override
    @Transactional  // Full transaction for write operations
    public ProductResponse addProduct(ProductRequest request) {
        log.info("Adding new product: {}", request.getName());

        // Step 1: Validate category exists
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found with id: " + request.getCategoryId()));

        // Step 2: manually build entity instead of ModelMapper
                                                                                   // ModelMapper might map 'id' field from request -> could overwrite existing product
                                                                                 // Manual mapping gives us full control over what gets saved
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .size(request.getSize())
                .quantity(request.getQuantity())
                .category(category)
                .build();

        Product saved = productRepository.save(product);
        log.info("Product saved successfully with id: {}", saved.getId());

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        log.info("Updating product with id: {}", id);

        // Step 1: Find existing product
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        // Step 2: Find new category if changed
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found with id: " + request.getCategoryId()));

        // Step 3: Update only the fields — NEVER change the id
        existing.setName(request.getName());
        existing.setDescription(request.getDescription());
        existing.setPrice(request.getPrice());
        existing.setQuantity(request.getQuantity());
        existing.setSize(request.getSize());
        existing.setCategory(category);

                                         // No need to call productRepository.save() explicitly 
                                         //  existing is a MANAGED entity inside @Transactional
                                         // Spring/Hibernate automatically detects changes and saves at end of transaction
                                             // This is called "Dirty Checking" — a key Hibernate concept!
        log.info("Product updated successfully: {}", id);

        return mapToResponse(existing);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        log.info("Deleting product with id: {}", id);

        // Verify exists first — throw proper error if not found
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }

        // deleteById() — ONE db call instead of findById() + delete() = TWO calls
        productRepository.deleteById(id);
        log.info("Product deleted successfully: {}", id);
    }

    // PRIVATE HELPER

    //  Manual mapping entity -> response (safe and explicit)
    // We control exactly what fields go into the response
    private ProductResponse mapToResponse(Product product) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setSize(product.getSize());
        response.setQuantity(product.getQuantity());
        response.setInStock(product.isInStock());  // uses our helper method from entity

        // Size label — human readable ("1 kg", "2 kg")
        if (product.getSize() != null) {
            response.setSizeLabel(product.getSize().getLabel());
        }

        // Category info
        if (product.getCategory() != null) {
            response.setCategoryId(product.getCategory().getId());
            response.setCategoryName(product.getCategory().getName());
        }

        return response;
    }
}