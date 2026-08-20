package com.businessos.modules.hrm.leave.holiday;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.List;

public interface HolidayService {

    // ADMIN/OWNER: create a company holiday
    HolidayResponse create(HolidayRequest request);

    // ADMIN/OWNER: ask AI to draft a holiday for review - not persisted until saved via create()
    HolidayDraftResponse draftWithAi(HolidayDraftRequest request);

    HolidayResponse getById(Long id);

    // ADMIN/OWNER: list all holidays with pagination
    Page<HolidayResponse> listAll(Pageable pageable);

    //ALL: list all holidays for a given year
    List<HolidayResponse> listByYear(int year);

    // ALL: list holidays within a date range
    List<HolidayResponse> listByRange(LocalDate from, LocalDate to);

    // ADMIN / OWNER: update an existing holiday
    HolidayResponse update(Long id, HolidayRequest request);

    //ADMIN / OWNER: soft-delete a holiday
    void delete(Long id);

}
