package com.businessos.modules.crm.contact;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClientContactRepository extends JpaRepository<ClientContact, Long> {

    Optional<ClientContact> findByIdAndCompanyId(Long id, Long companyId);

    List<ClientContact> findByClientIdAndCompanyIdOrderByPrimaryContactDescCreatedAtDesc(Long clientId, Long companyId);

    // Cross-client global list - the Contacts page isn't scoped to one Client.
    // Ordering comes from the Pageable's Sort (set by the controller), not the method name.
    Page<ClientContact> findByCompanyId(Long companyId, Pageable pageable);

    @Query("SELECT c FROM ClientContact c WHERE c.company.id = :companyId AND " +
           "(LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' OR " +
           "LOWER(c.client.clientCompanyName) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!') AND " +
           "c.deleted = false")
    Page<ClientContact> searchContacts(@Param("companyId") Long companyId, @Param("keyword") String keyword, Pageable pageable);

    boolean existsByEmailAndClientIdAndCompanyIdAndDeletedFalse(String email, Long clientId, Long companyId);

    /**
     * The client's primary contact - the address a portal invite is sent to.
     * Ordered by id so a client with more than one flagged primary (possible,
     * since nothing enforces uniqueness) resolves deterministically instead of
     * picking a different row each call.
     */
    Optional<ClientContact> findFirstByClientIdAndCompanyIdAndPrimaryContactTrueAndDeletedFalseOrderByIdAsc(Long clientId, Long companyId);

    Optional<ClientContact> findFirstByEmailIgnoreCaseAndCompanyIdAndDeletedFalse(String email, Long companyId);

    Optional<ClientContact> findFirstByPhoneAndCompanyIdAndDeletedFalse(String phone, Long companyId);

    @Modifying
    @Query("UPDATE ClientContact c SET c.primaryContact = false WHERE c.client.id = :clientId AND c.company.id = :companyId")
    void clearPrimaryContact(@Param("clientId") Long clientId, @Param("companyId") Long companyId);
}
