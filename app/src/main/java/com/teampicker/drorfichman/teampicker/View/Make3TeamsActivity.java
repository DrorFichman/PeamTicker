package com.teampicker.drorfichman.teampicker.View;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.DragEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.teampicker.drorfichman.teampicker.Adapter.PlayerTeamGameAdapter;
import com.teampicker.drorfichman.teampicker.Controller.TeamDivision.TeamDivision3Teams;
import com.teampicker.drorfichman.teampicker.Data.Configurations;
import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.Data.TeamData;
import com.teampicker.drorfichman.teampicker.Data.WeatherData;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.PreferenceHelper;
import com.teampicker.drorfichman.teampicker.tools.ScreenshotHelper;
import com.teampicker.drorfichman.teampicker.tools.WeatherService;
import com.teampicker.drorfichman.teampicker.tools.WeatherSettingsDialog;
import com.teampicker.drorfichman.teampicker.tools.analytics.Event;
import com.teampicker.drorfichman.teampicker.tools.analytics.EventType;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/**
 * Activity for dividing players into 3 teams
 */
public class Make3TeamsActivity extends AppCompatActivity {

    static final int RECENT_GAMES = 50;

    public ArrayList<Player> players1 = new ArrayList<>();
    public ArrayList<Player> players2 = new ArrayList<>();
    public ArrayList<Player> players3 = new ArrayList<>();
    ArrayList<Player> movedPlayers = new ArrayList<>();

    private TeamDivision3Teams.DivisionStrategy3Teams selectedDivision = TeamDivision3Teams.DivisionStrategy3Teams.Grade;
    private boolean mMoveMode;

    private ListView list1, list2, list3;
    private TextView teamData1, teamData2, teamData3;
    LinearLayout area1, area2, area3;

    private View teamsScreenArea;
    private View screenshotArea;
    private View teamStatsLayout;
    private View buttonsLayout;
    private View shuffleLayout, moveLayout;

    private Toolbar toolbar;
    private Button shuffleView, moveView;
    private Button shuffleOptions;

    private View weatherDisplay;
    private TextView weatherEmoji, weatherTemperature, weatherTimeRange;
    private TextView weatherDate;

    @Nullable
    public static Intent getIntent(Context ctx) {
        ArrayList<Player> comingPlayers = DbHelper.getComingPlayers(ctx, 0);
        if (!comingPlayers.isEmpty()) {
            Event.logEvent(FirebaseAnalytics.getInstance(ctx), EventType.make_3teams);
            return new Intent(ctx, Make3TeamsActivity.class);
        } else {
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_make_3teams_activity);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.make_3teams_title);
        }

        teamStatsLayout = findViewById(R.id.internal_stats_layout);
        teamData1 = findViewById(R.id.total_list1);
        teamData2 = findViewById(R.id.total_list2);
        teamData3 = findViewById(R.id.total_list3);

        teamsScreenArea = findViewById(R.id.teams_list_area);
        screenshotArea = findViewById(R.id.screenshot_area);
        buttonsLayout = findViewById(R.id.buttons_layout);
        shuffleLayout = findViewById(R.id.shuffle_views);
        moveLayout = findViewById(R.id.move_views);

        area1 = findViewById(R.id.panel1);
        area2 = findViewById(R.id.panel2);
        area3 = findViewById(R.id.panel3);
        list1 = findViewById(R.id.team_1);
        list2 = findViewById(R.id.team_2);
        list3 = findViewById(R.id.team_3);

        // Weather display
        weatherDisplay = findViewById(R.id.weather_display);
        weatherEmoji = findViewById(R.id.weather_emoji);
        weatherTemperature = findViewById(R.id.weather_temperature);
        weatherTimeRange = findViewById(R.id.weather_time_range);
        weatherDate = findViewById(R.id.weather_date);
        setWeatherData(true);

        // Buttons
        shuffleView = findViewById(R.id.shuffle);
        moveView = findViewById(R.id.move);
        shuffleOptions = findViewById(R.id.shuffle_options);

        shuffleView.setOnClickListener(view -> onShuffleClicked());
        shuffleOptions.setOnClickListener(view -> showShuffleOptions());
        moveView.setOnClickListener(view -> toggleMoveMode());

        // Set up drag and drop
        area1.setOnDragListener(playerMoveDragListener);
        area2.setOnDragListener(playerMoveDragListener);
        area3.setOnDragListener(playerMoveDragListener);
        list1.setOnItemClickListener(playerClicked);
        list2.setOnItemClickListener(playerClicked);
        list3.setOnItemClickListener(playerClicked);
        list1.setOnItemLongClickListener(playerLongClicked);
        list2.setOnItemLongClickListener(playerLongClicked);
        list3.setOnItemLongClickListener(playerLongClicked);

        setDefaultShuffleStrategy();
        initialTeams();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.make_3teams_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.action_share) {
            onSendClicked();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mMoveMode) {
            exitMoveMode();
        } else {
            super.onBackPressed();
        }
    }

    private ArrayList<Player> getPlayers() {
        return DbHelper.getComingPlayers(this, RECENT_GAMES);
    }

    //region initial teams
    private void initialTeams() {
        divideComingPlayers(selectedDivision);
    }

    protected void divideComingPlayers(TeamDivision3Teams.DivisionStrategy3Teams selectedDivision) {
        ArrayList<Player> comingPlayers = getPlayers();
        
        int totalPlayers = comingPlayers.size();
        Log.d("teams", "Total " + totalPlayers + " players for 3 teams");

        TeamDivision3Teams.dividePlayers(this, comingPlayers, players1, players2, players3, selectedDivision, null);

        exitMoveMode();
        refreshPlayers();
    }

    private void setDefaultShuffleStrategy() {
        String local = PreferenceHelper.getSharedPreference(Make3TeamsActivity.this).getString(PreferenceHelper.pref_shuffle, null);
        if (local != null) {
            TeamDivision3Teams.DivisionStrategy3Teams strategy = TeamDivision3Teams.DivisionStrategy3Teams.fromString(local);
            if (strategy != null) {
                setShuffleState(strategy);
            }
        }
    }

    private void setShuffleState(TeamDivision3Teams.DivisionStrategy3Teams strategy) {
        selectedDivision = strategy;
        shuffleView.setText(getString(R.string.shuffle_grade));
    }
    //endregion

    //region shuffle
    private void onShuffleClicked() {
        divideComingPlayers(selectedDivision);
        Event.logEvent(FirebaseAnalytics.getInstance(this), EventType.shuffle_3teams);
    }

    private void showShuffleOptions() {
        PopupMenu popup = new PopupMenu(Make3TeamsActivity.this, shuffleOptions);
        popup.getMenuInflater().inflate(R.menu.shuffle_options, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.divide_by_grade) {
                setShuffleState(TeamDivision3Teams.DivisionStrategy3Teams.Grade);
                PreferenceHelper.setSharedPreferenceString(this, PreferenceHelper.pref_shuffle, "grade");
                divideComingPlayers(selectedDivision);
                return true;
            }
            return false;
        });
        popup.show();
    }
    //endregion

    //region Move mode
    private void toggleMoveMode() {
        if (mMoveMode) {
            exitMoveMode();
        } else {
            enterMoveMode();
        }
    }

    private void enterMoveMode() {
        mMoveMode = true;
        moveView.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
        Snackbar.make(this, teamsScreenArea, getString(R.string.operation_move), Snackbar.LENGTH_LONG).show();
    }

    private void exitMoveMode() {
        mMoveMode = false;
        moveView.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        movedPlayers.clear();
        refreshPlayers();
    }

    private boolean isMoveMode() {
        return mMoveMode;
    }
    //endregion

    //region Weather
    private void setWeatherData(boolean show) {
        if (show && Configurations.isWeatherFeatureEnabled()) {
            weatherDisplay.setOnClickListener(v -> showWeatherSettingsDialog());
            weatherDisplay.setVisibility(View.VISIBLE);
            fetchWeatherData();
        } else {
            weatherDisplay.setVisibility(View.GONE);
        }
    }

    private void showWeatherSettingsDialog() {
        WeatherSettingsDialog.show(this, this::fetchWeatherData);
    }

    private void fetchWeatherData() {
        if (!Configurations.isWeatherFeatureEnabled()) {
            return;
        }
        
        WeatherService weatherService = new WeatherService(this);
        
        weatherService.fetchWeatherForGameTime(new WeatherService.WeatherCallback() {
            @Override
            public void onWeatherReceived(WeatherData weatherData) {
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

    private void updateWeatherDisplay(WeatherData weatherData) {
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
    //endregion

    //region refresh and display
    private void refreshPlayers() {
        sortPlayerNames(players1);
        sortPlayerNames(players2);
        sortPlayerNames(players3);

        ArrayList<Player> moved = new ArrayList<>(movedPlayers);

        list1.setAdapter(new PlayerTeamGameAdapter(this, players1, moved, null, new ArrayList<>(), false, false));
        list2.setAdapter(new PlayerTeamGameAdapter(this, players2, moved, null, new ArrayList<>(), false, false));
        list3.setAdapter(new PlayerTeamGameAdapter(this, players3, moved, null, new ArrayList<>(), false, false));

        updateStats();
    }

    private void updateStats() {
        TeamData data1 = new TeamData(players1);
        TeamData data2 = new TeamData(players2);
        TeamData data3 = new TeamData(players3);

        // Format: "Players: X\nGrade: XX\nAvg: X.X"
        teamData1.setText(formatStats(data1));
        teamData2.setText(formatStats(data2));
        teamData3.setText(formatStats(data3));
    }

    private String formatStats(TeamData data) {
        int playerCount = data.players.size();
        float totalGrade = data.getSum();
        float avgGrade = playerCount > 0 ? totalGrade / playerCount : 0;
        
        return String.format("Players: %d\nGrade: %.0f\nAvg: %.1f", playerCount, totalGrade, avgGrade);
    }

    private void sortPlayerNames(List<Player> playersList) {
        playersList.sort(Comparator.comparing(p -> p.mName));
    }
    //endregion

    //region player interactions
    AdapterView.OnItemClickListener playerClicked = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            if (isMoveMode()) {
                Snackbar.make(Make3TeamsActivity.this, teamsScreenArea, getString(R.string.hint_drag_drop_3teams), Snackbar.LENGTH_LONG).show();
            }
        }
    };
    //endregion

    //region send teams
    private void onSendClicked() {
        enterSendMode();

        final Runnable r = () -> {
            ScreenshotHelper.takeScreenshot(Make3TeamsActivity.this, screenshotArea);
            Log.d("teams", "Exit send mode - Shot taken");
            exitSendMode();
        };

        new Handler().postDelayed(r, 200);

        Event.logEvent(FirebaseAnalytics.getInstance(this), EventType.send_3teams);
    }

    private void enterSendMode() {
        exitMoveMode();
        teamStatsLayout.setVisibility(View.INVISIBLE);
        weatherDisplay.setVisibility(View.GONE);
    }

    private void exitSendMode() {
        teamStatsLayout.setVisibility(View.VISIBLE);
        if (Configurations.isWeatherFeatureEnabled()) {
            weatherDisplay.setVisibility(View.VISIBLE);
        }
    }
    //endregion

    //region drag and drop
    PlayerTeam getTeamArea(View v) {
        if (v == list1 || v == area1) return PlayerTeam.Team1;
        else if (v == list2 || v == area2) return PlayerTeam.Team2;
        else return PlayerTeam.Team3;
    }

    enum PlayerTeam {
        Team1,
        Team2,
        Team3
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
        enterMoveMode();
        Player player = (Player) parent.getItemAtPosition(position);
        ClipData data = ClipData.newPlainText("", "");
        View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
        view.startDragAndDrop(data, shadowBuilder, new DragData(player, getTeamArea(parent)), 0);

        return true;
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
                    if (origin == PlayerTeam.Team3) players3.remove(player);

                    if (destination == PlayerTeam.Team1) players1.add(player);
                    if (destination == PlayerTeam.Team2) players2.add(player);
                    if (destination == PlayerTeam.Team3) players3.add(player);

                    if (movedPlayers.contains(player)) {
                        movedPlayers.remove(player);
                    } else {
                        movedPlayers.add(player);
                    }

                    refreshPlayers();

                    Event.logEvent(FirebaseAnalytics.getInstance(this), EventType.move_player_3teams);
                }
        }

        return true;
    };
    //endregion
}

