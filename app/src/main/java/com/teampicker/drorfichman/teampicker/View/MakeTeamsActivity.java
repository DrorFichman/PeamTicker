package com.teampicker.drorfichman.teampicker.View;

import static com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialManager.TutorialDisplayState.NotDisplayed;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;

import com.google.android.material.snackbar.Snackbar;
import com.teampicker.drorfichman.teampicker.Adapter.PlayerTeamGameAdapter;
import com.teampicker.drorfichman.teampicker.Adapter.PlayerTeamAnalysisAdapter;
import com.teampicker.drorfichman.teampicker.Controller.Broadcast.LocalNotifications;
import com.teampicker.drorfichman.teampicker.Controller.TeamAnalyze.Collaboration;
import com.teampicker.drorfichman.teampicker.Controller.TeamAnalyze.CollaborationHelper;
import com.teampicker.drorfichman.teampicker.Controller.TeamAnalyze.PlayerCollaboration;
import com.teampicker.drorfichman.teampicker.Controller.TeamDivision.TeamDivision;
import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.Data.Game;
import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.Data.TeamData;
import com.teampicker.drorfichman.teampicker.Data.TeamEnum;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.ColorHelper;
import com.teampicker.drorfichman.teampicker.tools.DateHelper;
import com.teampicker.drorfichman.teampicker.tools.DialogHelper;
import com.teampicker.drorfichman.teampicker.tools.PreferenceHelper;
import com.teampicker.drorfichman.teampicker.tools.ScreenshotHelper;
import com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

public class MakeTeamsActivity extends AppCompatActivity {
    static final int RECENT_GAMES = 50;
    static final int MAX_SCORE = 15;

    public ArrayList<Player> players1 = new ArrayList<>();
    public ArrayList<Player> players2 = new ArrayList<>();
    ArrayList<Player> movedPlayers = new ArrayList<>();
    ArrayList<Player> benchedPlayers = new ArrayList<>();
    ArrayList<Player> returnFromBenchPlayers = new ArrayList<>();
    ArrayList<Player> missedPlayers = new ArrayList<>();

    public Collaboration analysisResult;
    private String analysisSelectedPlayer;
    public boolean analysisAsyncInProgress;

    private boolean mSetResult;
    private TeamDivision.DivisionStrategy selectedDivision = TeamDivision.DivisionStrategy.Grade;

    private boolean mMoveMode;

    private ListView list2;
    private ListView list1;
    private ListView benchList;
    private TextView teamData2;
    private TextView teamData1;
    private TextView headlines;

    private View benchListLayout;
    private View teamsScreenArea;
    private View progressBarTeamDivision;
    TextView progressBarTeamDivisionStatus;
    private View teamStatsLayout;
    private View buttonsLayout;
    private View shuffleLayout;

    private Button moveView;
    private Button shuffleView;
    private Button shuffleOptions;
    private Button saveView;

    private View resultViews;
    private NumberPicker team1Score;
    private NumberPicker team2Score;
    private Button setGameDate;

    protected View analysisHeaders1;
    protected View analysisHeaders2;

    public static Intent getIntent(Context ctx) {
        return new Intent(ctx, MakeTeamsActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_make_teams_activity);

        teamStatsLayout = findViewById(R.id.internal_stats_layout);
        teamData1 = findViewById(R.id.total_list1);
        teamData2 = findViewById(R.id.total_list2);
        headlines = findViewById(R.id.total_headlines);

        resultViews = findViewById(R.id.enter_result_views);
        team1Score = findViewById(R.id.team_1_score);
        team2Score = findViewById(R.id.team_2_score);
        setGameDate = findViewById(R.id.set_game_date);
        setGameDate(Calendar.getInstance());

        teamsScreenArea = findViewById(R.id.teams_list_area);
        buttonsLayout = findViewById(R.id.buttons_layout);
        shuffleLayout = findViewById(R.id.shuffle_views);
        progressBarTeamDivision = findViewById(R.id.calculating_teams_progress);
        progressBarTeamDivisionStatus = findViewById(R.id.calculating_teams_progress_status);

        list1 = findViewById(R.id.team_1);
        list2 = findViewById(R.id.team_2);
        list1.setOnItemClickListener(playerClicked);
        list1.setOnItemLongClickListener(playerBenched);
        list2.setOnItemClickListener(playerClicked);
        list2.setOnItemLongClickListener(playerBenched);
        setDefaultTeamColors();

        benchListLayout = findViewById(R.id.players_bench);
        benchList = findViewById(R.id.players_bench_list);
        benchList.setOnItemLongClickListener(playerBenched);
        findViewById(R.id.empty_bench).setOnClickListener(emptyBenchClick);

        moveView = findViewById(R.id.move);
        moveView.setOnClickListener(onMoveClicked);
        moveView.setOnLongClickListener(switchTeamsColors);

        saveView = findViewById(R.id.save_results);
        saveView.setOnClickListener(view -> saveResultsClicked());

        shuffleView = findViewById(R.id.shuffle);
        shuffleView.setOnClickListener(v -> shuffleClicked());

        shuffleOptions = findViewById(R.id.shuffle_options);
        shuffleOptions.setOnClickListener(view -> showShuffleOptions());
        setDefaultShuffleStrategy();

        analysisHeaders1 = findViewById(R.id.analysis_headers_1);
        analysisHeaders2 = findViewById(R.id.analysis_headers_2);

        initialTeams();

        showTutorials();
    }

    private void showTutorials() {
        TutorialManager.TutorialDisplayState show = TutorialManager.displayTutorialStep(this, TutorialManager.Tutorials.pick_teams, false);
        if (show == NotDisplayed)
            show = TutorialManager.displayTutorialStep(this, TutorialManager.Tutorials.team_shuffle_stats, false);
        if (show == NotDisplayed)
            show = TutorialManager.displayTutorialStep(this, TutorialManager.Tutorials.team_analysis, false);
    }

    private void shuffleClicked() {
        TutorialManager.userActionTaken(this, TutorialManager.TutorialUserAction.clicked_shuffle);
        divideComingPlayers(selectedDivision);
    }

    private void setDefaultShuffleStrategy() {
        String local = PreferenceHelper.getSharedPreference(MakeTeamsActivity.this).getString(PreferenceHelper.pref_shuffle, null);
        if (local != null) {
            TeamDivision.DivisionStrategy strategy = TeamDivision.DivisionStrategy.fromString(local);
            if (strategy != null) {
                setShuffleState(strategy);
            }
        }
    }

    private boolean setShuffleState(TeamDivision.DivisionStrategy state) {
        switch (state) {
            case Age:
                selectedDivision = TeamDivision.DivisionStrategy.Age;
                PreferenceHelper.setSharedPreferenceString(MakeTeamsActivity.this, PreferenceHelper.pref_shuffle, selectedDivision.text);
                shuffleView.setText(getString(R.string.shuffle_age));
                return true;
            case Optimize:
                selectedDivision = TeamDivision.DivisionStrategy.Optimize;
                PreferenceHelper.setSharedPreferenceString(MakeTeamsActivity.this, PreferenceHelper.pref_shuffle, selectedDivision.text);
                shuffleView.setText(getString(R.string.shuffle_stats));
                return true;
            default:
                selectedDivision = TeamDivision.DivisionStrategy.Grade;
                PreferenceHelper.setSharedPreferenceString(MakeTeamsActivity.this, PreferenceHelper.pref_shuffle, selectedDivision.text);
                shuffleView.setText(getString(R.string.shuffle_grade));
                return true;
        }
    }

    private void showShuffleOptions() {
        PopupMenu popup = new PopupMenu(MakeTeamsActivity.this, shuffleOptions);
        popup.getMenuInflater().inflate(R.menu.shuffle_options, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.divide_by_age:
                    setShuffleState(TeamDivision.DivisionStrategy.Age);
                    return true;
                case R.id.divide_by_grade:
                    setShuffleState(TeamDivision.DivisionStrategy.Grade);
                    return true;
                case R.id.divide_by_ai:
                    setShuffleState(TeamDivision.DivisionStrategy.Optimize);
                    return true;
                default:
                    return false;
            }
        });
        popup.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.make_teams_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_analysis:
                analysisClicked();
                break;
            case R.id.action_enter_results:
                initSetResults();
                break;
            case R.id.action_share:
                onSendClicked();
                break;
        }

        return super.onOptionsItemSelected(item);
    }


    private ArrayList<Player> getPlayers() {
        return DbHelper.getComingPlayers(this, RECENT_GAMES);
    }

    private void saveResultsClicked() {
        if (!mSetResult) {
            initSetResults();
        } else {
            // Remove bench players before persisting the
            saveCurrentTeams(false);

            int currGame = DbHelper.getActiveGame(this);
            Game game = new Game(currGame, getGameDateString(), team1Score.getValue(), team2Score.getValue());

            DbHelper.insertGame(this, game);

            // TODO FirebaseHelper.syncGame(this, game)
            // TODO initCollaboration(); and print / keep expected winner?

            LocalNotifications.sendNotification(this, LocalNotifications.GAME_UPDATE_ACTION);

            Toast.makeText(this, "Results saved", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    public void initSetResults() {
        team1Score.setValue(0);
        team1Score.setMinValue(0);
        team1Score.setMaxValue(100);
        team1Score.setWrapSelectorWheel(false);
        team2Score.setValue(0);
        team2Score.setMinValue(0);
        team2Score.setMaxValue(100);
        team2Score.setWrapSelectorWheel(false);

        backFromAnalysis(false);
        setActivityTitle(getString(R.string.pick_teams_title_results));

        displayResultsViews(true);

        // save teams when switching to results
        saveCurrentTeams();
    }

    private void cancelSetResults() {
        displayResultsViews(false);
        setActivityTitle(null);
    }

    private void displayResultsViews(boolean enterResults) {
        mSetResult = enterResults;
        saveView.setText(enterResults ? R.string.save : R.string.enter_results);

        teamStatsLayout.setVisibility(mSetResult ? View.GONE : View.VISIBLE);
        moveView.setVisibility(mSetResult ? View.GONE : View.VISIBLE);
        shuffleLayout.setVisibility(mSetResult ? View.GONE : View.VISIBLE);

        resultViews.setVisibility(mSetResult ? View.VISIBLE : View.GONE);
        team1Score.setValue(0);
        team2Score.setValue(0);
        team1Score.setVisibility(mSetResult ? View.VISIBLE : View.INVISIBLE);
        team2Score.setVisibility(mSetResult ? View.VISIBLE : View.INVISIBLE);
        setGameDate.setVisibility(mSetResult ? View.VISIBLE : View.GONE);
    }

    //region set date
    public void showDatePicker(View view) {
        Calendar calendar = setGameDate.getTag() != null ? (Calendar) setGameDate.getTag() : Calendar.getInstance();

        DatePickerDialog d = new DatePickerDialog(this, (datePicker, year, month, day) -> {
            Calendar selectedDate = new Calendar.Builder().setDate(year, month, day).build();
            if (selectedDate.getTimeInMillis() > Calendar.getInstance().getTimeInMillis())
                Toast.makeText(this, "Future date is not allowed", Toast.LENGTH_SHORT).show();
            else
                setGameDate(selectedDate);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE));
        d.show();
    }

    private void setGameDate(Calendar cal) {
        setGameDate.setTag(cal);
        setGameDate.setText(DateHelper.getDisplayDate(this, getGameDateString()));
    }

    private Calendar getGameDate() {
        return (setGameDate.getTag() != null) ? (Calendar) setGameDate.getTag() : Calendar.getInstance();
    }

    private String getGameDateString() {
        return DateHelper.getDate(getGameDate().getTimeInMillis());
    }
    //endregion

    private void initialTeams() {

        int currGame = DbHelper.getActiveGame(this);
        if (currGame < 0) {
            Toast.makeText(this, "Initial teams", Toast.LENGTH_SHORT).show();
            divideComingPlayers(selectedDivision);
        } else {
            players1 = DbHelper.getCurrTeam(this, currGame, TeamEnum.Team1, RECENT_GAMES);
            players2 = DbHelper.getCurrTeam(this, currGame, TeamEnum.Team2, RECENT_GAMES);

            benchedPlayers = DbHelper.getCurrTeam(this, currGame, TeamEnum.Bench, RECENT_GAMES);

            boolean changed = handleComingChanges(getPlayers());
            if (benchedPlayers.size() > 0) {
                enterMoveMode();
                if (changed)
                    Snackbar.make(this, benchListLayout, "Notice: some players are benched", Snackbar.LENGTH_SHORT).show();
            }

            refreshPlayers();
        }
    }

    private boolean handleComingChanges(ArrayList<Player> comingPlayers) {
        boolean isChanged = false;

        HashMap<String, Player> all = new HashMap<>();
        for (Player coming : comingPlayers) {
            all.put(coming.mName, coming);
        }

        isChanged = removeNonComingPlayers(players1, all) || isChanged;
        isChanged = removeNonComingPlayers(players2, all) || isChanged;
        isChanged = removeNonComingPlayers(benchedPlayers, all) || isChanged;
        isChanged = isChanged || all.values().size() > 0;

        benchedPlayers.addAll(all.values());

        return isChanged;
    }

    private boolean removeNonComingPlayers(ArrayList<Player> players, HashMap<String, Player> all) {
        boolean isChanged = false;
        Iterator<Player> i = players.iterator();
        while (i.hasNext()) {
            Player p = i.next();
            if (all.containsKey(p.mName)) {
                all.remove(p.mName); // remove from coming player
            } else {
                i.remove(); // remove from team
                isChanged = true;
            }
        }
        return isChanged;
    }

    private void onSendClicked() {
        saveCurrentTeams();
        enterSendMode();

        final Runnable r = () -> {
            ScreenshotHelper.takeScreenshot(MakeTeamsActivity.this, teamsScreenArea);
            Log.d("teams", "Exit send mode - Shot taken");
            exitSendMode();
        };

        new Handler().postDelayed(r, 200);
    }

    private void enterSendMode() {
        exitMovedPlayersMode();
        teamStatsLayout.setVisibility(View.INVISIBLE);

        refreshPlayers(false);
    }

    private void exitSendMode() {

        teamStatsLayout.setVisibility(View.VISIBLE);

        refreshPlayers(true);
    }

    private void divideComingPlayers(TeamDivision.DivisionStrategy division) {
        selectedDivision = division;

        if (division == TeamDivision.DivisionStrategy.Optimize) {
            Snackbar.make(list1, R.string.operation_divide_by_collaboration, Snackbar.LENGTH_SHORT).show();

            TutorialManager.userActionTaken(this, TutorialManager.TutorialUserAction.clicked_shuffle_stats);
            dividePlayersAsync();
        } else {
            divideComingPlayers(division, true);
        }
    }

    protected void divideComingPlayers(TeamDivision.DivisionStrategy selectedDivision, boolean refreshPlayersView) {

        // Divide only the players that are not on the bench
        ArrayList<Player> comingPlayers = getPlayers();
        comingPlayers.removeAll(benchedPlayers);

        int totalPlayers = comingPlayers.size();
        int teamSize = totalPlayers / 2;
        Log.d("teams", "Total " + totalPlayers + ", team " + teamSize);

        if (totalPlayers == 0) {
            Toast.makeText(this, "Why you wanna play alone?!?", Toast.LENGTH_LONG).show();
        }

        TeamDivision.dividePlayers(this, comingPlayers, players1, players2, selectedDivision,
                this::updateAnalysisProgress);

        scramble();

        if (refreshPlayersView) {
            postDividePlayers();
        }
    }

    void postDividePlayers() {
        if (isAnalysisMode()) {
            analysisSelectedPlayer = null;
            initCollaboration();
        }

        exitMovedPlayersMode();
        refreshPlayers();
    }

    @Override
    public void onBackPressed() {
        if (mSetResult) {
            cancelSetResults();
        } else if (backFromMove()) { // exit move mode
            return;
        } else if (backFromAnalysis(true)) { // exit analysis modes
            return;
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        // Applies the changes made (shuffle/manual) when leaving activity
        // It also copies the latest game teams after a game result is saved
        saveCurrentTeams();
        super.onPause();
    }

    private void saveCurrentTeams() {
        saveCurrentTeams(true);
    }

    private void saveCurrentTeams(boolean saveBench) {
        DbHelper.saveTeams(this, players1, players2, saveBench ? benchedPlayers : null, missedPlayers);
    }

    private void exitMovedPlayersMode() {
        movedPlayers.clear();
        returnFromBenchPlayers.clear();
        moveView.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.move, 0, 0);
        mMoveMode = false;
        exitBenchMode();
    }

    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exitSendMode();
                Snackbar.make(list1, "We're ready! you can now share your screenshot :)", Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    private void refreshPlayers() {
        refreshPlayers(true);
    }

    private void refreshPlayers(boolean showInternalData) {
        sortPlayerNames(players1);
        sortPlayerNames(players2);

        ArrayList<Player> moved = getMovedPlayers();

        if (analysisResult != null) {
            list1.setAdapter(new PlayerTeamAnalysisAdapter(this, players1, moved, missedPlayers, analysisResult, analysisSelectedPlayer));
            list2.setAdapter(new PlayerTeamAnalysisAdapter(this, players2, moved, missedPlayers, analysisResult, analysisSelectedPlayer));
        } else {
            list1.setAdapter(new PlayerTeamGameAdapter(this, players1, moved, missedPlayers, showInternalData, true));
            list2.setAdapter(new PlayerTeamGameAdapter(this, players2, moved, missedPlayers, showInternalData, true));
            benchList.setAdapter(new PlayerTeamGameAdapter(this, benchedPlayers, benchedPlayers, null, false, false));
        }

        updateStats();

        moveView.setText(benchedPlayers.size() == 0 ?
                getString(R.string.move_players) :
                getString(R.string.move_players_bench, benchedPlayers.size()));
    }

    private ArrayList<Player> getMovedPlayers() {
        ArrayList<Player> list = new ArrayList<Player>();
        list.addAll(movedPlayers);
        list.addAll(returnFromBenchPlayers);
        return list;
    }

    public void preDivideAsyncHideLists() {
        progressBarTeamDivision.setVisibility(View.VISIBLE);
        teamStatsLayout.setVisibility(View.INVISIBLE);
        buttonsLayout.setVisibility(View.INVISIBLE);
        benchListLayout.setVisibility(View.INVISIBLE);

        list1.setAdapter(null);
        list2.setAdapter(null);
    }

    public void postDivideAsyncShowTeams() {
        progressBarTeamDivisionStatus.setText("");
        progressBarTeamDivision.setVisibility(View.GONE);
        teamStatsLayout.setVisibility(View.VISIBLE);
        buttonsLayout.setVisibility(View.VISIBLE);
    }

    private void updateStats() {
        int count = Math.min(players1.size(), players2.size());
        TeamData team1Data = new TeamData(players1, count);
        TeamData team2Data = new TeamData(players2, count);

        if (isAnalysisMode()) {
            team1Data.forecastWinRate = analysisResult.getCollaborationWinRate(team1Data.players);
            team1Data.forecastStdDev = analysisResult.getExpectedWinRateStdDiv(team1Data.players);
            team2Data.forecastWinRate = analysisResult.getCollaborationWinRate(team2Data.players);
            team2Data.forecastStdDev = analysisResult.getExpectedWinRateStdDiv(team2Data.players);
            if (team1Data.forecastWinRate > 0 && team2Data.forecastWinRate > 0) {
                int sum = team1Data.forecastWinRate + team2Data.forecastWinRate;
                team1Data.forecastWinRate = (int) Math.round((double) team1Data.forecastWinRate * 100 / sum);
                team2Data.forecastWinRate = 100 - team1Data.forecastWinRate;
            }
        }

        if (isAnalysisMode())
            headlines.setText(R.string.team_data_headline_forecast);
        else
            headlines.setText(R.string.team_data_headline);

        updateTeamData(teamData1, findViewById(R.id.team1_public_stats), team1Data);
        updateTeamData(teamData2, findViewById(R.id.team2_public_stats), team2Data);
    }

    private void scramble() {
        if (new Random().nextInt(3) % 2 == 1) {
            Log.d("Team", "Scramble");
            ArrayList<Player> temp = players1;
            players1 = players2;
            players2 = temp;
        }
    }

    private void updateTeamData(TextView stats, TextView publicStats, TeamData players) {

        // TODO improve visual stats table

        String collaborationWinRate = "";
        String teamStdDev = "";
        if (isAnalysisMode()) {
            if (players.forecastWinRate != 0) {
                collaborationWinRate = getString(R.string.team_data_forecast, players.forecastWinRate);
                teamStdDev = getString(R.string.team_data_win_rate_stdev, players.forecastStdDev);
            }
        }

        stats.setText(getString(R.string.team_data,
                players.getAllCount(),
                players.getAverage(),
                players.getStdDev(),
                players.getSuccess(),
                players.getWinRate(),
                teamStdDev,
                collaborationWinRate));

        int age = players.getAge();
        publicStats.setText(age > 0 ? getString(R.string.team_public_stats, age) : "");

        publicStats.setVisibility(View.VISIBLE);
    }

    private void sortPlayerNames(ArrayList<Player> playersList) {
        Collections.sort(playersList, Comparator.comparing(p -> p.mName));
    }

    private boolean isMoveMode() {
        return mMoveMode;
    }

    private View.OnClickListener onMoveClicked = view -> {
        if (!backFromMove()) { // enter move mode
            // TODO tutorial? Snackbar.make(list1, R.string.operation_move, Snackbar.LENGTH_SHORT).show();
            enterMoveMode();
        }
    };

    private void enterMoveMode() {
        moveView.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.move_green, 0, 0);
        mMoveMode = true;
        enterBenchMode();
    }

    private View.OnClickListener emptyBenchClick = view -> {
        players1.addAll(benchedPlayers);
        returnFromBenchPlayers.addAll(benchedPlayers);
        benchedPlayers.clear();
        exitBenchMode();
        refreshPlayers();
    };

    private boolean backFromMove() {
        if (isMoveMode()) { // exit move mode
            exitMovedPlayersMode();
            exitBenchMode();
            setActivityTitle(null);
            refreshPlayers();
            return true;
        }
        return false;
    }

    private void enterBenchMode() {
        benchListLayout.setVisibility(View.VISIBLE);
        teamStatsLayout.setVisibility(View.GONE);
    }

    private void exitBenchMode() {
        benchListLayout.setVisibility(View.GONE);
        teamStatsLayout.setVisibility(View.VISIBLE);
    }

    View.OnLongClickListener switchTeamsColors = v -> {
        DialogHelper.showApprovalDialog(MakeTeamsActivity.this,
                "Switch colors?", null,
                (dialog, which) -> {
                    ArrayList<Player> temp = players1;
                    players1 = players2;
                    players2 = temp;
                    refreshPlayers();
                });
        return true;
    };

    private AdapterView.OnItemLongClickListener playerBenched = (adapterView, view, i, l) -> {
        if (!isMoveMode()) enterMoveMode();
        Player player = (Player) adapterView.getItemAtPosition(i);
        benchPlayer(player);
        return true;
    };

    AdapterView.OnItemClickListener playerClicked = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

            Player player = (Player) adapterView.getItemAtPosition(i);

            if (isAnalysisMode() && player.mName.equals(analysisSelectedPlayer)) { // cancel analysis player selection

                analysisSelectedPlayer = null;
                refreshPlayers();

            } else if (isMoveMode()) { // Moving when making teams

                switchPlayer(player);

                // After a player is moved - recalculate team's collaboration
                if (isAnalysisMode()) initCollaboration();

            } else if (isAnalysisMode() && !player.mName.equals(analysisSelectedPlayer)) { // set analysis player selection

                PlayerCollaboration playerStats = analysisResult.getPlayer(player.mName);
                if (playerStats != null) analysisSelectedPlayer = player.mName;

                refreshPlayers();

            } else if (mSetResult) { // Setting "Missed" when setting results

                // Switch player NA/Missed status
                if (missedPlayers.contains(player)) {
                    missedPlayers.remove(player);
                } else {
                    missedPlayers.add(player);
                }

                refreshPlayers();
            } else {

                Intent intent = PlayerChemistryActivity.getPlayerParticipationActivity(
                        MakeTeamsActivity.this, player.mName, players2, players1);
                startActivity(intent);
            }
        }
    };

    private void switchPlayer(Player movedPlayer) {

        if (movedPlayers.contains(movedPlayer)) {
            movedPlayers.remove(movedPlayer);
        } else {
            movedPlayers.add(movedPlayer);
        }

        if (players1.contains(movedPlayer)) {
            players1.remove(movedPlayer);
            players2.add(movedPlayer);
        } else if (players2.contains(movedPlayer)) {
            players1.add(movedPlayer);
            players2.remove(movedPlayer);
        }

        refreshPlayers();
    }

    private void benchPlayer(Player benchedPlayer) {
        if (players1.contains(benchedPlayer)) {
            players1.remove(benchedPlayer);
            benchedPlayers.add(benchedPlayer);
        } else if (players2.contains(benchedPlayer)) {
            players2.remove(benchedPlayer);
            benchedPlayers.add(benchedPlayer);
        } else if (benchedPlayers.contains(benchedPlayer)) {
            benchedPlayers.remove(benchedPlayer);
            returnFromBenchPlayers.add(benchedPlayer);
            players1.add(benchedPlayer);
        }

        refreshPlayers();
    }

    private void setDefaultTeamColors() {
        int[] colors = ColorHelper.getTeamsColors(this);
        list1.setBackgroundColor(colors[0]);
        list2.setBackgroundColor(colors[1]);
    }

    private void setActivityTitle(String mode) {
        setTitle(getString(R.string.pick_teams_title) + (!TextUtils.isEmpty(mode) ? " - " + mode : ""));
    }

    //region Analysis
    private void analysisClicked() {
        TutorialManager.userActionTaken(this, TutorialManager.TutorialUserAction.clicked_analysis);

        if (!backFromAnalysis(false) && !analysisAsyncInProgress) { // enter analysis mode
            cancelSetResults();
            buttonsLayout.setVisibility(View.INVISIBLE);
            list1.setBackgroundColor(Color.GRAY);
            list2.setBackgroundColor(Color.GRAY);
            setActivityTitle(getString(R.string.pick_teams_title_analysis));
            AsyncTeamsAnalysis async = new AsyncTeamsAnalysis(this, this::refreshPlayers);
            async.execute();
        }
    }

    public void enterAnalysisAsync() {
        analysisAsyncInProgress = true;
        teamStatsLayout.setVisibility(View.INVISIBLE);
        exitMovedPlayersMode();
    }

    public void exitAnalysisAsync() {
        analysisAsyncInProgress = false;
        teamStatsLayout.setVisibility(View.VISIBLE);
        analysisHeaders1.setVisibility(View.VISIBLE);
        analysisHeaders2.setVisibility(View.VISIBLE);
    }

    private void hideAnalysis() {
        buttonsLayout.setVisibility(View.VISIBLE);
        setActivityTitle(null);
        refreshPlayers();
    }

    private boolean backFromAnalysis(boolean backClicked) {

        // TODO Ugly
        boolean returnValue = false;

        if (isAnalysisPlayerSelectedMode()) { // cancel player analysis selection
            analysisSelectedPlayer = null;

            if (backClicked) {
                refreshPlayers();
                return true;
            } else {
                returnValue = true;
            }
        }

        if (isAnalysisMode()) { // cancel analysis
            analysisResult = null;
            analysisSelectedPlayer = null;
            analysisHeaders1.setVisibility(View.GONE);
            analysisHeaders2.setVisibility(View.GONE);

            setDefaultTeamColors();

            if (backClicked) {
                hideAnalysis();
                return true;
            } else {
                returnValue = true;
            }
        }

        // If not a single back - and if anything was cancelled
        if (!backClicked && returnValue) {
            hideAnalysis();
            return true;
        } else {
            return false;
        }
    }

    private boolean isAnalysisMode() {
        return analysisResult != null;
    }

    private boolean isAnalysisPlayerSelectedMode() {
        return analysisResult != null && analysisSelectedPlayer != null;
    }
    //endregion

    //region team shuffle
    private void initCollaboration() {
        analysisResult = CollaborationHelper.getCollaborationData(MakeTeamsActivity.this, players1, players2);
        refreshPlayers();
    }

    private void dividePlayersAsync() {
        AsyncDivideCollaboration divide = new AsyncDivideCollaboration(this, this::postDividePlayers);
        divide.execute();
    }

    private void updateAnalysisProgress(int progress, String score) {
        runOnUiThread(() -> progressBarTeamDivisionStatus.setText(
                getString(R.string.analysis_progress_update, progress, score)));
    }
    //endregion
}
