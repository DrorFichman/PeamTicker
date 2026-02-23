package com.teampicker.drorfichman.teampicker.View;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.teampicker.drorfichman.teampicker.Controller.Broadcast.LocalNotifications;
import com.teampicker.drorfichman.teampicker.Controller.TeamAnalyze.TripletHelper;
import com.teampicker.drorfichman.teampicker.R;

import java.util.List;
import java.util.Locale;

/**
 * RecyclerView leaderboard of power triplets (3-player combinations) sorted by win rate.
 * Supports All time / Recent toggle. Tap a row to open the games where all three played.
 */
public class PowerTripletsFragment extends Fragment {

    private static final int RECENT_GAMES_COUNT = 20;

    private RecyclerView recyclerView;
    private TextView emptyMessage;
    private View header;
    private RadioGroup toggle;

    private int gameCount = -1;

    private BroadcastReceiver dataUpdateReceiver;

    public PowerTripletsFragment() {
        super(R.layout.fragment_power_triplets);
    }

    public static PowerTripletsFragment newInstance() {
        return new PowerTripletsFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dataUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                loadData();
            }
        };
        LocalNotifications.registerBroadcastReceiver(getContext(), LocalNotifications.GAME_UPDATE_ACTION, dataUpdateReceiver);
        LocalNotifications.registerBroadcastReceiver(getContext(), LocalNotifications.PULL_DATA_ACTION, dataUpdateReceiver);
        LocalNotifications.registerBroadcastReceiver(getContext(), LocalNotifications.PLAYER_UPDATE_ACTION, dataUpdateReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalNotifications.unregisterBroadcastReceiver(getContext(), dataUpdateReceiver);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        assert root != null;

        recyclerView = root.findViewById(R.id.triplets_list);
        emptyMessage = root.findViewById(R.id.triplets_empty);
        header = root.findViewById(R.id.triplets_header);
        toggle = root.findViewById(R.id.triplets_toggle);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

        toggle.setOnCheckedChangeListener((group, checkedId) -> {
            gameCount = (checkedId == R.id.triplets_recent) ? RECENT_GAMES_COUNT : -1;
            loadData();
        });

        loadData();
        return root;
    }

    private void loadData() {
        if (getContext() == null) return;

        List<TripletHelper.TripletStats> triplets = TripletHelper.computeTriplets(getContext(), gameCount);

        if (triplets.isEmpty()) {
            showEmpty(getString(R.string.triplets_no_data, TripletHelper.MIN_GAMES_TOGETHER));
            return;
        }

        showList(triplets);
    }

    private void showEmpty(String message) {
        recyclerView.setVisibility(View.GONE);
        header.setVisibility(View.GONE);
        emptyMessage.setVisibility(View.VISIBLE);
        emptyMessage.setText(message);
    }

    private void showList(List<TripletHelper.TripletStats> triplets) {
        emptyMessage.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        header.setVisibility(View.VISIBLE);
        recyclerView.setAdapter(new TripletsAdapter(triplets));
    }

    // region Adapter

    private class TripletsAdapter extends RecyclerView.Adapter<TripletsAdapter.ViewHolder> {
        private final List<TripletHelper.TripletStats> items;

        TripletsAdapter(List<TripletHelper.TripletStats> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_triplet, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            TripletHelper.TripletStats stats = items.get(position);

            holder.rank.setText(String.valueOf(position + 1));
            holder.names.setText(stats.displayLabel());
            holder.games.setText(String.valueOf(stats.gamesTogether));

            int winRate = stats.getWinRate();
            holder.winRate.setText(String.format(Locale.getDefault(), "%d%%", winRate));
            holder.winRate.setTextColor(winRate >= 60 ? Color.rgb(27, 130, 54)
                    : winRate <= 40 ? Color.rgb(198, 40, 40)
                    : Color.DKGRAY);

            holder.itemView.setOnClickListener(v -> {
                if (getContext() == null) return;
                Intent intent = GamesActivity.getGameActivityIntent(
                        getContext(), stats.playerNames(), false);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView rank;
            final TextView names;
            final TextView games;
            final TextView winRate;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                rank = itemView.findViewById(R.id.triplet_rank);
                names = itemView.findViewById(R.id.triplet_names);
                games = itemView.findViewById(R.id.triplet_games);
                winRate = itemView.findViewById(R.id.triplet_winrate);
            }
        }
    }

    // endregion
}
