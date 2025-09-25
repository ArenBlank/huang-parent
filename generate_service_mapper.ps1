# 批量生成Service和Mapper文件的PowerShell脚本
param(
    [string]$ProjectPath = "D:\DevelopmentLOOK\Idea\idea_project_workspace\huang-parent"
)

# 实体类名称数组（排除User，因为已经创建）
$entities = @(
    "Role", "UserRole", "Coach", "HealthRecord", "BodyMeasurement", 
    "CourseCategory", "Course", "CourseSchedule", "CourseEnrollment", 
    "ExerciseRecord", "ExercisePlan", "PaymentRecord", "SystemConfig",
    "CoachAvailability", "CoachCertificationApply", "CoachScheduleChange", "CoachService",
    "ArticleCategory", "HealthArticle", "ArticleAuditLog", "ArticleLike", 
    "ArticleCollect", "ArticleCollectFolder", "ArticleComment"
)

$servicePath = "$ProjectPath\model\src\main\java\com\huang\model\service"
$serviceImplPath = "$ProjectPath\model\src\main\java\com\huang\model\service\impl"
$mapperPath = "$ProjectPath\model\src\main\java\com\huang\model\mapper"
$mapperXmlPath = "$ProjectPath\model\src\main\resources\mapper"

foreach ($entity in $entities) {
    Write-Host "正在生成 $entity 的Service和Mapper文件..."
    
    # 生成Service接口
    $serviceContent = @"
package com.huang.model.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.huang.model.entity.$entity;

/**
 * ${entity}服务接口
 * @author system
 * @since 2025-01-24
 */
public interface ${entity}Service extends IService<$entity> {

}
"@
    
    $serviceFile = "$servicePath\${entity}Service.java"
    $serviceContent | Out-File -FilePath $serviceFile -Encoding UTF8
    
    # 生成Service实现类
    $serviceImplContent = @"
package com.huang.model.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huang.model.entity.$entity;
import com.huang.model.mapper.${entity}Mapper;
import com.huang.model.service.${entity}Service;
import org.springframework.stereotype.Service;

/**
 * ${entity}服务实现类
 * @author system
 * @since 2025-01-24
 */
@Service
public class ${entity}ServiceImpl extends ServiceImpl<${entity}Mapper, $entity> implements ${entity}Service {

}
"@
    
    $serviceImplFile = "$serviceImplPath\${entity}ServiceImpl.java"
    $serviceImplContent | Out-File -FilePath $serviceImplFile -Encoding UTF8
    
    # 生成Mapper接口
    $mapperContent = @"
package com.huang.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huang.model.entity.$entity;
import org.apache.ibatis.annotations.Mapper;

/**
 * ${entity}Mapper接口
 * @author system
 * @since 2025-01-24
 */
@Mapper
public interface ${entity}Mapper extends BaseMapper<$entity> {

}
"@
    
    $mapperFile = "$mapperPath\${entity}Mapper.java"
    $mapperContent | Out-File -FilePath $mapperFile -Encoding UTF8
    
    # 生成Mapper XML文件
    $mapperXmlContent = @"
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.huang.web.app.mapper.${entity}Mapper">

    <!-- ${entity}表结果映射 -->
    <resultMap id="BaseResultMap" type="com.huang.model.entity.$entity">
        <!-- 根据实际字段配置 -->
    </resultMap>

</mapper>
"@
    
    $mapperXmlFile = "$mapperXmlPath\${entity}Mapper.xml"
    $mapperXmlContent | Out-File -FilePath $mapperXmlFile -Encoding UTF8
}

Write-Host "所有Service和Mapper文件生成完成！"