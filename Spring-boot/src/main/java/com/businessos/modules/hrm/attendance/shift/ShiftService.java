package com.businessos.modules.hrm.attendance.shift;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ShiftService {
    ShiftResponse create(ShiftRequest request);
    ShiftResponse getById(Long id);
    Page<ShiftResponse> listAll(Pageable pageable);
    List<ShiftResponse> listActive();
    ShiftResponse update(Long id, ShiftRequest request);
    ShiftResponse toggleActive(Long id);
    void delete(Long id);
}
