package com.agrodairy.product.service;

import com.agrodairy.common.exception.NotFoundException;
import com.agrodairy.product.dto.CreateProductRequest;
import com.agrodairy.product.dto.ProductResponse;
import com.agrodairy.product.dto.UpdateProductRequest;
import com.agrodairy.product.entity.CategoryKind;
import com.agrodairy.product.entity.Product;
import com.agrodairy.product.entity.ProductCategory;
import com.agrodairy.product.repository.ProductCategoryRepository;
import com.agrodairy.product.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, ProductCategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public Page<ProductResponse> list(UUID categoryId, CategoryKind kind, Boolean active, String search, Pageable pageable) {
        Specification<Product> spec = (root, query, cb) -> cb.conjunction();
        if (categoryId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId));
        }
        if (kind != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category").get("kind"), kind));
        }
        if (active != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("active"), active));
        }
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern));
        }
        return productRepository.findAll(spec, pageable).map(ProductResponse::from);
    }

    public ProductResponse get(UUID id) {
        return ProductResponse.from(findProductOrThrow(id));
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        ProductCategory category = findCategoryOrThrow(request.categoryId());
        Product product = Product.builder()
                .category(category)
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .unit(request.unit())
                .shelfLifeDays(request.shelfLifeDays())
                .imageUrl(request.imageUrl())
                .active(true)
                .build();
        productRepository.saveAndFlush(product);
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse update(UUID id, UpdateProductRequest request) {
        Product product = findProductOrThrow(id);
        if (request.categoryId() != null) {
            product.setCategory(findCategoryOrThrow(request.categoryId()));
        }
        if (request.name() != null) {
            product.setName(request.name());
        }
        if (request.description() != null) {
            product.setDescription(request.description());
        }
        if (request.price() != null) {
            product.setPrice(request.price());
        }
        if (request.unit() != null) {
            product.setUnit(request.unit());
        }
        if (request.shelfLifeDays() != null) {
            product.setShelfLifeDays(request.shelfLifeDays());
        }
        if (request.imageUrl() != null) {
            product.setImageUrl(request.imageUrl());
        }
        productRepository.saveAndFlush(product);
        return ProductResponse.from(product);
    }

    @Transactional
    public void deactivate(UUID id) {
        Product product = findProductOrThrow(id);
        product.setActive(false);
        productRepository.save(product);
    }

    private Product findProductOrThrow(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found"));
    }

    private ProductCategory findCategoryOrThrow(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found"));
    }
}
