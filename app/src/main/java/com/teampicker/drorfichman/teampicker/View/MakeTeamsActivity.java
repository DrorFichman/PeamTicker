package com.teampicker.drorfichman.teampicker.View;

import android.app.DatePickerDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.DragEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.core.util.Pair;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.teampicker.drorfichman.teampicker.Adapter.PlayerTeamAnalysisAdapter;
import com.teampicker.drorfichman.teampicker.Adapter.PlayerTeamGameAdapter;
import com.teampicker.drorfichman.teampicker.Controller.Broadcast.LocalNotifications;
import com.teampicker.drorfichman.teampicker.Controller.TeamAnalyze.Collaboration;
import com.teampicker.drorfichman.teampicker.Controller.TeamAnalyze.CollaborationHelper;
import com.teampicker.drorfichman.teampicker.Controller.TeamAnalyze.PlayerCollaboration;
import com.teampicker.drorfichman.teampicker.Controller.TeamAnalyze.TeamPrediction;
import com.teampicker.drorfichman.teampicker.Controller.TeamDivision.TeamDivision;
import com.teampicker.drorfichman.teampicker.Data.Configurations;
import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.Data.Game;
import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.Data.TeamData;
import com.teampicker.drorfichman.teampicker.Data.TeamEnum;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.ColorHelper;
import com.teampicker.drorfichman.teampicker.tools.DateHelper;
import com.teampicker.drorfichman.teampicker.tools.InAppReviewHelper;
import com.teampicker.drorfichman.teampicker.tools.PreferenceHelper;
import com.teampicker.drorfichman.teampicker.tools.ScreenshotHelper;
import com.teampicker.drorfichman.teampicker.tools.WeatherService;
import com.teampicker.drorfichman.teampicker.tools.WeatherSettingsDialog;
import com.teampicker.drorfichman.teampicker.tools.analytics.Event;
import com.teampicker.drorfichman.teampicker.tools.analytics.EventType;
import com.teampicker.drorfichman.teampicker.tools.analytics.ParameterType;
import com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialManager;

import nl.dionsegijn.konfetti.core.Party;
import nl.dionsegijn.konfetti.core.PartyFactory;
import nl.dionsegijn.konfetti.core.emitter.Emitter;
import nl.dionsegijn.konfetti.core.emitter.EmitterConfig;
import nl.dionsegijn.konfetti.core.models.Shape;
import nl.dionsegijn.konfetti.core.models.Size;
import nl.dionsegijn.konfetti.xml.KonfettiView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

public class MakeTeamsActivity extends AppCompatActivity {
    private static final String SET_RESULT = "SET_RESULT";

    static final int RECENT_GAMES = 50;
    static final int MAX_SCORE = 100;

    public ArrayList<Player> players1 = new ArrayList<>();
    public ArrayList<Player> players2 = new ArrayList<>();
    ArrayList<Player> movedPlayers = new ArrayList<>();
    ArrayList<Player> benchedPlayers = new ArrayList<>();
    ArrayList<Player> returnFromBenchPlayers = new ArrayList<>();
    HashSet<Player> missedPlayers = new HashSet<>();
    HashSet<Player> mvpPlayers = new HashSet<>();

    public Collaboration analysisResult;
    private String analysisSelectedPlayer;
    public boolean analysisAsyncInProgress;

    private boolean mSetResult;
    private TeamDivision.DivisionStrategy selectedDivision = TeamDivision.DivisionStrategy.Grade;

    private boolean mMoveMode;

    private ListView benchList, list1, list2;
    private TextView teamData1, teamData2;
    private TextView headlines;
    LinearLayout area1, area2, areaBench;

    private View benchListLayout;
    private View teamsScreenArea;
    private View screenshotArea;
    private View progressBarTeamDivision;
    private TextView progressBarTeamDivisionStatus;
    private TextView progressBarTeamDivisionScore;
    private LinearProgressIndicator progressIndicator;
    private View teamStatsLayout;
    private View buttonsLayout;
    private View shuffleLayout, moveLayout;

    private Toolbar toolbar;
    private Button shuffleView, moveView, saveView;
    private Button shuffleOptions, moveOptions;

    private View scoreDisplayContainer;
    private TextView team1ScoreDisplay, team2ScoreDisplay;
    private Button team1Plus, team1Minus, team2Plus, team2Minus;
    private Button setGameDate;
    private int team1ScoreValue = 0;
    private int team2ScoreValue = 0;
    
    private View weatherDisplay;
    private TextView weatherEmoji, weatherTemperature, weatherTimeRange;
    private TextView weatherDate;

    private KonfettiView konfettiView;

    protected View analysisHeaders1, analysisHeaders2;

    // Prediction UI
    private View predictionHeader;
    private TextView predictionTeam1Percent, predictionTeam2Percent;
    private Button predictionInfoButton;
    private Button revealPredictionButton;
    private TeamPrediction currentPrediction;

    @Nullable
    public static Intent getIntent(Context ctx) {
        ArrayList<Player> comingPlayers = DbHelper.getComingPlayers(ctx, 0);
        if (!comingPlayers.isEmpty()) {
            Event.logEvent(FirebaseAnalytics.getInstance(ctx), EventType.make_teams);
            TutorialManager.userActionTaken(ctx, TutorialManager.TutorialUserAction.clicked_teams);
            return new Intent(ctx, MakeTeamsActivity.class);
        } else {
            return null;
        }
    }

    public static void setResult(Intent intent) {
        intent.putExtra(MakeTeamsActivity.SET_RESULT, true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_make_teams_activity);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        teamStatsLayout = findViewById(R.id.internal_stats_layout);
        teamData1 = findViewById(R.id.total_list1);
        teamData2 = findViewById(R.id.total_list2);
        headlines = findViewById(R.id.total_headlines);

        scoreDisplayContainer = findViewById(R.id.score_display_container);
        team1ScoreDisplay = findViewById(R.id.team1_score_display);
        team2ScoreDisplay = findViewById(R.id.team2_score_display);
        team1Plus = findViewById(R.id.team1_plus);
        team1Minus = findViewById(R.id.team1_minus);
        team2Plus = findViewById(R.id.team2_plus);
        team2Minus = findViewById(R.id.team2_minus);
        setGameDate = findViewById(R.id.set_game_date);
        setGameDate(Calendar.getInstance());
        
        setupScoreControls();

        teamsScreenArea = findViewById(R.id.teams_list_area);
        screenshotArea = findViewById(R.id.screenshot_area);
        buttonsLayout = findViewById(R.id.buttons_layout);
        shuffleLayout = findViewById(R.id.shuffle_views);
        moveLayout = findViewById(R.id.move_views);
        progressBarTeamDivision = findViewById(R.id.calculating_teams_progress);
        progressBarTeamDivisionStatus = findViewById(R.id.calculating_teams_progress_status);
        progressBarTeamDivisionScore = findViewById(R.id.calculating_teams_progress_score);
        progressIndicator = findViewById(R.id.progress_indicator);

        area1 = findViewById(R.id.panel1);
        area2 = findViewById(R.id.panel2);
        list1 = findViewById(R.id.team_1);
        list2 = findViewById(R.id.team_2);
        setDefaultTeamColors();

        areaBench = findViewById(R.id.panel_bench);
        benchListLayout = findViewById(R.id.players_bench);
        benchList = findViewById(R.id.players_bench_list);

        moveView = findViewById(R.id.move);
        moveView.setOnClickListener(onMoveClicked);

        saveView = findViewById(R.id.save_results);
        saveView.setOnClickListener(view -> saveResultsClicked());

        shuffleView = findViewById(R.id.shuffle);
        shuffleView.setOnClickListener(v -> shuffleClicked());

        shuffleOptions = findViewById(R.id.shuffle_options);
        shuffleOptions.setOnClickListener(view -> showShuffleOptions());
        setDefaultShuffleStrategy();

        moveOptions = findViewById(R.id.move_options);
        moveOptions.setOnClickListener(view -> showMoveOptions());

        analysisHeaders1 = findViewById(R.id.analysis_headers_1);
        analysisHeaders2 = findViewById(R.id.analysis_headers_2);

        // Prediction UI
        predictionHeader = findViewById(R.id.prediction_header);
        predictionTeam1Percent = findViewById(R.id.prediction_team1_percent);
        predictionTeam2Percent = findViewById(R.id.prediction_team2_percent);
        predictionInfoButton = findViewById(R.id.prediction_info_button);
        predictionInfoButton.setOnClickListener(v -> showPredictionExplanationDialog());
        revealPredictionButton = findViewById(R.id.reveal_prediction_button);
        revealPredictionButton.setOnClickListener(v -> showRevealPredictionDialog());
        
        weatherDisplay = findViewById(R.id.weather_display);
        weatherEmoji = findViewById(R.id.weather_emoji);
        weatherTemperature = findViewById(R.id.weather_temperature);
        weatherTimeRange = findViewById(R.id.weather_time_range);
        weatherDate = findViewById(R.id.weather_date);
        setWeatherData(true);

        konfettiView = findViewById(R.id.konfetti_view);

        area1.setOnDragListener(playerMoveDragListener);
        area2.setOnDragListener(playerMoveDragListener);
        areaBench.setOnDragListener(playerMoveDragListener);
        list1.setOnItemClickListener(playerClicked);
        list2.setOnItemClickListener(playerClicked);
        list1.setOnItemLongClickListener(playerLongClicked);
        list2.setOnItemLongClickListener(playerLongClicked);
        benchList.setOnItemLongClickListener(playerLongClicked);

        initialTeams();

        // Delay tutorial showing to ensure views are ready
        shuffleView.post(this::showTutorials);
    }

    private void setWeatherData(boolean show) {
        // Show/hide weather based on configuration
        if (show && Configurations.isWeatherFeatureEnabled()) {
            weatherDisplay.setOnClickListener(v -> showWeatherSettingsDialog());
            weatherDisplay.setVisibility(View.VISIBLE);
            fetchWeatherData();
        } else {
            weatherDisplay.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.make_teams_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.action_analysis) {
            analysisClicked();
        } else if (item.getItemId() == R.id.action_enter_results) {
            initSetResults();
        } else if (item.getItemId() == R.id.action_share) {
            onSendClicked();
        }

        return super.onOptionsItemSelected(item);
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
        // Skip if results were just saved - teams were cleared by insertGame()
        // (mSetResult stays true after saving, but is set to false when canceling)
        if (!mSetResult) {
            saveCurrentTeams();
        }
        super.onPause();
    }

    private ArrayList<Player> getPlayers() {
        return DbHelper.getComingPlayers(this, RECENT_GAMES);
    }

    private void showTutorials() {
        if (TutorialManager.isSkipAllTutorials(this)) {
            return;
        }

        // Show tutorials in sequence - only one will show at a time
        boolean shown = TutorialManager.displayTutorialStep(this, TutorialManager.Tutorials.pick_teams, shuffleView, false);
        if (!shown) {
            shown = TutorialManager.displayTutorialStep(this, TutorialManager.Tutorials.team_shuffle_stats, shuffleOptions, false);
        }
        if (!shown) {
            // team_analysis tutorial targets the analysis menu item in the toolbar
            TutorialManager.displayTutorialStepOnMenuItem(this,
                    TutorialManager.Tutorials.team_analysis, toolbar, R.id.action_analysis, false);
        }
    }

    //region initial teams
    private void initialTeams() {

        mSetResult = getIntent().getBooleanExtra(SET_RESULT, false);
        if (!DbHelper.hasActiveGame(this)) {
            divideComingPlayers(selectedDivision);
        } else {
            players1 = DbHelper.getActiveGameTeam(this, TeamEnum.Team1, RECENT_GAMES);
            players2 = DbHelper.getActiveGameTeam(this, TeamEnum.Team2, RECENT_GAMES);

            if (mSetResult) {
                refreshPlayers();
                initSetResults();
            } else {
                benchedPlayers = DbHelper.getActiveGameTeam(this, TeamEnum.Bench, RECENT_GAMES);

                boolean changed = handleComingChanges(getPlayers());
                if (!benchedPlayers.isEmpty()) {
                    enterMoveMode();
                    if (changed) {
                        Snackbar.make(this, benchListLayout, "Notice: some players are benched", Snackbar.LENGTH_SHORT).show();

                        new Handler().postDelayed(this::showMoveOptions, 300);
                    }
                }

                refreshPlayers();
            }
        }
    }

    private boolean handleComingChanges(ArrayList<Player> comingPlayers) {
        boolean isChanged;

        HashMap<String, Player> all = new HashMap<>();
        for (Player coming : comingPlayers) {
            all.put(coming.mName, coming);
        }

        isChanged = removeNonComingPlayers(players1, all);
        isChanged = removeNonComingPlayers(players2, all) || isChanged;
        isChanged = removeNonComingPlayers(benchedPlayers, all) || isChanged;
        isChanged = isChanged || !all.isEmpty();

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
    //endregion

    //region shuffle
    private void shuffleClicked() {
        TutorialManager.userActionTaken(this, TutorialManager.TutorialUserAction.clicked_shuffle);
        divideComingPlayers(selectedDivision);
    }

    private void divideComingPlayers(TeamDivision.DivisionStrategy division) {
        Event event = new Event(EventType.shuffle_teams,
                new Pair<>(ParameterType.type, division.name()));
        event.log(FirebaseAnalytics.getInstance(this));

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

        TeamDivision.dividePlayers(this, comingPlayers, players1, players2, selectedDivision,
                this::updateAnalysisProgress);

        scramble();

        if (refreshPlayersView) {
            postDividePlayers();
        }
    }

    private void scramble() {
        if (new Random().nextInt(3) % 2 == 1) {
            Log.d("Team", "Scramble");
            ArrayList<Player> temp = players1;
            players1 = players2;
            players2 = temp;
        }
    }

    void postDividePlayers() {
        if (isAnalysisMode()) {
            analysisSelectedPlayer = null;
            initCollaboration();
        }

        exitMoveMode();
        refreshPlayers();
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

    private void setShuffleState(TeamDivision.DivisionStrategy state) {
        switch (state) {
            case Age:
                selectedDivision = TeamDivision.DivisionStrategy.Age;
                PreferenceHelper.setSharedPreferenceString(MakeTeamsActivity.this, PreferenceHelper.pref_shuffle, selectedDivision.text);
                shuffleView.setText(getString(R.string.shuffle_age));
                return;
            case Optimize:
                selectedDivision = TeamDivision.DivisionStrategy.Optimize;
                PreferenceHelper.setSharedPreferenceString(MakeTeamsActivity.this, PreferenceHelper.pref_shuffle, selectedDivision.text);
                shuffleView.setText(getString(R.string.shuffle_stats));
                return;
            default:
                selectedDivision = TeamDivision.DivisionStrategy.Grade;
                PreferenceHelper.setSharedPreferenceString(MakeTeamsActivity.this, PreferenceHelper.pref_shuffle, selectedDivision.text);
                shuffleView.setText(getString(R.string.shuffle_grade));
        }
    }

    private void showShuffleOptions() {
        PopupMenu popup = new PopupMenu(MakeTeamsActivity.this, shuffleOptions);
        popup.getMenuInflater().inflate(R.menu.shuffle_options, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.divide_by_age) {
                setShuffleState(TeamDivision.DivisionStrategy.Age);
                return true;
            } else if (item.getItemId() == R.id.divide_by_grade) {
                setShuffleState(TeamDivision.DivisionStrategy.Grade);
                return true;
            } else if (item.getItemId() == R.id.divide_by_ai) {
                setShuffleState(TeamDivision.DivisionStrategy.Optimize);
                return true;
            } else {
                return false;
            }
        });
        popup.show();
    }
    //endregion

    //region set results
    private void saveResultsClicked() {
        if (!mSetResult) {
            initSetResults();
        } else {
            // Save team assignments (without bench players for final game)
            saveCurrentTeams(false);

            int nextGameId = DbHelper.getNextGameId(this);
            Game game = new Game(nextGameId, getGameDateString(), team1ScoreValue, team2ScoreValue);

            // Finalize the game - writes player_game records with results
            DbHelper.insertGame(this, game, missedPlayers);
            
            // Clear missed state for next game
            missedPlayers.clear();

            Event.logEvent(FirebaseAnalytics.getInstance(this), EventType.save_results);
            LocalNotifications.sendNotification(this, LocalNotifications.GAME_UPDATE_ACTION);

            // Request in-app review if appropriate (after positive experience)
            InAppReviewHelper.requestReviewIfAppropriate(this);

            // Show celebratory confetti animation
            showConfetti();

            // Delay finish to let users enjoy the celebration
            new Handler().postDelayed(this::finish, 1500);
        }
    }

    private void showConfetti() {
        // Team colors: blue (#1565C0) and orange (#E65100)
        int teamBlue = 0xFF1565C0;
        int teamOrange = 0xFFE65100;
        int gold = 0xFFFFD700;
        int white = 0xFFFFFFFF;

        // Create confetti burst - using fewer particles to avoid memory issues
        EmitterConfig emitterConfig = new Emitter(50, TimeUnit.MILLISECONDS).max(30);

        Party party = new PartyFactory(emitterConfig)
                .spread(120)
                .shapes(Arrays.asList(Shape.Square.INSTANCE, Shape.Circle.INSTANCE))
                .colors(Arrays.asList(teamBlue, teamOrange, gold, white))
                .sizes(new ArrayList<>(Arrays.asList(new Size(6, 30f, 5f))))
                .position(0.3, 0.5)
                .build();

        konfettiView.start(party);
    }

    private void setupScoreControls() {
        team1Plus.setOnClickListener(v -> {
            if (team1ScoreValue < MAX_SCORE) {
                team1ScoreValue++;
                team1ScoreDisplay.setText(String.valueOf(team1ScoreValue));
            }
        });
        
        team1Minus.setOnClickListener(v -> {
            if (team1ScoreValue > 0) {
                team1ScoreValue--;
                team1ScoreDisplay.setText(String.valueOf(team1ScoreValue));
            }
        });
        
        team2Plus.setOnClickListener(v -> {
            if (team2ScoreValue < MAX_SCORE) {
                team2ScoreValue++;
                team2ScoreDisplay.setText(String.valueOf(team2ScoreValue));
            }
        });
        
        team2Minus.setOnClickListener(v -> {
            if (team2ScoreValue > 0) {
                team2ScoreValue--;
                team2ScoreDisplay.setText(String.valueOf(team2ScoreValue));
            }
        });
    }

    public void initSetResults() {
        team1ScoreValue = 0;
        team2ScoreValue = 0;
        team1ScoreDisplay.setText("0");
        team2ScoreDisplay.setText("0");

        mvpPlayers.clear();

        exitMoveMode();

        backFromAnalysis(false);
        setActivityTitle(getString(R.string.pick_teams_title_results));

        displayResultsViews(true);

        // save teams when switching to results
        saveCurrentTeams();
    }

    private void cancelSetResults() {
        mvpPlayers.clear();
        missedPlayers.clear();
        displayResultsViews(false);
        setActivityTitle(null);
    }

    private void displayResultsViews(boolean enterResults) {
        mSetResult = enterResults;
        saveView.setText(enterResults ? R.string.save : R.string.enter_results);

        teamStatsLayout.setVisibility(mSetResult ? View.GONE : View.VISIBLE);
        moveLayout.setVisibility(mSetResult ? View.GONE : View.VISIBLE);
        shuffleLayout.setVisibility(mSetResult ? View.GONE : View.VISIBLE);

        // Hide weather when in result mode
        weatherDisplay.setVisibility(mSetResult ? View.GONE : View.VISIBLE);

        // Hide prediction header when in result mode
        predictionHeader.setVisibility(mSetResult ? View.GONE : View.VISIBLE);

        // Show/hide score display
        scoreDisplayContainer.setVisibility(mSetResult ? View.VISIBLE : View.GONE);
    }
    //endregion

    //region set date
    public void showDatePicker(View view) {
        Calendar calendar = setGameDate.getTag() != null ? (Calendar) setGameDate.getTag() : Calendar.getInstance();

        DatePickerDialog d = new DatePickerDialog(this, (datePicker, year, month, day) -> {
            Calendar selectedDate = new Calendar.Builder().setDate(year, month, day).build();
            if (selectedDate.getTimeInMillis() > Calendar.getInstance().getTimeInMillis())
                Toast.makeText(this, getString(R.string.toast_validation_future_date), Toast.LENGTH_SHORT).show();
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

    //region player result status dialog
    private void showPlayerResultStatusDialog(Player player) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_player_result_status, null);
        
        TextView playerNameView = dialogView.findViewById(R.id.dialog_player_name);
        CheckBox missedCheckbox = dialogView.findViewById(R.id.dialog_missed_checkbox);
        CheckBox mvpCheckbox = dialogView.findViewById(R.id.dialog_mvp_checkbox);
        Button cancelButton = dialogView.findViewById(R.id.dialog_cancel_button);
        Button okButton = dialogView.findViewById(R.id.dialog_ok_button);
        
        playerNameView.setText(player.mName);
        missedCheckbox.setChecked(missedPlayers.contains(player));
        mvpCheckbox.setChecked(mvpPlayers.contains(player));
        
        builder.setView(dialogView);
        android.app.AlertDialog dialog = builder.create();
        
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        
        okButton.setOnClickListener(v -> {
            // Update missed status
            if (missedCheckbox.isChecked() && !missedPlayers.contains(player)) {
                missedPlayers.add(player);
            } else if (!missedCheckbox.isChecked() && missedPlayers.contains(player)) {
                missedPlayers.remove(player);
            }
            
            // Update MVP status
            if (mvpCheckbox.isChecked() && !mvpPlayers.contains(player)) {
                mvpPlayers.add(player);
            } else if (!mvpCheckbox.isChecked() && mvpPlayers.contains(player)) {
                mvpPlayers.remove(player);
            }
            
            refreshPlayers();
            dialog.dismiss();
        });
        
        dialog.show();
    }
    //endregion

    //region send teams
    private void onSendClicked() {
        saveCurrentTeams();
        enterSendMode();

        final Runnable r = () -> {
            ScreenshotHelper.takeScreenshot(MakeTeamsActivity.this, screenshotArea);
            Log.d("teams", "Exit send mode - Shot taken");
            exitSendMode();
        };

        new Handler().postDelayed(r, 200);

        Event.logEvent(FirebaseAnalytics.getInstance(this), EventType.send_teams);
    }

    private void enterSendMode() {
        exitMoveMode();
        teamStatsLayout.setVisibility(View.INVISIBLE);

        if (mSetResult) {
            // Hide weather when sharing screenshot
            weatherDisplay.setVisibility(View.GONE);
        }
        
        // Hide score controls (plus/minus buttons) when taking screenshot
        if (mSetResult) {
            team1Plus.setVisibility(View.GONE);
            team1Minus.setVisibility(View.GONE);
            team2Plus.setVisibility(View.GONE);
            team2Minus.setVisibility(View.GONE);
        }

        refreshPlayers(false);
    }

    private void exitSendMode() {
        teamStatsLayout.setVisibility(View.VISIBLE);
        // Restore weather visibility (unless in result mode)
        if (!mSetResult) {
            weatherDisplay.setVisibility(View.VISIBLE);
        }
        
        // Restore score controls visibility
        if (mSetResult) {
            team1Plus.setVisibility(View.VISIBLE);
            team1Minus.setVisibility(View.VISIBLE);
            team2Plus.setVisibility(View.VISIBLE);
            team2Minus.setVisibility(View.VISIBLE);
        }

        refreshPlayers(true);
    }
    //endregion

    private void saveCurrentTeams() {
        saveCurrentTeams(true);
    }

    private void saveCurrentTeams(boolean saveBench) {
        DbHelper.saveTeams(this, players1, players2, saveBench ? benchedPlayers : null, missedPlayers, mvpPlayers);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                exitSendMode();
                Snackbar.make(list1, "We're ready! you can now share your screenshot :)", Snackbar.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.toast_error_permissions_denied), Toast.LENGTH_SHORT).show();
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
            list1.setAdapter(new PlayerTeamGameAdapter(this, players1, moved, missedPlayers, mvpPlayers, showInternalData, true));
            list2.setAdapter(new PlayerTeamGameAdapter(this, players2, moved, missedPlayers, mvpPlayers, showInternalData, true));
            benchList.setAdapter(new PlayerTeamGameAdapter(this, benchedPlayers, benchedPlayers, null, new ArrayList<>(), false, false));
        }

        updateStats();

        moveView.setText(benchedPlayers.isEmpty() ?
                getString(R.string.move_players) :
                getString(R.string.move_players_bench, benchedPlayers.size()));
    }

    private void updateStats() {
        int count = Math.min(players1.size(), players2.size());
        TeamData team1Data = new TeamData(players1, count);
        TeamData team2Data = new TeamData(players2, count);

        // Always calculate prediction (not just in analysis mode)
        currentPrediction = TeamPrediction.calculate(this, players1, players2);

        // Update prediction header
        updatePredictionHeader();

        // Always use unified headline (removed separate forecast headline)
        headlines.setText(R.string.team_data_headline);

        // Pass chemistry values to updateTeamData
        updateTeamData(teamData1, findViewById(R.id.team1_public_stats), team1Data, currentPrediction.team1ChemistryWinRate);
        updateTeamData(teamData2, findViewById(R.id.team2_public_stats), team2Data, currentPrediction.team2ChemistryWinRate);
    }

    private void updatePredictionHeader() {
        if (currentPrediction != null && currentPrediction.hasData()) {
            predictionTeam1Percent.setText(getString(R.string.progress_percentage, currentPrediction.team1Probability));
            predictionTeam2Percent.setText(getString(R.string.progress_percentage, currentPrediction.team2Probability));
            predictionHeader.setVisibility(mSetResult ? View.GONE : View.VISIBLE);
        } else {
            predictionTeam1Percent.setText("--");
            predictionTeam2Percent.setText("--");
            predictionHeader.setVisibility(mSetResult ? View.GONE : View.VISIBLE);
        }
    }

    private void updateTeamData(TextView stats, TextView publicStats, TeamData players, int chemistryWinRate) {

        // Chemistry is now always displayed
        String chemistry = chemistryWinRate > 0 ? getString(R.string.team_data_chemistry, chemistryWinRate) : "";

        stats.setText(getString(R.string.team_data,
                players.getAllCount(),
                players.getAverage(),
                players.getStdDev(),
                players.getSuccess(),
                players.getWinRate(),
                chemistry));

        int age = players.getAge();
        publicStats.setText(age > 0 ? getString(R.string.team_public_stats, age) : "");

        publicStats.setVisibility(View.VISIBLE);
    }

    private void sortPlayerNames(ArrayList<Player> playersList) {
        playersList.sort(Comparator.comparing(p -> p.mName));
    }

    AdapterView.OnItemClickListener playerClicked = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

            Player player = (Player) adapterView.getItemAtPosition(i);

            if (isAnalysisMode() && player.mName.equals(analysisSelectedPlayer)) { // cancel analysis player selection

                analysisSelectedPlayer = null;
                refreshPlayers();

            } else if (isAnalysisMode() && !player.mName.equals(analysisSelectedPlayer)) { // set analysis player selection

                PlayerCollaboration playerStats = analysisResult.getPlayer(player.mName);
                if (playerStats != null) analysisSelectedPlayer = player.mName;

                refreshPlayers();

                Event.logEvent(FirebaseAnalytics.getInstance(MakeTeamsActivity.this), EventType.analysis_mode_player_clicked);

            } else if (mSetResult) { // Setting "Missed" and "MVP" when setting results

                showPlayerResultStatusDialog(player);

            } else if (isMoveMode()) { // Moving when making teams

                Snackbar.make(MakeTeamsActivity.this, benchListLayout, getString(R.string.operation_move), Snackbar.LENGTH_LONG).show();

            } else {

                // Determine which team the clicked player is on
                ArrayList<Player> playerTeam;
                ArrayList<Player> opposingTeam;
                if (players1.contains(player)) {
                    playerTeam = players1;
                    opposingTeam = players2;
                } else {
                    playerTeam = players2;
                    opposingTeam = players1;
                }
                
                Intent intent = PlayerChemistryActivity.getPlayerCollaborationChartActivity(
                        MakeTeamsActivity.this, player.mName, playerTeam, opposingTeam, RECENT_GAMES);
                startActivity(intent);
            }
        }
    };

    private void setDefaultTeamColors() {
        int[] colors = ColorHelper.getTeamsColors(this);
        area1.setBackgroundColor(colors[0]);
        area2.setBackgroundColor(colors[1]);
        list1.setBackgroundColor(colors[0]);
        list2.setBackgroundColor(colors[1]);
    }

    private void setActivityTitle(String mode) {
        setTitle(getString(R.string.pick_teams_title) + (!TextUtils.isEmpty(mode) ? " - " + mode : ""));
    }

    //region move and bench
    private boolean isMoveMode() {
        return mMoveMode;
    }

    private final View.OnClickListener onMoveClicked = view -> {
        if (!backFromMove()) { // enter move mode
            // TODO Snackbar.make(list1, R.string.operation_move, Snackbar.LENGTH_SHORT).show();
            enterMoveMode();
        }
    };

    private void enterMoveMode() {
        moveView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, R.drawable.move_green);
        mMoveMode = true;
        enterBenchMode();
    }

    private void exitMoveMode() {
        movedPlayers.clear();
        returnFromBenchPlayers.clear();
        moveView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, R.drawable.move);
        mMoveMode = false;
        exitBenchMode();
    }

    private void clearBench(PlayerTeam team) {
        enterMoveMode();

        if (team == PlayerTeam.Team1) players1.addAll(benchedPlayers);
        else players2.addAll(benchedPlayers);

        returnFromBenchPlayers.addAll(benchedPlayers);
        benchedPlayers.clear();
        exitBenchMode();
        refreshPlayers();
    }

    private boolean backFromMove() {
        if (isMoveMode()) { // exit move mode
            exitMoveMode();
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

    private ArrayList<Player> getMovedPlayers() {
        ArrayList<Player> list = new ArrayList<>();
        list.addAll(movedPlayers);
        list.addAll(returnFromBenchPlayers);
        return list;
    }

    private void showMoveOptions() {
        PopupMenu popup = new PopupMenu(MakeTeamsActivity.this, moveOptions);
        popup.getMenuInflater().inflate(R.menu.move_options, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.move_empty_bench_left) {
                clearBench(PlayerTeam.Team1);
                return true;
            } else if (item.getItemId() == R.id.move_empty_bench_right) {
                clearBench(PlayerTeam.Team2);
                return true;
            } else if (item.getItemId() == R.id.move_switch_teams) {
                ArrayList<Player> temp = players1;
                players1 = players2;
                players2 = temp;
                refreshPlayers();
                return true;
            } else {
                return false;
            }
        });
        popup.show();
    }
    //endregion

    //region drag and drop
    PlayerTeam getTeamArea(View v) {
        if (v == list1 || v == area1) return PlayerTeam.Team1;
        else if (v == list2 || v == area2) return PlayerTeam.Team2;
        else return PlayerTeam.Bench;
    }

    enum PlayerTeam {
        Team1,
        Team2,
        Bench
    }

    static class DragData {
        DragData(Player player, PlayerTeam from) {
            p = player;
            origin = from;
        }

        Player p;
        PlayerTeam origin;
    }

    private final AdapterView.OnItemLongClickListener playerLongClicked = (parent, view, position, id) -> {
        if (isAnalysisMode() || mSetResult) {
            return false;
        } else {
            enterMoveMode();
            Player player = (Player) parent.getItemAtPosition(position);
            ClipData data = ClipData.newPlainText("", "");
            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
            view.startDragAndDrop(data, shadowBuilder, new DragData(player, getTeamArea(parent)), 0);

            return true;
        }
    };

    View.OnDragListener playerMoveDragListener = (v, event) -> {
        PlayerTeam destination = getTeamArea(v);

        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_ENTERED:
                v.setAlpha(0.5F);
                return true;
            case DragEvent.ACTION_DRAG_EXITED:
                v.setAlpha(1F);
                return true;
            case DragEvent.ACTION_DROP:
                v.setAlpha(1F);

                DragData data = (DragData) event.getLocalState();
                Player player = data.p;
                PlayerTeam origin = data.origin;

                if (destination != origin) {

                    if (origin == PlayerTeam.Team1) players1.remove(player);
                    if (origin == PlayerTeam.Team2) players2.remove(player);
                    if (origin == PlayerTeam.Bench) benchedPlayers.remove(player);

                    if (destination == PlayerTeam.Team1) players1.add(player);
                    if (destination == PlayerTeam.Team2) players2.add(player);
                    if (destination == PlayerTeam.Bench) benchedPlayers.add(player);

                    if (destination != PlayerTeam.Bench && origin != PlayerTeam.Bench) {
                        if (movedPlayers.contains(player)) {
                            movedPlayers.remove(player);
                        } else {
                            movedPlayers.add(player);
                        }
                    } else if (destination == PlayerTeam.Bench) {
                        returnFromBenchPlayers.remove(player);
                    } else { // if (origin == PlayerTeam.Bench)
                        returnFromBenchPlayers.add(player);
                    }

                    refreshPlayers();

                    Event.logEvent(FirebaseAnalytics.getInstance(this), EventType.move_player);
                }
        }

        return true;
    };
    //endregion

    //region Analysis
    private void analysisClicked() {
        TutorialManager.userActionTaken(this, TutorialManager.TutorialUserAction.clicked_analysis);
        Event.logEvent(FirebaseAnalytics.getInstance(this), EventType.analysis_mode);

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
        exitMoveMode();
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

    //region async division
    public void preDivideAsyncHideLists() {
        // Reset and show progress UI
        progressIndicator.setProgress(0, false);
        progressBarTeamDivisionStatus.setText(getString(R.string.progress_percentage, 0));
        progressBarTeamDivisionScore.setText(getString(R.string.progress_best_score, "--"));
        progressBarTeamDivision.setVisibility(View.VISIBLE);
        
        teamStatsLayout.setVisibility(View.INVISIBLE);
        buttonsLayout.setVisibility(View.INVISIBLE);
        benchListLayout.setVisibility(View.INVISIBLE);
        predictionHeader.setVisibility(View.INVISIBLE);
        setWeatherData(false);

        list1.setAdapter(null);
        list2.setAdapter(null);
    }

    public void postDivideAsyncShowTeams() {
        // Hide progress UI
        progressBarTeamDivisionStatus.setText("");
        progressBarTeamDivisionScore.setText("");
        progressIndicator.setProgress(0, false);
        progressBarTeamDivision.setVisibility(View.GONE);
        
        teamStatsLayout.setVisibility(View.VISIBLE);
        buttonsLayout.setVisibility(View.VISIBLE);
        predictionHeader.setVisibility(mSetResult ? View.GONE : View.VISIBLE);
        setWeatherData(true);
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
        runOnUiThread(() -> {
            // Update the progress indicator
            progressIndicator.setProgress(progress, true);
            
            // Update the percentage text
            progressBarTeamDivisionStatus.setText(getString(R.string.progress_percentage, progress));
            
            // Update the best score text
            progressBarTeamDivisionScore.setText(getString(R.string.progress_best_score, 
                    score != null ? score : "--"));
        });
    }
    //endregion
    
    //region Weather
    private void fetchWeatherData() {
        // Skip if feature is disabled
        if (!Configurations.isWeatherFeatureEnabled()) {
            return;
        }
        
        WeatherService weatherService = new WeatherService(this);
        
        weatherService.fetchWeatherForGameTime(new WeatherService.WeatherCallback() {
            @Override
            public void onWeatherReceived(com.teampicker.drorfichman.teampicker.Data.WeatherData weatherData) {
                runOnUiThread(() -> updateWeatherDisplay(weatherData));
            }

            @Override
            public void onWeatherError(String error) {
                runOnUiThread(() -> {
                    weatherEmoji.setText("❓");
                    weatherTemperature.setText("Weather unavailable");
                    Log.w("Weather", "Failed to fetch weather: " + error);
                });
            }
        });
    }
    
    private void updateWeatherDisplay(com.teampicker.drorfichman.teampicker.Data.WeatherData weatherData) {
        if (weatherData.hasError()) {
            weatherEmoji.setText("");
            weatherTemperature.setText(weatherData.getErrorMessage());
            weatherDate.setVisibility(View.GONE);
            weatherTimeRange.setVisibility(View.GONE);
            return;
        }
        
        weatherEmoji.setText(weatherData.getWeatherEmoji());
        
        int minTemp = Math.round(weatherData.getMinTemperature());
        int maxTemp = Math.round(weatherData.getMaxTemperature());
        
        if (minTemp == maxTemp) {
            weatherTemperature.setText(String.format("%d°C", minTemp));
        } else {
            weatherTemperature.setText(String.format("%d-%d°C", minTemp, maxTemp));
        }
        
        int startHour = WeatherService.getWeatherStartHour(this);
        int startMinute = WeatherService.getWeatherStartMinute(this);
        weatherTimeRange.setText(String.format("(%02d:%02d)", startHour, startMinute));
        weatherTimeRange.setVisibility(View.VISIBLE);
        Calendar selectedDate = WeatherService.getWeatherDate(this);

        weatherDate.setText(WeatherService.formatWeatherDate(selectedDate));
        weatherDate.setVisibility(View.VISIBLE);
    }
    
    private void showWeatherSettingsDialog() {
        // Skip if feature is disabled
        if (!Configurations.isWeatherFeatureEnabled()) {
            Toast.makeText(this, getString(R.string.toast_instruction_weather_disabled), Toast.LENGTH_SHORT).show();
            return;
        }
        
        WeatherSettingsDialog.show(this, this::fetchWeatherData);
    }
    //endregion

    //region Prediction Dialogs
    private void showPredictionExplanationDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(R.string.prediction_explanation_title);

        // Choose explanation based on current shuffle mode
        if (selectedDivision == TeamDivision.DivisionStrategy.Optimize) {
            builder.setMessage(R.string.prediction_explanation_stats_mode);
        } else {
            builder.setMessage(R.string.prediction_explanation_other_mode);
        }

        builder.setPositiveButton(R.string.done, (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showRevealPredictionDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(R.string.prediction_reveal_title);

        if (currentPrediction != null && currentPrediction.hasData()) {
            builder.setMessage(getString(R.string.prediction_reveal_message,
                    currentPrediction.team1Probability,
                    currentPrediction.team2Probability));
        } else {
            builder.setMessage(R.string.prediction_no_data);
        }

        builder.setPositiveButton(R.string.done, (dialog, which) -> dialog.dismiss());
        builder.show();
    }
    //endregion
}
