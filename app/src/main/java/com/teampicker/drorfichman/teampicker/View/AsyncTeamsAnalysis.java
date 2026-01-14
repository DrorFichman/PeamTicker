package com.teampicker.drorfichman.teampicker.View;

import com.teampicker.drorfichman.teampicker.Controller.TeamAnalyze.CollaborationHelper;
import com.teampicker.drorfichman.teampicker.tools.AsyncExecutor;

/**
 * Async task for analyzing team collaboration data.
 * Uses modern ExecutorService + Handler pattern instead of deprecated AsyncTask.
 */
class AsyncTeamsAnalysis {

    private final AsyncExecutor<MakeTeamsActivity> executor;
    private final OnTaskComplete doneHandler;

    public interface OnTaskComplete {
        void execute();
    }

    AsyncTeamsAnalysis(MakeTeamsActivity activity, OnTaskComplete done) {
        this.executor = new AsyncExecutor<>(activity);
        this.doneHandler = done;
    }

    public void execute() {
        executor
                .onPreExecute(() -> {
                    MakeTeamsActivity activity = executor.getActivity();
                    if (activity != null) {
                        activity.enterAnalysisAsync();
                    }
                })
                .doInBackground(() -> {
                    MakeTeamsActivity activity = executor.getActivity();
                    if (activity != null && executor.isActivityValid()) {
                        activity.analysisResult = CollaborationHelper.getCollaborationData(
                                activity, activity.players1, activity.players2);
                    }
                })
                .onPostExecute(() -> {
                    MakeTeamsActivity activity = executor.getActivity();
                    if (activity != null) {
                        if (doneHandler != null) {
                            doneHandler.execute();
                        }
                        activity.exitAnalysisAsync();
                    }
                })
                .execute();
    }
}
