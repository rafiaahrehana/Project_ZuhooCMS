package com.businessos.modules.servicedesk.servicerequest;

import com.businessos.modules.crm.client.Client;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.auth.user.User;
import com.businessos.modules.servicedesk.companyservice.PackageSubscription;
import com.businessos.modules.servicedesk.requestcomment.RequestComment;
import com.businessos.modules.servicedesk.requestcomment.RequestCommentResponse;
import com.businessos.modules.servicedesk.requeststatus.RequestStatusHistory;
import com.businessos.modules.servicedesk.requeststatus.RequestStatusHistoryResponse;

public final class ServiceRequestMapper {

    public static ServiceRequestResponse toResponse(ServiceRequest req,
                                                     long taskCount,
                                                     long completedTaskCount) {
        Client client = req.getClient();
        Employee assigned = req.getAssignedEmployee();
        User clientUser = client != null ? client.getUser() : null;
        User assignedUser = assigned != null ? assigned.getUser() : null;
        PackageSubscription sub = req.getSubscription();

        ServiceRequestResponse r = new ServiceRequestResponse();
        r.setId(req.getId());
        r.setTitle(req.getTitle());
        r.setDescription(req.getDescription());
        r.setStatus(req.getStatus());
        r.setPriority(req.getPriority());
        r.setAgreedPrice(req.getAgreedPrice());
        r.setSlaDeadline(req.getSlaDeadline());
        r.setSlaBreach(req.isSlaBreach());
        r.setAssignedAt(req.getAssignedAt());
        r.setCompletedAt(req.getCompletedAt());
        r.setResubmitCount(req.getResubmitCount());
        r.setPermanentlyClosed(req.isPermanentlyClosed());
        r.setCompanyId(req.getCompany() != null ? req.getCompany().getId() : null);
        r.setClientId(client != null ? client.getId() : null);
        r.setClientName(clientUser != null ? clientUser.getFullName() : null);
        r.setHubServiceId(req.getCompanyService() != null ? req.getCompanyService().getId() : null);
        r.setHubServiceName(req.getCompanyService() != null ? req.getCompanyService().getName() : null);
        r.setAssignedEmployeeId(assigned != null ? assigned.getId() : null);
        r.setAssignedEmployeeName(assignedUser != null ? assignedUser.getFullName() : null);
        r.setSubscriptionId(sub != null ? sub.getId() : null);
        r.setPackageName(sub != null && sub.getServicePackage() != null
            ? sub.getServicePackage().getName() : null);
        r.setInvoiceId(req.getInvoiceId());
        r.setGovRefNumber(req.getGovRefNumber());
        r.setGovRefType(req.getGovRefType());
        r.setTaskCount(taskCount);
        r.setCompletedTaskCount(completedTaskCount);
        r.setCreatedAt(req.getCreatedAt());
        r.setUpdatedAt(req.getUpdatedAt());

        // Quotation mappings
        r.setQuotationAmount(req.getQuotationAmount());
        r.setQuotationCurrency(req.getQuotationCurrency());
        r.setQuotationNotes(req.getQuotationNotes());
        r.setQuotationValidUntil(req.getQuotationValidUntil());
        r.setQuotationStatus(req.getQuotationStatus());

        return r;
    }


    public static RequestCommentResponse toCommentResponse(RequestComment comment) {
        User author = comment.getAuthor();
        RequestCommentResponse response = new RequestCommentResponse();
        response.setId(comment.getId());
        response.setContent(comment.getContent());
        response.setVisibility(comment.getVisibility());
        response.setAttachmentUrl(comment.getAttachmentUrl());
        response.setAuthorId(author != null ? author.getId() : null);
        response.setAuthorName(author != null ? author.getFullName() : null);
        response.setCreatedAt(comment.getCreatedAt());

        return response;
    }

    public static RequestStatusHistoryResponse toHistoryResponse(RequestStatusHistory history) {
        User changedBy = history.getChangedBy();
        RequestStatusHistoryResponse r = new RequestStatusHistoryResponse();
        r.setId(history.getId());
        r.setOldStatus(history.getOldStatus());
        r.setNewStatus(history.getNewStatus());
        r.setReason(history.getReason());
        r.setChangedById(changedBy != null ? changedBy.getId() : null);
        r.setChangedByName(changedBy != null ? changedBy.getFullName() : null);
        r.setChangedAt(history.getChangedAt());
        return r;
    }
}
