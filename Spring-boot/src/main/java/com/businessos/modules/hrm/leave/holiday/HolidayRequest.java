package com.businessos.modules.hrm.leave.holiday;

import com.businessos.enums.HolidayType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class HolidayRequest {
    @NotBlank(message = "Holiday name is required")
    @Size(max = 150)
    private String name;
    @NotNull(message = "Date is required")
    private LocalDate holidayDate;
    @NotNull(message = "Holiday type is required")
    private HolidayType holidayType;
    private String description;
}
