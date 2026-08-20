package com.businessos.modules.hrm.asset;

import com.businessos.modules.company.Company;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.enums.AssetStatus;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;

import com.businessos.modules.hrm.employee.EmployeeRepository;
import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.enums.NotificationType;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.notification.CreateNotificationRequest;
import com.businessos.shared.notification.NotificationService;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor

public class AssetServiceImpl implements AssetService {

    private final AssetRepository assetRepository;
    private final EmployeeRepository employeeRepository;
    private final AssetAssignmentHistoryRepository historyRepository;
    private final SecurityUtil securityUtil;
    private final AuthorizationService authorizationService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public AssetResponse create(AssetRequest request) {
        authorizationService.checkPermission(PermissionCode.HARDWARE_CREATE);
        Long companyId = requireCompanyId();
        // The CSV importer already checks this (existsByCompanyIdAndSerialNumber/
        // AssetTag exist specifically for it) - manual entry through this form
        // never did.
        if (request.getSerialNumber() != null && !request.getSerialNumber().isBlank()
                && assetRepository.existsByCompanyIdAndSerialNumber(companyId, request.getSerialNumber())) {
            throw new BadRequestException("An asset with this serial number already exists");
        }
        if (request.getAssetTag() != null && !request.getAssetTag().isBlank()
                && assetRepository.existsByCompanyIdAndAssetTag(companyId, request.getAssetTag())) {
            throw new BadRequestException("An asset with this asset tag already exists");
        }
        Asset asset = new Asset(); asset.setName(request.getName()); asset.setCategory(request.getCategory()); asset.setSerialNumber(request.getSerialNumber()); asset.setNotes(request.getDescription()); asset.setPurchaseDate(request.getPurchaseDate()); asset.setPurchasePrice(request.getPurchaseCost()); asset.setStatus(AssetStatus.AVAILABLE); asset.setCompany(companyRef(companyId));
        asset.setAssetTag(request.getAssetTag());
        asset.setBrand(request.getBrand());
        asset.setModel(request.getModel());
        asset.setIpAddress(request.getIpAddress());
        asset.setMacAddress(request.getMacAddress());
        asset.setProcessorModel(request.getProcessorModel());
        asset.setRamSize(request.getRamSize());
        asset.setStorageSize(request.getStorageSize());
        asset.setOperatingSystem(request.getOperatingSystem());
        asset.setWarrantyExpiry(request.getWarrantyExpiry());

        if (request.getAssignedToId() != null) {
            Employee emp = findEmployee(request.getAssignedToId(), companyId);
            asset.setAssignedTo(emp);
            asset.setStatus(AssetStatus.ASSIGNED);
            asset.setAssignedAt(LocalDate.now());
            assetRepository.save(asset);
            recordHistory(asset, emp, LocalDate.now(), null, null, companyId);
            notifyAssigned(asset, emp, companyId);
        } else {
            assetRepository.save(asset);
        }
        return AssetMapper.toAssetResponse(asset);
    }

    @Override
    @Transactional(readOnly = true)
    public AssetResponse getById(Long id) {
        return AssetMapper.toAssetResponse(findInTenant(id));
    }

    // This single endpoint backs both the ITAM Hardware admin page and the HRM Assets
    // page, so either permission unlocks it.
    @Override
    @Transactional(readOnly = true)
    public Page<AssetResponse> listAll(AssetStatus status, Pageable pageable) {
        authorizationService.checkAnyPermission(PermissionCode.HARDWARE_VIEW, PermissionCode.ASSET_VIEW);
        Long companyId = requireCompanyId();
        Page<Asset> page = status != null
            ? assetRepository.findByCompanyIdAndStatus(companyId, status, pageable)
            : assetRepository.findByCompanyId(companyId, pageable);
        return page.map(AssetMapper::toAssetResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetResponse> listForEmployee(Long employeeId) {
        return assetRepository.findByCompanyIdAndAssignedToId(requireCompanyId(), employeeId)
            .stream().map(AssetMapper::toAssetResponse).toList();
    }

    @Override
    @Transactional
    public AssetResponse update(Long id, AssetRequest request) {
        authorizationService.checkPermission(PermissionCode.HARDWARE_UPDATE);
        Long companyId = requireCompanyId();
        Asset asset = findInTenant(id);
        if (request.getSerialNumber() != null && !request.getSerialNumber().isBlank()
                && !request.getSerialNumber().equals(asset.getSerialNumber())
                && assetRepository.existsByCompanyIdAndSerialNumber(companyId, request.getSerialNumber())) {
            throw new BadRequestException("An asset with this serial number already exists");
        }
        if (request.getAssetTag() != null && !request.getAssetTag().isBlank()
                && !request.getAssetTag().equals(asset.getAssetTag())
                && assetRepository.existsByCompanyIdAndAssetTag(companyId, request.getAssetTag())) {
            throw new BadRequestException("An asset with this asset tag already exists");
        }
        if (request.getName()         != null) asset.setName(request.getName());
        if (request.getCategory()     != null) asset.setCategory(request.getCategory());
        if (request.getSerialNumber() != null) asset.setSerialNumber(request.getSerialNumber());
        if (request.getDescription()  != null) asset.setNotes(request.getDescription());
        if (request.getPurchaseDate() != null) asset.setPurchaseDate(request.getPurchaseDate());
        if (request.getPurchaseCost() != null) asset.setPurchasePrice(request.getPurchaseCost());
        if (request.getNotes()        != null) asset.setNotes(request.getNotes());
        if (request.getAssetTag()        != null) asset.setAssetTag(request.getAssetTag());
        if (request.getBrand()           != null) asset.setBrand(request.getBrand());
        if (request.getModel()           != null) asset.setModel(request.getModel());
        if (request.getIpAddress()       != null) asset.setIpAddress(request.getIpAddress());
        if (request.getMacAddress()      != null) asset.setMacAddress(request.getMacAddress());
        if (request.getProcessorModel()  != null) asset.setProcessorModel(request.getProcessorModel());
        if (request.getRamSize()         != null) asset.setRamSize(request.getRamSize());
        if (request.getStorageSize()     != null) asset.setStorageSize(request.getStorageSize());
        if (request.getOperatingSystem() != null) asset.setOperatingSystem(request.getOperatingSystem());
        if (request.getWarrantyExpiry()  != null) asset.setWarrantyExpiry(request.getWarrantyExpiry());
        return AssetMapper.toAssetResponse(asset);
    }

    @Override
    @Transactional
    public AssetResponse assign(Long id, Long employeeId) {
        authorizationService.checkPermission(PermissionCode.HARDWARE_UPDATE);
        Long companyId = requireCompanyId();
        // Locked read-then-write: two admins assigning the same asset to two
        // different new hires at once previously raced, silently losing one
        // assignment with an orphaned open history row. Matches
        // SoftwareLicenseServiceImpl's assignSeat()/releaseSeat() pattern.
        Asset asset = assetRepository.findByIdAndCompanyIdForUpdate(id, companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + id));
        if (asset.getStatus() == AssetStatus.ASSIGNED) {
            throw new BadRequestException("Asset is already assigned. Unassign it first.");
        }
        Employee emp = findEmployee(employeeId, companyId);
        asset.setAssignedTo(emp);
        asset.setStatus(AssetStatus.ASSIGNED);
        asset.setAssignedAt(LocalDate.now());
        asset.setReturnedAt(null);
        recordHistory(asset, emp, LocalDate.now(), null, null, companyId);
        notifyAssigned(asset, emp, companyId);
        return AssetMapper.toAssetResponse(asset);
    }

    @Override
    @Transactional
    public AssetResponse setMaintenance(Long id, boolean underMaintenance) {
        authorizationService.checkPermission(PermissionCode.HARDWARE_UPDATE);
        Asset asset = findInTenant(id);
        if (asset.getStatus() == AssetStatus.ASSIGNED) {
            throw new BadRequestException("Cannot change maintenance status on an assigned asset. Unassign it first.");
        }
        if (asset.getStatus() == AssetStatus.DISPOSED) {
            throw new BadRequestException("This asset has been disposed and can no longer be modified.");
        }
        if (underMaintenance) {
            if (asset.getStatus() != AssetStatus.AVAILABLE) {
                throw new BadRequestException("Asset is already under maintenance");
            }
            asset.setStatus(AssetStatus.UNDER_MAINTENANCE);
        } else {
            if (asset.getStatus() != AssetStatus.UNDER_MAINTENANCE) {
                throw new BadRequestException("Asset is not currently under maintenance");
            }
            asset.setStatus(AssetStatus.AVAILABLE);
        }
        return AssetMapper.toAssetResponse(asset);
    }

    @Override
    @Transactional
    public AssetResponse dispose(Long id, String reason) {
        authorizationService.checkPermission(PermissionCode.HARDWARE_UPDATE);
        Asset asset = findInTenant(id);
        if (asset.getStatus() == AssetStatus.ASSIGNED) {
            throw new BadRequestException("Cannot dispose an assigned asset. Unassign it first.");
        }
        if (asset.getStatus() == AssetStatus.DISPOSED) {
            throw new BadRequestException("This asset has already been disposed");
        }
        asset.setStatus(AssetStatus.DISPOSED);
        asset.setDisposalDate(LocalDate.now());
        asset.setDisposalReason(reason);
        return AssetMapper.toAssetResponse(asset);
    }

    @Override
    @Transactional
    public AssetResponse unassign(Long id) {
        authorizationService.checkPermission(PermissionCode.HARDWARE_UPDATE);
        Long companyId = requireCompanyId();
        Asset asset = findInTenant(id);
        if (asset.getStatus() != AssetStatus.ASSIGNED) {
            throw new BadRequestException("Asset is not currently assigned");
        }
        // Close the open history record
        historyRepository.findTopByAssetIdAndCompanyIdAndReturnedAtIsNullOrderByAssignedAtDesc(id, companyId)
            .ifPresent(h -> {
                h.setReturnedAt(LocalDate.now());
                historyRepository.save(h);
            });
        asset.setAssignedTo(null);
        asset.setStatus(AssetStatus.AVAILABLE);
        asset.setReturnedAt(LocalDate.now());
        return AssetMapper.toAssetResponse(asset);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        authorizationService.checkPermission(PermissionCode.HARDWARE_DELETE);
        Asset asset = findInTenant(id);
        if (asset.getStatus() == AssetStatus.ASSIGNED) {
            throw new BadRequestException("Cannot delete an assigned asset. Unassign it first.");
        }
        asset.softDelete();
    }

    // Previously nobody was told when an asset was handed to them - they only
    // found out by checking "My Assets" themselves.
    private void notifyAssigned(Asset asset, Employee emp, Long companyId) {
        if (emp.getUser() == null) return;
        notificationService.send(CreateNotificationRequest.of(
                NotificationType.ASSET_ASSIGNED,
                "Asset assigned to you",
                asset.getName() + (asset.getAssetTag() != null ? " (" + asset.getAssetTag() + ")" : "") + " has been assigned to you",
                "/itam/hardware",
                emp.getUser().getId(),
                companyId));
    }

    private void recordHistory(Asset asset, Employee emp, LocalDate assignedAt,
                                LocalDate returnedAt, String condition, Long companyId) {
        Company c = new Company(); c.setId(companyId);
        com.businessos.modules.itam.shared.AssetHistory history = com.businessos.modules.itam.shared.AssetHistory.builder()
            .asset(asset)
            .employee(emp)
            .company(c)
            .assignedAt(assignedAt)
            .returnedAt(returnedAt)
            .condition(condition)
            .assignedBy(securityUtil.getCurrentUser())
            .build();
        historyRepository.save(history);
    }

    private Asset findInTenant(Long id) {
        return assetRepository.findByIdAndCompanyId(id, requireCompanyId())
            .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + id));
    }

    private Employee findEmployee(Long employeeId, Long companyId) {
        return employeeRepository.findByIdAndCompanyId(employeeId, companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
    }

    private Long requireCompanyId() {
        Long id = securityUtil.getCurrentCompanyId();
        if (id == null) throw new BadRequestException("No company context");
        return id;
    }

    private Company companyRef(Long companyId) {
        Company c = new Company(); c.setId(companyId); return c;
    }
}

