package com.businessos.modules.hrm.payroll;

/**
 * A rendered payslip and the filename it should download as.
 *
 * The filename is built from the employee number and pay period, which only the
 * service can see once the entity is loaded - returning it alongside the bytes
 * saves the controller a second lookup just to name the attachment.
 */
public record PayslipDocument(byte[] content, String fileName) {
}
