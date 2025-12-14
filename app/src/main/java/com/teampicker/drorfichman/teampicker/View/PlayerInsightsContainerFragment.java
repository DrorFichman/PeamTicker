package com.teampicker.drorfichman.teampicker.View;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.R;

/**
 * Container fragment that hosts multiple insight chart fragments in a ViewPager2.
 * Uses tab buttons for navigation to avoid nested horizontal swipe conflicts.
 */
public class PlayerInsightsContainerFragment extends Fragment {

    private static final String ARG_PLAYER = "player";
    private static final int NUM_CHARTS = 3;

    private Player player;
    private ViewPager2 viewPager;
    private RadioGroup tabGroup;

    public PlayerInsightsContainerFragment() {
        super(R.layout.fragment_player_insights_container);
    }

    public static PlayerInsightsContainerFragment newInstance(Player player) {
        PlayerInsightsContainerFragment fragment = new PlayerInsightsContainerFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PLAYER, player);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            player = (Player) getArguments().getSerializable(ARG_PLAYER);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);

        assert root != null;
        viewPager = root.findViewById(R.id.insights_view_pager);
        tabGroup = root.findViewById(R.id.insights_tab_group);

        setupViewPager();
        setupTabNavigation();

        return root;
    }

    private void setupViewPager() {
        InsightsChartAdapter adapter = new InsightsChartAdapter(this);
        viewPager.setAdapter(adapter);

        // Disable swipe to avoid conflict with parent ViewPager
        viewPager.setUserInputEnabled(false);

        // Sync ViewPager changes to tabs
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateTabSelection(position);
            }
        });
    }

    private void setupTabNavigation() {
        tabGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.tab_win_rate) {
                viewPager.setCurrentItem(0, true);
            } else if (checkedId == R.id.tab_activity) {
                viewPager.setCurrentItem(1, true);
            } else if (checkedId == R.id.tab_chemistry) {
                viewPager.setCurrentItem(2, true);
            }
        });
    }

    private void updateTabSelection(int position) {
        switch (position) {
            case 0:
                tabGroup.check(R.id.tab_win_rate);
                break;
            case 1:
                tabGroup.check(R.id.tab_activity);
                break;
            case 2:
                tabGroup.check(R.id.tab_chemistry);
                break;
        }
    }

    private class InsightsChartAdapter extends FragmentStateAdapter {

        public InsightsChartAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return PlayerWinRateChartFragment.newInstance(player);
                case 1:
                    return PlayerParticipationChartFragment.newInstance(player);
                case 2:
                    return PlayerCollaborationChartFragment.newInstance(player);
                default:
                    return PlayerWinRateChartFragment.newInstance(player);
            }
        }

        @Override
        public int getItemCount() {
            return NUM_CHARTS;
        }
    }
}
