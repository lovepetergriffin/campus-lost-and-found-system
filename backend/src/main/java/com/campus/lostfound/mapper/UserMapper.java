package com.campus.lostfound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.lostfound.domain.User;

/**
 * {@link User} 的数据访问接口。
 *
 * <p>继承 {@code BaseMapper} 即获得按主键增删改查、条件构造器查询等通用能力，
 * MyBatis-Plus 会在扫描到本接口时自动生成实现，无需 XML 或手写 SQL。
 * 目前账号相关的查询条件都简单到可以用 {@code LambdaQueryWrapper} 表达，
 * 因此这里不需要定义额外方法。
 */
public interface UserMapper extends BaseMapper<User> {
}
