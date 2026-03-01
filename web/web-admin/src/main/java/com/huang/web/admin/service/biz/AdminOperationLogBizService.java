package com.huang.web.admin.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.common.login.LoginUser;
import com.huang.common.login.LoginUserHolder;
import com.huang.model.entity.OperationLog;
import com.huang.web.admin.mapper.OperationLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@Slf4j
@Service
public class AdminOperationLogBizService {

    private final OperationLogMapper operationLogMapper;

    public AdminOperationLogBizService(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    public void record(String module, String action, String detail, boolean success) {
        try {
            OperationLog logRow = new OperationLog();
            LoginUser loginUser = LoginUserHolder.getLoginUser();
            if (loginUser != null) {
                logRow.setOperatorId(loginUser.getUserId());
            }
            logRow.setModule(trim(module, 50));
            logRow.setAction(trim(action, 50));
            logRow.setDetail(trim(detail, 500));
            logRow.setIp(trim(resolveClientIp(), 50));
            logRow.setSuccess(success ? 1 : 0);
            operationLogMapper.insert(logRow);
        } catch (Exception ex) {
            log.warn("operation log insert failed: module={}, action={}, err={}", module, action, ex.getMessage());
        }
    }

    public List<OperationLog> list(String module, String action, Integer success, Long operatorId, Integer limit) {
        int safeLimit = (limit == null || limit <= 0) ? 50 : Math.min(limit, 200);
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<OperationLog>()
                .orderByDesc(OperationLog::getId)
                .last("LIMIT " + safeLimit);
        if (module != null && !module.isBlank()) {
            wrapper.eq(OperationLog::getModule, module);
        }
        if (action != null && !action.isBlank()) {
            wrapper.eq(OperationLog::getAction, action);
        }
        if (success != null) {
            wrapper.eq(OperationLog::getSuccess, success);
        }
        if (operatorId != null) {
            wrapper.eq(OperationLog::getOperatorId, operatorId);
        }
        return operationLogMapper.selectList(wrapper);
    }

    private String resolveClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "N/A";
        }
        HttpServletRequest request = attributes.getRequest();
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String[] parts = xff.split(",");
            return parts[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String trim(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
