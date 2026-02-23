package com.teampicker.drorfichman.teampicker.View;

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

import com.teampicker.drorfichman.teampicker.Controller.TeamAnalyze.PairHelper;
import com.teampicker.drorfichman.teampicker.R;

import java.util.List;
import java.util.Locale;

/**
 * RecyclerView leaderboard of power pairs (2-player combinations).
 * Sorted by wins-minus-losses so volume matters: 9W/1L outranks 3W/0L.
 * Supports All time / Recent toggle. Tap a row to open the games where both played.
 */
public class PowerPairsFragment extends Fragment {

    private static final int RECENT_GAMES_COUNT = 20;

    private RecyclerView recyclerView;
    private TextView emptyMessage;
    private View header;
    private RadioGroup toggle;

    private int gameCount = -1;

    public PowerPairsFragment() {
        super(R.layout.fragment_power_pairs);
    }

    public static PowerPairsFragment newInstance() {
        return new PowerPairsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        assert root != null;

        recyclerView = root.findViewById(R.id.pairs_list);
        emptyMessage = root.findViewById(R.id.pairs_empty);
        header = root.findViewById(R.id.pairs_header);
        toggle = root.findViewById(R.id.pairs_toggle);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

        toggle.setOnCheckedChangeListener((group, checkedId) -> {
            gameCount = (checkedId == R.id.pairs_recent) ? RECENT_GAMES_COUNT : -1;
            loadData();
        });

        loadData();
        return root;
    }

    private void loadData() {
        if (getContext() == null) return;

        List<PairHelper.PairStats> pairs = PairHelper.computePairs(getContext(), gameCount);

        if (pairs.isEmpty()) {
            showEmpty(getString(R.string.pairs_no_data, PairHelper.MIN_GAMES_TOGETHER));
            return;
        }

        showList(pairs);
    }

    private void showEmpty(String message) {
        recyclerView.setVisibility(View.GONE);
        header.setVisibility(View.GONE);
        emptyMessage.setVisibility(View.VISIBLE);
        emptyMessage.setText(message);
    }

    private void showList(List<PairHelper.PairStats> pairs) {
        emptyMessage.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        header.setVisibility(View.VISIBLE);
        recyclerView.setAdapter(new PairsAdapter(pairs));
    }

    // region Adapter

    private class PairsAdapter extends RecyclerView.Adapter<PairsAdapter.ViewHolder> {
        private final List<PairHelper.PairStats> items;

        PairsAdapter(List<PairHelper.PairStats> items) {
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
            PairHelper.PairStats stats = items.get(position);

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
