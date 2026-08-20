package com.businessos.modules.hrm.recruitment.offerletter;

import com.businessos.enums.LetterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OfferLetterRepository extends JpaRepository<OfferLetter, Long> {

    Optional<OfferLetter> findByIdAndCompanyId(Long id, Long companyId);

    Page<OfferLetter> findByCompanyIdAndEmployeeId(
        Long companyId, Long employeeId, Pageable pageable);

    Page<OfferLetter> findByCompanyId(Long companyId, Pageable pageable);

    boolean existsByCompanyIdAndReferenceNumber(Long companyId, String referenceNumber);

    long countByCompanyIdAndLetterType(Long companyId, LetterType letterType);
}
