package com.businessos.modules.crm.client;

import com.businessos.enums.ClientStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByUserId(Long userId);

    Optional<Client> findByIdAndCompanyId(Long id, Long companyId);

    boolean existsByUserIdAndCompanyId(Long userId, Long companyId);

    Page<Client> findByCompanyId(Long companyId, Pageable pageable);

    Page<Client> findByCompanyIdAndStatus(Long companyId, ClientStatus status, Pageable pageable);

    List<Client> findByCompanyIdAndStatus(Long companyId, ClientStatus status);

    Page<Client> findByCompanyIdAndTagEntitiesId(Long companyId, Long tagId, Pageable pageable);

    long countByCompanyId(Long companyId);

    @Query("SELECT c FROM Client c LEFT JOIN c.user u WHERE c.company.id = :companyId AND " +
           "(LOWER(c.clientCompanyName) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' OR " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!') AND " +
           "c.deleted = false")
    Page<Client> searchClients(@Param("companyId") Long companyId, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT c.company.id FROM Client c WHERE c.user.id = :userId AND c.deleted = false")
    Optional<Long> findCompanyIdByUserId(Long userId);

    /** Portal user ids for a company. Clients without a portal login are skipped. */
    @Query("SELECT c.user.id FROM Client c WHERE c.company.id = :companyId AND c.user IS NOT NULL AND c.deleted = false")
    List<Long> findUserIdsByCompanyId(@Param("companyId") Long companyId);
    boolean existsByCompanyId(Long companyId);

    boolean existsByClientCompanyNameIgnoreCaseAndCompanyIdAndDeletedFalse(String clientCompanyName, Long companyId);

    Optional<Client> findFirstByClientCompanyNameIgnoreCaseAndCompanyIdAndDeletedFalse(String clientCompanyName, Long companyId);

    @Query("SELECT c FROM Client c WHERE c.company.id = :companyId AND c.deleted = false AND " +
           "LOWER(c.website) LIKE LOWER(CONCAT('%', :domain, '%')) ESCAPE '!'")
    List<Client> findByWebsiteContainingDomain(@Param("companyId") Long companyId, @Param("domain") String domain);
}
