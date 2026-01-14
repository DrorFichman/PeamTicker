package com.teampicker.drorfichman.teampicker.tools;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Modern replacement for AsyncTask using ExecutorService + Handler pattern.
 * Handles background work with progress updates and lifecycle-aware callbacks.
 *
 * @param <T> The type of the activity using this executor
 */
public class AsyncExecutor<T extends Activity> {

    private final WeakReference<T> activityRef;
    private final Handler mainHandler;
    private final ExecutorService executor;

    private Runnable onPreExecute;
    private Runnable backgroundTask;
    private Runnable onPostExecute;

    public AsyncExecutor(T activity) {
        this.activityRef = new WeakReference<>(activity);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Set the task to run on the UI thread before background execution.
     */
    public AsyncExecutor<T> onPreExecute(Runnable task) {
        this.onPreExecute = task;
        return this;
    }

    /**
     * Set the background task to execute.
     */
    public AsyncExecutor<T> doInBackground(Runnable task) {
        this.backgroundTask = task;
        return this;
    }

    /**
     * Set the task to run on the UI thread after background execution completes.
     */
    public AsyncExecutor<T> onPostExecute(Runnable task) {
        this.onPostExecute = task;
        return this;
    }

    /**
     * Execute the async operation.
     */
    public void execute() {
        T activity = activityRef.get();
        if (activity == null || activity.isFinishing()) {
            return;
        }

        // Run pre-execute on main thread
        if (onPreExecute != null) {
            onPreExecute.run();
        }

        // Submit background work
        executor.execute(() -> {
            T activityInBackground = activityRef.get();
            if (activityInBackground == null || activityInBackground.isFinishing()) {
                return;
            }

            // Run background task
            if (backgroundTask != null) {
                backgroundTask.run();
            }

            // Post result to main thread
            mainHandler.post(() -> {
                T activityOnPost = activityRef.get();
                if (activityOnPost == null || activityOnPost.isFinishing()) {
                    return;
                }

                if (onPostExecute != null) {
                    onPostExecute.run();
                }
            });
        });
    }

    /**
     * Post a progress update to the main thread.
     * Call this from within the background task.
     */
    public void postProgress(Runnable updateTask) {
        mainHandler.post(() -> {
            T activity = activityRef.get();
            if (activity != null && !activity.isFinishing()) {
                updateTask.run();
            }
        });
    }

    /**
     * Check if the activity is still valid.
     * Call this from within the background task to check if work should continue.
     */
    public boolean isActivityValid() {
        T activity = activityRef.get();
        return activity != null && !activity.isFinishing();
    }

    /**
     * Get the activity reference. May return null if activity was garbage collected.
     */
    public T getActivity() {
        return activityRef.get();
    }

    /**
     * Shutdown the executor. Call this when the activity is destroyed.
     */
    public void shutdown() {
        executor.shutdownNow();
    }
}

