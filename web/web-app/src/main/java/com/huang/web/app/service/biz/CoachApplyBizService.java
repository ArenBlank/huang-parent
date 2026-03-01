package com.huang.web.app.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.common.login.LoginUserHolder;
import com.huang.model.entity.CoachProfile;
import com.huang.web.app.dto.coach.CoachApplyDTO;
import com.huang.web.app.mapper.CoachProfileMapper;
import com.huang.web.app.vo.coach.MyCoachApplyVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;

@Service
public class CoachApplyBizService {

    private static final int CERT_PENDING = 0;
    private static final int CERT_APPROVED = 1;
    private static final int CERT_REJECTED = 2;
    private static final SimpleDateFormat DATETIME_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final CoachProfileMapper coachProfileMapper;

    public CoachApplyBizService(CoachProfileMapper coachProfileMapper) {
        this.coachProfileMapper = coachProfileMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long apply(CoachApplyDTO dto) {
        Long userId = LoginUserHolder.getLoginUser().getUserId();
        CoachProfile exists = coachProfileMapper.selectOne(new LambdaQueryWrapper<CoachProfile>()
                .eq(CoachProfile::getUserId, userId)
                .last("LIMIT 1"));
        if (exists == null) {
            CoachProfile profile = new CoachProfile();
            profile.setUserId(userId);
            fillApplyData(profile, dto);
            profile.setRating(BigDecimal.ZERO);
            profile.setCertStatus(CERT_PENDING);
            profile.setStatus(1);
            coachProfileMapper.insert(profile);
            return profile.getId();
        }

        fillApplyData(exists, dto);
        // 重新提交后进入待审核
        exists.setCertStatus(CERT_PENDING);
        coachProfileMapper.updateById(exists);
        return exists.getId();
    }

    public MyCoachApplyVO myApply() {
        Long userId = LoginUserHolder.getLoginUser().getUserId();
        CoachProfile profile = coachProfileMapper.selectOne(new LambdaQueryWrapper<CoachProfile>()
                .eq(CoachProfile::getUserId, userId)
                .last("LIMIT 1"));
        if (profile == null) {
            return null;
        }
        MyCoachApplyVO vo = new MyCoachApplyVO();
        vo.setProfileId(profile.getId());
        vo.setBio(profile.getBio());
        vo.setExpertise(profile.getExpertise());
        vo.setYears(profile.getYears());
        vo.setPrice(profile.getPrice());
        vo.setCertStatus(profile.getCertStatus());
        vo.setCertStatusText(toCertStatusText(profile.getCertStatus()));
        vo.setStatus(profile.getStatus());
        vo.setUpdateTime(profile.getUpdateTime() == null ? null : DATETIME_FMT.format(profile.getUpdateTime()));
        return vo;
    }

    private void fillApplyData(CoachProfile profile, CoachApplyDTO dto) {
        profile.setBio(dto.getBio());
        profile.setExpertise(dto.getExpertise());
        profile.setYears(dto.getYears());
        profile.setPrice(dto.getPrice());
    }

    private String toCertStatusText(Integer certStatus) {
        if (certStatus == null) {
            return "UNKNOWN";
        }
        return switch (certStatus) {
            case CERT_PENDING -> "PENDING";
            case CERT_APPROVED -> "APPROVED";
            case CERT_REJECTED -> "REJECTED";
            default -> "UNKNOWN";
        };
    }
}
