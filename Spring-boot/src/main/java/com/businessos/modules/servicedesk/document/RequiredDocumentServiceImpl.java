package com.businessos.modules.servicedesk.document;

import com.businessos.modules.servicedesk.companyservice.CompanyService;
import com.businessos.modules.servicedesk.companyservice.CompanyServiceRepository;
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
public class RequiredDocumentServiceImpl implements RequiredDocumentService {

    private final RequiredDocumentRepository requiredDocumentRepository;
    private final CompanyServiceRepository companyServiceRepository;
    private final SecurityUtil securityUtil;

    @Override
    @Transactional
    public RequiredDocumentResponse create(Long serviceId, RequiredDocumentRequest request) {
        Long companyId = requireCompanyId();

        CompanyService service = companyServiceRepository.findByIdAndCompanyId(serviceId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));

        RequiredDocument doc = RequiredDocument.builder()
                .company(service.getCompany())
                .service(service)
                .docName(request.getDocName())
                .description(request.getDescription())
                .mandatory(request.isMandatory())
                .maxAgeDays(request.getMaxAgeDays())
                .allowedFormats(request.getAllowedFormats())
                .sortOrder(request.getSortOrder())
                .build();

        requiredDocumentRepository.save(doc);
        return RequiredDocumentMapper.toResponse(doc);
    }

    @Override
    @Transactional
    public RequiredDocumentResponse update(Long id, RequiredDocumentRequest request) {
        Long companyId = requireCompanyId();

        RequiredDocument doc = requiredDocumentRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Required document not found"));

        if (request.getDocName() != null) doc.setDocName(request.getDocName());
        if (request.getDescription() != null) doc.setDescription(request.getDescription());
        doc.setMandatory(request.isMandatory());
        if (request.getMaxAgeDays() != null) doc.setMaxAgeDays(request.getMaxAgeDays());
        if (request.getAllowedFormats() != null) doc.setAllowedFormats(request.getAllowedFormats());
        doc.setSortOrder(request.getSortOrder());

        requiredDocumentRepository.save(doc);
        return RequiredDocumentMapper.toResponse(doc);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Long companyId = requireCompanyId();
        RequiredDocument doc = requiredDocumentRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Required document not found"));

        doc.softDelete();
        requiredDocumentRepository.save(doc);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RequiredDocumentResponse> listByService(Long serviceId) {
        Long companyId = requireCompanyId();
        return requiredDocumentRepository.findByCompanyIdAndServiceIdOrderBySortOrderAsc(companyId, serviceId).stream()
                .filter(d -> !d.isDeleted())
                .map(RequiredDocumentMapper::toResponse)
                .collect(Collectors.toList());
    }

    private Long requireCompanyId() {
        Long id = securityUtil.getCurrentCompanyId();
        if (id == null) throw new BadRequestException("No company context");
        return id;
    }
}
