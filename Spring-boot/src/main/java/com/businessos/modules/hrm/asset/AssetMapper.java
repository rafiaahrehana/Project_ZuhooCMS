package com.businessos.modules.hrm.asset;

import com.businessos.auth.user.User;
import com.businessos.modules.hrm.employee.Employee;

public class AssetMapper {

    /** A terminated employee's lazy proxy throws on any field access beyond its id - see TimesheetMapper.safeUser. */
    private static User safeUser(Employee emp) {
        if (emp == null) return null;
        try {
            return emp.getUser();
        } catch (Exception e) {
            return null;
        }
    }

    public static AssetResponse toAssetResponse(Asset a) {
        Employee assigned = a.getAssignedTo();
        User assignedUser = safeUser(assigned);
        AssetResponse r = new AssetResponse();
        r.setId(a.getId());
        r.setName(a.getName());
        r.setCategory(a.getCategory());
        r.setSerialNumber(a.getSerialNumber());
        r.setDescription(a.getNotes());
        r.setPurchaseDate(a.getPurchaseDate());
        r.setPurchaseCost(a.getPurchasePrice());
        r.setStatus(a.getStatus());
        r.setAssignedAt(a.getAssignedAt());
        r.setReturnDate(a.getReturnedAt());
        r.setNotes(a.getNotes());
        r.setAssignedToId(assigned != null ? assigned.getId() : null);
        r.setAssignedToName(assignedUser != null ? assignedUser.getFullName() : null);
        r.setCreatedAt(a.getCreatedAt());
        r.setAssetTag(a.getAssetTag());
        r.setBrand(a.getBrand());
        r.setModel(a.getModel());
        r.setIpAddress(a.getIpAddress());
        r.setMacAddress(a.getMacAddress());
        r.setProcessorModel(a.getProcessorModel());
        r.setRamSize(a.getRamSize());
        r.setStorageSize(a.getStorageSize());
        r.setOperatingSystem(a.getOperatingSystem());
        r.setWarrantyExpiry(a.getWarrantyExpiry());
        return r;
    }
}
