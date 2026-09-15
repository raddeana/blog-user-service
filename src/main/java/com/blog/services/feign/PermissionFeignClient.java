package com.blog.services.feign;

import com.blog.services.common.Result;
import com.blog.services.permissions.models.dto.PermissionDTO;
import com.blog.services.permissions.models.vo.PermissionVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限服务 Feign 客户端
 * <p>
 * 镜像 PermissionsController 的 REST API，通过服务名 blog-service 路由。
 * 未来拆分为独立 permission-service 后，只需修改 name 为 permission-service。
 */
@FeignClient(name = "blog-service", path = "/api/permissions")
public interface PermissionFeignClient {

    @GetMapping
    Result<List<PermissionDTO>> listPermissions();

    @GetMapping("/{id}")
    Result<PermissionDTO> getPermissionById(@PathVariable("id") Long id);

    @PostMapping
    Result<PermissionDTO> createPermission(@RequestBody PermissionVO vo);

    @PutMapping("/{id}")
    Result<PermissionDTO> updatePermission(@PathVariable("id") Long id, @RequestBody PermissionVO vo);

    @DeleteMapping("/{id}")
    Result<Void> deletePermission(@PathVariable("id") Long id);
}
