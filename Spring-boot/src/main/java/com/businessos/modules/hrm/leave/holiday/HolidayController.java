package com.businessos.modules.hrm.leave.holiday;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hr/holidays")
@PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
public class HolidayController {

    private final HolidayService holidayService;

    @PostMapping
    public ResponseEntity<HolidayResponse> create(@RequestBody HolidayRequest request) {
        return new ResponseEntity<>(holidayService.create(request), HttpStatus.CREATED);
    }

    /** Ask AI to draft a holiday for review - not persisted until the user saves it via POST above. */
    @PostMapping("/ai-draft")
    public ResponseEntity<HolidayDraftResponse> draftWithAi(@Valid @RequestBody HolidayDraftRequest request) {
        return ResponseEntity.ok(holidayService.draftWithAi(request));
    }

    @GetMapping
    public ResponseEntity<Page<HolidayResponse>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(holidayService.listAll(
                PageRequest.of(page, size, Sort.by("date").ascending())));
    }

    @GetMapping("/year/{year}")
    public ResponseEntity<List<HolidayResponse>> listByYear(@PathVariable int year) {
        return ResponseEntity.ok(holidayService.listByYear(year));
    }

    @GetMapping("/current-year")
    public ResponseEntity<List<HolidayResponse>> listCurrentYear() {
        return ResponseEntity.ok(holidayService.listByYear(Year.now().getValue()));
    }

    @GetMapping("/range")
    public ResponseEntity<List<HolidayResponse>> listByRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(holidayService.listByRange(from, to));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HolidayResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(holidayService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HolidayResponse> update(
            @PathVariable Long id,
            @RequestBody HolidayRequest request) {
        return ResponseEntity.ok(holidayService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        holidayService.delete(id);
        return ResponseEntity.ok("Deleted successfully");
    }
}


