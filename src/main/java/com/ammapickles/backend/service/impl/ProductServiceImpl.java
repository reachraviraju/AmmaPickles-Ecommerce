package com.ammapickles.backend.service.impl;

import com.ammapickles.backend.dto.product.ProductGroupResponse;
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

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    

    // READ METHODS

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        log.info("Fetching all products - page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());
        return productRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Cacheable(value = "product", key = "#id")
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

    // GROUPED METHODS — for web frontend

    @Override
    @Cacheable("productsGrouped")
    @Transactional(readOnly = true)
    public List<ProductGroupResponse> getAllProductsGrouped() {
        log.info("Fetching all products grouped by name");
        List<Product> products = productRepository.findAllByOrderByNameAscSizeAsc();
        return groupProducts(products);
    }

    @Override
    @Cacheable(value = "productsByCategory", key = "#categoryId")
    @Transactional(readOnly = true)
    public List<ProductGroupResponse> getProductsGroupedByCategory(Long categoryId) {
        log.info("Fetching products grouped by name for category: {}", categoryId);
        categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
        List<Product> products = productRepository.findByCategoryIdOrderByNameAscSizeAsc(categoryId);
        return groupProducts(products);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductGroupResponse> searchProductsGrouped(String keyword) {
        log.info("Searching grouped products with keyword: {}", keyword);
        List<Product> products = productRepository.findByNameContainingIgnoreCaseOrderByNameAscSizeAsc(keyword);
        return groupProducts(products);
    }

    // WRITE METHODS

    @Override
    @CacheEvict(value = {"productsGrouped", "productsByCategory", "product"}, allEntries = true)
    @Transactional
    public ProductResponse addProduct(ProductRequest request) {
        log.info("Adding new product: {}", request.getName());
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found with id: " + request.getCategoryId()));
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
    @CacheEvict(value = {"productsGrouped", "productsByCategory", "product"}, allEntries = true)
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        log.info("Updating product with id: {}", id);
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found with id: " + request.getCategoryId()));
        existing.setName(request.getName());
        existing.setDescription(request.getDescription());
        existing.setPrice(request.getPrice());
        existing.setQuantity(request.getQuantity());
        existing.setSize(request.getSize());
        existing.setCategory(category);
        productRepository.save(existing); 
        log.info("Product updated successfully: {}", id);
        return mapToResponse(existing);
    }
    
    @Override
    @Transactional(readOnly = true)
    public ProductGroupResponse getProductGroupByVariantId(Long variantId) {
        Product product = productRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + variantId));
        List<Product> variants = productRepository.findByNameAndCategoryIdOrderBySizeAsc(
                product.getName(), product.getCategory().getId());
        return mapToGroupResponse(variants);
    }


    @Override
    @CacheEvict(value = {"productsGrouped", "productsByCategory", "product"}, allEntries = true)
    @Transactional
    public void deleteProduct(Long id) {
        log.info("Deleting product with id: {}", id);
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
        log.info("Product deleted successfully: {}", id);
    }

    // PRIVATE HELPERS

    private ProductResponse mapToResponse(Product product) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setSize(product.getSize());
        response.setQuantity(product.getQuantity());
        response.setInStock(product.isInStock());
        if (product.getSize() != null) {
            response.setSizeLabel(product.getSize().getLabel());
        }
        if (product.getCategory() != null) {
            response.setCategoryId(product.getCategory().getId());
            response.setCategoryName(product.getCategory().getName());
        }
        return response;
    }

    private ProductGroupResponse mapToGroupResponse(List<Product> variants) {
        Product first = variants.get(0);

        ProductGroupResponse group = new ProductGroupResponse();
        group.setName(first.getName());
        group.setDescription(first.getDescription());
        group.setCategoryName(first.getCategory().getName());
        group.setCategoryId(first.getCategory().getId());
        
        
        
        List<ProductGroupResponse.ProductVariant> variantList = variants.stream()
                .map(p -> {
                    ProductGroupResponse.ProductVariant v = new ProductGroupResponse.ProductVariant();
                    v.setId(p.getId());
                    v.setSize(p.getSize());
                    v.setSizeLabel(p.getSize() != null ? p.getSize().getLabel() : "Standard");
                    v.setPrice(p.getPrice());
                    v.setInStock(p.isInStock());
                    v.setQuantity(p.getQuantity());
                    return v;
                })
                .collect(Collectors.collectingAndThen(
                    Collectors.toMap(
                        v -> v.getSize() != null ? v.getSize().name() : "STANDARD",
                        v -> v,
                        (existing, duplicate) -> existing
                    ),
                    map -> new java.util.ArrayList<>(map.values())
                ));
       

        group.setVariants(variantList);
        return group;
    }

    private List<ProductGroupResponse> groupProducts(List<Product> products) {
        return products.stream()
                .collect(Collectors.groupingBy(
                        Product::getName,
                        LinkedHashMap::new,
                        Collectors.toList()))
                .values()
                .stream()
                .map(this::mapToGroupResponse)
                .toList();
    }
}
