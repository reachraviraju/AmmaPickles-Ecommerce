package com.ammapickles.backend.service;

import com.ammapickles.backend.dto.product.CategoryRequest;
import com.ammapickles.backend.dto.product.CategoryResponse;

import java.util.List;

public interface CategoryService {

    List<CategoryResponse> getAllCategories();

    CategoryResponse getCategoryById(Long id);

    CategoryResponse getCategoryByName(String name);

    CategoryResponse addCategory(CategoryRequest request);

    CategoryResponse updateCategory(Long id, CategoryRequest request);

    void deleteCategory(Long id);
}