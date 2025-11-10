package com.biometric.serv.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biometric.serv.dto.UserDTO;
import com.biometric.serv.entity.User;
import com.biometric.serv.service.UserService;
import com.biometric.serv.vo.ResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 创建用户
     */
    @PostMapping
    public ResultVO<User> createUser(@Validated @RequestBody UserDTO userDTO) {
        try {
            User user = userService.createUser(userDTO);
            return ResultVO.success("用户创建成功", user);
        } catch (Exception e) {
            log.error("Create user failed", e);
            return ResultVO.error(e.getMessage());
        }
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    public ResultVO<User> updateUser(@PathVariable Long id, 
                                     @Validated @RequestBody UserDTO userDTO) {
        try {
            User user = userService.updateUser(id, userDTO);
            return ResultVO.success("用户更新成功", user);
        } catch (Exception e) {
            log.error("Update user failed", e);
            return ResultVO.error(e.getMessage());
        }
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public ResultVO<Boolean> deleteUser(@PathVariable Long id) {
        try {
            boolean success = userService.deleteUser(id);
            return ResultVO.success("用户删除成功", success);
        } catch (Exception e) {
            log.error("Delete user failed", e);
            return ResultVO.error(e.getMessage());
        }
    }

    /**
     * 查询用户详情
     */
    @GetMapping("/{id}")
    public ResultVO<User> getUser(@PathVariable Long id) {
        try {
            User user = userService.getUserById(id);
            if (user == null) {
                return ResultVO.error("用户不存在");
            }
            return ResultVO.success(user);
        } catch (Exception e) {
            log.error("Get user failed", e);
            return ResultVO.error(e.getMessage());
        }
    }

    /**
     * 分页查询用户列表
     */
    @GetMapping("/list")
    public ResultVO<Page<User>> listUsers(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        try {
            Page<User> page = userService.listUsers(pageNum, pageSize, keyword);
            return ResultVO.success(page);
        } catch (Exception e) {
            log.error("List users failed", e);
            return ResultVO.error(e.getMessage());
        }
    }
}

