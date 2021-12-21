package com.teampicker.drorfichman.teampicker.Controller.Sort;

import android.app.Activity;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.teampicker.drorfichman.teampicker.R;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

public class Sorting {

    private HashMap<View, sortType> headlines = new HashMap<>();

    public interface sortingCallbacks {
        void sortingChanged();
    }

    private sortType sort;
    private boolean originalOrder = true;
    private sortingCallbacks handler;

    enum HeadlineSortType {
        Sorted,
        Reversed,
        None
    }

    public Sorting(sortingCallbacks handle, sortType defaultSort) {
        sort = defaultSort;
        handler = handle;
    }

    public void sort(ArrayList<? extends Sortable> players) {

        Comparator<Sortable> comparator = getSortableComparator();

        if (comparator != null) { // last order is always by name
            comparator = comparator.thenComparing((p1, p2) -> p2.name().compareTo(p1.name()));
        }

        // Original order = from high to low -> reversed compare
        if (originalOrder) comparator = comparator.reversed();
        players.sort(comparator);
    }

    private Comparator<Sortable> getSortableComparator() {

        switch (sort) {

            // Main
            case name:
                return (p1, p2) -> p2.name().compareTo(p1.name());
            case grade:
                return Comparator.comparing(Sortable::grade);
            case suggestedGrade:
                return Comparator.comparing(Sortable::suggestedGrade).
                        thenComparing(Sortable::grade);
            case age:
                return Comparator.comparing(Sortable::age);
            case attributes:
                return Comparator.comparing(Sortable::attributes);
            case coming:
                return Comparator.comparing(Sortable::coming);

            // Statistics
            case success:
                return Comparator.comparing(Sortable::success).
                        thenComparing(Sortable::winRate);
            case winPercentage:
                return Comparator.comparing(Sortable::winRate);
            case games:
                return Comparator.comparing(Sortable::games);

            // Participation
            case gamesWith:
                return Comparator.comparing(Sortable::gamesWithCount).
                        thenComparing(Sortable::successWith).
                        thenComparing(Sortable::winRateWith);
            case successWith:
                return Comparator.comparing(Sortable::successWith).
                        thenComparing(Sortable::winRateWith);
            case gamesVs:
                return Comparator.comparing(Sortable::gamesVsCount).
                        thenComparing(Sortable::successVs).
                        thenComparing(Sortable::winRateVs);
            case successVs:
                return Comparator.comparing(Sortable::successVs).
                        thenComparing(Sortable::winRateVs);
            default:
                return null;
        }
    }

    public void setHeadlineSorting(View root, int textField, String headline, final sortType sorting) {
        TextView headlineView = null;
        if (textField > 0)
            headlineView = root.findViewById(textField);

        setHeadlineSorting(headlineView, headline, sorting);
    }

    public void removeHeadlineSorting(Activity ctx, int textField, String title) {
        TextView headlineView = null;
        if (textField > 0)
            headlineView = ctx.findViewById(textField);

        if (headlineView != null) {
            if (TextUtils.isEmpty(title)) {
                headlineView.setVisibility(View.INVISIBLE);
            } else if (headlineView instanceof TextView) {
                headlineView.setVisibility(View.VISIBLE);
                headlineView.setText(title);
            }
            headlineView.setOnClickListener(null);
        }
    }

    public void setHeadlineSorting(Activity ctx, int textField, String headline, final sortType sorting) {
        TextView headlineView = null;
        if (textField > 0)
            headlineView = ctx.findViewById(textField);

        setHeadlineSorting(headlineView, headline, sorting);
    }

    void setHeadlineSorting(View headlineView, String headlineTitle, final sortType sorting) {

        if (headlineView != null) {
            headlines.put(headlineView, sorting);

            if (headlineView instanceof TextView)
                ((TextView) headlineView).setText(headlineTitle);

            headlineView.setOnClickListener(view -> {
                if (sort == sorting) {
                    originalOrder = !originalOrder;
                    if (originalOrder) setSorted((TextView) view);
                    else setReverseSorted((TextView) view);
                } else {
                    originalOrder = true;
                    sort = sorting;
                    setSorted((TextView) view);
                }
                handler.sortingChanged();
            });

            headlineView.setVisibility(View.VISIBLE);
        }
    }

    private void setSorted(TextView sortingView) {
        if (sortingView != null) {
            setSelected(sortingView, HeadlineSortType.Sorted);
            resetSorting(sortingView);
        }
    }

    private void setReverseSorted(TextView reversedSortingView) {
        if (reversedSortingView != null) {
            setSelected(reversedSortingView, HeadlineSortType.Reversed);
            resetSorting(reversedSortingView);
        }
    }

    private void resetSorting(TextView exceptView) {
        for (View otherHeadlines : headlines.keySet()) {
            if (otherHeadlines.getId() != exceptView.getId()) {
                setSelected(otherHeadlines, HeadlineSortType.None);
            }
        }
    }

    private void setSelected(View headline, HeadlineSortType type) {
        if (headline instanceof TextView)
            if (type == HeadlineSortType.Sorted)
                ((TextView) headline).setTextAppearance(R.style.greenHeadline);
            else if (type == HeadlineSortType.Reversed)
                ((TextView) headline).setTextAppearance(R.style.redHeadline);
            else // default
                ((TextView) headline).setTextAppearance(R.style.regularHeadline);
        if (headline instanceof CheckBox)
            if (type == HeadlineSortType.Sorted || type == HeadlineSortType.Reversed)
                ((CheckBox) headline).setChecked(true);
            else // default
                ((CheckBox) headline).setChecked(false);
    }
}
