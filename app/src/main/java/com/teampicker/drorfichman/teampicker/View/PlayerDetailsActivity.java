package com.teampicker.drorfichman.teampicker.View;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.analytics.Event;
import com.teampicker.drorfichman.teampicker.tools.analytics.EventType;

public class PlayerDetailsActivity extends AppCompatActivity {
    private static final String EXTRA_PLAYER = "existing_player";
    private static final String EXTRA_PLAYER_IDENTIFIER = "new_player_identifier";
    private static final String EXTRA_HIGHLIGHT_PLAYER = "highlight_player";
    private static final String EXTRA_START_TAB = "start_tab";
    
    private static final int TAB_INSIGHTS = 3;

    Player pPlayer;
    private String createFromIdentifier;
    private String highlightPlayer;
    private int startTab = 0;

    PlayerViewAdapter mAdapter;
    ViewPager mPager;

    @NonNull
    public static Intent getEditPlayerIntent(Context context, String playerName) {
        Event.logEvent(FirebaseAnalytics.getInstance(context), EventType.player_clicked);
        Intent intent = new Intent(context, PlayerDetailsActivity.class);
        intent.putExtra(PlayerDetailsActivity.EXTRA_PLAYER, playerName);
        return intent;
    }

    @NonNull
    public static Intent getNewPlayerFromIdentifierIntent(Context context, String identifier) {
        Intent intent = new Intent(context, PlayerDetailsActivity.class);
        intent.putExtra(PlayerDetailsActivity.EXTRA_PLAYER_IDENTIFIER, identifier);
        return intent;
    }

    @NonNull
    public static Intent getNewPlayerIntent(Context context) {
        Event.logEvent(FirebaseAnalytics.getInstance(context), EventType.new_player);
        return new Intent(context, PlayerDetailsActivity.class);
    }

    /**
     * Creates an intent to open a player's chemistry chart and highlight another player's dots.
     * @param context The context
     * @param playerName The player whose chemistry chart to show
     * @param highlightPlayerName The player to highlight in the chart (the source player)
     * @return Intent to start this activity at the Insights/Chemistry tab
     */
    @NonNull
    public static Intent getPlayerChemistryIntent(Context context, String playerName, String highlightPlayerName) {
        Intent intent = new Intent(context, PlayerDetailsActivity.class);
        intent.putExtra(PlayerDetailsActivity.EXTRA_PLAYER, playerName);
        intent.putExtra(PlayerDetailsActivity.EXTRA_HIGHLIGHT_PLAYER, highlightPlayerName);
        intent.putExtra(PlayerDetailsActivity.EXTRA_START_TAB, TAB_INSIGHTS);
        return intent;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_details_activity);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        createFromIdentifier = getIntent().getStringExtra(EXTRA_PLAYER_IDENTIFIER);
        highlightPlayer = getIntent().getStringExtra(EXTRA_HIGHLIGHT_PLAYER);
        startTab = getIntent().getIntExtra(EXTRA_START_TAB, 0);

        refreshData(getIntent().getStringExtra(EXTRA_PLAYER));
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void refreshData(String name) {
        if (!TextUtils.isEmpty(name)) {
            pPlayer = DbHelper.getPlayer(this, name);
            setTitle(pPlayer.mName);
        } else {
            setTitle("New Player");
        }

        mAdapter = new PlayerViewAdapter(getSupportFragmentManager(), pPlayer, createFromIdentifier, highlightPlayer, this::finish);
        mPager = findViewById(R.id.player_pager);
        mPager.setAdapter(mAdapter);

        TabLayout tabLayout = findViewById(R.id.player_tabs);
        tabLayout.setupWithViewPager(mPager);

        mPager.setCurrentItem(startTab);

        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position == 0)
                    Event.logEvent(FirebaseAnalytics.getInstance(PlayerDetailsActivity.this), EventType.player_details_tab);
                else if (position == 1)
                    Event.logEvent(FirebaseAnalytics.getInstance(PlayerDetailsActivity.this), EventType.player_games_tab);
                else if (position == 2)
                    Event.logEvent(FirebaseAnalytics.getInstance(PlayerDetailsActivity.this), EventType.player_insights_tab);
                else
                    Event.logEvent(FirebaseAnalytics.getInstance(PlayerDetailsActivity.this), EventType.player_team_tab);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    void finish(String newName) {
        finish();
    }

    public static class PlayerViewAdapter extends FragmentPagerAdapter {

        Player p;
        private final String createFromIdentifier;
        private final String highlightPlayer;
        private final PlayerDetailsFragment.PlayerUpdated updateListener;

        PlayerViewAdapter(FragmentManager fm, Player player, String createFromIdentifier, String highlightPlayer, PlayerDetailsFragment.PlayerUpdated listener) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            p = player;
            this.createFromIdentifier = createFromIdentifier;
            this.highlightPlayer = highlightPlayer;
            updateListener = listener;
        }

        @Override
        public int getCount() {
            return p != null ? 4 : 1;
        }

        @Override
        public long getItemId(int position) {
            return super.getItemId(position);
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            if (position == 0) {
                return "Details";
            } else if (position == 1) {
                return "Games";
            } else if (position == 2) {
                return "Team";
            } else {
                return "Charts";
            }
        }

        @Override
        @NonNull
        public Fragment getItem(int position) {
            if (position == 0) {
                return PlayerDetailsFragment.newInstance(p, createFromIdentifier, updateListener);
            } else if (position == 1) {
                return GamesFragment.newInstance(p.mName, null, false, null);
            } else if (position == 2) {
                return PlayerTeamFragment.newInstance(p, null, null);
            } else {
                return PlayerInsightsContainerFragment.newInstance(p, highlightPlayer);
            }
        }
    }
}