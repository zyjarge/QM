package com.qm.org;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qm.common.exception.BizException;
import com.qm.org.entity.User;
import com.qm.org.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    public User getByFeishuOpenId(String openId) {
        return userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getFeishuOpenId, openId)
                .last("LIMIT 1"));
    }

    @Transactional
    public User createFromFeishu(String openId, String name, String email) {
        User user = new User();
        user.setFeishuOpenId(openId);
        user.setName(name);
        user.setEmail(email);
        user.setRole("REQUESTER");
        user.setStatus("active");
        userMapper.insert(user);
        log.info("User created from feishu: openId={} name={}", openId, name);
        return user;
    }

    public User getById(String id) {
        return userMapper.selectById(id);
    }
}
