package com.ai.agent.real.web.service;

import com.ai.agent.real.application.service.PlaygroundRoleplayRoleService;
import com.ai.agent.real.domain.entity.roleplay.PlaygroundRoleplayRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Preload role setups (prompt, voice) into memory at application startup.
 * Non-blocking: uses reactive subscribe to fill cache; getSetup reads from memory.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoleSetupProvider {

    private final PlaygroundRoleplayRoleService roleService;
    private final OmniProperties properties;

    private final Map<Long, PlaygroundRoleplayRole> cache = new ConcurrentHashMap<>();

    @EventListener(ApplicationReadyEvent.class)
    public void preload() {
        roleService.findActiveRoles()
                .collectList()
                .subscribe(this::fillCache, err -> log.warn("[role-setup] preload failed: {}", err.toString()));
    }

    public RoleSetup getSetup(String roleId) {
        String prompt = defaultPrompt();
        String voice = defaultVoice();
        try {
            if (roleId != null && !roleId.isBlank()) {
                Long id = Long.parseLong(roleId);
                PlaygroundRoleplayRole role = cache.get(id);
                if (role != null) {
                    prompt = composePrompt(role, prompt);
                    if (role.getVoice() != null) {
                        voice = role.getVoice().getValue();
                    }
                }
            }
        } catch (Exception ignore) {
            // keep defaults
        }
        return new RoleSetup(prompt, voice);
    }

    private void fillCache(List<PlaygroundRoleplayRole> roles) {
        cache.clear();
        for (PlaygroundRoleplayRole r : roles) {
            if (r.getId() != null) {
                cache.put(r.getId(), r);
            }
        }
        log.info("[role-setup] cached roles: {}", cache.size());
    }

    private String defaultVoice() {
        String v = properties.getVoice();
        return (v == null || v.isBlank()) ? "Cherry" : v;
    }

    private String defaultPrompt() {
        return "智能助手，说话友好自然，善于解答各种问题";
    }

    private String composePrompt(PlaygroundRoleplayRole role, String fallback) {
        if (role == null) return fallback;
        String name = role.getName();
        String desc = role.getDescription();
        if (desc != null && !desc.isBlank()) {
            if (name != null && !name.isBlank()) return name + "：" + desc;
            return desc;
        }
        if (name != null && !name.isBlank()) return name + "，智能助手";
        return fallback;
    }

    @Data
    @AllArgsConstructor
    public static class RoleSetup {
        private String prompt;
        private String voice;
    }
}
