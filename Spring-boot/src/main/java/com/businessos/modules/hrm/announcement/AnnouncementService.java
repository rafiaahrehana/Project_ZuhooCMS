package com.businessos.modules.hrm.announcement;

import org.springframework.data.domain.Page;

import org.springframework.data.domain.Pageable;
import java.util.List;

public interface AnnouncementService {

    /** ADMIN / OWNER: create a new draft announcement */
    AnnouncementResponse create(AnnouncementRequest request);

    /** ALL: get announcement by id */
    AnnouncementResponse getById(Long id);

    /** ADMIN / OWNER: list all announcements with pagination */
    Page<AnnouncementResponse> listAll(Pageable pageable);

    /** ALL: list published and non-expired announcements */
    List<AnnouncementResponse> listActive();

    /** ADMIN / OWNER: publish draft announcement and notify audience */
    AnnouncementResponse publish(Long id);

    /** ADMIN / OWNER: update a draft announcement */
    AnnouncementResponse update(Long id, AnnouncementRequest request);

    /** ADMIN / OWNER: soft-delete a draft announcement */
    void delete(Long id);

    /** ADMIN / OWNER: ask AI to draft a title/body, with real company context injected. Not persisted. */
    AnnouncementDraftResponse draftWithAi(AnnouncementDraftRequest request);

    /** System entry point (AnnouncementScheduledPublishScheduler) - no security context. */
    void publishDueScheduled();
}
