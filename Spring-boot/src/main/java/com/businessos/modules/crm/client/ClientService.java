package com.businessos.modules.crm.client;

import com.businessos.enums.ClientStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ClientService {

    ClientResponse create(CreateClientRequest request);

    /** Public, unauthenticated self-registration - the client picks their own company. */
    ClientResponse registerPublic(PublicClientRegisterRequest request);

    ClientResponse getById(Long id);

    ClientResponse getMyProfile();

    ClientResponse updateMyProfile(UpdateMyClientProfileRequest request);

    Page<ClientResponse> listAll(ClientStatus status, Long tagId, Pageable pageable);

    /** Lightweight, ungated - the active-client picker used by Invoices, Payment Receipts, and Pipeline. */
    List<ClientResponse> listActive();

      ClientResponse update(Long id, UpdateClientRequest request);

    /**
     * Creates (or links) a CLIENT-role login for this client and emails a
     * one-time set-password link. No password is ever chosen by staff, so none
     * has to be communicated out of band.
     */
    ClientResponse inviteToPortal(Long id);

    void delete(Long id);

    long getClientCount();

    boolean isClient(Long userId);
}
