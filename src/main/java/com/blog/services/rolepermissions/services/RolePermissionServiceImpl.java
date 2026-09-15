package com.blog.services.rolepermissions.services;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.services.common.Result;
import com.blog.services.feign.PermissionFeignClient;
import com.blog.services.feign.RoleFeignClient;
import com.blog.services.permissions.models.dto.PermissionDTO;
import com.blog.services.rolepermissions.mappers.RolePermissionMapper;
import com.blog.services.rolepermissions.models.RolePermission;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 角色-权限关联服务实现类
 * <p>
 * 通过 Feign 客户端调用角色服务和权限服务，替代直接注入 Service/Mapper。
 * 未来拆分为独立微服务时，只需修改 @FeignClient(name=...) 即可。
 */
@Service
public class RolePermissionServiceImpl implements RolePermissionService {

    @Resource
    private RolePermissionMapper rolePermissionMapper;

    @Resource
    private RoleFeignClient roleFeignClient;

    @Resource
    private PermissionFeignClient permissionFeignClient;

    @Override
    public List<PermissionDTO> getRolePermissions(Long roleId) {
        List<PermissionDTO> result = new ArrayList<>();
        List<RolePermission> relations = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId));
        for (RolePermission relation : relations) {
            Result<PermissionDTO> permResult = permissionFeignClient.getPermissionById(relation.getPermissionId());
            if (permResult != null && permResult.getCode() == 200 && permResult.getData() != null) {
                result.add(permResult.getData());
            }
        }
        return result;
    }

    @Override
    public List<PermissionDTO> assignPermissions(Long roleId, List<Long> permissionIds) {
        // 通过 Feign 校验角色存在
        Result<?> roleResult = roleFeignClient.getRoleById(roleId);
        if (roleResult == null || roleResult.getCode() != 200 || roleResult.getData() == null) {
            throw new IllegalArgumentException("角色不存在，ID: " + roleId);
        }
        for (Long permissionId : permissionIds) {
            // 通过 Feign 校验权限存在
            Result<PermissionDTO> permResult = permissionFeignClient.getPermissionById(permissionId);
            if (permResult == null || permResult.getCode() != 200 || permResult.getData() == null) {
                continue;
            }
            // 已存在关联则跳过（唯一约束兜底）
            Long exists = rolePermissionMapper.selectCount(
                    new LambdaQueryWrapper<RolePermission>()
                            .eq(RolePermission::getRoleId, roleId)
                            .eq(RolePermission::getPermissionId, permissionId));
            if (exists != null && exists > 0) {
                continue;
            }
            RolePermission relation = new RolePermission();
            relation.setRoleId(roleId);
            relation.setPermissionId(permissionId);
            rolePermissionMapper.insert(relation);
        }
        return getRolePermissions(roleId);
    }

    @Override
    public boolean removePermission(Long roleId, Long permissionId) {
        int rows = rolePermissionMapper.delete(
                new LambdaQueryWrapper<RolePermission>()
                        .eq(RolePermission::getRoleId, roleId)
                        .eq(RolePermission::getPermissionId, permissionId));
        return rows > 0;
    }
}
