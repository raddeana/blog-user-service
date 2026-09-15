package com.blog.services.feign;

import com.blog.services.common.Result;
import com.blog.services.roles.models.dto.RoleDTO;
import com.blog.services.roles.models.vo.CreateRoleVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色服务 Feign 客户端
 * <p>
 * 镜像 RolesController 的 REST API，通过服务名 blog-service 路由。
 * 未来拆分为独立 role-service 后，只需修改 name 为 role-service。
 */
@FeignClient(name = "blog-service", path = "/api/roles")
public interface RoleFeignClient {

    @GetMapping
    Result<List<RoleDTO>> listRoles();

    @GetMapping("/{id}")
    Result<RoleDTO> getRoleById(@PathVariable("id") Long id);

    @PostMapping
    Result<RoleDTO> createRole(@RequestBody CreateRoleVO vo);

    @PutMapping("/{id}")
    Result<RoleDTO> updateRole(@PathVariable("id") Long id, @RequestBody CreateRoleVO vo);

    @DeleteMapping("/{id}")
    Result<Void> deleteRole(@PathVariable("id") Long id);
}
