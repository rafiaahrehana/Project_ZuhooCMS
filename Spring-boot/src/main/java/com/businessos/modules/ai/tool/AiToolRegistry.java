package com.businessos.modules.ai.tool;

import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Every AiTool is a Spring bean; this just collects them and applies the one
 * cross-cutting rule every tool shares: filter by the caller's actual
 * permissions BEFORE the list is ever sent to a provider, so a tool a user
 * isn't authorized for is not just refused if called - it's never visible to
 * the model as an option in the first place.
 *
 * <p>Takes {@code ObjectProvider<List<AiTool>>} rather than {@code List<AiTool>}
 * directly: several tools wrap services (e.g. AnnouncementServiceImpl) that
 * themselves depend on AiService for their own draftWithAi()-style features.
 * An eager List<AiTool> here would force those tool beans to be constructed
 * while AiServiceImpl (which depends on this registry) is still being built,
 * forming a bean-creation cycle. ObjectProvider defers resolving the list
 * until a method below is actually called, by which point every bean in the
 * cycle already exists.
 */
@Component
@RequiredArgsConstructor
public class AiToolRegistry {

    private final ObjectProvider<List<AiTool>> toolsProvider;
    private final AuthorizationService authorizationService;

    public List<AiTool> all() {
        return toolsProvider.getObject();
    }

    /** Tools the current caller is actually allowed to invoke, right now. */
    public List<AiTool> availableForCurrentUser() {
        return all().stream()
            .filter(this::isAvailable)
            .toList();
    }

    public Optional<AiTool> byName(String name) {
        return all().stream().filter(t -> t.name().equals(name)).findFirst();
    }

    private boolean isAvailable(AiTool tool) {
        PermissionCode required = tool.requiredPermission();
        return required == null || authorizationService.hasPermission(required);
    }
}
