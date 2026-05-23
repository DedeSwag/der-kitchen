package com.der.kitchen.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.der.kitchen.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
