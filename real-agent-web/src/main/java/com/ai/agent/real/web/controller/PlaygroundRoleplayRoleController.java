package com.ai.agent.real.web.controller;

import com.ai.agent.real.common.protocol.*;
import com.ai.agent.real.domain.entity.roleplay.PlaygroundRoleplayRole;
import com.ai.agent.real.application.dto.*;
import com.ai.agent.real.application.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.*;

/**
 * @author han
 * @time 2025/9/28 05:37
 */
@Slf4j
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class PlaygroundRoleplayRoleController {
    
    private final PlaygroundRoleplayRoleService roleService;
    
    /**
     * 查询角色列表
     */
    @GetMapping
    public Mono<ResponseResult<List<PlaygroundRoleplayRole>>> getRoles(@RequestParam(defaultValue = "false") boolean activeOnly) {
        if (activeOnly) {
            return roleService.findActiveRoles()
                    .collectList()
                    .map(ResponseResult::success);
        }
        return roleService.findAllRoles()
                .collectList()
                .map(ResponseResult::success);
    }
    
    /**
     * 根据ID查询角色
     */
    @GetMapping("/{id}")
    public Mono<ResponseResult<PlaygroundRoleplayRole>> getRoleById(@PathVariable Long id) {
        return roleService.findById(id)
                .map(ResponseResult::success);
    }
    

    /**
     * 创建角色
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ResponseResult<PlaygroundRoleplayRole>> createRole(@Valid @RequestBody RoleCreateRequest request) {
        return roleService.createRole(request)
                .map(ResponseResult::success);
    }
    
    /**
     * 更新角色
     */
    @PutMapping("/{id}")
    public Mono<ResponseResult<PlaygroundRoleplayRole>> updateRole(@PathVariable Long id,
                                                   @Valid @RequestBody RoleCreateRequest request) {
        return roleService.updateRole(id, request)
                .map(ResponseResult::success);
    }
    
    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<ResponseResult<Void>> deleteRole(@PathVariable Long id) {
        return roleService.deleteRole(id)
                .then(Mono.just(ResponseResult.success()));
    }
    
    /**
     * 启用/禁用角色
     */
    @PutMapping("/{id}/toggle-status")
    public Mono<ResponseResult<PlaygroundRoleplayRole>> toggleRoleStatus(@PathVariable Long id) {
        return roleService.toggleRoleStatus(id)
                .map(ResponseResult::success);
    }
}
