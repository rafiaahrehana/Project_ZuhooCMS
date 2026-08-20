package com.businessos.modules.crm.contact;

import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.modules.crm.client.Client;
import com.businessos.modules.crm.client.ClientRepository;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ClientContactServiceImpl implements ClientContactService {

    private final ClientContactRepository clientContactRepository;
    private final ClientRepository clientRepository;
    private final SecurityUtil securityUtil;
    private final AuthorizationService authorizationService;

    @Override
    public ClientContactResponse create(Long clientId, ClientContactRequest request) {
        authorizationService.checkPermission(PermissionCode.CONTACT_CREATE);
        Long companyId = requireCompanyId();
        Client client = clientRepository.findByIdAndCompanyId(clientId, companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        if (request.getEmail() != null && !request.getEmail().isBlank()
            && clientContactRepository.existsByEmailAndClientIdAndCompanyIdAndDeletedFalse(request.getEmail(), clientId, companyId)) {
            throw new BadRequestException("A contact with this email already exists for this client");
        }

        boolean primary = Boolean.TRUE.equals(request.getPrimaryContact());
        if (primary) {
            clientContactRepository.clearPrimaryContact(clientId, companyId);
        }

        ClientContact contact = ClientContact.builder()
            .fullName(request.getFullName())
            .email(request.getEmail())
            .phone(request.getPhone())
            .jobTitle(request.getJobTitle())
            .department(request.getDepartment())
            .primaryContact(primary)
            .notes(request.getNotes())
            .client(client)
            .company(client.getCompany())
            .build();

        return ClientContactMapper.toResponse(clientContactRepository.save(contact));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientContactResponse> listByClient(Long clientId) {
        authorizationService.checkPermission(PermissionCode.CONTACT_VIEW);
        Long companyId = requireCompanyId();
        clientRepository.findByIdAndCompanyId(clientId, companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        return clientContactRepository
            .findByClientIdAndCompanyIdOrderByPrimaryContactDescCreatedAtDesc(clientId, companyId)
            .stream()
            .map(ClientContactMapper::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClientContactResponse> listAll(String keyword, Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.CONTACT_VIEW);
        Long companyId = requireCompanyId();
        Page<ClientContact> page = keyword != null && !keyword.isBlank()
                ? clientContactRepository.searchContacts(companyId, escapeLikeKeyword(keyword.trim()), pageable)
                : clientContactRepository.findByCompanyId(companyId, pageable);
        return page.map(ClientContactMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientContactResponse getById(Long clientId, Long id) {
        return ClientContactMapper.toResponse(findOwned(clientId, id));
    }

    @Override
    public ClientContactResponse update(Long clientId, Long id, ClientContactRequest request) {
        authorizationService.checkPermission(PermissionCode.CONTACT_UPDATE);
        ClientContact contact = findOwned(clientId, id);

        if (request.getFullName() != null) contact.setFullName(request.getFullName());
        contact.setEmail(request.getEmail());
        contact.setPhone(request.getPhone());
        contact.setJobTitle(request.getJobTitle());
        contact.setDepartment(request.getDepartment());
        contact.setNotes(request.getNotes());

        if (Boolean.TRUE.equals(request.getPrimaryContact()) && !contact.isPrimaryContact()) {
            clientContactRepository.clearPrimaryContact(contact.getClient().getId(), requireCompanyId());
            contact.setPrimaryContact(true);
        }

        return ClientContactMapper.toResponse(clientContactRepository.save(contact));
    }

    @Override
    public ClientContactResponse markPrimary(Long clientId, Long id) {
        authorizationService.checkPermission(PermissionCode.CONTACT_UPDATE);
        ClientContact contact = findOwned(clientId, id);
        clientContactRepository.clearPrimaryContact(contact.getClient().getId(), requireCompanyId());
        contact.setPrimaryContact(true);
        return ClientContactMapper.toResponse(clientContactRepository.save(contact));
    }

    @Override
    public void delete(Long clientId, Long id) {
        authorizationService.checkPermission(PermissionCode.CONTACT_DELETE);
        ClientContact contact = findOwned(clientId, id);
        contact.softDelete();
        clientContactRepository.save(contact);
    }

    private ClientContact findOwned(Long clientId, Long id) {
        ClientContact contact = clientContactRepository.findByIdAndCompanyId(id, requireCompanyId())
            .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));
        if (!contact.getClient().getId().equals(clientId)) {
            throw new ResourceNotFoundException("Contact not found");
        }
        return contact;
    }

    private Long requireCompanyId() {
        Long companyId = securityUtil.getCurrentCompanyId();
        if (companyId == null) {
            throw new BadRequestException("No company context for current platformuser");
        }
        return companyId;
    }

    // '!' is the LIKE escape character used by ClientContactRepository's ESCAPE '!'
    // queries. Mirrors GlobalSearchServiceImpl.escapeLikeKeyword.
    private String escapeLikeKeyword(String keyword) {
        if (keyword == null) return null;
        return keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
