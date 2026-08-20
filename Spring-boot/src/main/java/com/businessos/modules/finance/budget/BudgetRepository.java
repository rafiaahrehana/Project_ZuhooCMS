package com.businessos.modules.finance.budget;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    Optional<Budget> findByIdAndCompanyId(Long id, Long companyId);

    List<Budget> findByCompanyIdAndFiscalYearOrderByCategoryAsc(Long companyId, int fiscalYear);

    Optional<Budget> findByCompanyIdAndCategoryIgnoreCaseAndFiscalYear(Long companyId, String category, int fiscalYear);

    boolean existsByCompanyIdAndCategoryIgnoreCaseAndFiscalYear(Long companyId, String category, int fiscalYear);

    /** Every distinct category a budget has ever been set for, across all fiscal years - used to
     * suggest matching category names when logging an expense, so free-text entry doesn't drift. */
    @Query("SELECT DISTINCT b.category FROM Budget b WHERE b.companyId = :companyId AND b.deleted = false ORDER BY b.category")
    List<String> findDistinctCategoriesByCompanyId(@Param("companyId") Long companyId);
}
