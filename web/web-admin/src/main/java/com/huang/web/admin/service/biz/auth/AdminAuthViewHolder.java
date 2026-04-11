package com.huang.web.admin.service.biz.auth;

public final class AdminAuthViewHolder {

    private static final ThreadLocal<AdminAuthView> HOLDER = new ThreadLocal<>();

    private AdminAuthViewHolder() {
    }

    public static void set(AdminAuthView view) {
        HOLDER.set(view);
    }

    public static AdminAuthView get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
