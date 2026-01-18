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

import com.teampicker.drorfichman.teampicker.R;

/**
 * Container fragment that hosts multiple game chart fragments in a ViewPager2.
 * Uses radio buttons for navigation to avoid nested horizontal swipe conflicts.
 */
public class GamesChartsContainerFragment extends Fragment {

    private static final int NUM_CHARTS = 2;
    private static final int TAB_DISTRIBUTION = 0;
    private static final int TAB_TREND = 1;

    private ViewPager2 viewPager;
    private RadioGroup tabGroup;

    public GamesChartsContainerFragment() {
        super(R.layout.fragment_games_charts_container);
    }

    public static GamesChartsContainerFragment newInstance() {
        return new GamesChartsContainerFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);

        assert root != null;
        viewPager = root.findViewById(R.id.charts_view_pager);
        tabGroup = root.findViewById(R.id.charts_tab_group);

        setupViewPager();
        setupTabNavigation();

        return root;
    }

    private void setupViewPager() {
        ChartsAdapter adapter = new ChartsAdapter(this);
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
            if (checkedId == R.id.tab_goal_diff_distribution) {
                viewPager.setCurrentItem(TAB_DISTRIBUTION, true);
            } else if (checkedId == R.id.tab_goal_diff_trend) {
                viewPager.setCurrentItem(TAB_TREND, true);
            }
        });
    }

    private void updateTabSelection(int position) {
        switch (position) {
            case TAB_DISTRIBUTION:
                tabGroup.check(R.id.tab_goal_diff_distribution);
                break;
            case TAB_TREND:
                tabGroup.check(R.id.tab_goal_diff_trend);
                break;
        }
    }

    private class ChartsAdapter extends FragmentStateAdapter {

        public ChartsAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case TAB_DISTRIBUTION:
                    return GoalDiffDistributionChartFragment.newInstance();
                case TAB_TREND:
                    return GoalDiffMedianChartFragment.newInstance();
                default:
                    return GoalDiffDistributionChartFragment.newInstance();
            }
        }

        @Override
        public int getItemCount() {
            return NUM_CHARTS;
        }
    }
}

