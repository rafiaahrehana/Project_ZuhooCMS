package com.businessos.modules.support.category;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupportCategoryRepository extends JpaRepository<SupportCategory, Long> {

    Optional<SupportCategory> findByCategoryName(String name);

    Page<SupportCategory> findByActive(boolean active, Pageable pageable);
    
    java.util.List<SupportCategory> findByActiveTrue();
}
