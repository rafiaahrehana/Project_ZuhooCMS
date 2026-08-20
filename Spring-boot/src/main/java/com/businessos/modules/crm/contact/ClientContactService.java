package com.businessos.modules.crm.contact;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ClientContactService {

    ClientContactResponse create(Long clientId, ClientContactRequest request);

    List<ClientContactResponse> listByClient(Long clientId);

    // Cross-client global list, for the standalone Contacts page.
    Page<ClientContactResponse> listAll(String keyword, Pageable pageable);

    ClientContactResponse getById(Long clientId, Long id);

    ClientContactResponse update(Long clientId, Long id, ClientContactRequest request);

    ClientContactResponse markPrimary(Long clientId, Long id);

    void delete(Long clientId, Long id);
}
