package com.teampicker.drorfichman.teampicker.Controller.Search;

import android.text.TextUtils;
import android.widget.SearchView;

import java.util.List;

public class FilterView {

    public interface Filterable {
        String filterBy();
    }

    SearchView mSearchView;
    boolean expanded = false;

    public interface FilterInterface {
        void onFilter(String value);
    }

    public FilterView(SearchView view, FilterInterface handler) {
        mSearchView = view;

        mSearchView.setOnCloseListener(() -> {
            expanded = false;
            handler.onFilter(null);
            return false;
        });

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                handler.onFilter(s);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                handler.onFilter(s);
                return false;
            }
        });

        mSearchView.setOnQueryTextFocusChangeListener((v, hasFocus) -> expanded = hasFocus);
    }

    public static boolean match(String value, String filter) {
        return (value != null && filter != null && !filter.isEmpty() && value.toLowerCase().contains(filter.toLowerCase()));
    }

    public interface onFilterNoResults {
        void noFilterResults();
    }

    public static int positionOfFirstFilterItem(List<? extends Filterable> list, String filter,
                                                onFilterNoResults handler) {
        if (list == null) return 0;
        if (list.size() > 1000) return 0;
        if (TextUtils.isEmpty(filter)) return 0;
        for (int i = 0; i < list.size(); ++i) {
            Filterable p = list.get(i);
            if (FilterView.match(p.filterBy(), filter)) return i;
        }
        if (handler != null) handler.noFilterResults();
        return 0;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void collapseSearchView() {
        if (!expanded) return; // nothing to collapse
        mSearchView.onActionViewCollapsed();
    }
}
