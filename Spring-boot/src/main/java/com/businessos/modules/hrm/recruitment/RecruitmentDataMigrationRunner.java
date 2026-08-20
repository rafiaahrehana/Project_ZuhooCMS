package com.businessos.modules.hrm.recruitment;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * One-time data repair for the Candidate/JobApplication split and the
 * expanded offer sub-pipeline (ApplicationStatus.OFFERED -> OFFER_*). There's
 * no Flyway in this project - schema evolution that only needs new nullable
 * columns is handled by hibernate.ddl-auto=update, but fixing up EXISTING
 * rows needs code, so it happens here on boot.
 *
 * Safe to run on every startup: both steps are no-ops once there's nothing
 * left to fix, and step 2 is skipped entirely on a fresh database that never
 * had the old applicant_* columns in the first place.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class RecruitmentDataMigrationRunner implements ApplicationRunner {

    private final EntityManager entityManager;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        dropStaleStatusCheckConstraint();
        migrateOfferedStatus();
        backfillCandidates();
        relaxLegacyNotNullConstraints();
    }

    /**
     * Hibernate generates a CHECK constraint enumerating the enum's values at
     * the time the column was first created, and ddl-auto=update never
     * refreshes it for an existing column - a database that ever ran the old
     * 9-value ApplicationStatus still has a constraint that rejects every
     * status this refactor adds (SELECTED, the four OFFER_* values). Dropping
     * it is safe: enum validity is still enforced by Hibernate's own mapping,
     * this constraint was only ever a second line of defense.
     */
    private void dropStaleStatusCheckConstraint() {
        entityManager.createNativeQuery(
                "ALTER TABLE job_applications DROP CONSTRAINT IF EXISTS job_applications_status_check")
            .executeUpdate();
    }

    /**
     * applicant_name/applicant_email were NOT NULL on the old schema. Hibernate
     * never drops orphaned columns, and it no longer populates them either -
     * without this, every new application insert fails the database's own
     * constraint on a column the application doesn't know exists anymore.
     */
    private void relaxLegacyNotNullConstraints() {
        if (!legacyApplicantColumnsExist()) return;
        entityManager.createNativeQuery("ALTER TABLE job_applications ALTER COLUMN applicant_name DROP NOT NULL").executeUpdate();
        entityManager.createNativeQuery("ALTER TABLE job_applications ALTER COLUMN applicant_email DROP NOT NULL").executeUpdate();
    }

    private void migrateOfferedStatus() {
        int updated = entityManager.createNativeQuery(
                "UPDATE job_applications SET status = 'OFFER_SENT' WHERE status = 'OFFERED'")
            .executeUpdate();
        if (updated > 0) {
            log.info("Recruitment migration: remapped {} legacy OFFERED application(s) to OFFER_SENT", updated);
        }
    }

    @SuppressWarnings("unchecked")
    private void backfillCandidates() {
        if (!legacyApplicantColumnsExist()) {
            return; // fresh database - JobApplication never had these columns
        }

        List<Tuple> rows = entityManager.createNativeQuery(
                "SELECT id, company_id, applicant_name, applicant_email, applicant_phone, "
                    + "resume_url, linked_in_url, portfolio_url FROM job_applications WHERE candidate_id IS NULL",
                Tuple.class).getResultList();
        if (rows.isEmpty()) return;

        int created = 0, linked = 0;
        for (Tuple row : rows) {
            Long applicationId = ((Number) row.get("id")).longValue();
            Long companyId = ((Number) row.get("company_id")).longValue();
            String email = (String) row.get("applicant_email");
            if (email == null || email.isBlank()) continue; // was already required NOT NULL, but guard anyway
            String normalizedEmail = email.toLowerCase().trim();

            Long candidateId = findExistingCandidateId(companyId, normalizedEmail);
            if (candidateId == null) {
                candidateId = insertCandidate(companyId, (String) row.get("applicant_name"), normalizedEmail,
                    (String) row.get("applicant_phone"), (String) row.get("resume_url"),
                    (String) row.get("linked_in_url"), (String) row.get("portfolio_url"));
                created++;
            } else {
                linked++;
            }
            entityManager.createNativeQuery("UPDATE job_applications SET candidate_id = :cid WHERE id = :aid")
                .setParameter("cid", candidateId)
                .setParameter("aid", applicationId)
                .executeUpdate();
        }
        log.info("Recruitment migration: backfilled candidate_id for {} application(s) "
            + "({} new candidate(s) created, {} linked to an existing candidate)", rows.size(), created, linked);
    }

    private Long findExistingCandidateId(Long companyId, String normalizedEmail) {
        List<Number> ids = entityManager.createNativeQuery(
                "SELECT id FROM recruitment_candidates WHERE company_id = :companyId AND lower(email) = :email")
            .setParameter("companyId", companyId)
            .setParameter("email", normalizedEmail)
            .getResultList();
        return ids.isEmpty() ? null : ids.get(0).longValue();
    }

    private Long insertCandidate(Long companyId, String name, String email, String phone,
                                  String resumeUrl, String linkedInUrl, String portfolioUrl) {
        Number id = (Number) entityManager.createNativeQuery(
                "INSERT INTO recruitment_candidates "
                    + "(company_id, name, email, phone, resume_url, linked_in_url, portfolio_url, deleted, created_at, updated_at) "
                    + "VALUES (:companyId, :name, :email, :phone, :resumeUrl, :linkedInUrl, :portfolioUrl, false, now(), now()) "
                    + "RETURNING id")
            .setParameter("companyId", companyId)
            .setParameter("name", name != null ? name : email)
            .setParameter("email", email)
            .setParameter("phone", phone)
            .setParameter("resumeUrl", resumeUrl)
            .setParameter("linkedInUrl", linkedInUrl)
            .setParameter("portfolioUrl", portfolioUrl)
            .getSingleResult();
        return id.longValue();
    }

    private boolean legacyApplicantColumnsExist() {
        Number count = (Number) entityManager.createNativeQuery(
                "SELECT count(*) FROM information_schema.columns "
                    + "WHERE table_name = 'job_applications' AND column_name = 'applicant_email'")
            .getSingleResult();
        return count.longValue() > 0;
    }
}
