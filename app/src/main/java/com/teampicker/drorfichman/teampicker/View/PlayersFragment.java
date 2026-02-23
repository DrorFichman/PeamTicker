package com.teampicker.drorfichman.teampicker.View;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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
import android.widget.SearchView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.tabs.TabLayout;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.teampicker.drorfichman.teampicker.Adapter.PlayerAdapter;
import com.teampicker.drorfichman.teampicker.Controller.Broadcast.LocalNotifications;
import com.teampicker.drorfichman.teampicker.Controller.Search.FilterView;
import com.teampicker.drorfichman.teampicker.Controller.Sort.SortType;
import com.teampicker.drorfichman.teampicker.Controller.Sort.Sorting;
import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.DbAsync;
import com.teampicker.drorfichman.teampicker.tools.PreferenceHelper;
import com.teampicker.drorfichman.teampicker.tools.SettingsHelper;
import com.teampicker.drorfichman.teampicker.tools.analytics.Event;
import com.teampicker.drorfichman.teampicker.tools.analytics.EventType;
import com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlayersFragment extends Fragment implements Sorting.sortingCallbacks {

    private static final int RECENT_GAMES_COUNT = 10; // for +/- grade suggestion

    Sorting sorting = new Sorting(this, SortType.coming);
    boolean playerComingChanged = false;

    private boolean showArchivedPlayers = false;
    private boolean showInjuredPlayers = false;
    private boolean showPastedPlayers = false;
    private Set<String> mPastedPlayers;
    private final Set<String> mAutoCreatedPlayers = new HashSet<>(); // Track auto-created players for undo

    private View rootView;
    private ListView playersList;
    private FilterView filterView;
    private String currentFilterValue;

    private PlayerAdapter playersAdapter;

    private PlayerUpdateBroadcast notificationHandler;

    // Tutorial target views
    private View makeTeamsButton;
    private View exitPastedModeButton;
    private View cancelPastedModeButton;
    private View emptyStateContainer;
    private View emptyInjuredStateContainer;

    // Contact import
    private ActivityResultLauncher<String> contactPermissionLauncher;
    private boolean hadPlayersBeforeImport = false;

    public PlayersFragment() {
        super(R.layout.layout_players_fragment);
    }

    public static PlayersFragment newInstance() {
        return new PlayersFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        setHasOptionsMenu(true);

        this.rootView = root;
        assert root != null;
        playersList = root.findViewById(R.id.players_list);
        makeTeamsButton = root.findViewById(R.id.main_make_teams);
        exitPastedModeButton = root.findViewById(R.id.exit_pasted_mode);
        cancelPastedModeButton = root.findViewById(R.id.cancel_pasted_mode);
        emptyStateContainer = root.findViewById(R.id.empty_state_container);
        emptyInjuredStateContainer = root.findViewById(R.id.empty_injured_state_container);

        // Set up empty state buttons
        root.findViewById(R.id.empty_state_paste_button).setOnClickListener(v -> pasteComingPlayers());
        root.findViewById(R.id.empty_state_import_button).setOnClickListener(v -> launchContactPicker());

        refreshPlayers();

        setActionButtons();
        setComingPlayersCount();

        return root;
    }

    // region tutorials
    private void showTutorials() {
        if (TutorialManager.isSkipAllTutorials(getContext())) {
            return;
        }

        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        // Get toolbar from activity for menu item tutorials
        Toolbar toolbar = activity.findViewById(R.id.toolbar);

        // Show tutorials in sequence - only one will show at a time
        // For attendance tutorial, target the header row's RSVP title (always visible)
        View attendanceTarget = getAttendanceTargetView();
        boolean shown = TutorialManager.displayTutorialStep(activity, TutorialManager.Tutorials.attendance,
                attendanceTarget, false);

        if (!shown) {
            shown = TutorialManager.displayTutorialStep(activity, TutorialManager.Tutorials.start_pick_teams,
                    makeTeamsButton, false);
        }
        if (!shown) {
            // save_results doesn't have a good target on this screen, show as info dialog
            // if applicable
            shown = TutorialManager.displayTutorialStepAsDialog(activity, TutorialManager.Tutorials.save_results,
                    false);
        }
        if (!shown) {
            // game_history - target the Games tab
            View gamesTabTarget = getGamesTabView(activity);
            shown = TutorialManager.displayTutorialStep(activity, TutorialManager.Tutorials.game_history,
                    gamesTabTarget, false);
        }
        if (!shown) {
            // cloud - show as dialog since navigation drawer isn't easily targetable
            TutorialManager.displayTutorialStepAsDialog(activity, TutorialManager.Tutorials.cloud, false);
        }
    }
    // endregion

    private View getAttendanceTargetView() {
        // Use the header row's RSVP title as target (always visible)
        // This is in the player_titles include
        if (rootView != null) {
            View headerRow = rootView.findViewById(R.id.player_titles);
            if (headerRow != null) {
                View rsvpTitle = headerRow.findViewById(R.id.player_rsvp_title);
                if (rsvpTitle != null && rsvpTitle.getVisibility() == View.VISIBLE) {
                    return rsvpTitle;
                }

                // Fallback to the checkbox in header if RSVP title is not visible
                View checkbox = headerRow.findViewById(R.id.player_coming);
                if (checkbox != null) {
                    return checkbox;
                }
            }
        }

        // Fallback to first player's checkbox if header not available
        if (playersList != null && playersList.getChildCount() > 0) {
            View firstItem = playersList.getChildAt(0);
            if (firstItem != null) {
                return firstItem.findViewById(R.id.player_coming);
            }
        }

        return null;
    }

    private View getGamesTabView(Activity activity) {
        // Get the Games tab (index 1) from the TabLayout
        TabLayout tabLayout = activity.findViewById(R.id.main_tab);
        if (tabLayout != null) {
            TabLayout.Tab gamesTab = tabLayout.getTabAt(1); // Index 1 = Games tab
            if (gamesTab != null) {
                return gamesTab.view;
            }
        }
        return null;
    }

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

            if (showInjuredPlayers) {
                showInjuredPlayers = false;
                refreshPlayers();
                backPress.setEnabled(handleBackPress());
                return;
            }

            if (showPastedPlayers) {
                // If there are auto-created players, cancel (undo) instead of just exiting
                if (!mAutoCreatedPlayers.isEmpty()) {
                    cancelPastedPlayersMode();
                } else {
                    exitPastedPlayersMode();
                }
                return;
            }

            backPress.setEnabled(handleBackPress());
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        notificationHandler = new PlayerUpdateBroadcast();
        LocalNotifications.registerBroadcastReceiver(getContext(), LocalNotifications.PLAYER_UPDATE_ACTION,
                notificationHandler);
        LocalNotifications.registerBroadcastReceiver(getContext(), LocalNotifications.PULL_DATA_ACTION,
                notificationHandler);
        LocalNotifications.registerBroadcastReceiver(getContext(), LocalNotifications.SETTING_MODIFIED_ACTION,
                notificationHandler);
        LocalNotifications.registerBroadcastReceiver(getContext(), LocalNotifications.GAME_UPDATE_ACTION,
                notificationHandler);

        requireActivity().getOnBackPressedDispatcher().addCallback(this, backPress);
        backPress.setEnabled(false);

        // Register contact permission launcher
        contactPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        showContactSelectionDialog();
                    } else {
                        Toast.makeText(getContext(), R.string.import_contacts_permission_denied, Toast.LENGTH_LONG)
                                .show();
                    }
                });
    }

    private boolean handleBackPress() {
        return (showArchivedPlayers || showInjuredPlayers || showPastedPlayers ||
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

        // Delay tutorial showing to ensure views are ready
        if (rootView != null) {
            rootView.post(this::showTutorials);
        }

        Log.i("Coming", "resume " + playerComingChanged);

        if (playerComingChanged) {
            playerComingChanged = false;
            refreshPlayers();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        backPress.setEnabled(false);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.players_menu, menu);
        setSearchView((SearchView) menu.findItem(R.id.player_search).getActionView());
        super.onCreateOptionsMenu(menu, inflater);
    }

    private void setSearchView(SearchView view) {
        filterView = new FilterView(view, value -> {
            currentFilterValue = value;
            if (TextUtils.isEmpty(value)) {
                // Refresh the list to apply sorting after search is cleared
                refreshPlayers();
            } else {
                playersAdapter.setFilter(value);
                int pos = playersAdapter.positionOfFirstFilterItem(
                        () -> Snackbar.make(requireContext(), playersList, "no results", Snackbar.LENGTH_SHORT).show());
                playersList.smoothScrollToPosition(pos);
            }
            backPress.setEnabled(handleBackPress());
        });
        if (playersAdapter != null)
            playersAdapter.setFilter(null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.make_teams) {
            launchMakeTeams();
        } else if (item.getItemId() == R.id.paste_coming_players) {
            pasteComingPlayers();
        } else if (item.getItemId() == R.id.import_contacts) {
            launchContactPicker();
        } else if (item.getItemId() == R.id.add_player) {
            startActivity(PlayerDetailsActivity.getNewPlayerIntent(getContext()));
        } else if (item.getItemId() == R.id.show_injured_players) {
            switchInjuredPlayersView();
        } else if (item.getItemId() == R.id.show_archived_players) {
            switchArchivedPlayersView();
        } else if (item.getItemId() == R.id.clear_all) {
            clearAttendance();
        }

        return super.onOptionsItemSelected(item);
    }

    private void clearAttendance() {
        DbAsync.runWrite(
                () -> DbHelper.clearComingPlayers(getContext()),
                () -> {
                    if (!isAdded()) return;
                    refreshPlayers();
                    Event.logEvent(FirebaseAnalytics.getInstance(requireContext()), EventType.clear_attendance);
                });
    }

    private void onPlayerComingChanged(boolean coming) {
        playerComingChanged = coming || playerComingChanged;
        Log.i("Coming", "Player coming changed " + playerComingChanged + ", " + coming);

        setComingPlayersCount();
    }

    private void onPlayerGradeChanged(Player player, int newGrade) {
        DbAsync.runWrite(
                () -> DbHelper.updatePlayerGrade(getContext(), player.mName, newGrade),
                null);
        Log.i("Grade", "Player grade changed: " + player.mName + " -> " + newGrade);
    }

    private void setComingPlayersCount() {
        android.content.Context ctx = getContext();
        if (ctx == null) return;
        DbAsync.run(
                () -> DbHelper.getComingPlayersCount(ctx),
                count -> {
                    if (!isAdded()) return;
                    ((Button) rootView.findViewById(R.id.main_make_teams))
                            .setText(getString(R.string.main_make_teams, count));
                });
    }

    private void setActionButtons() {
        rootView.findViewById(R.id.main_make_teams).setOnClickListener(view -> launchMakeTeams());
        rootView.findViewById(R.id.main_make_teams).setOnLongClickListener(v -> {
            TeamCountSelectionDialog.show(getContext(), this::launchTeamsActivity);
            return true;
        });
        exitPastedModeButton.setOnClickListener(view -> savePastedPlayersMode());
        cancelPastedModeButton.setOnClickListener(view -> cancelPastedPlayersMode());
    }

    private void exitPastedPlayersMode() {
        showPastedPlayers = false;
        mPastedPlayers = null;
        mAutoCreatedPlayers.clear();
        updatePastedModeUI();
        refreshPlayers();
        backPress.setEnabled(handleBackPress());
    }

    private void savePastedPlayersMode() {
        // Keep the auto-created players and exit pasted mode
        mAutoCreatedPlayers.clear();
        exitPastedPlayersMode();
    }

    private void cancelPastedPlayersMode() {
        android.content.Context ctx = getContext();
        if (ctx == null) { exitPastedPlayersMode(); return; }
        Set<String> autoCreated = new HashSet<>(mAutoCreatedPlayers);
        ArrayList<String> pastedList = mPastedPlayers != null ? new ArrayList<>(mPastedPlayers) : null;

        DbAsync.runWrite(
                () -> {
                    for (String playerName : autoCreated) {
                        DbHelper.deletePlayer(ctx, playerName);
                    }
                    if (pastedList != null) {
                        ArrayList<Player> existingPasted = DbHelper.getPlayersByIdentifier(ctx, pastedList);
                        for (Player p : existingPasted) {
                            if (!autoCreated.contains(p.mName)) {
                                DbHelper.updatePlayerComing(ctx, p.mName, false);
                            }
                        }
                    }
                },
                () -> { if (isAdded()) exitPastedPlayersMode(); });
    }

    private void updatePastedModeUI() {
        makeTeamsButton.setVisibility(showPastedPlayers ? View.GONE : View.VISIBLE);
        // Show "Save" button text when there are auto-created players, otherwise "Done"
        Button exitButton = (Button) exitPastedModeButton;
        if (showPastedPlayers && !mAutoCreatedPlayers.isEmpty()) {
            exitButton.setText(R.string.save);
        } else {
            exitButton.setText(R.string.done);
        }
        exitPastedModeButton.setVisibility(showPastedPlayers ? View.VISIBLE : View.GONE);
        // Only show cancel button if there are auto-created players
        cancelPastedModeButton
                .setVisibility(showPastedPlayers && !mAutoCreatedPlayers.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void launchMakeTeams() {
        // Check if we have coming players first
        Intent makeTeamsIntent = MakeTeamsActivity.getIntent(getContext());
        if (makeTeamsIntent == null) {
            Toast.makeText(getContext(), getString(R.string.toast_instruction_select_players_first), Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        // Check if dialog has been shown before
        if (!TeamCountSelectionDialog.hasDialogBeenShown(getContext())) {
            // Show dialog for first time
            TeamCountSelectionDialog.show(getContext(), teamCount -> {
                launchTeamsActivity(teamCount);
            });
        } else {
            // Use saved preference
            int teamCount = SettingsHelper.getTeamCount(getContext());
            Log.d("team_count", "launchMakeTeams: " + teamCount);
            launchTeamsActivity(teamCount);
        }
    }

    private void launchTeamsActivity(int teamCount) {
        Intent intent;
        if (teamCount == 3) {
            intent = Make3TeamsActivity.getIntent(getContext());
        } else {
            intent = MakeTeamsActivity.getIntent(getContext());
        }

        if (intent != null) {
            startActivity(intent);
        }
    }

    private boolean shouldShowIndications() {
        return !showPastedPlayers && !showArchivedPlayers;
    }

    private void setPlayersList(List<Player> players, AdapterView.OnItemClickListener clickHandler) {
        boolean hasPlayers = (players != null && !players.isEmpty());
        playersList.setVisibility(hasPlayers ? View.VISIBLE : View.GONE);

        // Show empty state only when no players and not in special modes
        boolean showEmptyState = !hasPlayers && !showPastedPlayers && !showArchivedPlayers && !showInjuredPlayers;
        boolean showEmptyInjuredState = !hasPlayers && showInjuredPlayers;
        emptyStateContainer.setVisibility(showEmptyState ? View.VISIBLE : View.GONE);
        emptyInjuredStateContainer.setVisibility(showEmptyInjuredState ? View.VISIBLE : View.GONE);
        boolean hideHeaders = showEmptyState || showEmptyInjuredState;
        rootView.findViewById(R.id.player_titles).setVisibility(hideHeaders ? View.GONE : View.VISIBLE);
        rootView.findViewById(R.id.header_divider).setVisibility(hideHeaders ? View.GONE : View.VISIBLE);

        setHeadlines(true);
        playersAdapter = new PlayerAdapter(getContext(), players, this::onPlayerComingChanged);
        playersAdapter.setShowIndications(shouldShowIndications());
        playersAdapter.setOnPlayerGradeChangeListener(this::onPlayerGradeChanged);

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

        android.content.Context ctx = getContext();
        if (ctx == null) return;
        boolean injured = showInjuredPlayers;
        boolean archived = showArchivedPlayers;

        DbAsync.run(
                () -> injured
                        ? DbHelper.getInjuredPlayers(ctx, RECENT_GAMES_COUNT)
                        : DbHelper.getPlayers(ctx, RECENT_GAMES_COUNT, archived),
                players -> {
                    if (!isAdded()) return;
                    setPlayersList(players, null);
                    sorting.sort(players);
                    if (!TextUtils.isEmpty(currentFilterValue)) {
                        playersAdapter.setFilter(currentFilterValue);
                    }
                });
    }

    private void switchArchivedPlayersView() {
        showArchivedPlayers = !showArchivedPlayers;
        if (showArchivedPlayers) {
            showPastedPlayers = false;
            showInjuredPlayers = false;
            android.content.Context ctx = getContext();
            if (ctx == null) return;
            DbAsync.run(
                    () -> DbHelper.getPlayers(ctx, 0, true),
                    players -> {
                        if (!isAdded()) return;
                        if (players.isEmpty()) {
                            Toast.makeText(getContext(),
                                    getString(R.string.toast_instruction_no_archived_players),
                                    Toast.LENGTH_SHORT).show();
                            showArchivedPlayers = false;
                        }
                        backPress.setEnabled(showArchivedPlayers);
                        refreshPlayers();
                        Event.logEvent(FirebaseAnalytics.getInstance(requireContext()), EventType.players_archive);
                    });
        } else {
            backPress.setEnabled(false);
            refreshPlayers();
            Event.logEvent(FirebaseAnalytics.getInstance(requireContext()), EventType.players_archive);
        }
    }

    private void switchInjuredPlayersView() {
        showInjuredPlayers = !showInjuredPlayers;
        if (showInjuredPlayers) {
            showPastedPlayers = false;
            showArchivedPlayers = false;
        }

        backPress.setEnabled(showInjuredPlayers);
        refreshPlayers();
    }

    private void setHeadlines(boolean show) {

        FragmentActivity activity = getActivity();
        if (show) {
            sorting.setHeadlineSorting(rootView, R.id.player_name, this.getString(R.string.name), SortType.name);
            sorting.setHeadlineSorting(rootView, R.id.player_age, this.getString(R.string.age), SortType.age);
            sorting.setHeadlineSorting(rootView, R.id.player_attributes, this.getString(R.string.attributes),
                    SortType.attributes);
            sorting.setHeadlineSorting(rootView, R.id.player_recent_performance, this.getString(R.string.plus_minus),
                    SortType.suggestedGrade);
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

    // region player archive & deletion
    private void checkPlayerDeletion(final Player player) {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(requireContext());

        if (showArchivedPlayers) {
            alertDialogBuilder.setTitle("Do you want to remove the player?")
                    .setCancelable(true)
                    .setItems(new CharSequence[] { "Unarchive", "Remove", "Cancel" },
                            (dialog, which) -> {
                                switch (which) {
                                    case 0: // Unarchive
                                        DbAsync.runWrite(
                                                () -> DbHelper.archivePlayer(getContext(), player.mName, false),
                                                () -> { if (isAdded()) refreshPlayers(); });
                                        break;
                                    case 1: // Remove
                                        DbAsync.runWrite(
                                                () -> DbHelper.deletePlayer(getContext(), player.mName),
                                                () -> { if (isAdded()) refreshPlayers(); });
                                        break;
                                    case 2: // Cancel
                                        break;
                                }
                            });
        } else {
            alertDialogBuilder.setTitle("Do you want to archive the player?")
                    .setCancelable(true)
                    .setItems(new CharSequence[] { "Archive", "Cancel" },
                            (dialog, which) -> {
                                switch (which) {
                                    case 0: // Archive
                                        DbAsync.runWrite(
                                                () -> DbHelper.archivePlayer(getContext(), player.mName, true),
                                                () -> { if (isAdded()) refreshPlayers(); });
                                        break;
                                    case 2: // Cancel
                                        break;
                                }
                            });
        }

        alertDialogBuilder.create().show();
    }
    // endregion

    // region pasted players
    private void pasteComingPlayers() {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData primaryClip = clipboard.getPrimaryClip();
        if (primaryClip == null) {
            Toast.makeText(getContext(), getString(R.string.toast_instruction_paste_whatsapp), Toast.LENGTH_LONG)
                    .show();
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

            if (!pasted.isEmpty()) {
                Event.logEvent(FirebaseAnalytics.getInstance(requireContext()), EventType.paste_players);
                // Clear auto-created players from previous paste
                mAutoCreatedPlayers.clear();
                displayPastedIdentifiers(pasted);
            } else {
                Toast.makeText(getContext(), getString(R.string.toast_instruction_paste_multiple), Toast.LENGTH_SHORT)
                        .show();
            }

        } catch (Exception e) {
            Log.e("Coming", "Failed to process " + pasteData);
            Toast.makeText(getContext(), getString(R.string.toast_error_paste_process_failed, pasteData),
                    Toast.LENGTH_LONG).show();
        }
    }

    private static class PasteDisplayData {
        final ArrayList<Player> knownPasted;
        final String[] allPlayerNames;
        final ArrayList<String> archivedPlayers;
        final Set<String> autoCreated;

        PasteDisplayData(ArrayList<Player> knownPasted, String[] allPlayerNames,
                         ArrayList<String> archivedPlayers, Set<String> autoCreated) {
            this.knownPasted = knownPasted;
            this.allPlayerNames = allPlayerNames;
            this.archivedPlayers = archivedPlayers;
            this.autoCreated = autoCreated;
        }
    }

    private void displayPastedIdentifiers(Set<String> pasted) {
        android.content.Context ctx = getContext();
        if (ctx == null) return;
        ArrayList<String> pastedArray = new ArrayList<>(pasted);
        Set<String> prevAutoCreated = new HashSet<>(mAutoCreatedPlayers);

        DbAsync.run(
                () -> {
                    Set<String> unknownSet = new HashSet<>(pasted);
                    ArrayList<Player> known = DbHelper.getPlayersByIdentifier(ctx, pastedArray);
                    for (Player k : known) unknownSet.remove(k.msgDisplayName);

                    ArrayList<String> archived = new ArrayList<>();
                    Set<String> newAutoCreated = new HashSet<>(prevAutoCreated);
                    for (String identifier : unknownSet) {
                        if (newAutoCreated.contains(identifier)) continue;
                        Player existing = DbHelper.getPlayer(ctx, identifier);
                        if (existing != null) {
                            if (existing.archived) {
                                archived.add(identifier);
                                Log.i("Identify", "Player " + identifier + " exists in archive, skipping");
                            }
                            continue;
                        }
                        Player newPlayer = new Player(identifier, 80);
                        newPlayer.msgDisplayName = identifier;
                        if (DbHelper.insertPlayer(ctx, newPlayer)) {
                            DbHelper.setPlayerIdentifier(ctx, identifier, identifier);
                            newAutoCreated.add(identifier);
                            Log.i("Identify", "Auto-created player: " + identifier);
                        }
                    }

                    // Re-fetch and mark all pasted as coming
                    ArrayList<Player> finalKnown = DbHelper.getPlayersByIdentifier(ctx, pastedArray);
                    for (Player player : finalKnown) {
                        if (!player.isComing) {
                            DbHelper.updatePlayerComing(ctx, player.mName, true);
                            player.isComing = true;
                        }
                    }
                    String[] allNames = DbHelper.getPlayersNames(ctx);
                    return new PasteDisplayData(finalKnown, allNames, archived, newAutoCreated);
                },
                data -> {
                    if (!isAdded()) return;
                    mAutoCreatedPlayers.clear();
                    mAutoCreatedPlayers.addAll(data.autoCreated);

                    if (!data.archivedPlayers.isEmpty()) {
                        String message = data.archivedPlayers.size() == 1
                                ? "Player \"" + data.archivedPlayers.get(0) + "\" exists in archive. Unarchive to use."
                                : data.archivedPlayers.size() + " players exist in archive: "
                                        + TextUtils.join(", ", data.archivedPlayers);
                        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                    }

                    showPastedPlayers = true;
                    mPastedPlayers = pasted;
                    backPress.setEnabled(true);
                    updatePastedModeUI();

                    AdapterView.OnItemClickListener handler = (parent, view, position, id) -> {
                        Player p = (Player) view.getTag();
                        Log.i("Identify", "Clicked on " + p.mName + " = " + p.msgDisplayName);
                        setComingPlayerIdentity(p.mName, p.msgDisplayName, pasted, data.allPlayerNames);
                    };
                    setPlayersList(data.knownPasted, handler);
                    setHeadlines(false);
                    setComingPlayersCount();
                });
    }

    private void setComingPlayerIdentity(String currPlayer, String identity, Set<String> comingSet,
            String[] playerNames) {

        if (TextUtils.isEmpty(identity)) {
            Log.i("Identifier", "Empty identifier");
            Toast.makeText(getContext(), getString(R.string.toast_instruction_empty_identifier), Toast.LENGTH_LONG)
                    .show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Enter player name for : \n" + identity);

        final AutoCompleteTextView input = new AutoCompleteTextView(getContext());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line,
                playerNames);
        input.setAdapter(adapter);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setThreshold(1);
        input.setDropDownHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        input.requestFocus();
        input.setText(currPlayer);
        builder.setView(input);

        if (TextUtils.isEmpty(currPlayer)) {
            builder.setNeutralButton("New Player", (dialogInterface, i) -> startActivity(
                    PlayerDetailsActivity.getNewPlayerFromIdentifierIntent(getContext(), identity)));
        }

        builder.setPositiveButton("OK", (dialog, which) -> {
            String newPlayer = input.getText().toString();
            if (!TextUtils.isEmpty(newPlayer) && !TextUtils.isEmpty(currPlayer) &&
                    newPlayer.equals(currPlayer)) {
                Log.i("Identifier", "Curr name " + currPlayer + " not modified for " + identity);
                return;
            }
            android.content.Context ctx = getContext();
            if (ctx == null) return;
            if (TextUtils.isEmpty(newPlayer)) {
                if (!TextUtils.isEmpty(currPlayer)) {
                    Log.i("Identifier", "Count 0 - Clearing " + currPlayer);
                    DbAsync.runWrite(
                            () -> DbHelper.clearPlayerIdentifier(ctx, currPlayer),
                            () -> {
                                if (!isAdded()) return;
                                Toast.makeText(ctx,
                                        getString(R.string.toast_instruction_identifier_cleared, currPlayer),
                                        Toast.LENGTH_LONG).show();
                                displayPastedIdentifiers(comingSet);
                            });
                } else {
                    Log.i("Identifier", "No name set for identifier " + identity);
                }
                return;
            }

            Log.i("Identifier", "Identify " + newPlayer + " as " + identity);
            DbAsync.run(
                    () -> DbHelper.setPlayerIdentifier(ctx, newPlayer, identity),
                    count -> {
                        if (!isAdded()) return;
                        if (count == 1) {
                            Log.i("Identifier", "Count + " + count + " remove identifier from " + currPlayer);
                            Toast.makeText(ctx,
                                    getString(R.string.toast_instruction_identifier_set, newPlayer, identity),
                                    Toast.LENGTH_SHORT).show();
                            if (currPlayer != null) {
                                DbAsync.runWrite(
                                        () -> DbHelper.clearPlayerIdentifier(ctx, currPlayer), null);
                            }
                            displayPastedIdentifiers(comingSet);
                        } else {
                            Log.i("Identifier", "Count 0 - " + newPlayer + " not found");
                            Toast.makeText(ctx,
                                    getString(R.string.toast_instruction_player_not_found, newPlayer),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        dialog.show();
    }
    // endregion

    // region import contacts
    private void launchContactPicker() {
        android.content.Context ctx = getContext();
        if (ctx == null) return;
        DbAsync.run(
                () -> DbHelper.getPlayers(ctx, 0, false),
                existingPlayers -> {
                    if (!isAdded()) return;
                    hadPlayersBeforeImport = existingPlayers != null && !existingPlayers.isEmpty();
                    if (requireContext().checkSelfPermission(android.Manifest.permission.READ_CONTACTS)
                            == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        showContactSelectionDialog();
                    } else {
                        contactPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS);
                    }
                });
    }

    private void showContactSelectionDialog() {
        new ContactPickerDialog(requireContext(), selectedContacts -> {
            Event.logEvent(FirebaseAnalytics.getInstance(requireContext()), EventType.import_contacts);
            processImportedContacts(selectedContacts);
        }).show();
    }

    private void processImportedContacts(ArrayList<String> contactNames) {
        android.content.Context ctx = getContext();
        if (ctx == null) return;

        DbAsync.run(
                () -> {
                    ArrayList<String> imported = new ArrayList<>();
                    ArrayList<String> skipped = new ArrayList<>();
                    for (String displayName : contactNames) {
                        if (TextUtils.isEmpty(displayName)) continue;
                        Player existing = DbHelper.getPlayer(ctx, displayName);
                        if (existing != null) {
                            skipped.add(displayName);
                            if (!existing.isComing) DbHelper.updatePlayerComing(ctx, displayName, true);
                            continue;
                        }
                        Player newPlayer = new Player(displayName, 80);
                        newPlayer.msgDisplayName = displayName;
                        newPlayer.isComing = true;
                        if (DbHelper.insertPlayer(ctx, newPlayer)) {
                            DbHelper.setPlayerIdentifier(ctx, displayName, displayName);
                            DbHelper.updatePlayerComing(ctx, displayName, true);
                            imported.add(displayName);
                            Log.i("ImportContacts", "Created player: " + displayName);
                        }
                    }
                    return new ArrayList[]{imported, skipped};
                },
                results -> {
                    if (!isAdded()) return;
                    @SuppressWarnings("unchecked")
                    ArrayList<String> importedPlayers = (ArrayList<String>) results[0];
                    @SuppressWarnings("unchecked")
                    ArrayList<String> skippedPlayers = (ArrayList<String>) results[1];
                    refreshPlayers();
                    if (!importedPlayers.isEmpty()) {
                        showImportResult(importedPlayers, skippedPlayers);
                    } else if (!skippedPlayers.isEmpty()) {
                        Toast.makeText(getContext(),
                                skippedPlayers.size() == 1
                                        ? "Player already exists: " + skippedPlayers.get(0)
                                        : skippedPlayers.size() + " players already exist",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showImportResult(ArrayList<String> importedPlayers, ArrayList<String> skippedPlayers) {
        // Check if this is the first time using contact import
        boolean isFirstImport = !PreferenceHelper.getSharedPreference(getContext())
                .contains(PreferenceHelper.pref_first_contact_import_shown);

        if (isFirstImport) {
            // Mark as shown
            PreferenceHelper.setSharedPreferenceString(getContext(),
                    PreferenceHelper.pref_first_contact_import_shown, "1");

            // Show first-time snackbar
            Snackbar.make(rootView, R.string.import_contacts_first_time_hint, Snackbar.LENGTH_LONG).show();
        }

        // If there were players before import, show dialog with imported names
        if (hadPlayersBeforeImport && !importedPlayers.isEmpty()) {
            StringBuilder message = new StringBuilder();
            message.append(getString(R.string.import_contacts_added_players)).append("\n\n");
            for (String name : importedPlayers) {
                message.append("• ").append(name).append("\n");
            }
            if (!skippedPlayers.isEmpty()) {
                message.append("\n").append(getString(R.string.import_contacts_skipped_existing)).append("\n");
                for (String name : skippedPlayers) {
                    message.append("• ").append(name).append("\n");
                }
            }

            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.import_contacts_result_title)
                    .setMessage(message.toString().trim())
                    .setPositiveButton(R.string.done, null)
                    .show();
        } else if (!hadPlayersBeforeImport) {
            // First players - just show a toast
            Toast.makeText(getContext(),
                    getString(R.string.import_contacts_success, importedPlayers.size()),
                    Toast.LENGTH_SHORT).show();
        }
    }
    // endregion

    // region broadcasts
    class PlayerUpdateBroadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("Broadcast players", "new data");
            refreshPlayers();
            if (rootView != null) {
                rootView.post(PlayersFragment.this::showTutorials);
            }
        }
    }
    // endregion
}
