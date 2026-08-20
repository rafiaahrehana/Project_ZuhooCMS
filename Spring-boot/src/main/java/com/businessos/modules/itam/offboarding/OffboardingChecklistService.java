package com.businessos.modules.itam.offboarding;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OffboardingChecklistService {

    OffboardingChecklistResponse create(OffboardingChecklistRequest request);

    OffboardingChecklistResponse getById(Long id);

    OffboardingChecklistResponse getByEmployee(Long employeeId);

    Page<OffboardingChecklistResponse> getAll(Pageable pageable);

    List<OffboardingChecklistResponse> getPendingChecklists();

    void markHardwareCollected(Long id, String notes);

    void markLicensesRevoked(Long id, String notes);

    void markAccessRevoked(Long id, String notes);

    void markDataHandedOver(Long id, String notes);

    void markExitInterviewCompleted(Long id, String notes);

    OffboardingChecklistResponse delete(Long id);
}
