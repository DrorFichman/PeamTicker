package com.teampicker.drorfichman.teampicker.View;

import static com.teampicker.drorfichman.teampicker.tools.TutorialManager.TutorialDisplayState.NotDisplayed;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.snackbar.Snackbar;
import com.teampicker.drorfichman.teampicker.Adapter.PlayerAdapter;
import com.teampicker.drorfichman.teampicker.Controller.Broadcast.LocalNotifications;
import com.teampicker.drorfichman.teampicker.Controller.Search.FilterView;
import com.teampicker.drorfichman.teampicker.Controller.Sort.SortType;
import com.teampicker.drorfichman.teampicker.Controller.Sort.Sorting;
import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.DialogHelper;
import com.teampicker.drorfichman.teampicker.tools.TutorialManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlayersFragment extends Fragment implements Sorting.sortingCallbacks {

    private static final int RECENT_GAMES_COUNT = 10; // for +/- grade suggestion

    Sorting sorting = new Sorting(this::sortingChanged, SortType.coming);

    private boolean showArchivedPlayers = false;
    private boolean showPastedPlayers = false;
    private Set<String> mPastedPlayers;

    private View rootView;
    private ListView playersList;
    private FilterView filterView;

    private PlayerAdapter playersAdapter;

    private PlayerUpdateBroadcast notificationHandler;

    private View p;
    private ProgressBar progress;
    private TextView progressText;

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

        setTutorials(root);

        refreshPlayers();

        setActionButtons();
        setComingPlayersCount();

        return root;
    }

    //region tutorials
    private void setTutorials(View root) {
        p = root.findViewById(R.id.tutorial_main_layout);
        progress = root.findViewById(R.id.tutorial_progress);
        progressText = root.findViewById(R.id.tutorial_progress_text);

        p.setOnClickListener(view -> onTutorialClicked(progress.getProgress()));

        showTutorials();
    }

    private void onTutorialClicked(int progressStatus) {
        if (progressStatus == 100) {
            DialogHelper.showApprovalDialog(getContext(), getString(R.string.tutorial_completed_title),
                    getString(R.string.tutorial_completed_message),
                    (dialogInterface, i) -> p.setVisibility(View.GONE));
        } else {
            TutorialManager.displayTutorialFlow(getContext(), null);
        }
    }

    private void updateTutorialProgress() {
        int progressFrom = progress.getProgress();
        if (getContext() == null) return;

        int tutorialProgress = TutorialManager.getProgress(getContext());
        if (progressFrom == 0 && tutorialProgress == 100) p.setVisibility(View.GONE);

        if (progressFrom < tutorialProgress) {
            final Runnable r = () -> {
                int from = progressFrom + Math.max(1, (tutorialProgress - progressFrom) / 6);
                setProgress(from);
                updateTutorialProgress();
            };
            new Handler().postDelayed(r, 100);
        } else {
            setProgress(tutorialProgress);
        }
    }

    private void setProgress(int value) {
        this.progress.setProgress(value);
        progressText.setText(String.valueOf(value) + '%');
    }

    private void showTutorials() {
        if (TutorialManager.isSkipAllTutorials(getContext())) {
            p.setVisibility(View.GONE);
            return;
        }

        p.setVisibility(progress.getProgress() == 100 ? View.GONE : View.VISIBLE);

        updateTutorialProgress();

        TutorialManager.TutorialDisplayState show = TutorialManager.displayTutorialStep(getContext(), TutorialManager.Tutorials.players, false);
        if (show == NotDisplayed)
            show = TutorialManager.displayTutorialStep(getContext(), TutorialManager.Tutorials.attendance, false);
        if (show == NotDisplayed)
            show = TutorialManager.displayTutorialStep(getContext(), TutorialManager.Tutorials.start_pick_teams, false);
        if (show == NotDisplayed)
            show = TutorialManager.displayTutorialStep(getContext(), TutorialManager.Tutorials.save_results, false);
        if (show == NotDisplayed)
            show = TutorialManager.displayTutorialStep(getContext(), TutorialManager.Tutorials.game_history, false);
        if (show == NotDisplayed)
            show = TutorialManager.displayTutorialStep(getContext(), TutorialManager.Tutorials.cloud, false);
    }
    //endregion

    final OnBackPressedCallback backPress = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            if (filterView != null && filterView.isExpanded()) {
                filterView.collapseSearchView();
                backPress.setEnabled(handleBackPress());
                return;
            }

            if (showArchivedPlayers) {
                showArchivedPlayers = false;
                refreshPlayers();
                backPress.setEnabled(handleBackPress());
                return;
            }

            if (showPastedPlayers) {
                showPastedPlayers = false;
                mPastedPlayers = null;
                refreshPlayers();
                backPress.setEnabled(handleBackPress());
                return;
            }

            backPress.setEnabled(handleBackPress());
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        notificationHandler = new PlayerUpdateBroadcast();
        LocalNotifications.registerBroadcastReceiver(getContext(), LocalNotifications.PLAYER_UPDATE_ACTION, notificationHandler);
        LocalNotifications.registerBroadcastReceiver(getContext(), LocalNotifications.PULL_DATA_ACTION, notificationHandler);
        LocalNotifications.registerBroadcastReceiver(getContext(), LocalNotifications.SETTING_MODIFIED_ACTION, notificationHandler);

        requireActivity().getOnBackPressedDispatcher().addCallback(this, backPress);
        backPress.setEnabled(false);
    }

    private boolean handleBackPress() {
        return (showArchivedPlayers || showPastedPlayers ||
                (filterView != null && filterView.isExpanded()));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalNotifications.unregisterBroadcastReceiver(getContext(), notificationHandler);
    }

    @Override
    public void onResume() {
        super.onResume();
        backPress.setEnabled(handleBackPress());
        showTutorials();
    }

    @Override
    public void onPause() {
        super.onPause();
        backPress.setEnabled(false);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.players_menu, menu);
        setSearchView((SearchView) menu.findItem(R.id.player_search).getActionView());
        super.onCreateOptionsMenu(menu, inflater);
    }

    private void setSearchView(SearchView view) {
        filterView = new FilterView(view, value -> {
            playersAdapter.setFilter(value);
            int pos = playersAdapter.positionOfFirstFilterItem(() ->
                    Snackbar.make(getContext(), playersList, "no results", Snackbar.LENGTH_SHORT).show());
            playersList.smoothScrollToPosition(pos);
            backPress.setEnabled(handleBackPress());
        });
        if (playersAdapter != null) playersAdapter.setFilter(null);
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
        // TODO ugly
        if (progress.getProgress() < 20) showTutorials(); // when attendance is relevant..
    }

    private void setActionButtons() {
        rootView.findViewById(R.id.main_make_teams).setOnClickListener(view -> {
            launchMakeTeams();
        });
    }

    private void launchMakeTeams() {
        ArrayList<Player> comingPlayers = DbHelper.getComingPlayers(getContext(), 0);
        if (comingPlayers.size() > 0) {
            TutorialManager.userActionTaken(getContext(), TutorialManager.TutorialUserAction.clicked_teams);
            startActivity(MakeTeamsActivity.getIntent(getContext()));
        } else {
            Toast.makeText(getContext(), "First - select attending players", Toast.LENGTH_SHORT).show();
        }
    }

    private void setPlayersList(List<Player> players, AdapterView.OnItemClickListener clickHandler) {
        boolean hasPlayers = (players != null && players.size() > 0);
        playersList.setVisibility(hasPlayers ? View.VISIBLE : View.GONE);

        setHeadlines(true);
        playersAdapter = new PlayerAdapter(getContext(), players, this::setComingPlayersCount);

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
        if (showPastedPlayers && mPastedPlayers != null) {
            displayPastedIdentifiers(mPastedPlayers);
            return;
        }

        ArrayList<Player> players = DbHelper.getPlayers(getContext(), RECENT_GAMES_COUNT, showArchivedPlayers);

        setPlayersList(players, null);

        sorting.sort(players);
    }

    private void switchArchivedPlayersView() {
        showArchivedPlayers = !showArchivedPlayers;
        if (showArchivedPlayers) {
            showPastedPlayers = false;
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
            Set<String> pasted = new HashSet<>();
            String[] split = ((String) pasteData).split("\n");
            for (String coming : split) {
                try {
                    String numberOrName = coming.split("] ")[1].split(":")[0];
                    pasted.add(numberOrName);
                } catch (Exception e) {
                    Log.e("Coming", "Failed to process " + coming);
                }
            }

            if (pasted.size() > 0) {
                displayPastedIdentifiers(pasted);
            } else {
                Toast.makeText(getContext(), "Paste multiple messages", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e("Coming", "Failed to process " + pasteData);
            Toast.makeText(getContext(), "Failed to process : " + pasteData, Toast.LENGTH_LONG).show();
        }
    }

    private void displayPastedIdentifiers(Set<String> pasted) {
        Set<String> unknownPastedSet = new HashSet<>(pasted);
        ArrayList<String> pastedArray = new ArrayList<>(unknownPastedSet);

        ArrayList<Player> knownPasted = DbHelper.getPlayersByIdentifier(getContext(), pastedArray);
        for (Player known : knownPasted) {
            unknownPastedSet.remove(known.msgDisplayName);
        }

        ArrayList<Player> unknownPasted = new ArrayList<>();
        for (String identifier : unknownPastedSet) {
            Player unknownPastedPlayer = new Player(null, -1);
            unknownPastedPlayer.msgDisplayName = identifier;
            unknownPasted.add(unknownPastedPlayer);
        }

        ArrayList<Player> pastedPlayers = new ArrayList<>();
        pastedPlayers.addAll(knownPasted);
        pastedPlayers.addAll(unknownPasted);

        showPastedPlayers = true;
        mPastedPlayers = pasted;
        backPress.setEnabled(true);

        String[] allPlayerNames = DbHelper.getPlayersNames(getContext());
        AdapterView.OnItemClickListener handler = (parent, view, position, id) -> {
            Player p = (Player) view.getTag();
            Log.i("Identify", "Clicked on " + p.mName + " = " + p.msgDisplayName);
            setComingPlayerIdentity(p.mName, p.msgDisplayName, pasted, allPlayerNames);
        };

        // Filter the players list only to the pasted players identifiers
        setPlayersList(pastedPlayers, handler);
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
                    displayPastedIdentifiers(comingSet);
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
                displayPastedIdentifiers(comingSet);
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
            showTutorials();
        }
    }
    //endregion
}
