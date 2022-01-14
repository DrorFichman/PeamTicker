package com.teampicker.drorfichman.teampicker.View;

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

import com.google.android.material.snackbar.Snackbar;
import com.teampicker.drorfichman.teampicker.Adapter.PlayerTeamAdapter;
import com.teampicker.drorfichman.teampicker.Adapter.PlayerTeamAnalysisAdapter;
import com.teampicker.drorfichman.teampicker.Controller.TeamAnalyze.Collaboration;
import com.teampicker.drorfichman.teampicker.Controller.TeamAnalyze.CollaborationHelper;
import com.teampicker.drorfichman.teampicker.Controller.TeamAnalyze.PlayerCollaboration;
import com.teampicker.drorfichman.teampicker.Controller.TeamDivision.TeamDivision;
import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.Data.Game;
import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.Data.ResultEnum;
import com.teampicker.drorfichman.teampicker.Data.TeamData;
import com.teampicker.drorfichman.teampicker.Data.TeamEnum;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.ColorHelper;
import com.teampicker.drorfichman.teampicker.tools.DateHelper;
import com.teampicker.drorfichman.teampicker.tools.DialogHelper;
import com.teampicker.drorfichman.teampicker.tools.ScreenshotHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MakeTeamsActivity extends AppCompatActivity {
    static final int RECENT_GAMES = 50;
    static final int MAX_SCORE = 15;

    public ArrayList<Player> players1 = new ArrayList<>();
    public ArrayList<Player> players2 = new ArrayList<>();
    ArrayList<Player> movedPlayers = new ArrayList<>();
    ArrayList<Player> missedPlayers = new ArrayList<>();

    public Collaboration analysisResult;
    private String analysisSelectedPlayer;
    public boolean analysisAsyncInProgress;

    private boolean mSetResult;
    private TeamDivision.DivisionStrategy selectedDivision = TeamDivision.DivisionStrategy.Grade;

    private ListView list2;
    private ListView list1;
    private TextView teamData2;
    private TextView teamData1;
    private TextView headlines;

    private View teamsScreenArea;
    View progressBarTeamDivision;
    TextView progressBarTeamDivisionStatus;
    protected View teamStatsLayout;
    protected View buttonsLayout;

    private View moveView;
    private View shuffleView;

    private View resultViews;
    private NumberPicker team1Score;
    private NumberPicker team2Score;
    private View saveView;
    private Button setGameDate;

    private AlertDialog makeTeamsDialog;
    protected View analysisHeaders1;
    protected View analysisHeaders2;

    public static Intent getInstance(Context ctx) {
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
        progressBarTeamDivision = findViewById(R.id.calculating_teams_progress);
        progressBarTeamDivisionStatus = findViewById(R.id.calculating_teams_progress_status);

        list1 = findViewById(R.id.team_1);
        list2 = findViewById(R.id.team_2);
        list1.setOnItemClickListener(playerClicked);
        list2.setOnItemClickListener(playerClicked);
        setDefaultTeamColors();

        moveView = findViewById(R.id.move);
        moveView.setOnClickListener(onMoveClicked);
        moveView.setOnLongClickListener(switchTeamsColors);

        saveView = findViewById(R.id.save_results);
        saveView.setOnClickListener(view -> saveResults());

        shuffleView = findViewById(R.id.shuffle);
        shuffleView.setOnClickListener(v -> divideComingPlayers(selectedDivision));
        shuffleView.setOnLongClickListener(v -> showMakeTeamOptionsDialog());

        analysisHeaders1 = findViewById(R.id.analysis_headers_1);
        analysisHeaders2 = findViewById(R.id.analysis_headers_2);

        initialTeams();
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

    private void saveResults() {
        int currGame = DbHelper.getActiveGame(this);
        Game game = new Game(currGame, getGameDateString(), team1Score.getValue(), team2Score.getValue());

        DbHelper.insertGame(this, game);

        // TODO FirebaseHelper.syncGame(this, game)

        // TODO initCollaboration(); and print / keep expected winner?

        Toast.makeText(this, "Results saved", Toast.LENGTH_LONG).show();
        finish();
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

        DbHelper.saveTeams(this, players1, players2); // save teams when switching to results
    }

    private void cancelSetResults() {
        displayResultsViews(false);
        setActivityTitle(null);
    }

    private void displayResultsViews(boolean enterResults) {
        mSetResult = enterResults;

        teamStatsLayout.setVisibility(mSetResult ? View.GONE : View.VISIBLE);
        moveView.setVisibility(mSetResult ? View.GONE : View.VISIBLE);
        shuffleView.setVisibility(mSetResult ? View.GONE : View.VISIBLE);

        resultViews.setVisibility(mSetResult ? View.VISIBLE : View.GONE);
        saveView.setVisibility(mSetResult ? View.VISIBLE : View.GONE);
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
                Toast.makeText(this, "Future date is not allowed", Toast.LENGTH_LONG).show();
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
            Log.d("teams", "Initial data curr game > 0 - so getting from DB");
            players1 = DbHelper.getCurrTeam(this, currGame, TeamEnum.Team1, RECENT_GAMES);
            players2 = DbHelper.getCurrTeam(this, currGame, TeamEnum.Team2, RECENT_GAMES);

            ArrayList<Player> comingPlayers = getPlayers();

            if (players1 != null && players1.size() > 0 && players2 != null && players2.size() > 0) {
                boolean changed = handleComingChanges(comingPlayers, players1, players2);
                refreshPlayers();
                if (changed)
                    Toast.makeText(this, "Changes in coming players applied", Toast.LENGTH_SHORT).show();
            } else {
                Log.e("TEAMS", "Unable to find teams for curr game " + currGame);
                divideComingPlayers(selectedDivision);
            }
        }
    }

    private boolean handleComingChanges(ArrayList<Player> comingPlayers,
                                        ArrayList<Player> players1, ArrayList<Player> players2) {
        boolean isChanged = false;

        HashMap<String, Player> all = new HashMap<>();
        for (Player coming : comingPlayers) {
            all.put(coming.mName, coming);
        }

        isChanged = removeNonComingPlayers(players1, all) || isChanged;
        isChanged = removeNonComingPlayers(players2, all) || isChanged;
        isChanged = isChanged || all.values().size() > 0;
        players1.addAll(all.values());

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
        Log.d("teams", "Enter send mode");
        DbHelper.saveTeams(MakeTeamsActivity.this, players1, players2);
        enterSendMode();

        final Runnable r = () -> {
            ScreenshotHelper.takeScreenshot(MakeTeamsActivity.this, teamsScreenArea);
            Log.d("teams", "Exit send mode - Shot taken");
            exitSendMode();
        };

        new Handler().postDelayed(r, 200);
    };

    private void enterSendMode() {

        clearMovedPlayers();
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
            Toast.makeText(this, R.string.operation_divide_by_collaboration, Toast.LENGTH_SHORT).show();

            dividePlayersAsync();
        } else {
            if (division == TeamDivision.DivisionStrategy.Age)
                Toast.makeText(this, R.string.operation_divide_by_age, Toast.LENGTH_SHORT).show();

            divideComingPlayers(division, true);
        }
    }

    protected void divideComingPlayers(TeamDivision.DivisionStrategy selectedDivision, boolean refreshPlayersView) {

        ArrayList<Player> comingPlayers = getPlayers();

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

        clearMovedPlayers();
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
        DbHelper.saveTeams(this, players1, players2); // save teams when leaving activity
        super.onPause();
    }

    private void clearMovedPlayers() {
        movedPlayers.clear();
        moveView.setAlpha(1F);
    }

    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exitSendMode();
                Toast.makeText(this, "We're ready! you can now share your screenshot :)", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void refreshPlayers(boolean showInternalData) {
        sortPlayerNames(players1);
        sortPlayerNames(players2);

        if (analysisResult != null) {
            list1.setAdapter(new PlayerTeamAnalysisAdapter(this, players1, movedPlayers, missedPlayers, analysisResult, analysisSelectedPlayer));
            list2.setAdapter(new PlayerTeamAnalysisAdapter(this, players2, movedPlayers, missedPlayers, analysisResult, analysisSelectedPlayer));
        } else {
            list1.setAdapter(new PlayerTeamAdapter(this, players1, movedPlayers, missedPlayers, showInternalData));
            list2.setAdapter(new PlayerTeamAdapter(this, players2, movedPlayers, missedPlayers, showInternalData));
        }

        updateStats();
    }

    public void clearLists() {
        list1.setAdapter(null);
        list2.setAdapter(null);
    }

    private void refreshPlayers() {
        refreshPlayers(true);
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
                team1Data.forecastWinRate = (int) Math.round((double)team1Data.forecastWinRate * 100 / sum);
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
        Collections.sort(playersList, (p1, t1) -> p1.mName.compareTo(t1.mName));
    }

    private boolean isMoveMode() {
        return moveView != null && moveView.getAlpha() < 1F;
    }

    private View.OnClickListener onMoveClicked = view -> {
        if (!backFromMove()) { // enter move mode
            moveView.setAlpha(0.5F);
        }
    };

    private boolean backFromMove() {
        if (isMoveMode()) { // exit move mode
            clearMovedPlayers();
            setActivityTitle(null);
            refreshPlayers();
            return true;
        }
        return false;
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

    View.OnLongClickListener explainOperation = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View view) {
            switch (view.getId()) {
                case R.id.shuffle:
                    Snackbar.make(shuffleView, "Shuffle teams", Snackbar.LENGTH_SHORT).show();
                    return true;
                case R.id.move:
                    Snackbar.make(shuffleView, "Enter manual players moving mode", Snackbar.LENGTH_SHORT).show();
                    return true;
            }
            return false;
        }
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
                    DbHelper.setPlayerResult(MakeTeamsActivity.this,
                            DbHelper.getActiveGame(MakeTeamsActivity.this), player.mName, ResultEnum.NA);
                } else {
                    missedPlayers.add(player);
                    DbHelper.setPlayerResult(MakeTeamsActivity.this,
                            DbHelper.getActiveGame(MakeTeamsActivity.this), player.mName, ResultEnum.Missed);
                }

                refreshPlayers();

            } else {

                Intent intent = PlayerParticipationActivity.getPlayerParticipationActivity(
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

        if (!backFromAnalysis(false) && !analysisAsyncInProgress) { // enter analysis mode
            cancelSetResults();
            moveView.setVisibility(View.INVISIBLE);
            shuffleView.setVisibility(View.INVISIBLE);
            list1.setBackgroundColor(Color.GRAY);
            list2.setBackgroundColor(Color.GRAY);
            setActivityTitle(getString(R.string.pick_teams_title_analysis));
            AsyncTeamsAnalysis async = new AsyncTeamsAnalysis(this, this::refreshPlayers);
            async.execute();
        }
    }

    public void enterAnalysis() {
        teamStatsLayout.setVisibility(View.VISIBLE);
        analysisHeaders1.setVisibility(View.VISIBLE);
        analysisHeaders2.setVisibility(View.VISIBLE);
    }

    private void hideAnalysis() {
        moveView.setVisibility(View.VISIBLE);
        shuffleView.setVisibility(View.VISIBLE);
        setActivityTitle(null);
        refreshPlayers();
    }

    private boolean backFromAnalysis(boolean singleBack) {

        // TODO Ugly
        boolean returnValue = false;

        if (isAnalysisPlayerSelectedMode()) { // cancel player analysis selection
            analysisSelectedPlayer = null;

            if (singleBack) {
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

            if (singleBack) {
                hideAnalysis();
                return true;
            } else {
                returnValue = true;
            }
        }

        // If not a single back - and if anything was cancelled
        if (!singleBack && returnValue) {
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

    private boolean showMakeTeamOptionsDialog() {

        if (makeTeamsDialog != null && makeTeamsDialog.isShowing()) {
            return false;
        }

        if (getPlayers().size() == 0) {
            return false;
        }

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        alertDialogBuilder.setTitle("Shake things up?");

        alertDialogBuilder
                .setCancelable(true)
                .setItems(new CharSequence[]
                                {"Divide by grade", "Divide by age", "AI - beep boop beep"},
                        (dialog, which) -> {
                            switch (which) {
                                case 0:
                                    divideComingPlayers(TeamDivision.DivisionStrategy.Grade);
                                    break;
                                case 1:
                                    divideComingPlayers(TeamDivision.DivisionStrategy.Age);
                                    break;
                                case 2:
                                    divideComingPlayers(TeamDivision.DivisionStrategy.Optimize);
                            }
                        });

        makeTeamsDialog = alertDialogBuilder.create();

        makeTeamsDialog.show();

        return true;
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
