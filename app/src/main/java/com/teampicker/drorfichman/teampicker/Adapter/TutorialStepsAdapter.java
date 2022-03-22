package com.teampicker.drorfichman.teampicker.Adapter;

import static com.teampicker.drorfichman.teampicker.tools.TutorialManager.TutorialStepStatus.Done;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.TutorialManager;

import java.util.List;

/**
 * Created by drorfichman on 7/30/16.
 */
public class TutorialStepsAdapter extends ArrayAdapter<TutorialManager.Tutorials> {

    private final Context context;
    private final List<TutorialManager.Tutorials> mSteps;

    public TutorialStepsAdapter(Context ctx, List<TutorialManager.Tutorials> steps) {
        super(ctx, -1, steps);
        context = ctx;
        mSteps = steps;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.tutorial_item, parent, false);
        TextView stepView = view.findViewById(R.id.tutorial_name);
        TextView statusView = view.findViewById(R.id.tutorial_status);
        View infoView = view.findViewById(R.id.tutorial_info);

        final TutorialManager.Tutorials step = mSteps.get(position);
        view.setTag(step);

        setTutorial(stepView, statusView, infoView, step);
        view.setOnClickListener(v -> TutorialManager.displayTutorialStep(context, step, true));

        return view;
    }

    private void setTutorial(TextView stepView, TextView statusView, View infoView, TutorialManager.Tutorials step) {
        TutorialManager.TutorialStepStatus status = step.dialog.shouldBeDisplayed(context);

        stepView.setText(step.dialog.name());

        statusView.setText(getDialogRequiredString(status));

        infoView.setVisibility(status == Done ? View.INVISIBLE : View.VISIBLE);
    }

    private static String getDialogRequiredString(TutorialManager.TutorialStepStatus status) {
        switch (status) {
            case ToDo:
                return "To Do";
            case Done:
                return "Done";
            default:
                return "Not Applicable";
        }
    }
}