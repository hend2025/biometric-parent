package com.biometric.serv.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biometric.serv.dto.UserDTO;
import com.biometric.serv.entity.User;
import com.biometric.serv.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;

/**
 * 用户服务
 */
@Slf4j
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    /**
     * 创建用户
     */
    public User createUser(UserDTO userDTO) {
        User user = new User();
        BeanUtils.copyProperties(userDTO, user);
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        
        userMapper.insert(user);
        log.info("Created user: id={}, username={}", user.getId(), user.getUsername());
        return user;
    }

    /**
     * 更新用户
     */
    public User updateUser(Long id, UserDTO userDTO) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        BeanUtils.copyProperties(userDTO, user);
        user.setUpdateTime(new Date());
        
        userMapper.updateById(user);
        log.info("Updated user: id={}", id);
        return user;
    }

    /**
     * 删除用户
     */
    public boolean deleteUser(Long id) {
        int rows = userMapper.deleteById(id);
        log.info("Deleted user: id={}, success={}", id, rows > 0);
        return rows > 0;
    }

    /**
     * 根据ID查询用户
     */
    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }

    /**
     * 分页查询用户
     */
    public Page<User> listUsers(int pageNum, int pageSize, String keyword) {
        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(keyword)) {
            wrapper.like(User::getUsername, keyword)
                   .or()
                   .like(User::getRealName, keyword)
                   .or()
                   .like(User::getMobile, keyword);
        }
        
        wrapper.orderByDesc(User::getCreateTime);
        
        return userMapper.selectPage(page, wrapper);
    }
}

