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
 * Container fragment that hosts the Games History and Charts tabs.
 * Uses ViewPager2 with TabLayout for navigation.
 */
public class GamesContainerFragment extends Fragment {

    private static final int NUM_TABS = 2;
    private static final int TAB_HISTORY = 0;
    private static final int TAB_CHARTS = 1;

    private ViewPager2 viewPager;
    private TabLayout tabLayout;

    public GamesContainerFragment() {
        super(R.layout.fragment_games_container);
    }

    public static GamesContainerFragment newInstance() {
        return new GamesContainerFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);

        assert root != null;
        viewPager = root.findViewById(R.id.games_view_pager);
        tabLayout = root.findViewById(R.id.games_tabs);

        setupViewPager();
        setupTabs();

        return root;
    }

    private void setupViewPager() {
        GamesTabAdapter adapter = new GamesTabAdapter(this);
        viewPager.setAdapter(adapter);
    }

    private void setupTabs() {
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case TAB_HISTORY:
                    tab.setText(R.string.games_tab_history);
                    break;
                case TAB_CHARTS:
                    tab.setText(R.string.games_tab_charts);
                    break;
            }
        }).attach();
    }

    private class GamesTabAdapter extends FragmentStateAdapter {

        public GamesTabAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case TAB_HISTORY:
                    return GamesFragment.newInstance(java.util.Collections.emptyList(), true, null);
                case TAB_CHARTS:
                    return GamesChartsContainerFragment.newInstance();
                default:
                    return GamesFragment.newInstance(java.util.Collections.emptyList(), true, null);
            }
        }

        @Override
        public int getItemCount() {
            return NUM_TABS;
        }
    }
}

