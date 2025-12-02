package com.teampicker.drorfichman.teampicker.View;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseUser;
import com.teampicker.drorfichman.teampicker.BuildConfig;
import com.teampicker.drorfichman.teampicker.Controller.Broadcast.LocalNotifications;
import com.teampicker.drorfichman.teampicker.Data.Configurations;
import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.AuthHelper;
import com.teampicker.drorfichman.teampicker.tools.DialogHelper;
import com.teampicker.drorfichman.teampicker.tools.analytics.Event;
import com.teampicker.drorfichman.teampicker.tools.analytics.EventType;
import com.teampicker.drorfichman.teampicker.tools.analytics.UserProperty;
import com.teampicker.drorfichman.teampicker.tools.analytics.UserPropertyType;
import com.teampicker.drorfichman.teampicker.tools.cloud.FirebaseHelper;
import com.teampicker.drorfichman.teampicker.tools.cloud.SyncProgress;
import com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialManager;

import java.io.File;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, SyncProgress {

    private static final int ACTIVITY_RESULT_IMPORT_FILE_SELECTED = 2;
    private static final int ACTIVITY_RESULT_SIGN_IN = 3;

    View syncInProgress;
    TextView syncProgressStatus;
    ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        syncInProgress = findViewById(R.id.sync_progress);
        syncProgressStatus = findViewById(R.id.sync_progress_status);

        setSupportActionBar(toolbar);

        setNavigationDrawer(toolbar);

        setUsernameView();

        setUserProperties();

        setTabs();
    }

    private void setUserProperties() {
        UserProperty.log(FirebaseAnalytics.getInstance(this),
                UserPropertyType.games_count,
                String.valueOf(DbHelper.getGames(this).size()));

        UserProperty.log(FirebaseAnalytics.getInstance(this),
                UserPropertyType.players_count,
                String.valueOf(DbHelper.getPlayers(this).size()));

        UserProperty.log(FirebaseAnalytics.getInstance(this),
                UserPropertyType.tutorial_progress,
                String.valueOf(TutorialManager.getProgress(this)));

        UserProperty.log(FirebaseAnalytics.getInstance(this),
                UserPropertyType.tutorial_dismissed,
                String.valueOf(TutorialManager.isSkipAllTutorials(this)));
    }

    private void setTabs() {
        MainAdapter mAdapter = new MainAdapter(getSupportFragmentManager());
        viewPager = findViewById(R.id.main_tabs_pager);
        viewPager.setAdapter(mAdapter);

        TabLayout tabLayout = findViewById(R.id.main_tab);
        tabLayout.setupWithViewPager(viewPager);

        tabLayout.getTabAt(0).setIcon(R.drawable.player_icon);
        tabLayout.getTabAt(1).setIcon(R.drawable.soccer_icon);
        tabLayout.getTabAt(2).setIcon(R.drawable.stat_icon);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position == 0)
                    Event.logEvent(FirebaseAnalytics.getInstance(MainActivity.this), EventType.main_players_tab);
                else if (position == 1)
                    Event.logEvent(FirebaseAnalytics.getInstance(MainActivity.this), EventType.main_games_tab);
                else
                    Event.logEvent(FirebaseAnalytics.getInstance(MainActivity.this), EventType.main_insights_tab);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    private void setUsernameView() {
        View headerView = ((NavigationView) findViewById(R.id.nav_view)).getHeaderView(0);
        TextView usernameView = headerView.findViewById(R.id.appConnectedUser);
        TextView authActionView = headerView.findViewById(R.id.appConnectedUserAction);

        FirebaseUser user = AuthHelper.getUser();
        usernameView.setText(user != null ? user.getEmail() : "");
        authActionView.setText(user != null ? R.string.main_logout : R.string.main_login);
        authActionView.setOnClickListener(view -> {
            if (user == null) { // log in
                authenticate();
                Event.logEvent(FirebaseAnalytics.getInstance(this), EventType.sign_in);
            } else { // log out
                DialogHelper.showApprovalDialog(this,
                        getString(R.string.main_sign_out_dialog_title),
                        getString(R.string.main_sign_out_dialog_message),
                        (dialogInterface, i) -> {
                            AuthUI.getInstance().signOut(this).addOnCompleteListener(
                                    task -> setUsernameView());
                            Event.logEvent(FirebaseAnalytics.getInstance(this),
                                    EventType.sign_out);
                        }
                );
            }
        });

        FirebaseHelper.getInstance().storeAccountData();

        FirebaseHelper.fetchConfigurations(this);
    }

    private void authenticate() {
        AuthHelper.requireLogin(this, ACTIVITY_RESULT_SIGN_IN);
        setUsernameView();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.i("Player", "Result to activity " + requestCode + " - " + resultCode);

        if (requestCode == ACTIVITY_RESULT_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                Log.i("AccountFB", "User sign in success");

            } else {
                // Sign in failed. Either user canceled the sign-in flow using the back button.
                // Or response.getError().getErrorCode() with additional details
                Log.w("AccountFB", "Failed login - " + response);
                Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show();
                // TODO connectivity issues?
            }

            setUsernameView();

        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) { // close drawer
            drawer.closeDrawer(GravityCompat.START);
        } else {
            // Check current tab position
            int currentTab = viewPager.getCurrentItem();
            if (currentTab == 1 || currentTab == 2) {
                // If in Games or Insights tab, navigate to Players tab
                viewPager.setCurrentItem(0);
            } else {
                // If in Players tab, exit the app
                super.onBackPressed();
            }
        }
    }

    private void setNavigationDrawer(Toolbar toolbar) {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        navigationView.setItemIconTintList(ColorStateList.valueOf(Color.BLUE));
        navigationView.setItemTextColor(null);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_settings) {
            Event.logEvent(FirebaseAnalytics.getInstance(this), EventType.settings_view);
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.nav_getting_started) {
            showGettingStartedDialog();
        } else if (id == R.id.nav_about) {
            showAbout();
        } else if (id == R.id.nav_data_sync) {
            FirebaseHelper.executePostConfiguration(this, true, this::syncToCloud);
        } else if (id == R.id.nav_data_pull) {
            FirebaseHelper.executePostConfiguration(this, true, this::pullFromCloud);
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    //region cloud snapshot
    private void pullFromCloud() {
        if (AuthHelper.getUser() == null && Configurations.isCloudFeatureSupported()) {
            Toast.makeText(this, getString(R.string.main_auth_required), Toast.LENGTH_SHORT).show();
            authenticate();
        } else {
            FirebaseHelper.getInstance().pullFromCloud(this, this::showSyncStatus);
            Event.logEvent(FirebaseAnalytics.getInstance(this), EventType.pull_from_cloud);
        }
    }

    private void syncToCloud() {
        if (AuthHelper.getUser() == null && Configurations.isCloudFeatureSupported()) {
            Toast.makeText(this, getString(R.string.main_auth_required), Toast.LENGTH_SHORT).show();
            authenticate();
        } else { // user is authenticated, and cloud is not disabled
            FirebaseHelper.getInstance().syncToCloud(this, this::showSyncStatus);
            Event.logEvent(FirebaseAnalytics.getInstance(this), EventType.sync_to_cloud);
        }
    }

    @Override
    public void showSyncStatus(String status) {
        if (status != null) {
            syncInProgress.setVisibility(View.VISIBLE);
            syncProgressStatus.setText(status);
        } else {
            syncInProgress.setVisibility(View.GONE);
            syncProgressStatus.setText("");
            DbHelper.onUnderlyingDataChange();

            // refresh after sync
            LocalNotifications.sendNotification(this, LocalNotifications.PULL_DATA_ACTION);
        }
    }
    //endregion

    //region Tutorial
    private void showGettingStartedDialog() {
        TutorialManager.displayTutorialFlow(this, null);
    }

    private void showAbout() {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        alertDialogBuilder.setTitle("About");

        alertDialogBuilder
                .setMessage("VERSION_CODE " + BuildConfig.VERSION_CODE + "\n" +
                        "VERSION_NAME " + BuildConfig.VERSION_NAME + "\n")
                .setCancelable(true)
                .setNeutralButton("Contact Support", (dialogInterface, i1) -> {
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("message/rfc822");
                    i.putExtra(Intent.EXTRA_EMAIL, new String[]{"drorfichman+teampickersupport@gmail.com"});
                    i.putExtra(Intent.EXTRA_SUBJECT, "Team Picker App");
                    i.putExtra(Intent.EXTRA_TEXT, "Suggestion, problems etc :)");
                    try {
                        startActivity(Intent.createChooser(i, "Send mail..."));
                    } catch (android.content.ActivityNotFoundException ex) {
                    }
                })
                .setPositiveButton("Got it", (dialog, id) -> {
                    dialog.dismiss();
                });

        alertDialogBuilder.create().show();
    }
    //endregion

    //region MainAdapter
    static class MainAdapter extends FragmentPagerAdapter {

        MainAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public long getItemId(int position) {
            return super.getItemId(position);
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            if (position == 0) {
                return "Players";
            } else if (position == 1) {
                return "Games";
            } else {
                return "Insights";
            }
        }

        @Override
        @NonNull
        public Fragment getItem(int position) {
            if (position == 0) {
                return PlayersFragment.newInstance();
            } else if (position == 1) {
                return GamesFragment.newInstance(null, null, true, null);
            } else {
                return StatisticsFragment.newInstance();
            }
        }
    }
    //endregion
}
