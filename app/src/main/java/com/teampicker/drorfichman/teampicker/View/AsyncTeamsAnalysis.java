package com.teampicker.drorfichman.teampicker.View;

import android.os.AsyncTask;
import android.view.View;

import com.teampicker.drorfichman.teampicker.Controller.TeamAnalyze.CollaborationHelper;

import java.lang.ref.WeakReference;

class AsyncTeamsAnalysis extends AsyncTask<Void, Void, String> {

    private final onTaskComplete doneHandler;

    public interface onTaskComplete {
        void execute();
    }

    private WeakReference<MakeTeamsActivity> ref;

    AsyncTeamsAnalysis(MakeTeamsActivity activity, onTaskComplete done) {
        ref = new WeakReference<>(activity);
        doneHandler = done;
    }

    @Override
    protected String doInBackground(Void... params) {
        MakeTeamsActivity activity = ref.get();
        if (activity == null || activity.isFinishing()) return null;

        activity.analysisResult = CollaborationHelper.getCollaborationData(activity, activity.players1, activity.players2);
        return null;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        MakeTeamsActivity activity = ref.get();
        if (activity == null || activity.isFinishing()) return;

        activity.teamStatsLayout.setVisibility(View.INVISIBLE);
        activity.analysisView.setEnabled(false);
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        MakeTeamsActivity activity = ref.get();
        if (activity == null || activity.isFinishing()) return;

        doneHandler.execute();

        activity.teamStatsLayout.setVisibility(View.VISIBLE);
        activity.analysisHeaders1.setVisibility(View.VISIBLE);
        activity.analysisHeaders2.setVisibility(View.VISIBLE);
        activity.analysisView.setEnabled(true);
    }
}
