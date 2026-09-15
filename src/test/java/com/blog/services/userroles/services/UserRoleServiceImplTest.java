package com.blog.services.userroles.services;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.services.common.Result;
import com.blog.services.feign.RoleFeignClient;
import com.blog.services.feign.UserFeignClient;
import com.blog.services.roles.models.dto.RoleDTO;
import com.blog.services.userroles.mappers.UserRoleMapper;
import com.blog.services.userroles.models.UserRole;
import com.blog.services.users.models.dto.UserDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 用户-角色关联服务单元测试
 * <p>
 * Mock Feign 客户端（UserFeignClient / RoleFeignClient），替代直接 Mock Service/Mapper。
 */
@ExtendWith(MockitoExtension.class)
class UserRoleServiceImplTest {

    @Mock
    private UserRoleMapper userRoleMapper;

    @Mock
    private UserFeignClient userFeignClient;

    @Mock
    private RoleFeignClient roleFeignClient;

    @InjectMocks
    private UserRoleServiceImpl userRoleService;

    @Test
    void getUserRoles_empty() {
        when(userRoleMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<RoleDTO> result = userRoleService.getUserRoles(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getUserRoles_hasData() {
        UserRole rel1 = new UserRole();
        rel1.setUserId(1L);
        rel1.setRoleId(10L);
        UserRole rel2 = new UserRole();
        rel2.setUserId(1L);
        rel2.setRoleId(20L);

        RoleDTO role1 = new RoleDTO();
        role1.setId(10L);
        role1.setRoleName("管理员");
        RoleDTO role2 = new RoleDTO();
        role2.setId(20L);
        role2.setRoleName("编辑");

        when(userRoleMapper.selectList(any())).thenReturn(Arrays.asList(rel1, rel2));
        when(roleFeignClient.getRoleById(10L)).thenReturn(Result.success(role1));
        when(roleFeignClient.getRoleById(20L)).thenReturn(Result.success(role2));

        List<RoleDTO> result = userRoleService.getUserRoles(1L);

        assertEquals(2, result.size());
        assertEquals("管理员", result.get(0).getRoleName());
        assertEquals("编辑", result.get(1).getRoleName());
    }

    @Test
    void getUserRoles_roleDeleted() {
        // 关联存在但角色已被删除，Feign 返回 404，应跳过
        UserRole rel = new UserRole();
        rel.setUserId(1L);
        rel.setRoleId(99L);

        when(userRoleMapper.selectList(any())).thenReturn(Collections.singletonList(rel));
        when(roleFeignClient.getRoleById(99L)).thenReturn(Result.notFound("角色不存在"));

        List<RoleDTO> result = userRoleService.getUserRoles(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void assignRoles_success() {
        UserDTO user = new UserDTO();
        user.setId(1L);
        when(userFeignClient.getUserById(1L)).thenReturn(Result.success(user));

        RoleDTO role1 = new RoleDTO();
        role1.setId(10L);
        role1.setRoleName("管理员");
        RoleDTO role2 = new RoleDTO();
        role2.setId(20L);
        role2.setRoleName("编辑");

        when(roleFeignClient.getRoleById(10L)).thenReturn(Result.success(role1));
        when(roleFeignClient.getRoleById(20L)).thenReturn(Result.success(role2));
        // 不存在关联
        when(userRoleMapper.selectCount(any())).thenReturn(0L);
        when(userRoleMapper.insert(any(UserRole.class))).thenReturn(1);

        // 查询返回
        UserRole rel1 = new UserRole();
        rel1.setRoleId(10L);
        UserRole rel2 = new UserRole();
        rel2.setRoleId(20L);
        when(userRoleMapper.selectList(any())).thenReturn(Arrays.asList(rel1, rel2));

        List<RoleDTO> result = userRoleService.assignRoles(1L, Arrays.asList(10L, 20L));

        assertEquals(2, result.size());
        verify(userRoleMapper, times(2)).insert(any(UserRole.class));
    }

    @Test
    void assignRoles_userNotFound() {
        when(userFeignClient.getUserById(999L)).thenReturn(Result.notFound("用户不存在"));

        assertThrows(IllegalArgumentException.class, () ->
                userRoleService.assignRoles(999L, Arrays.asList(10L)));
    }

    @Test
    void assignRoles_roleNotFound_filteredOut() {
        UserDTO user = new UserDTO();
        user.setId(1L);
        when(userFeignClient.getUserById(1L)).thenReturn(Result.success(user));

        // 角色不存在，Feign 返回 404，应跳过不报错
        when(roleFeignClient.getRoleById(999L)).thenReturn(Result.notFound("角色不存在"));
        when(userRoleMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<RoleDTO> result = userRoleService.assignRoles(1L, Collections.singletonList(999L));

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRoleMapper, never()).insert(any(UserRole.class));
    }

    @Test
    void assignRoles_alreadyAssociated_skipped() {
        UserDTO user = new UserDTO();
        user.setId(1L);
        when(userFeignClient.getUserById(1L)).thenReturn(Result.success(user));

        RoleDTO role = new RoleDTO();
        role.setId(10L);
        when(roleFeignClient.getRoleById(10L)).thenReturn(Result.success(role));
        // 已存在关联，应跳过 insert
        when(userRoleMapper.selectCount(any())).thenReturn(1L);

        // 查询返回已关联角色
        UserRole rel = new UserRole();
        rel.setRoleId(10L);
        when(userRoleMapper.selectList(any())).thenReturn(Collections.singletonList(rel));

        List<RoleDTO> result = userRoleService.assignRoles(1L, Collections.singletonList(10L));

        assertEquals(1, result.size());
        verify(userRoleMapper, never()).insert(any(UserRole.class));
    }

    @Test
    void removeRole_success() {
        when(userRoleMapper.delete(any())).thenReturn(1);

        boolean removed = userRoleService.removeRole(1L, 10L);

        assertTrue(removed);
    }

    @Test
    void removeRole_notAssociated() {
        when(userRoleMapper.delete(any())).thenReturn(0);

        boolean removed = userRoleService.removeRole(1L, 10L);

        assertFalse(removed);
    }
}
