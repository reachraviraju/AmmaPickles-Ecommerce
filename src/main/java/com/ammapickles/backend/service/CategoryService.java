package com.ammapickles.backend.service;

import com.ammapickles.backend.dto.CategoryDTO;
import com.ammapickles.backend.entity.Category;
import java.util.List;

public interface CategoryService {
	
	List<CategoryDTO> getAllCategories();
    CategoryDTO getCategoryById(Long id);
    CategoryDTO addCategory(CategoryDTO categoryDTO);
    CategoryDTO updateCategory(Long id, CategoryDTO categoryDTO);
    void deleteCategory(Long id);
}
