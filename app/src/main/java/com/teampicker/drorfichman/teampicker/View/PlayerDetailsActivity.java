package com.teampicker.drorfichman.teampicker.View;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.tabs.TabLayout;
import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

public class PlayerDetailsActivity extends AppCompatActivity {
    private static final String EXTRA_PLAYER = "player";
    Player pPlayer;

    MyAdapter mAdapter;

    ViewPager mPager;

    @NonNull
    public static Intent getDetailsPlayerIntent(Context context, String playerName) {
        Intent intent = new Intent(context, PlayerDetailsActivity.class);
        intent.putExtra(PlayerDetailsActivity.EXTRA_PLAYER, playerName);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_details_activity);

        refreshData(getIntent().getStringExtra(EXTRA_PLAYER));
    }

    private void refreshData(String name) {
        pPlayer = DbHelper.getPlayer(this, name);

        setTitle("Player details : " + pPlayer.mName);

        mAdapter = new MyAdapter(getSupportFragmentManager(), pPlayer, this::finish);
        mPager = findViewById(R.id.player_pager);
        mPager.setAdapter(mAdapter);

        TabLayout tabLayout = findViewById(R.id.player_tabs);
        tabLayout.setupWithViewPager(mPager);

        mPager.setCurrentItem(0);
    }

    void finish(String newName) {
        finish();
    }

    public static class MyAdapter extends FragmentPagerAdapter {

        Player p;
        private final PlayerCardFragment.PlayerUpdated updateListener;

        MyAdapter(FragmentManager fm, Player player, PlayerCardFragment.PlayerUpdated listener) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            p = player;
            updateListener = listener;
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public long getItemId(int position) {
            return super.getItemId(position);
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            if (position == 0) {
                return "Card";
            } else if (position == 1) {
                return "Games";
            } else {
                return "Team";
            }
        }

        @Override
        @NonNull
        public Fragment getItem(int position) {
            if (position == 0) {
                return PlayerCardFragment.newInstance(p, updateListener);
            } else if (position == 1) {
                return GamesFragment.newInstance(p.mName, null);
            } else {
                return PlayerParticipationFragment.newInstance(p, null, null);
            }
        }
    }
}