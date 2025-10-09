package com.huang.web.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huang.common.login.LoginUserHolder;
import com.huang.model.entity.*;
import com.huang.web.admin.dto.coach.CoachCertificationQueryDTO;
import com.huang.web.admin.dto.coach.CoachCertificationReviewDTO;
import com.huang.web.admin.mapper.*;
import com.huang.web.admin.vo.coach.CoachCertificationDetailVO;
import com.huang.web.admin.vo.coach.CoachCertificationListVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin端教练认证申请Service
 * @author system
 * @since 2025-01-24
 */
@Slf4j
@Service
public class AdminCoachCertificationService {

    @Autowired
    private CoachCertificationApplyMapper coachCertificationApplyMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CoachMapper coachMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private RoleMapper roleMapper;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd[ HH:mm:ss]")
            .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
            .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
            .toFormatter();

    /**
     * 分页查询教练认证申请列表
     */
    public Page<CoachCertificationListVO> getApplicationList(CoachCertificationQueryDTO queryDTO) {
        // 构建分页对象
        Page<CoachCertificationApply> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());

        // 构建查询条件
        LambdaQueryWrapper<CoachCertificationApply> queryWrapper = new LambdaQueryWrapper<>();

        // 真实姓名模糊查询
        if (StringUtils.hasText(queryDTO.getRealName())) {
            queryWrapper.like(CoachCertificationApply::getRealName, queryDTO.getRealName());
        }

        // 手机号模糊查询
        if (StringUtils.hasText(queryDTO.getPhone())) {
            queryWrapper.like(CoachCertificationApply::getPhone, queryDTO.getPhone());
        }

        // 状态筛选
        if (StringUtils.hasText(queryDTO.getStatus())) {
            queryWrapper.eq(CoachCertificationApply::getStatus, queryDTO.getStatus());
        }

        // 专长领域模糊查询
        if (StringUtils.hasText(queryDTO.getSpecialties())) {
            queryWrapper.like(CoachCertificationApply::getSpecialties, queryDTO.getSpecialties());
        }

        // 时间范围筛选
        if (StringUtils.hasText(queryDTO.getStartTime())) {
            LocalDateTime startTime = LocalDateTime.parse(queryDTO.getStartTime(), DATE_TIME_FORMATTER);
            queryWrapper.ge(CoachCertificationApply::getApplyTime, startTime);
        }
        if (StringUtils.hasText(queryDTO.getEndTime())) {
            LocalDateTime endTime = LocalDateTime.parse(queryDTO.getEndTime(), DATE_TIME_FORMATTER);
            queryWrapper.le(CoachCertificationApply::getApplyTime, endTime);
        }

        // 排序
        if ("asc".equalsIgnoreCase(queryDTO.getSortOrder())) {
            queryWrapper.orderByAsc(CoachCertificationApply::getApplyTime);
        } else {
            queryWrapper.orderByDesc(CoachCertificationApply::getApplyTime);
        }

        // 执行查询
        Page<CoachCertificationApply> applicationPage = coachCertificationApplyMapper.selectPage(page, queryWrapper);

        // 转换为VO
        Page<CoachCertificationListVO> resultPage = new Page<>();
        BeanUtils.copyProperties(applicationPage, resultPage);

        List<CoachCertificationListVO> applicationVOs = applicationPage.getRecords().stream().map(application -> {
            CoachCertificationListVO vo = new CoachCertificationListVO();
            BeanUtils.copyProperties(application, vo);

            // 查询用户信息
            User user = userMapper.selectById(application.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
            }

            // 查询审核人信息
            if (application.getReviewerId() != null) {
                User reviewer = userMapper.selectById(application.getReviewerId());
                if (reviewer != null) {
                    vo.setReviewerName(reviewer.getNickname() != null ? reviewer.getNickname() : reviewer.getUsername());
                }
            }

            // 设置状态描述
            vo.setStatusDesc(getStatusDesc(application.getStatus()));

            return vo;
        }).collect(Collectors.toList());

        resultPage.setRecords(applicationVOs);
        return resultPage;
    }

    /**
     * 获取教练认证申请详情
     */
    public CoachCertificationDetailVO getApplicationDetail(Long id) {
        CoachCertificationApply application = coachCertificationApplyMapper.selectById(id);
        if (application == null) {
            return null;
        }

        CoachCertificationDetailVO detailVO = new CoachCertificationDetailVO();
        BeanUtils.copyProperties(application, detailVO);

        // 查询用户信息
        User user = userMapper.selectById(application.getUserId());
        if (user != null) {
            detailVO.setUsername(user.getUsername());
            detailVO.setNickname(user.getNickname());
        }

        // 查询审核人信息
        if (application.getReviewerId() != null) {
            User reviewer = userMapper.selectById(application.getReviewerId());
            if (reviewer != null) {
                detailVO.setReviewerName(reviewer.getNickname() != null ? reviewer.getNickname() : reviewer.getUsername());
            }
        }

        // 设置状态描述
        detailVO.setStatusDesc(getStatusDesc(application.getStatus()));

        return detailVO;
    }

    /**
     * 审核教练认证申请
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean reviewApplication(CoachCertificationReviewDTO reviewDTO) {
        // 验证状态
        if (!"approved".equals(reviewDTO.getStatus()) && !"rejected".equals(reviewDTO.getStatus())) {
            throw new RuntimeException("无效的审核状态");
        }

        // 查询申请记录
        CoachCertificationApply application = coachCertificationApplyMapper.selectById(reviewDTO.getId());
        if (application == null) {
            throw new RuntimeException("申请记录不存在");
        }

        if (!"pending".equals(application.getStatus())) {
            throw new RuntimeException("该申请已经审核过，无法重复审核");
        }

        // 获取当前审核人信息
        Long currentUserId = LoginUserHolder.getLoginUser().getUserId();

        // 更新申请状态
        LambdaUpdateWrapper<CoachCertificationApply> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CoachCertificationApply::getId, reviewDTO.getId())
                    .set(CoachCertificationApply::getStatus, reviewDTO.getStatus())
                    .set(CoachCertificationApply::getReviewTime, LocalDateTime.now())
                    .set(CoachCertificationApply::getReviewerId, currentUserId)
                    .set(CoachCertificationApply::getReviewRemark, reviewDTO.getReviewRemark())
                    .set(CoachCertificationApply::getUpdateTime, LocalDateTime.now());

        // 如果审核通过，生成认证编号
        if ("approved".equals(reviewDTO.getStatus())) {
            String certificationNo = reviewDTO.getCertificationNo();
            if (!StringUtils.hasText(certificationNo)) {
                certificationNo = generateCertificationNo();
            }
            updateWrapper.set(CoachCertificationApply::getCertificationNo, certificationNo);
        }

        boolean updateResult = coachCertificationApplyMapper.update(null, updateWrapper) > 0;

        if (updateResult && "approved".equals(reviewDTO.getStatus())) {
            // 审核通过：创建教练记录并分配角色
            createCoachAndAssignRole(application, reviewDTO.getCertificationNo() != null ? 
                reviewDTO.getCertificationNo() : generateCertificationNo());
        }

        return updateResult;
    }

    /**
     * 创建教练记录并分配角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void createCoachAndAssignRole(CoachCertificationApply application, String certificationNo) {
        // 1. 创建教练记录
        Coach coach = new Coach();
        coach.setUserId(application.getUserId());
        coach.setRealName(application.getRealName());
        coach.setCertificationNo(certificationNo);
        coach.setSpecialties(application.getSpecialties());
        coach.setIntroduction(application.getIntroduction());
        coach.setExperienceYears(application.getExperienceYears());
        coach.setRating(new java.math.BigDecimal("0.0")); // 初始评分
        coach.setStatus(1); // 在职状态
        coach.setCreateTime(LocalDateTime.now());
        coach.setUpdateTime(LocalDateTime.now());
        coach.setIsDeleted((byte) 0);

        coachMapper.insert(coach);

        // 2. 分配教练角色
        Role coachRole = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getRoleCode, "coach"));
        if (coachRole != null) {
            // 检查是否已经有该角色
            UserRole existingUserRole = userRoleMapper.selectOne(
                new LambdaQueryWrapper<UserRole>()
                    .eq(UserRole::getUserId, application.getUserId())
                    .eq(UserRole::getRoleId, coachRole.getId())
            );

            if (existingUserRole == null) {
                UserRole userRole = new UserRole();
                userRole.setUserId(application.getUserId());
                userRole.setRoleId(coachRole.getId());
                userRole.setCreateTime(LocalDateTime.now());
                userRole.setUpdateTime(LocalDateTime.now());
                userRole.setIsDeleted((byte) 0);

                userRoleMapper.insert(userRole);
            }
        }

        log.info("创建教练记录成功: userId={}, certificationNo={}", application.getUserId(), certificationNo);
    }

    /**
     * 生成认证编号
     */
    private String generateCertificationNo() {
        // 格式：COACH + 年月日 + 5位序号
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        // 查询今日已生成的认证编号数量
        String prefix = "COACH" + dateStr;
        LambdaQueryWrapper<CoachCertificationApply> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(CoachCertificationApply::getCertificationNo, prefix)
               .isNotNull(CoachCertificationApply::getCertificationNo);
        
        Long count = coachCertificationApplyMapper.selectCount(wrapper);
        
        return String.format("%s%05d", prefix, count + 1);
    }

    /**
     * 获取状态描述
     */
    private String getStatusDesc(String status) {
        if (status == null) return "未知";
        switch (status) {
            case "pending": return "待审核";
            case "approved": return "已通过";
            case "rejected": return "已拒绝";
            default: return "未知";
        }
    }
}