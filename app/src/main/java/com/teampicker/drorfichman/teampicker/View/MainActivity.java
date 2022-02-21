package com.teampicker.drorfichman.teampicker.View;

import android.Manifest;
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
import com.google.firebase.auth.FirebaseUser;
import com.teampicker.drorfichman.teampicker.BuildConfig;
import com.teampicker.drorfichman.teampicker.Controller.Broadcast.LocalNotifications;
import com.teampicker.drorfichman.teampicker.Data.Configurations;
import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.AuthHelper;
import com.teampicker.drorfichman.teampicker.tools.DBSnapshotUtils;
import com.teampicker.drorfichman.teampicker.tools.DialogHelper;
import com.teampicker.drorfichman.teampicker.tools.FileHelper;
import com.teampicker.drorfichman.teampicker.tools.PermissionTools;
import com.teampicker.drorfichman.teampicker.tools.SnapshotHelper;
import com.teampicker.drorfichman.teampicker.tools.cloud.FirebaseHelper;
import com.teampicker.drorfichman.teampicker.tools.cloud.SyncProgress;

import java.io.File;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, SyncProgress {

    private static final int ACTIVITY_RESULT_IMPORT_FILE_SELECTED = 2;
    private static final int ACTIVITY_RESULT_SIGN_IN = 3;

    View syncInProgress;
    TextView syncProgressStatus;

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

        setTabs();
    }

    private void setTabs() {
        MainAdapter mAdapter = new MainAdapter(getSupportFragmentManager());
        ViewPager mPager = findViewById(R.id.main_tabs_pager);
        mPager.setAdapter(mAdapter);

        TabLayout tabLayout = findViewById(R.id.main_tab);
        tabLayout.setupWithViewPager(mPager);

        tabLayout.getTabAt(0).setIcon(R.drawable.player_icon);
        tabLayout.getTabAt(1).setIcon(R.drawable.soccer_icon);
        tabLayout.getTabAt(2).setIcon(R.drawable.stat_icon);
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
            } else { // log out
                DialogHelper.showApprovalDialog(this,
                        getString(R.string.main_sign_out_dialog_title),
                        getString(R.string.main_sign_out_dialog_message),
                        (dialogInterface, i) ->
                                AuthUI.getInstance().signOut(this).addOnCompleteListener(task ->
                                        setUsernameView()));
            }
        });

        FirebaseHelper.getInstance().storeAccountData();

        FirebaseHelper.fetchConfigurations(this, true, null);
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

        } else if (requestCode == ACTIVITY_RESULT_IMPORT_FILE_SELECTED &&
                resultCode == RESULT_OK &&
                data != null && data.getData() != null) {

            // Import data result
            SnapshotHelper.checkImportApproved(this, getImportListener(),
                    FileHelper.getPath(this, data.getData()));

        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) { // close drawer
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
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

        if (id == R.id.nav_save_snapshot) {
            DBSnapshotUtils.takeDBSnapshot(this, getExportListener(), null);
        } else if (id == R.id.nav_import_snapshot) {
            selectFileForImport();
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.nav_getting_started) {
            showGettingStartedDialog();
        } else if (id == R.id.nav_about) {
            showAbout();
        } else if (id == R.id.nav_data_sync) {
            FirebaseHelper.fetchConfigurations(this, true, this::syncToCloud);
        } else if (id == R.id.nav_data_pull) {
            FirebaseHelper.fetchConfigurations(this, true, this::pullFromCloud);
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    //region cloud snapshot
    private void pullFromCloud(Configurations remote) {
        if (AuthHelper.getUser() == null && Configurations.isCloudFeatureSupported()) {
            Toast.makeText(this, getString(R.string.main_auth_required), Toast.LENGTH_SHORT).show();
            authenticate();
        } else {
            FirebaseHelper.getInstance().pullFromCloud(this, this::showSyncStatus);
        }
    }

    private void syncToCloud(Configurations remote) {
        if (AuthHelper.getUser() == null && Configurations.isCloudFeatureSupported()) {
            Toast.makeText(this, getString(R.string.main_auth_required), Toast.LENGTH_SHORT).show();
            authenticate();
        } else {
            FirebaseHelper.getInstance().syncToCloud(this, this::showSyncStatus);
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

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        alertDialogBuilder.setTitle("Getting Started");

        alertDialogBuilder
                .setMessage("Welcome! \n\n" +
                        "1. 'Add player' - to create players. \n\n" +
                        "2. RSVP the attending players\n" +
                        "   (or paste whatsapp messages). \n\n" +
                        "3. 'Teams' - to pick teams. \n\n" +
                        "4. 'Results' under 'Teams' - \n" +
                        "   once the game is over. \n" +
                        "\n" +
                        "And don't forget to be awesome :)")
                .setCancelable(true)
                .setPositiveButton("Got it", (dialog, id) -> {
                    dialog.dismiss();
                });

        alertDialogBuilder.create().show();
    }

    private void showAbout() {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        alertDialogBuilder.setTitle("About");

        alertDialogBuilder
                .setMessage("VERSION_CODE " + BuildConfig.VERSION_CODE + "\n" +
                        "VERSION_NAME " + BuildConfig.VERSION_NAME + "\n")
                .setCancelable(true)
                .setPositiveButton("Got it", (dialog, id) -> {
                    dialog.dismiss();
                });

        alertDialogBuilder.create().show();
    }
    //endregion

    //region local snapshot
    private DBSnapshotUtils.ImportListener getImportListener() {
        return new DBSnapshotUtils.ImportListener() {
            @Override
            public void preImport() {
            }

            @Override
            public void importStarted() {
                Toast.makeText(MainActivity.this, "Import Started", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void importCompleted() {
                Toast.makeText(MainActivity.this, "Import Completed", Toast.LENGTH_SHORT).show();
                LocalNotifications.sendNotification(MainActivity.this, LocalNotifications.PULL_DATA_ACTION);
            }

            @Override
            public void importError(String msg) {
                Toast.makeText(MainActivity.this, "Import Failed : " + msg, Toast.LENGTH_LONG).show();
            }
        };
    }

    private DBSnapshotUtils.ExportListener getExportListener() {
        return new DBSnapshotUtils.ExportListener() {

            @Override
            public void exportStarted() {
                Toast.makeText(MainActivity.this, "Export Started", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void exportCompleted(File snapshot) {
                Toast.makeText(MainActivity.this, "Export Completed " + snapshot, Toast.LENGTH_SHORT).show();

                SnapshotHelper.sendSnapshot(MainActivity.this, snapshot);
            }

            @Override
            public void exportError(String msg) {
                Toast.makeText(MainActivity.this, "Data export failed " + msg, Toast.LENGTH_LONG).show();
            }
        };
    }

    public void selectFileForImport() {

        PermissionTools.checkPermissionsForExecution(this, 2, () -> {
            Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
            chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
            chooseFile.setType("*/*"); // TODO xls?
            startActivityForResult(
                    Intent.createChooser(chooseFile, "Select xls snapshot file to import"),
                    MainActivity.ACTIVITY_RESULT_IMPORT_FILE_SELECTED
            );
        }, Manifest.permission.READ_EXTERNAL_STORAGE);
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
