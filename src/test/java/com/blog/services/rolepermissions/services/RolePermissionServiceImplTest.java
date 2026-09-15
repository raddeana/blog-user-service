package com.blog.services.rolepermissions.services;

import com.blog.services.common.Result;
import com.blog.services.feign.PermissionFeignClient;
import com.blog.services.feign.RoleFeignClient;
import com.blog.services.permissions.models.dto.PermissionDTO;
import com.blog.services.rolepermissions.mappers.RolePermissionMapper;
import com.blog.services.rolepermissions.models.RolePermission;
import com.blog.services.roles.models.dto.RoleDTO;
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
 * 角色-权限关联服务单元测试
 * <p>
 * Mock Feign 客户端（RoleFeignClient / PermissionFeignClient），替代直接 Mock Service/Mapper。
 */
@ExtendWith(MockitoExtension.class)
class RolePermissionServiceImplTest {

    @Mock
    private RolePermissionMapper rolePermissionMapper;

    @Mock
    private RoleFeignClient roleFeignClient;

    @Mock
    private PermissionFeignClient permissionFeignClient;

    @InjectMocks
    private RolePermissionServiceImpl rolePermissionService;

    @Test
    void getRolePermissions_empty() {
        when(rolePermissionMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<PermissionDTO> result = rolePermissionService.getRolePermissions(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getRolePermissions_hasData() {
        RolePermission rel1 = new RolePermission();
        rel1.setRoleId(1L);
        rel1.setPermissionId(10L);
        RolePermission rel2 = new RolePermission();
        rel2.setRoleId(1L);
        rel2.setPermissionId(20L);

        PermissionDTO perm1 = new PermissionDTO();
        perm1.setId(10L);
        perm1.setPermissionName("文章查看");
        PermissionDTO perm2 = new PermissionDTO();
        perm2.setId(20L);
        perm2.setPermissionName("文章发布");

        when(rolePermissionMapper.selectList(any())).thenReturn(Arrays.asList(rel1, rel2));
        when(permissionFeignClient.getPermissionById(10L)).thenReturn(Result.success(perm1));
        when(permissionFeignClient.getPermissionById(20L)).thenReturn(Result.success(perm2));

        List<PermissionDTO> result = rolePermissionService.getRolePermissions(1L);

        assertEquals(2, result.size());
        assertEquals("文章查看", result.get(0).getPermissionName());
        assertEquals("文章发布", result.get(1).getPermissionName());
    }

    @Test
    void getRolePermissions_permissionDeleted() {
        RolePermission rel = new RolePermission();
        rel.setRoleId(1L);
        rel.setPermissionId(99L);

        when(rolePermissionMapper.selectList(any())).thenReturn(Collections.singletonList(rel));
        when(permissionFeignClient.getPermissionById(99L)).thenReturn(Result.notFound("权限不存在"));

        List<PermissionDTO> result = rolePermissionService.getRolePermissions(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void assignPermissions_success() {
        RoleDTO role = new RoleDTO();
        role.setId(1L);
        when(roleFeignClient.getRoleById(1L)).thenReturn(Result.success(role));

        PermissionDTO perm1 = new PermissionDTO();
        perm1.setId(10L);
        perm1.setPermissionName("查看");
        PermissionDTO perm2 = new PermissionDTO();
        perm2.setId(20L);
        perm2.setPermissionName("发布");

        when(permissionFeignClient.getPermissionById(10L)).thenReturn(Result.success(perm1));
        when(permissionFeignClient.getPermissionById(20L)).thenReturn(Result.success(perm2));
        when(rolePermissionMapper.selectCount(any())).thenReturn(0L);
        when(rolePermissionMapper.insert(any(RolePermission.class))).thenReturn(1);

        RolePermission r1 = new RolePermission();
        r1.setPermissionId(10L);
        RolePermission r2 = new RolePermission();
        r2.setPermissionId(20L);
        when(rolePermissionMapper.selectList(any())).thenReturn(Arrays.asList(r1, r2));

        List<PermissionDTO> result = rolePermissionService.assignPermissions(1L, Arrays.asList(10L, 20L));

        assertEquals(2, result.size());
        verify(rolePermissionMapper, times(2)).insert(any(RolePermission.class));
    }

    @Test
    void assignPermissions_roleNotFound() {
        when(roleFeignClient.getRoleById(999L)).thenReturn(Result.notFound("角色不存在"));

        assertThrows(IllegalArgumentException.class, () ->
                rolePermissionService.assignPermissions(999L, Arrays.asList(10L)));
    }

    @Test
    void assignPermissions_permissionNotFound_filteredOut() {
        RoleDTO role = new RoleDTO();
        role.setId(1L);
        when(roleFeignClient.getRoleById(1L)).thenReturn(Result.success(role));

        when(permissionFeignClient.getPermissionById(999L)).thenReturn(Result.notFound("权限不存在"));
        when(rolePermissionMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<PermissionDTO> result = rolePermissionService.assignPermissions(1L, Collections.singletonList(999L));

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(rolePermissionMapper, never()).insert(any(RolePermission.class));
    }

    @Test
    void assignPermissions_alreadyAssociated_skipped() {
        RoleDTO role = new RoleDTO();
        role.setId(1L);
        when(roleFeignClient.getRoleById(1L)).thenReturn(Result.success(role));

        PermissionDTO perm = new PermissionDTO();
        perm.setId(10L);
        perm.setPermissionName("查看");
        when(permissionFeignClient.getPermissionById(10L)).thenReturn(Result.success(perm));
        when(rolePermissionMapper.selectCount(any())).thenReturn(1L);

        RolePermission rel = new RolePermission();
        rel.setPermissionId(10L);
        when(rolePermissionMapper.selectList(any())).thenReturn(Collections.singletonList(rel));

        List<PermissionDTO> result = rolePermissionService.assignPermissions(1L, Collections.singletonList(10L));

        assertEquals(1, result.size());
        verify(rolePermissionMapper, never()).insert(any(RolePermission.class));
    }

    @Test
    void removePermission_success() {
        when(rolePermissionMapper.delete(any())).thenReturn(1);

        boolean removed = rolePermissionService.removePermission(1L, 10L);

        assertTrue(removed);
    }

    @Test
    void removePermission_notAssociated() {
        when(rolePermissionMapper.delete(any())).thenReturn(0);

        boolean removed = rolePermissionService.removePermission(1L, 10L);

        assertFalse(removed);
    }
}
