package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 临时文件记录表
 */
@Data
@TableName("temp_file_record")
public class TempFileRecord {

    /**
     * 记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件路径(MinIO路径)
     */
    private String filePath;

    /**
     * 文件大小(字节)
     */
    private Long fileSize;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 上传用户ID
     */
    private Long uploadUserId;

    /**
     * 业务类型: avatar头像 import导入 export导出
     */
    private String businessType;

    /**
     * 状态: temp临时 used已使用 expired已过期
     */
    private String status;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 使用时间
     */
    private LocalDateTime usedTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
