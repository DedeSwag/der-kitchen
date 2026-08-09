package com.der.kitchen.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.der.kitchen.user.entity.User;
import com.der.kitchen.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    public User getByOpenid(String openid) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getOpenid, openid));
    }

    public User getByUsername(String username) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    public User createUser(String openid, String nickname, String avatarUrl, String role) {
        User user = new User();
        user.setOpenid(openid);
        user.setNickname(nickname);
        user.setAvatarUrl(avatarUrl);
        user.setRole(role);
        user.setStatus("active");
        userMapper.insert(user);
        return user;
    }

    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    public void updateUser(User user) {
        userMapper.updateById(user);
    }
}
