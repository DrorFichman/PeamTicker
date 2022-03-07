package com.teampicker.drorfichman.teampicker.Controller.Search;

import android.text.TextUtils;
import android.view.View;
import android.widget.SearchView;

import java.util.List;

public class FilterView {

    public interface Filterable {
        String filterBy();
    }

    SearchView mSearchView;
    private String filterValue;

    public interface FilterInterface {
        void onFilter(String value);
    }

    public FilterView(SearchView view, FilterInterface handler) {
        mSearchView = view;

        mSearchView.setOnCloseListener(() -> {
            mSearchView.setVisibility(View.GONE);
            filterValue = null;
            handler.onFilter(null);
            return false;
        });

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                filterValue = s;
                handler.onFilter(s);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                filterValue = s;
                handler.onFilter(s);
                return false;
            }
        });
    }

    public String getFilter() {
        return filterValue;
    }

    public void toggleSearchVisibility() {
        if (mSearchView.getVisibility() == View.VISIBLE) {
            filterValue = null;
            mSearchView.setQuery("", true);
            mSearchView.setVisibility(View.GONE);
        } else {
            mSearchView.setVisibility(View.VISIBLE);
            mSearchView.postDelayed(() -> {
                mSearchView.setIconified(false);
                mSearchView.setFocusable(true);
                mSearchView.requestFocusFromTouch();
            }, 100);
        }
    }

    public static boolean match(String value, String filter) {
        return (value != null && filter != null && !filter.isEmpty() && value.contains(filter));
    }

    public static int positionOfFirstFilterItem(List<? extends Filterable> list, String filter) {
        if (list == null) return 0;
        if (list.size() > 1000) return 0;
        if (TextUtils.isEmpty(filter)) return 0;
        for (int i = 0; i < list.size(); ++i) {
            Filterable p = list.get(i);
            if (FilterView.match(p.filterBy(), filter)) return i;
        }
        return 0;
    }
}
