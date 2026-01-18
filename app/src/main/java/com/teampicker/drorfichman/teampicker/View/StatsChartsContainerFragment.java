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
 * Container fragment that hosts multiple statistics chart fragments in a ViewPager2.
 * Uses radio buttons for navigation to avoid nested horizontal swipe conflicts.
 */
public class StatsChartsContainerFragment extends Fragment {

    private static final int NUM_CHARTS = 2;
    private static final int TAB_SENIORITY = 0;
    private static final int TAB_SUCCESS = 1;

    private ViewPager2 viewPager;
    private RadioGroup tabGroup;

    public StatsChartsContainerFragment() {
        super(R.layout.fragment_stats_charts_container);
    }

    public static StatsChartsContainerFragment newInstance() {
        return new StatsChartsContainerFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);

        assert root != null;
        viewPager = root.findViewById(R.id.stats_charts_view_pager);
        tabGroup = root.findViewById(R.id.stats_charts_tab_group);

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
            if (checkedId == R.id.tab_seniority) {
                viewPager.setCurrentItem(TAB_SENIORITY, true);
            } else if (checkedId == R.id.tab_success) {
                viewPager.setCurrentItem(TAB_SUCCESS, true);
            }
        });
    }

    private void updateTabSelection(int position) {
        switch (position) {
            case TAB_SENIORITY:
                tabGroup.check(R.id.tab_seniority);
                break;
            case TAB_SUCCESS:
                tabGroup.check(R.id.tab_success);
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
                case TAB_SENIORITY:
                    return PlayerSeniorityChartFragment.newInstance();
                case TAB_SUCCESS:
                    return PlayerSuccessHeatmapFragment.newInstance();
                default:
                    return PlayerSeniorityChartFragment.newInstance();
            }
        }

        @Override
        public int getItemCount() {
            return NUM_CHARTS;
        }
    }
}

