package com.ammapickles.backend.service.impl;

import com.ammapickles.backend.dto.CategoryDTO;
import com.ammapickles.backend.entity.Category;
import com.ammapickles.backend.exception.ResourceNotFoundException;
import com.ammapickles.backend.repository.CategoryRepository;
import com.ammapickles.backend.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ModelMapper modelMapper;

    @Override
    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(category -> modelMapper.map(category, CategoryDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public CategoryDTO getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .map(category -> modelMapper.map(category, CategoryDTO.class))
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    @Override
    public CategoryDTO getCategoryByName(String name) {
        return categoryRepository.findByNameIgnoreCase(name.trim())
                .map(category -> modelMapper.map(category, CategoryDTO.class))
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with name: " + name));
    }

    @Override
    public CategoryDTO addCategory(CategoryDTO categoryDTO) {
        String name = categoryDTO.getName().trim();
        Optional<Category> existing = categoryRepository.findByNameIgnoreCase(name);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Category already exists: " + name);
        }
        Category saved = categoryRepository.save(new Category(null, name));
        return modelMapper.map(saved, CategoryDTO.class);
    }

    @Override
    public CategoryDTO updateCategory(Long id, CategoryDTO categoryDTO) {
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        existing.setName(categoryDTO.getName().trim());
        return modelMapper.map(categoryRepository.save(existing), CategoryDTO.class);
    }

    @Override
    public void deleteCategory(Long id) {
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        categoryRepository.delete(existing);
    }
}

