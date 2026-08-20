package com.businessos.modules.servicedesk.companyservice;

import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import com.businessos.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServicePrerequisiteServiceImpl implements ServicePrerequisiteService {

    private final ServicePrerequisiteRepository prerequisiteRepository;
    private final CompanyServiceRepository companyServiceRepository;
    private final SecurityUtil securityUtil;

    @Override
    @Transactional
    public ServicePrerequisiteResponse create(Long serviceId, ServicePrerequisiteRequest request) {
        Long companyId = requireCompanyId();

        CompanyService service = companyServiceRepository.findByIdAndCompanyId(serviceId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));

        if (request.getPrerequisiteServiceId().equals(serviceId)) {
            throw new BadRequestException("A service cannot be its own prerequisite");
        }

        CompanyService prerequisiteService = companyServiceRepository
                .findByIdAndCompanyId(request.getPrerequisiteServiceId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Prerequisite service not found"));

        ServicePrerequisite prerequisite = ServicePrerequisite.builder()
                .service(service)
                .prerequisiteService(prerequisiteService)
                .mandatory(request.isMandatory())
                .message(request.getMessage())
                .build();

        prerequisiteRepository.save(prerequisite);
        return ServicePrerequisiteMapper.toResponse(prerequisite);
    }

    @Override
    @Transactional
    public void delete(Long serviceId, Long id) {
        Long companyId = requireCompanyId();
        // Confirms the parent service is in this tenant before touching the row.
        companyServiceRepository.findByIdAndCompanyId(serviceId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));

        ServicePrerequisite prerequisite = prerequisiteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prerequisite not found"));
        if (!prerequisite.getService().getId().equals(serviceId)) {
            throw new ResourceNotFoundException("Prerequisite not found");
        }

        prerequisite.softDelete();
        prerequisiteRepository.save(prerequisite);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServicePrerequisiteResponse> listByService(Long serviceId) {
        return prerequisiteRepository.findByServiceIdOrderByIdAsc(serviceId).stream()
                .map(ServicePrerequisiteMapper::toResponse)
                .collect(Collectors.toList());
    }

    private Long requireCompanyId() {
        Long id = securityUtil.getCurrentCompanyId();
        if (id == null) throw new BadRequestException("No company context");
        return id;
    }
}
