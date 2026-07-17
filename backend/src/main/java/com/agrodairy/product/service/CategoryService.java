package com.agrodairy.product.service;

import com.agrodairy.product.dto.CategoryResponse;
import com.agrodairy.product.dto.CreateCategoryRequest;
import com.agrodairy.product.entity.ProductCategory;
import com.agrodairy.product.repository.ProductCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final ProductCategoryRepository categoryRepository;

    public CategoryService(ProductCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryResponse> list() {
        return categoryRepository.findAll().stream().map(CategoryResponse::from).toList();
    }

    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        ProductCategory category = ProductCategory.builder()
                .name(request.name())
                .kind(request.kind())
                .build();
        categoryRepository.save(category);
        return CategoryResponse.from(category);
    }
}
