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
 * Container fragment hosting the three Chemistry sub-tabs:
 * Lift (pair chemistry) | Triplets | Form (recent form grid).
 *
 * Uses RadioGroup + ViewPager2, matching the pattern of StatsChartsContainerFragment.
 */
public class ChemistryContainerFragment extends Fragment {

    private static final int NUM_CHARTS = 3;
    private static final int TAB_PAIRS = 0;
    private static final int TAB_TRIPLETS = 1;
    private static final int TAB_FORM = 2;

    private ViewPager2 viewPager;
    private RadioGroup tabGroup;

    public ChemistryContainerFragment() {
        super(R.layout.fragment_chemistry_container);
    }

    public static ChemistryContainerFragment newInstance() {
        return new ChemistryContainerFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        assert root != null;

        viewPager = root.findViewById(R.id.chemistry_view_pager);
        tabGroup = root.findViewById(R.id.chemistry_tab_group);

        setupViewPager();
        setupTabNavigation();

        return root;
    }

    private void setupViewPager() {
        viewPager.setAdapter(new ChemistryAdapter(this));
        viewPager.setUserInputEnabled(false); // avoid conflict with parent ViewPager

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateTabSelection(position);
            }
        });
    }

    private void setupTabNavigation() {
        tabGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.tab_chemistry_pairs) {
                viewPager.setCurrentItem(TAB_PAIRS, true);
            } else if (checkedId == R.id.tab_chemistry_triplets) {
                viewPager.setCurrentItem(TAB_TRIPLETS, true);
            } else if (checkedId == R.id.tab_chemistry_form) {
                viewPager.setCurrentItem(TAB_FORM, true);
            }
        });
    }

    private void updateTabSelection(int position) {
        switch (position) {
            case TAB_PAIRS:
                tabGroup.check(R.id.tab_chemistry_pairs);
                break;
            case TAB_TRIPLETS:
                tabGroup.check(R.id.tab_chemistry_triplets);
                break;
            case TAB_FORM:
                tabGroup.check(R.id.tab_chemistry_form);
                break;
        }
    }

    private static class ChemistryAdapter extends FragmentStateAdapter {

        ChemistryAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case TAB_PAIRS:
                    return PowerPairsFragment.newInstance();
                case TAB_TRIPLETS:
                    return PowerTripletsFragment.newInstance();
                case TAB_FORM:
                    return FormGridFragment.newInstance();
                default:
                    return PowerPairsFragment.newInstance();
            }
        }

        @Override
        public int getItemCount() {
            return NUM_CHARTS;
        }
    }
}
