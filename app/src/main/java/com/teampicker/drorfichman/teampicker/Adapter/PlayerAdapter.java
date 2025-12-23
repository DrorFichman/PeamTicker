package com.teampicker.drorfichman.teampicker.Adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.teampicker.drorfichman.teampicker.Controller.Search.FilterView;
import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.SettingsHelper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

/**
 * Created by drorfichman on 7/30/16.
 */
public class PlayerAdapter extends ArrayAdapter<Player> {

    private static final int LAST_GAME_DAYS = 180;

    public interface onPlayerComingChange {
        void playerComingChanged(boolean coming);
    }

    private final Context context;
    private final List<Player> mAllPlayers;
    private List<Player> mDisplayedPlayers;
    private final onPlayerComingChange handler;
    private String filterValue;
    private boolean showIndications = true;

    public PlayerAdapter(Context ctx, List<Player> players, onPlayerComingChange caller) {
        super(ctx, -1, players);
        context = ctx;
        mAllPlayers = players;
        mDisplayedPlayers = players;
        handler = caller;
    }

    @Override
    public int getCount() {
        return mDisplayedPlayers != null ? mDisplayedPlayers.size() : 0;
    }

    @Override
    public Player getItem(int position) {
        return mDisplayedPlayers != null ? mDisplayedPlayers.get(position) : null;
    }

    public void setShowIndications(boolean showIndications) {
        this.showIndications = showIndications;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.player_item, parent, false);
        TextView nameView = view.findViewById(R.id.player_name);
        TextView gradeView = view.findViewById(R.id.player_grade);
        final CheckBox vComing = view.findViewById(R.id.player_coming);
        View vComingTitle = view.findViewById(R.id.player_rsvp_title);
        ImageView recentPerformance = view.findViewById(R.id.player_recent_performance);
        TextView ageView = view.findViewById(R.id.player_age);
        TextView attributes = view.findViewById(R.id.player_attributes);

        final Player player = mDisplayedPlayers.get(position);
        view.setTag(player);

        setName(nameView, player);
        setGrade(gradeView, player);
        setAge(ageView, player);
        setPlayerRecentPerformance(recentPerformance, player);
        setAttributes(attributes, player);
        setComing(vComing, vComingTitle, player);

        return view;
    }

    private void setGrade(TextView grade, Player player) {
        if (!SettingsHelper.getShowGrades(context)) {
            grade.setVisibility(View.INVISIBLE);
            return;
        }

        if (isMsgIdentifier(player)) {
            grade.setVisibility(View.GONE);
        } else {
            grade.setText(String.valueOf(player.mGrade >= 0 ? player.mGrade : ""));
            grade.setVisibility(View.VISIBLE);
        }
    }

    private void setComing(CheckBox vComing, View vComingTitle, Player player) {
        vComingTitle.setVisibility(View.GONE);

        boolean canCome = (player.mName != null && !player.archived);
        vComing.setVisibility(canCome ? View.VISIBLE : View.INVISIBLE);
        vComing.setChecked(player.isComing);
        vComing.setOnClickListener(view1 -> {
            player.isComing = vComing.isChecked();
            DbHelper.updatePlayerComing(context, player.mName, vComing.isChecked());

            if (handler != null) {
                handler.playerComingChanged(vComing.isChecked());
            }
        });
    }

    private void setName(TextView name, Player player) {
        if (player.mName != null && isMsgIdentifier(player)) {
            name.setText(String.format("%s (%s)", player.mName, player.msgDisplayName));
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

    private void setPlayerRecentPerformance(ImageView recentPerformance, Player player) {
        if (!SettingsHelper.getShowGrades(context) || !showIndications) {
            recentPerformance.setVisibility(View.INVISIBLE);
            return;
        }

        int suggestedGrade = player.getSuggestedGrade();
        Date lastPlayerGame = player.lastPlayedGame();

        if (lastPlayerGame == null ||
                lastPlayerGame.before(Date.from(Instant.now().minus(LAST_GAME_DAYS, ChronoUnit.DAYS)))) {
            if (!player.archived) {
                recentPerformance.setImageResource(R.drawable.archive);
            }
        } else if (suggestedGrade > player.mGrade) {
            recentPerformance.setImageResource(R.drawable.increase);
            recentPerformance.setVisibility(View.VISIBLE);
        } else if (suggestedGrade < player.mGrade) {
            recentPerformance.setImageResource(R.drawable.decrease);
            recentPerformance.setVisibility(View.VISIBLE);
        } else {
            recentPerformance.setVisibility(View.INVISIBLE);
        }
    }

    private boolean isMsgIdentifier(Player p) {
        return !TextUtils.isEmpty(p.msgDisplayName);
    }

    public void setFilter(String value) {
        filterValue = value;
        if (TextUtils.isEmpty(value)) {
            mDisplayedPlayers = mAllPlayers;
        } else {
            mDisplayedPlayers = mAllPlayers.stream()
                    .filter(p -> FilterView.match(p.filterBy(), value))
                    .collect(java.util.stream.Collectors.toList());
        }
        notifyDataSetChanged();
    }

    public int positionOfFirstFilterItem(FilterView.onFilterNoResults handler) {
        if (mDisplayedPlayers.isEmpty() && !TextUtils.isEmpty(filterValue)) {
            if (handler != null) handler.noFilterResults();
        }
        return 0;
    }
}