package com.blog.services.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Nacos 配置读取示例
 * <p>
 * 演示三种读取 Nacos 配置的方式：
 * 1. @Value 注入单个属性（支持动态刷新）
 * 2. @RefreshScope 标注的 Bean，配置变更后自动重建
 * 3. @PostConstruct 启动时打印配置值，验证配置已加载
 * <p>
 * 测试方式：
 * 1. 在 Nacos 的 blog-service-dev.yaml 中加入自定义配置项（见下方示例）
 * 2. 重启服务，观察日志输出
 * 3. 在 Nacos 修改配置值，观察日志自动刷新（无需重启）
 * <p>
 * Nacos 配置示例（在 blog-service-dev.yaml 中追加）：
 * <pre>
 * blog:
 *   datasource:
 *     remark: 生产环境主库
 *   feature:
 *     user-register-enabled: true
 *     max-roles-per-user: 10
 * </pre>
 */
@Configuration
@RefreshScope
public class NacosConfigDemo {

    private static final Logger log = LoggerFactory.getLogger(NacosConfigDemo.class);

    /**
     * 方式一：@Value 注入，配合 @RefreshScope 支持动态刷新
     * 配置项：blog.feature.user-register-enabled
     */
    @Value("${blog.feature.user-register-enabled:true}")
    private boolean userRegisterEnabled;

    /**
     * 配置项：blog.feature.max-roles-per-user
     */
    @Value("${blog.feature.max-roles-per-user:5}")
    private int maxRolesPerUser;

    /**
     * 配置项：blog.datasource.remark（无默认值时 Nacos 未配置会启动失败，此处给默认值）
     */
    @Value("${blog.datasource.remark:未配置}")
    private String datasourceRemark;

    /**
     * 启动时打印配置值，验证 Nacos 配置是否生效
     */
    @PostConstruct
    public void init() {
        log.info("===== Nacos 配置加载结果 =====");
        log.info("用户注册开关 (blog.feature.user-register-enabled): {}", userRegisterEnabled);
        log.info("每用户最大角色数 (blog.feature.max-roles-per-user): {}", maxRolesPerUser);
        log.info("数据源备注 (blog.datasource.remark): {}", datasourceRemark);
        log.info("=============================");
    }

    /**
     * 业务方法示例：根据 Nacos 配置控制是否允许用户注册
     */
    public boolean isUserRegisterEnabled() {
        return userRegisterEnabled;
    }

    public int getMaxRolesPerUser() {
        return maxRolesPerUser;
    }

    public String getDatasourceRemark() {
        return datasourceRemark;
    }
}
