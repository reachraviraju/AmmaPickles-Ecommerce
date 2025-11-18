package com.ammapickles.backend.service;

import com.ammapickles.backend.dto.ProductDTO;
import java.util.List;

public interface ProductService {

    // CRUD
    List<ProductDTO> getAllProducts();
    ProductDTO getProductById(Long id);
    ProductDTO addProduct(ProductDTO productDTO);
    ProductDTO updateProduct(Long id, ProductDTO productDTO);
    void deleteProduct(Long id);

    // Filter
    List<ProductDTO> getProductsByCategory(Long categoryId);
    List<ProductDTO> searchProducts(String name);
}
