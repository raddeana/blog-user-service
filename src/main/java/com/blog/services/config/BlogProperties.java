package com.blog.services.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * Nacos 配置读取示例二：@ConfigurationProperties 批量读取（类型安全）
 * <p>
 * 与 @Value 逐个注入不同，@ConfigurationProperties 将同前缀的配置项
 * 绑定到一个 POJO，适合配置项较多的场景：
 * - 类型安全（编译期检查）
 * - 支持嵌套结构、集合、Map
 * - 配合 @RefreshScope 同样支持动态刷新
 * <p>
 * 对应 Nacos 配置（blog-service-dev.yaml）：
 * <pre>
 * blog:
 *   feature:
 *     user-register-enabled: true
 *     max-roles-per-user: 10
 *   datasource:
 *     remark: 本地开发环境主库
 * </pre>
 */
@Component
@RefreshScope
@ConfigurationProperties(prefix = "blog")
public class BlogProperties {

    /**
     * blog.feature.* 子配置
     */
    private Feature feature = new Feature();

    /**
     * blog.datasource.* 子配置
     */
    private Datasource datasource = new Datasource();

    public Feature getFeature() {
        return feature;
    }

    public void setFeature(Feature feature) {
        this.feature = feature;
    }

    public Datasource getDatasource() {
        return datasource;
    }

    public void setDatasource(Datasource datasource) {
        this.datasource = datasource;
    }

    /**
     * blog.feature.* 配置项
     */
    public static class Feature {
        /** 用户注册开关，默认开启 */
        private boolean userRegisterEnabled = true;

        /** 每用户最大角色数，默认 5 */
        private int maxRolesPerUser = 5;

        public boolean isUserRegisterEnabled() {
            return userRegisterEnabled;
        }

        public void setUserRegisterEnabled(boolean userRegisterEnabled) {
            this.userRegisterEnabled = userRegisterEnabled;
        }

        public int getMaxRolesPerUser() {
            return maxRolesPerUser;
        }

        public void setMaxRolesPerUser(int maxRolesPerUser) {
            this.maxRolesPerUser = maxRolesPerUser;
        }
    }

    /**
     * blog.datasource.* 配置项
     */
    public static class Datasource {
        /** 数据源备注说明 */
        private String remark = "未配置";

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }
}
