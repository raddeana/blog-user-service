package com.blog.services.feign;

import com.blog.services.common.Result;
import com.blog.services.users.models.dto.UserDTO;
import com.blog.services.users.models.vo.UserVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户服务 Feign 客户端
 * <p>
 * 镜像 UsersController 的 REST API，通过服务名 blog-service 路由。
 * 未来拆分为独立 user-service 后，只需修改 name 为 user-service。
 */
@FeignClient(name = "blog-service", path = "/api/users")
public interface UserFeignClient {

    @GetMapping
    Result<List<UserDTO>> listUsers();

    @GetMapping("/{id}")
    Result<UserDTO> getUserById(@PathVariable("id") Long id);

    @PostMapping
    Result<UserDTO> createUser(@RequestBody UserVO vo);

    @PutMapping("/{id}")
    Result<UserDTO> updateUser(@PathVariable("id") Long id, @RequestBody UserVO vo);

    @DeleteMapping("/{id}")
    Result<Void> deleteUser(@PathVariable("id") Long id);
}
