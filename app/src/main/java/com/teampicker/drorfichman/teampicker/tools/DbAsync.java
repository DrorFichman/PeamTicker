package com.teampicker.drorfichman.teampicker.tools;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Lightweight helper for off-main-thread DB queries.
 * Uses a single-thread executor to serialize SQLite access and avoid concurrent write conflicts.
 * Callers must guard the onResult callback with isAdded() (Fragment) or !isFinishing() (Activity).
 */
public class DbAsync {

    // Single-thread executor serializes all DB access, matching the SQLite singleton pattern.
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Run a DB query on a background thread and deliver the result on the main thread.
     *
     * @param query    Supplier that performs the DB read/write; runs off the main thread.
     * @param onResult Consumer called on the main thread with the query result.
     */
    public static <T> void run(Supplier<T> query, Consumer<T> onResult) {
        executor.execute(() -> {
            T result = query.get();
            mainHandler.post(() -> onResult.accept(result));
        });
    }

    /**
     * Run a fire-and-forget DB write (no result needed) on a background thread.
     *
     * @param task Runnable that performs the DB write; runs off the main thread.
     * @param onDone Optional Runnable to run on the main thread after the write completes.
     */
    public static void runWrite(Runnable task, Runnable onDone) {
        executor.execute(() -> {
            task.run();
            if (onDone != null) {
                mainHandler.post(onDone);
            }
        });
    }
}
