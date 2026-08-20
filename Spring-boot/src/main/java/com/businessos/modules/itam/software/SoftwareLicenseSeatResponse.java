package com.businessos.modules.itam.software;

import lombok.*;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SoftwareLicenseSeatResponse {
    private Long id;
    private Long licenseId;
    private String softwareName;
    private Long employeeId;
    private String employeeName;
    private LocalDate assignedAt;
}
