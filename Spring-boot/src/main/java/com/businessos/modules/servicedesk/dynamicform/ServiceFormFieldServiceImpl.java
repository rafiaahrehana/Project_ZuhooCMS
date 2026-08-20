package com.businessos.modules.servicedesk.dynamicform;

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
public class ServiceFormFieldServiceImpl implements ServiceFormFieldService {

    private final ServiceFormFieldRepository formFieldRepository;
    private final CompanyServiceRepository companyServiceRepository;
    private final SecurityUtil securityUtil;

    @Override
    @Transactional
    public ServiceFormFieldResponse create(Long serviceId, ServiceFormFieldRequest request) {
        Long companyId = requireCompanyId();

        CompanyService service = companyServiceRepository.findByIdAndCompanyId(serviceId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));

        ServiceFormField field = ServiceFormField.builder()
                .company(service.getCompany())
                .service(service)
                .label(request.getLabel())
                .fieldType(request.getFieldType())
                .required(request.isRequired())
                .validationRules(request.getValidationRules())
                .sortOrder(request.getSortOrder())
                .build();

        formFieldRepository.save(field);
        return ServiceFormFieldMapper.toResponse(field);
    }

    @Override
    @Transactional
    public ServiceFormFieldResponse update(Long id, ServiceFormFieldRequest request) {
        Long companyId = requireCompanyId();

        ServiceFormField field = formFieldRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Form field not found"));

        if (request.getLabel() != null) field.setLabel(request.getLabel());
        if (request.getFieldType() != null) field.setFieldType(request.getFieldType());
        field.setRequired(request.isRequired());
        if (request.getValidationRules() != null) field.setValidationRules(request.getValidationRules());
        field.setSortOrder(request.getSortOrder());

        formFieldRepository.save(field);
        return ServiceFormFieldMapper.toResponse(field);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Long companyId = requireCompanyId();
        ServiceFormField field = formFieldRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Form field not found"));
        
        field.softDelete();
        formFieldRepository.save(field);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceFormFieldResponse> listByService(Long serviceId) {
        Long companyId = requireCompanyId();
        return formFieldRepository.findByCompanyIdAndServiceIdOrderBySortOrderAsc(companyId, serviceId).stream()
                .filter(f -> !f.isDeleted())
                .map(ServiceFormFieldMapper::toResponse)
                .collect(Collectors.toList());
    }

    private Long requireCompanyId() {
        Long id = securityUtil.getCurrentCompanyId();
        if (id == null) throw new BadRequestException("No company context");
        return id;
    }
}
