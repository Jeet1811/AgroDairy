package com.agrodairy.product.repository;

import com.agrodairy.product.entity.CategoryKind;
import com.agrodairy.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    @Query("select p from Product p where (:categoryId is null or p.category.id = :categoryId) "
            + "and (:kind is null or p.category.kind = :kind) "
            + "and (:active is null or p.active = :active) "
            + "and (:search is null or lower(p.name) like lower(concat('%', :search, '%')))")
    Page<Product> search(@Param("categoryId") UUID categoryId,
                          @Param("kind") CategoryKind kind,
                          @Param("active") Boolean active,
                          @Param("search") String search,
                          Pageable pageable);
}
