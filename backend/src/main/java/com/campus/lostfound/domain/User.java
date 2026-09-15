package com.campus.lostfound.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户账号实体，映射数据库表 {@code sys_user}。
 *
 * <p>表名带 {@code sys_} 前缀是数据库保留字规避（{@code user} 在部分数据库中是关键字）。
 *
 * <p>本类只作持久化载体，不包含任何业务规则；账号的注册、登录与角色判断都在
 * {@code AuthService} 中。对外输出时必须经 {@code AuthDtos.UserView} 裁剪，
 * <b>不要直接把本对象放进 API 响应</b>，否则密码摘要会随之外泄。
 */
@Data
@TableName("sys_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录名，全表唯一，注册后不可修改。 */
    private String username;

    /** BCrypt 密码摘要，非明文；任何出口都不得包含此字段。 */
    private String password;

    /** 角色标识，取值 {@code USER} / {@code ADMIN}，对应 Spring Security 的 {@code ROLE_*} 权限。 */
    private String role;

    /** 联系方式，注册时选填，用于失物归还时联系本人。 */
    private String contact;

    private LocalDateTime createdAt;
}
