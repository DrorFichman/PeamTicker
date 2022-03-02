package com.teampicker.drorfichman.teampicker.View;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.teampicker.drorfichman.teampicker.Adapter.PlayerAdapter;
import com.teampicker.drorfichman.teampicker.Controller.Broadcast.LocalNotifications;
import com.teampicker.drorfichman.teampicker.Controller.Search.FilterView;
import com.teampicker.drorfichman.teampicker.Controller.Sort.SortType;
import com.teampicker.drorfichman.teampicker.Controller.Sort.Sorting;
import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlayersFragment extends Fragment implements Sorting.sortingCallbacks {

    private static final int RECENT_GAMES_COUNT = 10; // for +/- grade suggestion

    Sorting sorting = new Sorting(this::sortingChanged, SortType.coming);

    private boolean showArchivedPlayers = false;
    private boolean showPastedPlayers = false;

    private View rootView;
    private Button createPlayer;
    private ListView playersList;
    private FilterView filterView;

    private PlayerAdapter playersAdapter;

    private PlayerUpdateBroadcast notificationHandler;

    public PlayersFragment() {
        super(R.layout.layout_players_main);
    }

    public static PlayersFragment newInstance() {
        PlayersFragment playersFragment = new PlayersFragment();
        return playersFragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        setHasOptionsMenu(true);

        this.rootView = root;
        playersList = root.findViewById(R.id.players_list);
        createPlayer = root.findViewById(R.id.players_add_new_player);
        createPlayer.setOnClickListener(view -> startActivity(new Intent(getContext(), PlayerDetailsActivity.class)));

        setSearchView(root);
        refreshPlayers();

        setActionButtons();
        setComingPlayersCount();

        return root;
    }

    private void setSearchView(View root) {
        filterView = new FilterView(root.findViewById(R.id.players_search_players), value -> {
            playersAdapter.setFilter(value);
        });
    }

    final OnBackPressedCallback backPress = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            backPress.setEnabled(false);
            if (showArchivedPlayers) {
                showArchivedPlayers = false;
                refreshPlayers();
            }
            if (showPastedPlayers) {
                showPastedPlayers = false;
                refreshPlayers();
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        notificationHandler = new PlayerUpdateBroadcast();
        LocalNotifications.registerBroadcastReceiver(getContext(), LocalNotifications.PLAYER_UPDATE_ACTION, notificationHandler);
        LocalNotifications.registerBroadcastReceiver(getContext(), LocalNotifications.PULL_DATA_ACTION, notificationHandler);

        requireActivity().getOnBackPressedDispatcher().addCallback(this, backPress);
        backPress.setEnabled(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalNotifications.unregisterBroadcastReceiver(getContext(), notificationHandler);
    }

    @Override
    public void onResume() {
        super.onResume();
        backPress.setEnabled(showArchivedPlayers || showPastedPlayers);
    }

    @Override
    public void onPause() {
        super.onPause();
        backPress.setEnabled(false);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.players_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.make_teams:
                launchMakeTeams();
                break;
            case R.id.paste_coming_players:
                pasteComingPlayers();
                break;
            case R.id.add_player:
                startActivity(PlayerDetailsActivity.getNewPlayerIntent(getContext()));
                break;
            case R.id.search_players:
                filterView.toggleSearchVisibility();
                break;
            case R.id.show_archived_players:
                switchArchivedPlayersView();
                break;
            case R.id.clear_all:
                DbHelper.clearComingPlayers(getContext());
                refreshPlayers();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setComingPlayersCount() {
        ((Button) rootView.findViewById(R.id.main_make_teams)).setText(getString(R.string.main_make_teams, DbHelper.getComingPlayersCount(getContext())));
    }

    private void setActionButtons() {
        rootView.findViewById(R.id.main_make_teams).setOnClickListener(view -> {
            launchMakeTeams();
        });
    }

    private void launchMakeTeams() {
        ArrayList<Player> comingPlayers = DbHelper.getComingPlayers(getContext(), 0);
        if (comingPlayers.size() > 0) {
            startActivity(MakeTeamsActivity.getIntent(getContext()));
        } else {
            Toast.makeText(getContext(), "First - select attending players", Toast.LENGTH_SHORT).show();
        }
    }

    private void setPlayersList(List<Player> players, AdapterView.OnItemClickListener clickHandler) {
        boolean hasPlayers = (players != null && players.size() > 0);
        playersList.setVisibility(hasPlayers ? View.VISIBLE : View.GONE);
        createPlayer.setVisibility(hasPlayers ? View.GONE : View.VISIBLE);

        setHeadlines(true);
        playersAdapter = new PlayerAdapter(getContext(), players, this::setComingPlayersCount);
        playersAdapter.setFilter(filterView.getFilter());

        if (clickHandler != null) {
            playersList.setOnItemClickListener(clickHandler);
        } else {
            playersList.setOnItemClickListener((adapterView, view, i, l) -> {
                Player p = (Player) view.getTag();
                Intent intent = PlayerDetailsActivity.getEditPlayerIntent(getContext(), p.mName);
                startActivity(intent);
            });
        }

        playersList.setOnItemLongClickListener((adapterView, view, i, l) -> {
            checkPlayerDeletion((Player) view.getTag());
            return true;
        });

        playersList.setAdapter(playersAdapter);

        setComingPlayersCount();
    }

    private void refreshPlayers() {
        ArrayList<Player> players = DbHelper.getPlayers(getContext(), RECENT_GAMES_COUNT, showArchivedPlayers);

        setPlayersList(players, null);

        sorting.sort(players);
    }

    private void switchArchivedPlayersView() {
        showArchivedPlayers = !showArchivedPlayers;
        if (showArchivedPlayers) {
            ArrayList<Player> players = DbHelper.getPlayers(getContext(), RECENT_GAMES_COUNT, showArchivedPlayers);
            if (players.size() == 0) {
                Toast.makeText(getContext(), "No archived players found", Toast.LENGTH_SHORT).show();
                showArchivedPlayers = false;
            }
        }

        backPress.setEnabled(showArchivedPlayers);
        refreshPlayers();
    }

    private void setHeadlines(boolean show) {

        FragmentActivity activity = getActivity();
        if (show) {
            sorting.setHeadlineSorting(rootView, R.id.player_name, this.getString(R.string.name), SortType.name);
            sorting.setHeadlineSorting(rootView, R.id.player_age, this.getString(R.string.age), SortType.age);
            sorting.setHeadlineSorting(rootView, R.id.player_attributes, this.getString(R.string.attributes), SortType.attributes);
            sorting.setHeadlineSorting(rootView, R.id.player_recent_performance, this.getString(R.string.plus_minus), SortType.suggestedGrade);
            sorting.setHeadlineSorting(rootView, R.id.player_grade, this.getString(R.string.grade), SortType.grade);

            rootView.findViewById(R.id.player_coming).setVisibility(View.GONE);
            rootView.findViewById(R.id.player_rsvp_title).setVisibility(View.VISIBLE);
            sorting.setHeadlineSorting(rootView, R.id.player_rsvp_title, "RSVP", SortType.coming);

            ((CheckBox) rootView.findViewById(R.id.player_coming)).setChecked(
                    sorting.getCurrentSorting().equals(SortType.coming) && sorting.isAscending());

        } else {
            sorting.removeHeadlineSorting(activity, R.id.player_name, this.getString(R.string.name));
            sorting.removeHeadlineSorting(activity, R.id.player_age, "");
            sorting.removeHeadlineSorting(activity, R.id.player_attributes, "");
            sorting.removeHeadlineSorting(activity, R.id.player_recent_performance, "");
            sorting.removeHeadlineSorting(activity, R.id.player_grade, "");
            sorting.removeHeadlineSorting(activity, R.id.player_coming, "");

            rootView.findViewById(R.id.player_coming).setVisibility(View.INVISIBLE);
            rootView.findViewById(R.id.player_rsvp_title).setVisibility(View.GONE);
        }
    }

    @Override
    public void sortingChanged() {
        refreshPlayers();
    }

    //region player archive & deletion
    private void checkPlayerDeletion(final Player player) {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());

        if (showArchivedPlayers) {
            alertDialogBuilder.setTitle("Do you want to remove the player?")
                    .setCancelable(true)
                    .setItems(new CharSequence[]
                                    {"Unarchive", "Remove", "Cancel"},
                            (dialog, which) -> {
                                switch (which) {
                                    case 0: // Unarchive
                                        DbHelper.archivePlayer(getContext(), player.mName, false);
                                        refreshPlayers();
                                        break;
                                    case 1: // Remove
                                        DbHelper.deletePlayer(getContext(), player.mName);
                                        refreshPlayers();
                                        break;
                                    case 2: // Cancel
                                        break;
                                }
                            });
        } else {
            alertDialogBuilder.setTitle("Do you want to archive the player?")
                    .setCancelable(true)
                    .setItems(new CharSequence[]
                                    {"Archive", "Cancel"},
                            (dialog, which) -> {
                                switch (which) {
                                    case 0: // Archive
                                        DbHelper.archivePlayer(getContext(), player.mName, true);
                                        refreshPlayers();
                                        break;
                                    case 2: // Cancel
                                        break;
                                }
                            });
        }


        alertDialogBuilder.create().show();
    }
    //endregion

    //region pasted players
    private void pasteComingPlayers() {
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData primaryClip = clipboard.getPrimaryClip();
        if (primaryClip == null) {
            Toast.makeText(getContext(), "Copy some whatsapp messages to set attending players", Toast.LENGTH_LONG).show();
            return;
        }
        ClipData.Item item = primaryClip.getItemAt(0);
        CharSequence pasteData = item.getText();

        try {
            Set<String> comingSet = new HashSet<>();
            String[] split = ((String) pasteData).split("\n");
            for (String coming : split) {
                try {
                    String numberOrName = coming.split("] ")[1].split(":")[0];
                    comingSet.add(numberOrName);
                } catch (Exception e) {
                    Log.e("Coming", "Failed to process " + coming);
                }
            }

            ArrayList<Player> players = DbHelper.getPlayers(getContext(), 0, false);
            String[] playerNames = new String[players.size()];
            int i = 0;
            for (Player p : players) {
                playerNames[i] = p.mName;
                i++;
            }

            if (comingSet.size() > 0) {
                displayPastedIdentifiers(comingSet, playerNames);
            } else {
                Toast.makeText(getContext(), "Paste multiple messages", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e("Coming", "Failed to process " + pasteData);
            Toast.makeText(getContext(), "Failed to process : " + pasteData, Toast.LENGTH_LONG).show();
        }
    }

    private void displayPastedIdentifiers(Set<String> set, String[] playerNames) {
        Set<String> comingSet = new HashSet<>(set);
        ArrayList<String> coming = new ArrayList<>(comingSet);

        ArrayList<Player> knownPlayers = DbHelper.getPlayersByIdentifier(getContext(), coming);
        for (Player p : knownPlayers) {
            comingSet.remove(p.msgDisplayName);
        }

        for (String identifier : comingSet) {
            Player player = new Player(null, -1);
            player.msgDisplayName = identifier;
            knownPlayers.add(player);
        }

        showPastedPlayers = true;
        backPress.setEnabled(true);

        AdapterView.OnItemClickListener handler = (parent, view, position, id) -> {
            Player p = (Player) view.getTag();
            Log.i("Identify", "Clicked on " + p.mName + " = " + p.msgDisplayName);
            setComingPlayerIdentity(p.mName, p.msgDisplayName, set, playerNames);
        };

        // Filter the players list only to the pasted players identifiers
        setPlayersList(knownPlayers, handler);
        setHeadlines(false);
    }

    private void setComingPlayerIdentity(String currPlayer, String identity, Set<String> comingSet, String[] playerNames) {

        if (TextUtils.isEmpty(identity)) {
            Log.i("Identifier", "Empty identifier");
            Toast.makeText(getContext(), "Empty identifier can't be set", Toast.LENGTH_LONG).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Enter player name for : \n" + identity);

        final AutoCompleteTextView input = new AutoCompleteTextView(getContext());
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_dropdown_item_1line, playerNames);
        input.setAdapter(adapter);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.requestFocus();
        input.setText(currPlayer);
        builder.setView(input);

        if (TextUtils.isEmpty(currPlayer)) {
            builder.setNeutralButton("New Player", (dialogInterface, i) -> {
                startActivity(PlayerDetailsActivity.getNewPlayerFromIdentifierIntent(getContext(), identity));
            });
        }

        builder.setPositiveButton("OK", (dialog, which) -> {
            String newPlayer = input.getText().toString();
            if (!TextUtils.isEmpty(newPlayer) && !TextUtils.isEmpty(currPlayer) &&
                    newPlayer.equals(currPlayer)) { // current name for identifier hasn't changed
                Log.i("Identifier", "Curr name " + currPlayer + " not modified for " + identity);
                return;
            }
            if (TextUtils.isEmpty(newPlayer)) {
                if (!TextUtils.isEmpty(currPlayer)) { // empty name, for current player == clearing
                    Log.i("Identifier", "Count 0 - Clearing " + currPlayer);
                    DbHelper.clearPlayerIdentifier(getContext(), currPlayer);
                    Toast.makeText(getContext(), "Cleared " + currPlayer, Toast.LENGTH_LONG).show();
                    displayPastedIdentifiers(comingSet, playerNames);
                } else { // empty name set
                    Log.i("Identifier", "No name set for identifier " + identity);
                }
                return;
            }

            Log.i("Identifier", "Identify " + newPlayer + " as " + identity);
            int count = DbHelper.setPlayerIdentifier(getContext(), newPlayer, identity);
            if (count == 1) { // new player found and updated with the identifier
                Log.i("Identifier", "Count + " + count + " remove identifier from " + currPlayer);
                Toast.makeText(getContext(), "Set " + newPlayer + " with " + identity, Toast.LENGTH_SHORT).show();
                if (currPlayer != null) { // clear previous name from the identifier
                    DbHelper.clearPlayerIdentifier(getContext(), currPlayer);
                }
                displayPastedIdentifiers(comingSet, playerNames);
            } else { // new player name not found
                Log.i("Identifier", "Count 0 - " + newPlayer + " not found");
                Toast.makeText(getContext(), newPlayer + " not found", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }
    //endregion

    //region broadcasts
    class PlayerUpdateBroadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("Broadcast players", "new data");
            refreshPlayers();
        }
    }
    //endregion
}
