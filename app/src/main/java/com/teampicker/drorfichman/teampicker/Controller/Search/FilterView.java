package com.teampicker.drorfichman.teampicker.Controller.Search;

import android.view.View;
import android.widget.SearchView;

public class FilterView {

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
}
