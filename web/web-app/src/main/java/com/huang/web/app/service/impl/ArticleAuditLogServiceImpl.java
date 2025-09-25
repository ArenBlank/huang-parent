package com.huang.web.app.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huang.model.entity.ArticleAuditLog;
import com.huang.web.app.mapper.ArticleAuditLogMapper;
import com.huang.web.app.service.ArticleAuditLogService;
import org.springframework.stereotype.Service;

/**
 * ArticleAuditLog服务实现类
 * @author system
 * @since 2025-01-24
 */
@Service
public class ArticleAuditLogServiceImpl extends ServiceImpl<ArticleAuditLogMapper, ArticleAuditLog> implements ArticleAuditLogService {

}
