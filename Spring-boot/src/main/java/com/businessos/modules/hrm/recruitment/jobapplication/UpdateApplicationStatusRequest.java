package com.businessos.modules.hrm.recruitment.jobapplication;

import com.businessos.enums.ApplicationStatus;
import lombok.Data;

@Data
public class UpdateApplicationStatusRequest {
    private ApplicationStatus status;
    private String notes;
}
