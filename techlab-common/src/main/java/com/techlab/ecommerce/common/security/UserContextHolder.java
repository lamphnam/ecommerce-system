package com.techlab.ecommerce.common.security;

/**
 * Thread-local store of the {@link UserContext} for the current request.
 * Populated by {@link UserContextFilter} and cleared at the end of the request.
 */
public final class UserContextHolder {

    private static final ThreadLocal<UserContext> CONTEXT = new ThreadLocal<>();

    private UserContextHolder() {}

    public static void set(UserContext ctx) {
        CONTEXT.set(ctx);
    }

    public static UserContext get() {
        return CONTEXT.get();
    }

    public static Long currentUserId() {
        UserContext ctx = CONTEXT.get();
        return ctx != null ? ctx.getUserId() : null;
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
