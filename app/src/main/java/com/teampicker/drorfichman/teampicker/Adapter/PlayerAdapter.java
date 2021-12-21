package com.teampicker.drorfichman.teampicker.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.R;

import java.util.List;

/**
 * Created by drorfichman on 7/30/16.
 */
public class PlayerAdapter extends ArrayAdapter<Player> {

    public interface onPlayerComingChange {
        void handle();
    }

    private final Context context;
    private final List<Player> mPlayers;
    private onPlayerComingChange handler;

    public PlayerAdapter(Context ctx, List<Player> players, onPlayerComingChange caller) {
        super(ctx, -1, players);
        context = ctx;
        mPlayers = players;
        handler = caller;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.player_item, parent, false);
        TextView nameView = view.findViewById(R.id.player_name);
        TextView gradeView = view.findViewById(R.id.player_grade);
        final CheckBox vComing = view.findViewById(R.id.player_coming);
        TextView recentPerformance = view.findViewById(R.id.player_recent_performance);
        TextView ageView = view.findViewById(R.id.player_age);
        TextView attributes = view.findViewById(R.id.player_attributes);

        final Player player = mPlayers.get(position);
        view.setTag(player);

        setName(nameView, player);
        setGrade(gradeView, player);
        setAge(ageView, player);
        setPlayerRecentPerformance(recentPerformance, player);
        setAttributes(attributes, player);
        setComing(vComing, player);

        return view;
    }

    private void setGrade(TextView grade, Player player) {
        if (isMsgIdentifier(player)) {
            grade.setVisibility(View.GONE);
        } else {
            grade.setText(String.valueOf(player.mGrade >= 0 ? player.mGrade : ""));
            grade.setVisibility(View.VISIBLE);
        }
    }

    private void setComing(CheckBox vComing, Player player) {
        vComing.setVisibility(player.mName != null ? View.VISIBLE : View.INVISIBLE);
        vComing.setChecked(player.isComing);
        vComing.setOnClickListener(view1 -> {
            player.isComing = vComing.isChecked();
            DbHelper.updatePlayerComing(context, player.mName, vComing.isChecked());

            if (handler != null) {
                handler.handle();
            }
        });
    }

    private void setName(TextView name, Player player) {
        if (player.mName != null && isMsgIdentifier(player)) {
            name.setText(player.mName + "\n" + player.msgDisplayName);
        } else if (player.mName != null) {
            name.setText(player.mName);
        } else if (isMsgIdentifier(player)) {
            name.setText(player.msgDisplayName);
        } else {
            name.setText("");
        }
    }

    private void setAttributes(TextView attributes, Player player) {
        if (isMsgIdentifier(player)) {
            attributes.setVisibility(View.GONE);
        } else if (player.hasAttributes()) {
            attributes.setVisibility(View.VISIBLE);
            attributes.setText(player.getAttributes());
        } else {
            attributes.setVisibility(View.INVISIBLE);
        }
    }

    private void setAge(TextView ageView, Player player) {
        int age = player.getAge();
        if (isMsgIdentifier(player))
            ageView.setVisibility(View.GONE);
        else if (age > 0) {
            ageView.setText(String.valueOf(age));
            ageView.setVisibility(View.VISIBLE);
        } else {
            ageView.setVisibility(View.INVISIBLE);
        }
    }

    private void setPlayerRecentPerformance(TextView recentPerformance, Player player) {
        int suggestedGrade = player.getSuggestedGrade();

        if (suggestedGrade > player.mGrade) {
            recentPerformance.setText(String.valueOf(suggestedGrade));
            recentPerformance.setTextColor(Color.GREEN);
            recentPerformance.setVisibility(View.VISIBLE);
        } else if (suggestedGrade < player.mGrade) {
            recentPerformance.setText(String.valueOf(suggestedGrade));
            recentPerformance.setTextColor(Color.RED);
            recentPerformance.setVisibility(View.VISIBLE);
        } else {
            recentPerformance.setVisibility(View.INVISIBLE);
        }
    }

    private boolean isMsgIdentifier(Player p) {
        return !TextUtils.isEmpty(p.msgDisplayName);
    }
}