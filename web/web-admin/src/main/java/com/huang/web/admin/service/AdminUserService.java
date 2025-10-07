package com.huang.web.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huang.web.admin.dto.user.UserQueryDTO;
import com.huang.web.admin.dto.user.UserStatusUpdateDTO;
import com.huang.web.admin.dto.user.UserRoleAssignDTO;
import com.huang.web.admin.vo.user.UserListVO;
import com.huang.web.admin.vo.user.UserDetailVO;
import com.huang.web.admin.vo.user.UserStatisticsVO;

/**
 * Admin端用户业务服务接口
 * @author system
 * @since 2025-01-24
 */
public interface AdminUserService {

    /**
     * 分页查询用户列表
     * @param queryDTO 查询条件
     * @return 分页用户列表
     */
    Page<UserListVO> getUserList(UserQueryDTO queryDTO);

    /**
     * 获取用户详情
     * @param userId 用户ID
     * @return 用户详情
     */
    UserDetailVO getUserDetail(Long userId);

    /**
     * 更新用户状态
     * @param statusUpdateDTO 状态更新参数
     * @return 更新结果
     */
    boolean updateUserStatus(UserStatusUpdateDTO statusUpdateDTO);

    /**
     * 分配用户角色
     * @param assignDTO 角色分配参数
     * @return 分配结果
     */
    boolean assignUserRoles(UserRoleAssignDTO assignDTO);

    /**
     * 获取用户统计数据
     * @return 统计数据
     */
    UserStatisticsVO getUserStatistics();
}