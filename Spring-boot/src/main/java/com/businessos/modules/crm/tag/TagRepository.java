package com.businessos.modules.crm.tag;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

    List<Tag> findByCompanyIdOrderByNameAsc(Long companyId);

    Optional<Tag> findByIdAndCompanyId(Long id, Long companyId);

    boolean existsByNameIgnoreCaseAndCompanyId(String name, Long companyId);

    List<Tag> findByIdInAndCompanyId(List<Long> ids, Long companyId);
}
