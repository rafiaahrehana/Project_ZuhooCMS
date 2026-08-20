package com.businessos.modules.crm.tag;

import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.modules.company.Company;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final SecurityUtil securityUtil;
    private final AuthorizationService authorizationService;

    @Override
    @Transactional(readOnly = true)
    public List<TagResponse> list() {
        authorizationService.checkPermission(PermissionCode.TAG_VIEW);
        return TagMapper.toResponseList(tagRepository.findByCompanyIdOrderByNameAsc(requireCompanyId()));
    }

    @Override
    public TagResponse create(TagRequest request) {
        authorizationService.checkPermission(PermissionCode.TAG_MANAGE);
        Long companyId = requireCompanyId();
        if (tagRepository.existsByNameIgnoreCaseAndCompanyId(request.getName(), companyId)) {
            throw new BadRequestException("A tag with this name already exists");
        }
        Tag tag = Tag.builder()
                .name(request.getName())
                .color(request.getColor())
                .company(companyRef(companyId))
                .build();
        return TagMapper.toResponse(tagRepository.save(tag));
    }

    @Override
    public TagResponse update(Long id, TagRequest request) {
        authorizationService.checkPermission(PermissionCode.TAG_MANAGE);
        Tag tag = findOwned(id);
        if (request.getName() != null) tag.setName(request.getName());
        if (request.getColor() != null) tag.setColor(request.getColor());
        return TagMapper.toResponse(tagRepository.save(tag));
    }

    @Override
    public void delete(Long id) {
        authorizationService.checkPermission(PermissionCode.TAG_MANAGE);
        Tag tag = findOwned(id);
        tag.softDelete();
        tagRepository.save(tag);
    }

    private Tag findOwned(Long id) {
        return tagRepository.findByIdAndCompanyId(id, requireCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found: " + id));
    }

    private Company companyRef(Long companyId) {
        Company c = new Company();
        c.setId(companyId);
        return c;
    }

    private Long requireCompanyId() {
        Long companyId = securityUtil.getCurrentCompanyId();
        if (companyId == null) {
            throw new BadRequestException("No company context found");
        }
        return companyId;
    }
}
