package com.businessos.modules.finance.journalentry;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {

    Optional<JournalEntry> findByIdAndCompanyId(Long id, Long companyId);

    Page<JournalEntry> findByCompanyId(Long companyId, Pageable pageable);

    /**
     * Used by JournalEntryServiceImpl.generateJENumber - MAX-based (not COUNT) and
     * scoped per company, same reasoning as ExpenseRepository's equivalent query.
     */
    @Query("SELECT MAX(j.journalEntryNumber) FROM JournalEntry j WHERE j.companyId = :companyId AND j.journalEntryNumber LIKE CONCAT(:prefix, '%')")
    Optional<String> findMaxJENumberByCompanyAndPrefix(@Param("companyId") Long companyId, @Param("prefix") String prefix);
}
