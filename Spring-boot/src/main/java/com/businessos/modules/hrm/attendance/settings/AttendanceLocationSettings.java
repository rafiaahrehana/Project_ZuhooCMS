package com.businessos.modules.hrm.attendance.settings;

import com.businessos.core.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * One company's office-location policy for GPS-verified attendance: where the
 * office is, how far a check-in/check-out is allowed to be from it, and
 * whether that distance is enforced at all.
 *
 * Exists as its own table rather than more columns on Company, mirroring
 * PayrollSettings - this is attendance policy, not company identity, and it
 * carries its own permission.
 *
 * Enforcement defaults off and the coordinates default unset, so an existing
 * company sees no change in attendance behaviour until an owner deliberately
 * configures a location.
 */
@Entity
@Table(name = "attendance_location_settings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AttendanceLocationSettings extends BaseEntity {

    /** One row per company. */
    @Column(nullable = false, unique = true)
    private Long companyId;

    private Double officeLatitude;
    private Double officeLongitude;

    @Builder.Default
    private Integer radiusMeters = 200;

    @Builder.Default
    private boolean gpsEnforcementEnabled = false;
}
