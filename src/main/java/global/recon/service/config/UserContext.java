package global.recon.service.config;

public final class UserContext {

    public static final String EMAIL_HEADER = "X-User-Email";

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(String email) {
        CURRENT.set(email);
    }

    public static String get() {
        return CURRENT.get();
    }

    public static String require() {
        String email = CURRENT.get();
        if (email == null || email.isBlank()) {
            throw new IllegalStateException("Authenticated email is required");
        }
        return email;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
