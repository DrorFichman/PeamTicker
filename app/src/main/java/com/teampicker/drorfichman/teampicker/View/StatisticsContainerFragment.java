package com.teampicker.drorfichman.teampicker.View;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.teampicker.drorfichman.teampicker.R;

/**
 * Container fragment that hosts the Stats and Charts tabs.
 * Uses ViewPager2 with TabLayout for navigation.
 */
public class StatisticsContainerFragment extends Fragment {

    private static final int NUM_TABS = 3;
    private static final int TAB_STATS = 0;
    private static final int TAB_CHEMISTRY = 1;
    private static final int TAB_CHARTS = 2;

    private ViewPager2 viewPager;
    private TabLayout tabLayout;

    public StatisticsContainerFragment() {
        super(R.layout.fragment_statistics_container);
    }

    public static StatisticsContainerFragment newInstance() {
        return new StatisticsContainerFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);

        assert root != null;
        viewPager = root.findViewById(R.id.statistics_view_pager);
        tabLayout = root.findViewById(R.id.statistics_tabs);

        setupViewPager();
        setupTabs();

        return root;
    }

    private void setupViewPager() {
        StatisticsTabAdapter adapter = new StatisticsTabAdapter(this);
        viewPager.setAdapter(adapter);
    }

    private void setupTabs() {
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case TAB_STATS:
                    tab.setText(R.string.stats_tab_stats);
                    break;
                case TAB_CHEMISTRY:
                    tab.setText(R.string.stats_tab_chemistry);
                    break;
                case TAB_CHARTS:
                    tab.setText(R.string.stats_tab_charts);
                    break;
            }
        }).attach();
    }

    private class StatisticsTabAdapter extends FragmentStateAdapter {

        public StatisticsTabAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case TAB_STATS:
                    return StatisticsFragment.newInstance();
                case TAB_CHEMISTRY:
                    return ChemistryContainerFragment.newInstance();
                case TAB_CHARTS:
                    return StatsChartsContainerFragment.newInstance();
                default:
                    return StatisticsFragment.newInstance();
            }
        }

        @Override
        public int getItemCount() {
            return NUM_TABS;
        }
    }
}

