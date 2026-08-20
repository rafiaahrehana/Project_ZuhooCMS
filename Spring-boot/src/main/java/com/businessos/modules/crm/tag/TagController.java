package com.businessos.modules.crm.tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/crm/tags")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('COMPANY_OWNER','EMPLOYEE')")
public class TagController {

    private final TagService tagService;

    @GetMapping
    public List<TagResponse> list() {
        return tagService.list();
    }

    @PostMapping
    public TagResponse create(@Valid @RequestBody TagRequest request) {
        return tagService.create(request);
    }

    @PatchMapping("/{id}")
    public TagResponse update(@PathVariable Long id, @Valid @RequestBody TagRequest request) {
        return tagService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        tagService.delete(id);
    }
}
