package com.teampicker.drorfichman.teampicker.View;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.Data.Game;
import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.Data.PlayerGameStat;
import com.teampicker.drorfichman.teampicker.Data.ResultEnum;
import com.teampicker.drorfichman.teampicker.Data.TeamEnum;
import com.teampicker.drorfichman.teampicker.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Color-coded recent form grid.
 * Two sections: players seen in the last RECENT_GAMES_WINDOW games, then all others.
 * Each section sorted by success (wins - losses) over last FORM_GAMES_COUNT games.
 * Columns = last N games: W (green), L (red), T (gray), absent (empty).
 * Tap a row to open the player's details.
 */
public class FormGridFragment extends Fragment {

    private static final int FORM_GAMES_COUNT = 10;
    private static final int RECENT_GAMES_WINDOW = 4;

    private RecyclerView recyclerView;
    private TextView emptyMessage;
    private TextView description;

    public FormGridFragment() {
        super(R.layout.fragment_form_grid);
    }

    public static FormGridFragment newInstance() {
        return new FormGridFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        assert root != null;

        recyclerView = root.findViewById(R.id.form_grid_list);
        emptyMessage = root.findViewById(R.id.form_grid_empty);
        description = root.findViewById(R.id.form_grid_description);

        description.setText(getString(R.string.form_grid_description, FORM_GAMES_COUNT));

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

        loadData();
        return root;
    }

    private void loadData() {
        if (getContext() == null) return;

        ArrayList<Player> players = DbHelper.getPlayers(getContext(), 0, false); // non-archived only
        if (players == null || players.isEmpty()) {
            showEmpty(getString(R.string.form_grid_no_data));
            return;
        }

        // Determine which players appeared in the last RECENT_GAMES_WINDOW games
        Set<String> recentPlayerNames = new HashSet<>();
        ArrayList<Game> recentGames = DbHelper.getGames(getContext(), RECENT_GAMES_WINDOW);
        for (Game game : recentGames) {
            for (Player p : DbHelper.getGameTeam(getContext(), game.gameId, TeamEnum.Team1, 0))
                recentPlayerNames.add(p.mName);
            for (Player p : DbHelper.getGameTeam(getContext(), game.gameId, TeamEnum.Team2, 0))
                recentPlayerNames.add(p.mName);
        }

        List<FormRow> recentRows = new ArrayList<>();
        List<FormRow> otherRows = new ArrayList<>();

        for (Player p : players) {
            ArrayList<PlayerGameStat> results = DbHelper.getPlayerLastGames(getContext(), p, FORM_GAMES_COUNT);
            if (results.isEmpty()) continue;
            FormRow row = new FormRow(p, results);
            if (recentPlayerNames.contains(p.mName)) {
                recentRows.add(row);
            } else {
                otherRows.add(row);
            }
        }

        if (recentRows.isEmpty() && otherRows.isEmpty()) {
            showEmpty(getString(R.string.form_grid_no_data));
            return;
        }

        // Sort each section by success (wins - losses) descending
        recentRows.sort((a, b) -> Integer.compare(b.success, a.success));
        otherRows.sort((a, b) -> Integer.compare(b.success, a.success));

        // Build flat list with section headers
        List<ListItem> items = new ArrayList<>();
        if (!recentRows.isEmpty()) {
            items.add(ListItem.header(getString(R.string.form_section_recent)));
            for (FormRow r : recentRows) items.add(ListItem.row(r));
        }
        if (!otherRows.isEmpty()) {
            items.add(ListItem.header(getString(R.string.form_section_others)));
            for (FormRow r : otherRows) items.add(ListItem.row(r));
        }

        showList(items);
    }

    private void showEmpty(String message) {
        recyclerView.setVisibility(View.GONE);
        emptyMessage.setVisibility(View.VISIBLE);
        emptyMessage.setText(message);
    }

    private void showList(List<ListItem> items) {
        emptyMessage.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        recyclerView.setAdapter(new FormAdapter(items));
    }

    // region Data model

    static class FormRow {
        final Player player;
        final List<PlayerGameStat> results;
        final int success; // wins - losses over last FORM_GAMES_COUNT games

        FormRow(Player player, List<PlayerGameStat> results) {
            this.player = player;
            this.results = results;
            this.success = computeSuccess(results);
        }

        private static int computeSuccess(List<PlayerGameStat> results) {
            int wins = 0, losses = 0;
            for (PlayerGameStat stat : results) {
                if (stat.result == ResultEnum.Win) wins++;
                else if (stat.result == ResultEnum.Lose) losses++;
            }
            return wins - losses;
        }
    }

    /** Flat list item — either a section header or a player row. */
    private static class ListItem {
        final FormRow row;    // null if this is a header
        final String header;  // null if this is a row

        static ListItem header(String text) { return new ListItem(null, text); }
        static ListItem row(FormRow row)    { return new ListItem(row, null); }

        private ListItem(FormRow row, String header) {
            this.row = row;
            this.header = header;
        }

        int viewType() { return header != null ? FormAdapter.TYPE_HEADER : FormAdapter.TYPE_ROW; }
    }

    // endregion

    // region Adapter

    private class FormAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        static final int TYPE_HEADER = 0;
        static final int TYPE_ROW    = 1;

        private final List<ListItem> items;

        FormAdapter(List<ListItem> items) {
            this.items = items;
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).viewType();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == TYPE_HEADER) {
                TextView tv = new TextView(parent.getContext());
                tv.setPadding(
                        (int) (12 * getResources().getDisplayMetrics().density), 
                        (int) (10 * getResources().getDisplayMetrics().density),
                        0, 
                        (int) (4 * getResources().getDisplayMetrics().density));
                tv.setTextSize(13f);
                tv.setTextColor(Color.rgb(100, 100, 100));
                tv.setTypeface(null, android.graphics.Typeface.BOLD);
                tv.setBackgroundColor(Color.rgb(245, 245, 245));
                tv.setLayoutParams(new RecyclerView.LayoutParams(
                        RecyclerView.LayoutParams.MATCH_PARENT,
                        RecyclerView.LayoutParams.WRAP_CONTENT));
                return new HeaderViewHolder(tv);
            }
            View view = inflater.inflate(R.layout.item_form_row, parent, false);
            return new RowViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ListItem item = items.get(position);
            if (item.viewType() == TYPE_HEADER) {
                ((HeaderViewHolder) holder).title.setText(item.header);
            } else {
                FormRow row = item.row;
                RowViewHolder vh = (RowViewHolder) holder;

                vh.playerName.setText(row.player.mName);
                vh.cellsContainer.removeAllViews();

                for (PlayerGameStat stat : row.results) {
                    vh.cellsContainer.addView(makeCellView(vh.cellsContainer.getContext(), stat.result));
                }

                vh.itemView.setOnClickListener(v -> {
                    if (getContext() == null) return;
                    Intent intent = PlayerDetailsActivity.getEditPlayerIntent(getContext(), row.player.mName);
                    startActivity(intent);
                });
            }
        }

        private TextView makeCellView(android.content.Context ctx, ResultEnum result) {
            TextView cell = new TextView(ctx);
            int size = (int) (26 * ctx.getResources().getDisplayMetrics().density);
            int margin = (int) (2 * ctx.getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(margin, 0, margin, 0);
            cell.setLayoutParams(params);
            cell.setGravity(android.view.Gravity.CENTER);
            cell.setTextSize(9f);

            if (result == ResultEnum.Win) {
                cell.setBackgroundColor(Color.rgb(76, 175, 80));
                cell.setTextColor(Color.WHITE);
                cell.setText("W");
            } else if (result == ResultEnum.Lose) {
                cell.setBackgroundColor(Color.rgb(244, 67, 54));
                cell.setTextColor(Color.WHITE);
                cell.setText("L");
            } else if (result == ResultEnum.Tie) {
                cell.setBackgroundColor(Color.rgb(158, 158, 158));
                cell.setTextColor(Color.WHITE);
                cell.setText("T");
            } else {
                cell.setBackgroundColor(Color.TRANSPARENT);
            }

            return cell;
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class HeaderViewHolder extends RecyclerView.ViewHolder {
            final TextView title;
            HeaderViewHolder(TextView tv) {
                super(tv);
                title = tv;
            }
        }

        class RowViewHolder extends RecyclerView.ViewHolder {
            final TextView playerName;
            final LinearLayout cellsContainer;

            RowViewHolder(@NonNull View itemView) {
                super(itemView);
                playerName = itemView.findViewById(R.id.form_player_name);
                cellsContainer = itemView.findViewById(R.id.form_cells_container);
            }
        }
    }

    // endregion
}
