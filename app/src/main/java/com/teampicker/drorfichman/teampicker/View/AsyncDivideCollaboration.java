package com.teampicker.drorfichman.teampicker.View;

import com.teampicker.drorfichman.teampicker.Controller.TeamDivision.TeamDivision;
import com.teampicker.drorfichman.teampicker.tools.AsyncExecutor;

/**
 * Async task for dividing players into teams using collaboration/optimization strategy.
 * Uses modern ExecutorService + Handler pattern instead of deprecated AsyncTask.
 */
class AsyncDivideCollaboration {

    private final AsyncExecutor<MakeTeamsActivity> executor;
    private final OnTaskComplete doneHandler;

    public interface OnTaskComplete {
        void execute();
    }

    AsyncDivideCollaboration(MakeTeamsActivity activity, OnTaskComplete done) {
        this.executor = new AsyncExecutor<>(activity);
        this.doneHandler = done;
    }

    public void execute() {
        executor
                .onPreExecute(() -> {
                    MakeTeamsActivity activity = executor.getActivity();
                    if (activity != null) {
                        activity.preDivideAsyncHideLists();
                    }
                })
                .doInBackground(() -> {
                    MakeTeamsActivity activity = executor.getActivity();
                    if (activity != null && executor.isActivityValid()) {
                        activity.divideComingPlayers(TeamDivision.DivisionStrategy.Optimize, false);
                    }
                })
                .onPostExecute(() -> {
                    MakeTeamsActivity activity = executor.getActivity();
                    if (activity != null) {
                        if (doneHandler != null) {
                            doneHandler.execute();
                        }
                        activity.postDivideAsyncShowTeams();
                    }
                })
                .execute();
    }
}
