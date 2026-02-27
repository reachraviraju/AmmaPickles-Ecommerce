package com.ammapickles.backend.service.impl;

import com.ammapickles.backend.dto.product.CategoryRequest;
import com.ammapickles.backend.dto.product.CategoryResponse;
import com.ammapickles.backend.entity.Category;
import com.ammapickles.backend.exception.ResourceNotFoundException;
import com.ammapickles.backend.repository.CategoryRepository;
import com.ammapickles.backend.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

  

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        log.info("Fetching all categories");
        return categoryRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        log.info("Fetching category with id: {}", id);
        return categoryRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryByName(String name) {
        log.info("Fetching category with name: {}", name);
        return categoryRepository.findByNameIgnoreCase(name.trim())
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with name: " + name));
    }

   

    @Transactional
    public CategoryResponse addCategory(CategoryRequest request) {
        String name = request.getName().trim();
        log.info("Adding new category: {}", name);

        //  Chained directly — no unnecessary Optional variable
        if (categoryRepository.findByNameIgnoreCase(name).isPresent()) {
            throw new IllegalArgumentException("Category already exists: " + name);
        }

                        //  Using @Builder instead of new Category(null, name)
                       // Builder is safer — no risk of wrong constructor argument order
        Category saved = categoryRepository.save(
                Category.builder().name(name).build()
        );

        log.info("Category saved with id: {}", saved.getId());
        return mapToResponse(saved);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        log.info("Updating category with id: {}", id);

        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        existing.setName(request.getName().trim());

        // Dirty checking — no need to call save() explicitly!
        // Hibernate detects the change and saves automatically at end of transaction
        log.info("Category updated successfully: {}", id);
        return mapToResponse(existing);
    }

    @Transactional
    public void deleteCategory(Long id) {
        log.info("Deleting category with id: {}", id);

        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found with id: " + id);
        }

        //  ONE db call instead of find + delete = TWO calls
        categoryRepository.deleteById(id);
        log.info("Category deleted successfully: {}", id);
    }

    // PRIVATE HELPER

    private CategoryResponse mapToResponse(Category category) {
        CategoryResponse response = new CategoryResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        return response;
    }
}