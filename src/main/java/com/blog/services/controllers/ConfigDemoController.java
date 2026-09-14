package com.blog.services.controllers;

import com.blog.services.config.BlogProperties;
import com.blog.services.config.NacosConfigDemo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Nacos 配置读取验证 Controller
 * <p>
 * 通过 HTTP 接口实时返回当前配置值，验证三种读取方式 + 动态刷新：
 * 1. @Value（来自 NacosConfigDemo）
 * 2. @ConfigurationProperties（来自 BlogProperties）
 * 3. Environment 运行时读取
 * <p>
 * 动态刷新验证步骤：
 * 1. 启动服务后调用：curl http://localhost:8081/api/config/demo
 * 2. 在 Nacos 修改 blog.feature.max-roles-per-user 为 20，发布
 * 3. 再次调用同一接口，观察 maxRolesPerUser 已变为 20（无需重启服务）
 */
@RestController
@RequestMapping("/api/config")
public class ConfigDemoController {

    /** 方式一：@Value 注入 */
    @Autowired
    private NacosConfigDemo nacosConfigDemo;

    /** 方式二：@ConfigurationProperties 类型安全绑定 */
    @Autowired
    private BlogProperties blogProperties;

    /** 方式三：Environment 运行时读取（无需 @RefreshScope） */
    @Autowired
    private Environment environment;

    /**
     * 返回三种方式读取的配置值，用于对比验证
     */
    @GetMapping("/demo")
    public Map<String, Object> demo() {
        Map<String, Object> result = new LinkedHashMap<>();

        // 方式一：@Value
        Map<String, Object> byValue = new LinkedHashMap<>();
        byValue.put("userRegisterEnabled", nacosConfigDemo.isUserRegisterEnabled());
        byValue.put("maxRolesPerUser", nacosConfigDemo.getMaxRolesPerUser());
        byValue.put("datasourceRemark", nacosConfigDemo.getDatasourceRemark());
        result.put("1_@Value", byValue);

        // 方式二：@ConfigurationProperties
        Map<String, Object> byProps = new LinkedHashMap<>();
        byProps.put("userRegisterEnabled", blogProperties.getFeature().isUserRegisterEnabled());
        byProps.put("maxRolesPerUser", blogProperties.getFeature().getMaxRolesPerUser());
        byProps.put("datasourceRemark", blogProperties.getDatasource().getRemark());
        result.put("2_@ConfigurationProperties", byProps);

        // 方式三：Environment（每次请求实时读取，无缓存）
        Map<String, Object> byEnv = new LinkedHashMap<>();
        byEnv.put("userRegisterEnabled", environment.getProperty("blog.feature.user-register-enabled", Boolean.class, true));
        byEnv.put("maxRolesPerUser", environment.getProperty("blog.feature.max-roles-per-user", Integer.class, 5));
        byEnv.put("datasourceRemark", environment.getProperty("blog.datasource.remark", "未配置"));
        result.put("3_Environment", byEnv);

        return result;
    }
}
