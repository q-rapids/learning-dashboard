package com.upc.gessi.qrapids.app.config.security;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;

import com.upc.gessi.qrapids.app.config.ActionLogger;

@Component
@Scope("singleton")
public class SessionTimer {

    private static SessionTimer instance;
    private ScheduledExecutorService executorService;
    private Map<String, ScheduledFuture<?>> activeTimers;

    public SessionTimer() {
        executorService = Executors.newSingleThreadScheduledExecutor();
        activeTimers = new ConcurrentHashMap<>();
    }

    public static SessionTimer getInstance() {
        if (instance == null) {
            instance = new SessionTimer();
        }
        return instance;
    }

    public synchronized void startTimer(String sessionId, long delaySeconds) {
        cancelTimer(sessionId);
        ScheduledFuture<?> future = executorService.schedule(new SessionTimeoutTask(sessionId), delaySeconds, TimeUnit.SECONDS);
        activeTimers.put(sessionId, future);
    }

    public synchronized void cancelTimer(String sessionId) {
        ScheduledFuture<?> future = activeTimers.remove(sessionId);
        if (future != null) {
            future.cancel(false);
        }
    }

    private class SessionTimeoutTask implements Runnable {
        private String sessionId;

        public SessionTimeoutTask(String sessionId) {
            this.sessionId = sessionId;
        }

        @Override
        public void run() {
            // Perform the action you want to take when the session timeout occurs
            activeTimers.remove(sessionId);
            ActionLogger al = new ActionLogger();
            al.traceSessionTimeout(sessionId);
        }
    }
}
