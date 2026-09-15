package com.blog.services.userroles.services;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.services.common.Result;
import com.blog.services.feign.RoleFeignClient;
import com.blog.services.feign.UserFeignClient;
import com.blog.services.roles.models.dto.RoleDTO;
import com.blog.services.userroles.mappers.UserRoleMapper;
import com.blog.services.userroles.models.UserRole;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户-角色关联服务实现类
 * <p>
 * 通过 Feign 客户端调用用户服务和角色服务，替代直接注入 Service/Mapper。
 * 未来拆分为独立微服务时，只需修改 @FeignClient(name=...) 即可。
 */
@Service
public class UserRoleServiceImpl implements UserRoleService {

    @Resource
    private UserRoleMapper userRoleMapper;

    @Resource
    private UserFeignClient userFeignClient;

    @Resource
    private RoleFeignClient roleFeignClient;

    @Override
    public List<RoleDTO> getUserRoles(Long userId) {
        List<RoleDTO> result = new ArrayList<>();
        List<UserRole> relations = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        for (UserRole relation : relations) {
            Result<RoleDTO> roleResult = roleFeignClient.getRoleById(relation.getRoleId());
            if (roleResult != null && roleResult.getCode() == 200 && roleResult.getData() != null) {
                result.add(roleResult.getData());
            }
        }
        return result;
    }

    @Override
    public List<RoleDTO> assignRoles(Long userId, List<Long> roleIds) {
        // 通过 Feign 校验用户存在
        Result<?> userResult = userFeignClient.getUserById(userId);
        if (userResult == null || userResult.getCode() != 200 || userResult.getData() == null) {
            throw new IllegalArgumentException("用户不存在，ID: " + userId);
        }
        for (Long roleId : roleIds) {
            // 通过 Feign 校验角色存在
            Result<RoleDTO> roleResult = roleFeignClient.getRoleById(roleId);
            if (roleResult == null || roleResult.getCode() != 200 || roleResult.getData() == null) {
                continue;
            }
            // 已存在关联则跳过（唯一约束兜底）
            Long exists = userRoleMapper.selectCount(
                    new LambdaQueryWrapper<UserRole>()
                            .eq(UserRole::getUserId, userId)
                            .eq(UserRole::getRoleId, roleId));
            if (exists != null && exists > 0) {
                continue;
            }
            UserRole relation = new UserRole();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            userRoleMapper.insert(relation);
        }
        return getUserRoles(userId);
    }

    @Override
    public boolean removeRole(Long userId, Long roleId) {
        int rows = userRoleMapper.delete(
                new LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getUserId, userId)
                        .eq(UserRole::getRoleId, roleId));
        return rows > 0;
    }
}
