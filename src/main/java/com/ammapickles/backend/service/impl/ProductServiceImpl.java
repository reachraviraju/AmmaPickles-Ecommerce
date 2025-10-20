package com.ammapickles.backend.service.impl;

import com.ammapickles.backend.dto.ProductDTO;
import com.ammapickles.backend.entity.Category;
import com.ammapickles.backend.entity.Product;
import com.ammapickles.backend.exception.ResourceNotFoundException;
import com.ammapickles.backend.repository.CategoryRepository;
import com.ammapickles.backend.repository.ProductRepository;
import com.ammapickles.backend.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ModelMapper modelMapper;

    @Override
    public List<ProductDTO> getAllProducts() {
        return productRepository.findAll().stream()
                .map(product -> mapToDTO(product))
                .collect(Collectors.toList());
    }

    @Override
    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return mapToDTO(product);
    }

    @Override
    public ProductDTO addProduct(ProductDTO productDTO) {
        Category category = categoryRepository.findById(productDTO.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + productDTO.getCategoryId()));

        Product product = modelMapper.map(productDTO, Product.class);
        product.setCategory(category);
        Product saved = productRepository.save(product);
        return mapToDTO(saved);
    }
    
    @Override
    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
    	
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        
                                         // it will only map fields that should change
        
        existing.setName(productDTO.getName());
        existing.setDescription(productDTO.getDescription());
        existing.setPrice(productDTO.getPrice());
        existing.setQuantity(productDTO.getQuantity());
        existing.setSize(productDTO.getSize());

              // Handle category explicitly
        Category category = categoryRepository.findById(productDTO.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + productDTO.getCategoryId()));
        existing.setCategory(category);

        Product updated = productRepository.save(existing);
        return mapToDTO(updated);
    }


 
     

    @Override
     public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
     }

    
    
    @Override
    public List<ProductDTO> getProductsByCategory(Long categoryId) {
        List<Product> products = productRepository.findByCategoryId(categoryId);
        if (products.isEmpty()) {
            throw new ResourceNotFoundException("No products found for category id: " + categoryId);
        }
        return products.stream()
                       .map(this::mapToDTO)
                       .collect(Collectors.toList());
    }

    @Override
    public List<ProductDTO> searchProducts(String name) {
        return productRepository.findByNameContainingIgnoreCase(name).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList()); 
    }

    // Helper method for mapping
    private ProductDTO mapToDTO(Product product) {
        ProductDTO dto = modelMapper.map(product, ProductDTO.class);
        if (product.getCategory() != null) {
            dto.setCategoryId(product.getCategory().getId());
            dto.setCategoryName(product.getCategory().getName());
        }
        return dto;
    }
}
