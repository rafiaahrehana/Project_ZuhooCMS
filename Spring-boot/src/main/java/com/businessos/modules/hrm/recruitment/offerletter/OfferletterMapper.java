package com.businessos.modules.hrm.recruitment.offerletter;

import com.businessos.auth.user.User;
import com.businessos.modules.hrm.employee.Employee;

public class OfferletterMapper {

    public static OfferLetterResponse toLetterResponse(OfferLetter ol) {
        Employee emp = ol.getEmployee();
        User empUser = emp != null ? emp.getUser() : null;
        User createdBy = ol.getCreatedBy();
        OfferLetterResponse r = new OfferLetterResponse();
        r.setId(ol.getId());
        r.setLetterType(ol.getLetterType());
        r.setReferenceNumber(ol.getReferenceNumber());
        r.setIssueDate(ol.getIssueDate());
        r.setContent(ol.getContent());
        r.setSignedBy(ol.getSignedBy());
        r.setFileUrl(ol.getFileUrl());
        r.setIssued(ol.isIssued());
        r.setEmployeeId(emp != null ? emp.getId() : null);
        r.setEmployeeName(empUser != null ? empUser.getFullName() : null);
        r.setJobApplicationId(ol.getJobApplication() != null ? ol.getJobApplication().getId() : null);
        r.setRecipientName(ol.getRecipientName());
        r.setRecipientEmail(ol.getRecipientEmail());
        r.setCreatedById(createdBy != null ? createdBy.getId() : null);
        r.setCreatedByName(createdBy != null ? createdBy.getFullName() : null);
        r.setCreatedAt(ol.getCreatedAt());
        return r;
    }
}
