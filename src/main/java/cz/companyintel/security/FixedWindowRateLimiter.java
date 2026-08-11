package cz.companyintel.security;

import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

final class FixedWindowRateLimiter {

    private final int maxRequests;
    private final long windowMillis;
    private final int maxKeys;
    private final Clock clock;
    private final Map<String, Window> windows = new HashMap<String, Window>();

    FixedWindowRateLimiter(int maxRequests, Duration window, int maxKeys, Clock clock) {
        if (maxRequests < 1 || window == null || window.isZero() || window.isNegative() || maxKeys < 1) {
            throw new IllegalArgumentException("Konfigurace rate limitu není platná");
        }
        this.maxRequests = maxRequests;
        this.windowMillis = window.toMillis();
        this.maxKeys = maxKeys;
        this.clock = clock;
    }

    synchronized Decision acquire(String key) {
        long now = clock.millis();
        Window current = windows.get(key);
        if (current == null || current.resetAt <= now) {
            if (current == null && windows.size() >= maxKeys) {
                removeExpired(now);
                if (windows.size() >= maxKeys) {
                    return Decision.denied(secondsUntil(now + windowMillis, now));
                }
            }
            windows.put(key, new Window(1, now + windowMillis));
            return Decision.allowed();
        }
        if (current.requests >= maxRequests) {
            return Decision.denied(secondsUntil(current.resetAt, now));
        }
        current.requests++;
        return Decision.allowed();
    }

    private void removeExpired(long now) {
        Iterator<Map.Entry<String, Window>> iterator = windows.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().resetAt <= now) {
                iterator.remove();
            }
        }
    }

    private long secondsUntil(long resetAt, long now) {
        return Math.max(1L, (resetAt - now + 999L) / 1000L);
    }

    static final class Decision {

        private final boolean allowed;
        private final long retryAfterSeconds;

        private Decision(boolean allowed, long retryAfterSeconds) {
            this.allowed = allowed;
            this.retryAfterSeconds = retryAfterSeconds;
        }

        static Decision allowed() {
            return new Decision(true, 0L);
        }

        static Decision denied(long retryAfterSeconds) {
            return new Decision(false, retryAfterSeconds);
        }

        boolean isAllowed() {
            return allowed;
        }

        long getRetryAfterSeconds() {
            return retryAfterSeconds;
        }
    }

    private static final class Window {

        private int requests;
        private final long resetAt;

        private Window(int requests, long resetAt) {
            this.requests = requests;
            this.resetAt = resetAt;
        }
    }
}
