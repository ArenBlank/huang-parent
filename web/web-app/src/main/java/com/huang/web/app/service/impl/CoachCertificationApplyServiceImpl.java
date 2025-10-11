package com.huang.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huang.common.login.LoginUserHolder;
import com.huang.model.entity.CoachCertificationApply;
import com.huang.web.app.dto.coach.CoachCertificationApplyDTO;
import com.huang.web.app.mapper.CoachCertificationApplyMapper;
import com.huang.web.app.service.CoachCertificationApplyService;
import com.huang.web.app.vo.coach.CoachCertificationApplyVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CoachCertificationApply服务实现类
 * @author system
 * @since 2025-01-24
 */
@Slf4j
@Service
public class CoachCertificationApplyServiceImpl extends ServiceImpl<CoachCertificationApplyMapper, CoachCertificationApply> implements CoachCertificationApplyService {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 提交教练认证申请
     */
    @Transactional(rollbackFor = Exception.class)
    public CoachCertificationApplyVO submitApplication(CoachCertificationApplyDTO dto) {
        Long currentUserId = LoginUserHolder.getLoginUser().getUserId();
        log.info("提交教练认证申请: 用户ID={}, 真实姓名={}", currentUserId, dto.getRealName());
        
        // 1. 检查是否已有待审核的申请
        LambdaQueryWrapper<CoachCertificationApply> existingWrapper = new LambdaQueryWrapper<>();
        existingWrapper.eq(CoachCertificationApply::getUserId, currentUserId)
                      .eq(CoachCertificationApply::getStatus, "pending");
        
        CoachCertificationApply existingApply = this.getOne(existingWrapper);
        if (existingApply != null) {
            throw new RuntimeException("您已有待审核的教练认证申请，请等待审核结果");
        }
        
        // 2. 检查是否已通过认证
        LambdaQueryWrapper<CoachCertificationApply> approvedWrapper = new LambdaQueryWrapper<>();
        approvedWrapper.eq(CoachCertificationApply::getUserId, currentUserId)
                      .eq(CoachCertificationApply::getStatus, "approved");
        
        CoachCertificationApply approvedApply = this.getOne(approvedWrapper);
        if (approvedApply != null) {
            throw new RuntimeException("您已通过教练认证，无需重复申请");
        }
        
        // 3. 创建申请记录
        CoachCertificationApply apply = new CoachCertificationApply();
        BeanUtils.copyProperties(dto, apply);
        apply.setUserId(currentUserId);
        apply.setStatus("pending");
        apply.setApplyTime(LocalDateTime.now());
        apply.setCreateTime(LocalDateTime.now());
        apply.setUpdateTime(LocalDateTime.now());
        apply.setIsDeleted((byte) 0);
        
        // 4. 处理JSON字段
        try {
            if (dto.getCertificates() != null) {
                apply.setCertificates(objectMapper.writeValueAsString(dto.getCertificates()));
            }
            if (dto.getEducation() != null) {
                apply.setEducation(objectMapper.writeValueAsString(dto.getEducation()));
            }
            if (dto.getWorkExperience() != null) {
                apply.setWorkExperience(objectMapper.writeValueAsString(dto.getWorkExperience()));
            }
        } catch (Exception e) {
            log.error("JSON序列化失败", e);
            throw new RuntimeException("申请信息处理失败");
        }
        
        // 5. 保存申请
        boolean saveResult = this.save(apply);
        if (!saveResult) {
            throw new RuntimeException("提交申请失败，请稍后重试");
        }
        
        log.info("教练认证申请提交成功: 申请ID={}, 用户ID={}", apply.getId(), currentUserId);
        
        // 6. 返回结果
        return buildApplyVO(apply);
    }
    
    /**
     * 获取当前用户的申请状态
     */
    public CoachCertificationApplyVO getCurrentUserApplication() {
        Long currentUserId = LoginUserHolder.getLoginUser().getUserId();
        log.info("查询用户申请状态: 用户ID={}", currentUserId);
        
        LambdaQueryWrapper<CoachCertificationApply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CoachCertificationApply::getUserId, currentUserId)
               .orderByDesc(CoachCertificationApply::getApplyTime)
               .last("LIMIT 1");
        
        CoachCertificationApply apply = this.getOne(wrapper);
        if (apply == null) {
            return null;
        }
        
        return buildApplyVO(apply);
    }
    
    /**
     * 获取用户所有申请记录
     */
    public List<CoachCertificationApplyVO> getUserApplicationHistory() {
        Long currentUserId = LoginUserHolder.getLoginUser().getUserId();
        log.info("查询用户申请历史: 用户ID={}", currentUserId);
        
        LambdaQueryWrapper<CoachCertificationApply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CoachCertificationApply::getUserId, currentUserId)
               .orderByDesc(CoachCertificationApply::getApplyTime);
        
        List<CoachCertificationApply> applies = this.list(wrapper);
        
        return applies.stream().map(this::buildApplyVO).toList();
    }
    
    /**
     * 取消申请
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelApplication(Long applicationId, String cancelReason) {
        Long currentUserId = LoginUserHolder.getLoginUser().getUserId();
        log.info("取消教练认证申请: 申请ID={}, 用户ID={}, 取消原因={}", applicationId, currentUserId, cancelReason);
        
        // 1. 查询申请记录
        CoachCertificationApply apply = this.getById(applicationId);
        if (apply == null) {
            throw new RuntimeException("申请记录不存在");
        }
        
        // 2. 验证是否为当前用户的申请
        if (!apply.getUserId().equals(currentUserId)) {
            throw new RuntimeException("只能取消自己的申请");
        }
        
        // 3. 验证申请状态
        if (!"pending".equals(apply.getStatus())) {
            throw new RuntimeException("只能取消待审核状态的申请");
        }
        
        // 4. 更新申请状态
        LambdaUpdateWrapper<CoachCertificationApply> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CoachCertificationApply::getId, applicationId)
                    .set(CoachCertificationApply::getStatus, "cancelled")
                    .set(CoachCertificationApply::getReviewRemark, cancelReason != null ? "用户主动取消: " + cancelReason : "用户主动取消")
                    .set(CoachCertificationApply::getUpdateTime, LocalDateTime.now());
        
        boolean updateResult = this.update(updateWrapper);
        
        if (updateResult) {
            log.info("教练认证申请取消成功: 申请ID={}, 用户ID={}", applicationId, currentUserId);
        } else {
            log.error("教练认证申请取消失败: 申请ID={}, 用户ID={}", applicationId, currentUserId);
        }
        
        return updateResult;
    }
    
    /**
     * 构建申请VO
     */
    private CoachCertificationApplyVO buildApplyVO(CoachCertificationApply apply) {
        CoachCertificationApplyVO vo = new CoachCertificationApplyVO();
        BeanUtils.copyProperties(apply, vo);
        
        // 设置状态描述
        vo.setStatusDesc(getStatusDesc(apply.getStatus()));
        
        // 设置操作权限
        vo.setCanCancel("pending".equals(apply.getStatus()));
        vo.setCanReapply("rejected".equals(apply.getStatus()) || "cancelled".equals(apply.getStatus()));
        
        // 解析JSON字段
        try {
            if (apply.getCertificates() != null) {
                vo.setCertificates(objectMapper.readValue(apply.getCertificates(), 
                    new TypeReference<List<CoachCertificationApplyVO.CertificateInfo>>() {}));
            }
            if (apply.getEducation() != null) {
                vo.setEducation(objectMapper.readValue(apply.getEducation(), 
                    new TypeReference<List<CoachCertificationApplyVO.EducationInfo>>() {}));
            }
            if (apply.getWorkExperience() != null) {
                vo.setWorkExperience(objectMapper.readValue(apply.getWorkExperience(), 
                    new TypeReference<List<CoachCertificationApplyVO.WorkExperience>>() {}));
            }
        } catch (Exception e) {
            log.error("JSON反序列化失败", e);
        }
        
        return vo;
    }
    
    /**
     * 获取状态描述
     */
    private String getStatusDesc(String status) {
        if (status == null) return "未知";
        return switch (status) {
            case "pending" -> "待审核";
            case "approved" -> "已通过";
            case "rejected" -> "已拒绝";
            case "cancelled" -> "已取消";
            default -> "未知";
        };
    }
}