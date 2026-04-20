const ROLE_NAME_MAP = {
  ADMIN: '超级管理员',
  OPS_ADMIN: '运营管理员',
  AUDIT_ADMIN: '审核管理员',
  MEMBER: '会员'
}

const MODULE_NAME_MAP = {
  content: '内容运营',
  system: '系统管理',
  video: '视频资源',
  course: '课程管理',
  coach: '教练审核',
  user: '用户管理',
  payment: '支付审计',
  default: '默认分组'
}

const PERMISSION_NAME_MAP = {
  'banner:manage': 'Banner 管理',
  'notice:manage': '公告管理',
  'system:config': '系统配置',
  'operation:log:read': '操作日志查看',
  'task:run:read': '任务运行记录查看',
  'task:run:trigger': '任务手动触发',
  'video:asset': '视频资源管理',
  'video:upload': '视频上传',
  'video:status': '视频状态管理',
  'video:bind': '视频绑定计划项',
  'course:create': '课程创建',
  'course:delete': '课程删除',
  'course:update': '课程编辑',
  'course:publish': '课程发布',
  'course:schedule': '课程排期管理',
  'course:checkin': '课程核销',
  'booking:schedule': '教练档期管理',
  'coach:apply:audit': '教练申请审核',
  'refund:audit': '退款审核',
  'pay:callback:audit': '支付回调审计',
  'user:status': '用户状态管理',
  'user:role': '用户角色分配'
}

function isBrokenLabel(value) {
  if (!value) {
    return true
  }
  return /[?锟]/.test(value)
}

export function displayRoleName(role) {
  if (!role) {
    return '-'
  }
  const mapped = ROLE_NAME_MAP[role.roleCode]
  if (mapped) {
    return mapped
  }
  return isBrokenLabel(role.roleName) ? role.roleCode || '-' : role.roleName
}

export function displayRoleLabel(role) {
  if (!role) {
    return '-'
  }
  return `${displayRoleName(role)}（${role.roleCode || '-'}）`
}

export function displayPermissionName(perm) {
  if (!perm) {
    return '-'
  }
  const mapped = PERMISSION_NAME_MAP[perm.permCode]
  if (mapped) {
    return mapped
  }
  return isBrokenLabel(perm.permName) ? perm.permCode || '-' : perm.permName
}

export function displayPermissionLabel(perm) {
  if (!perm) {
    return '-'
  }
  return `${displayPermissionName(perm)}（${perm.permCode || '-'}）`
}

export function displayPermissionCodeLabel(code) {
  if (!code) {
    return '-'
  }
  const mapped = PERMISSION_NAME_MAP[code]
  if (mapped) {
    return `${mapped}（${code}）`
  }
  return code
}

export function displayModuleName(moduleCode) {
  if (!moduleCode) {
    return MODULE_NAME_MAP.default
  }
  return MODULE_NAME_MAP[moduleCode] || moduleCode
}
