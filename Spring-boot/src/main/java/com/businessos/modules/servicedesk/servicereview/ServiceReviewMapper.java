package com.businessos.modules.servicedesk.servicereview;

import com.businessos.auth.user.User;
import com.businessos.modules.crm.client.Client;
import com.businessos.modules.servicedesk.companyservice.CompanyService;

public class ServiceReviewMapper {

    public static ServiceReviewResponse toServiceReviewResponse(ServiceReview review) {
        CompanyService service = review.getHubService();
        Client client = review.getClient();
        User clientUser = client != null ? client.getUser() : null;

        ServiceReviewResponse r = new ServiceReviewResponse();
        r.setId(review.getId());
        r.setRating(review.getRating());
        r.setComment(review.getComment());
        r.setPublished(review.isPublished());
        r.setServiceRequestId(
            review.getServiceRequest() != null ? review.getServiceRequest().getId() : null);
        r.setHubServiceId(service != null ? service.getId() : null);
        r.setHubServiceName(service != null ? service.getName() : null);
        r.setClientId(client != null ? client.getId() : null);
        r.setClientName(clientUser != null ? clientUser.getFullName() : null);
        r.setCreatedAt(review.getCreatedAt());
        return r;
    }
}
