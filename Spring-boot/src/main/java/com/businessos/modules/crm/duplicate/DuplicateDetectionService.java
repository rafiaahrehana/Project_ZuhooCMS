package com.businessos.modules.crm.duplicate;

import com.businessos.modules.crm.client.Client;
import com.businessos.modules.crm.client.ClientRepository;
import com.businessos.modules.crm.contact.ClientContact;
import com.businessos.modules.crm.contact.ClientContactRepository;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Nudge-not-block duplicate detection for Lead and Client creation, matching on
 * company name / email / phone / domain. No fuzzy/trigram matching exists elsewhere
 * in this codebase, so this uses normalized exact/LIKE matching (lowercase + trim on
 * company name and domain, exact on email/phone) rather than introducing pg_trgm.
 */
@Service
@RequiredArgsConstructor
public class DuplicateDetectionService {

    private final ClientRepository clientRepository;
    private final ClientContactRepository clientContactRepository;
    private final SecurityUtil securityUtil;

    @Transactional(readOnly = true)
    public Optional<DuplicateMatch> findPossibleDuplicateClient(String companyName, String email, String phone) {
        Long companyId = requireCompanyId();

        if (companyName != null && !companyName.isBlank()) {
            Optional<Client> byName = clientRepository
                    .findFirstByClientCompanyNameIgnoreCaseAndCompanyIdAndDeletedFalse(companyName.trim(), companyId);
            if (byName.isPresent()) {
                return byName.map(c -> new DuplicateMatch(c.getId(), c.getClientCompanyName(), "company name"));
            }
        }

        if (email != null && !email.isBlank()) {
            Optional<ClientContact> byEmail = clientContactRepository
                    .findFirstByEmailIgnoreCaseAndCompanyIdAndDeletedFalse(email.trim(), companyId);
            if (byEmail.isPresent()) {
                Client c = byEmail.get().getClient();
                return Optional.of(new DuplicateMatch(c.getId(), c.getClientCompanyName(), "email"));
            }

            String domain = extractDomain(email);
            if (domain != null) {
                List<Client> byDomain = clientRepository.findByWebsiteContainingDomain(companyId, domain);
                if (!byDomain.isEmpty()) {
                    Client c = byDomain.get(0);
                    return Optional.of(new DuplicateMatch(c.getId(), c.getClientCompanyName(), "domain"));
                }
            }
        }

        if (phone != null && !phone.isBlank()) {
            Optional<ClientContact> byPhone = clientContactRepository
                    .findFirstByPhoneAndCompanyIdAndDeletedFalse(phone.trim(), companyId);
            if (byPhone.isPresent()) {
                Client c = byPhone.get().getClient();
                return Optional.of(new DuplicateMatch(c.getId(), c.getClientCompanyName(), "phone"));
            }
        }

        return Optional.empty();
    }

    private String extractDomain(String email) {
        int at = email.indexOf('@');
        if (at < 0 || at == email.length() - 1) return null;
        return email.substring(at + 1).trim().toLowerCase();
    }

    private Long requireCompanyId() {
        Long companyId = securityUtil.getCurrentCompanyId();
        if (companyId == null) {
            throw new BadRequestException("No company context found");
        }
        return companyId;
    }
}
