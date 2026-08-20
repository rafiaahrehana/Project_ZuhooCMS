package com.businessos.modules.finance.vendor;

import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VendorService {

    private final VendorRepository vendorRepository;
    private final VendorBillRepository billRepository;
    private final SecurityUtil securityUtil;
    private final AuthorizationService authorizationService;

    @Transactional
    public VendorDtos.VendorResponse create(VendorDtos.VendorRequest request) {
        authorizationService.checkPermission(PermissionCode.VENDOR_CREATE);
        Long companyId = requireCompanyId();
        if (vendorRepository.existsByCompanyIdAndNameIgnoreCase(companyId, request.getName().trim())) {
            throw new BadRequestException("A vendor with this name already exists");
        }
        Vendor vendor = Vendor.builder()
                .companyId(companyId)
                .name(request.getName().trim())
                .contactPerson(request.getContactPerson())
                .email(request.getEmail())
                .phone(request.getPhone())
                .taxId(request.getTaxId())
                .address(request.getAddress())
                .paymentTerms(request.getPaymentTerms())
                .notes(request.getNotes())
                .active(true)
                .build();
        vendor = vendorRepository.save(vendor);
        return VendorDtos.toResponse(vendor, BigDecimal.ZERO);
    }

    @Transactional
    public VendorDtos.VendorResponse update(Long id, VendorDtos.VendorRequest request) {
        authorizationService.checkPermission(PermissionCode.VENDOR_UPDATE);
        Vendor vendor = findInTenant(id);
        vendor.setName(request.getName().trim());
        vendor.setContactPerson(request.getContactPerson());
        vendor.setEmail(request.getEmail());
        vendor.setPhone(request.getPhone());
        vendor.setTaxId(request.getTaxId());
        vendor.setAddress(request.getAddress());
        vendor.setPaymentTerms(request.getPaymentTerms());
        vendor.setNotes(request.getNotes());
        vendor = vendorRepository.save(vendor);
        return VendorDtos.toResponse(vendor, outstandingFor(vendor));
    }

    @Transactional
    public VendorDtos.VendorResponse toggle(Long id) {
        authorizationService.checkPermission(PermissionCode.VENDOR_UPDATE);
        Vendor vendor = findInTenant(id);
        vendor.setActive(!vendor.isActive());
        vendor = vendorRepository.save(vendor);
        return VendorDtos.toResponse(vendor, outstandingFor(vendor));
    }

    @Transactional(readOnly = true)
    public Page<VendorDtos.VendorResponse> list(String search, Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.VENDOR_VIEW);
        Long companyId = requireCompanyId();
        Page<Vendor> page = (search != null && !search.isBlank())
                ? vendorRepository.findByCompanyIdAndNameContainingIgnoreCase(companyId, search.trim(), pageable)
                : vendorRepository.findByCompanyId(companyId, pageable);
        return page.map(v -> VendorDtos.toResponse(v, outstandingFor(v)));
    }

    @Transactional(readOnly = true)
    public List<VendorDtos.VendorResponse> listActive() {
        authorizationService.checkPermission(PermissionCode.VENDOR_VIEW);
        return vendorRepository.findByCompanyIdAndActiveTrueOrderByNameAsc(requireCompanyId())
                .stream()
                .map(v -> VendorDtos.toResponse(v, BigDecimal.ZERO))
                .collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long id) {
        authorizationService.checkPermission(PermissionCode.VENDOR_DELETE);
        Vendor vendor = findInTenant(id);
        if (billRepository.existsByVendorIdAndCompanyId(vendor.getId(), vendor.getCompanyId())) {
            throw new BadRequestException("This vendor has bills on record - deactivate it instead of deleting");
        }
        vendor.softDelete();
        vendorRepository.save(vendor);
    }

    private BigDecimal outstandingFor(Vendor vendor) {
        BigDecimal outstanding = billRepository.sumOutstandingByVendor(vendor.getCompanyId(), vendor.getId());
        return outstanding != null ? outstanding : BigDecimal.ZERO;
    }

    Vendor findInTenant(Long id) {
        return vendorRepository.findByIdAndCompanyId(id, requireCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found: " + id));
    }

    private Long requireCompanyId() {
        Long id = securityUtil.getCurrentCompanyId();
        if (id == null) throw new BadRequestException("No company context");
        return id;
    }
}
