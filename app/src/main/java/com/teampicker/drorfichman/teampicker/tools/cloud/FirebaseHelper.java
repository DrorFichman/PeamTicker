package com.teampicker.drorfichman.teampicker.tools.cloud;

import android.content.Context;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.text.HtmlCompat;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.teampicker.drorfichman.teampicker.BuildConfig;
import com.teampicker.drorfichman.teampicker.Data.Configurations;
import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.Data.Game;
import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.Data.PlayerGame;
import com.teampicker.drorfichman.teampicker.Data.TeamEnum;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.AuthHelper;
import com.teampicker.drorfichman.teampicker.tools.DialogHelper;
import com.teampicker.drorfichman.teampicker.tools.PreferenceHelper;
import com.teampicker.drorfichman.teampicker.tools.tutorials.TutorialManager;
import com.teampicker.drorfichman.teampicker.tools.cloud.queries.GetConfiguration;
import com.teampicker.drorfichman.teampicker.tools.cloud.queries.GetLastGame;
import com.teampicker.drorfichman.teampicker.tools.cloud.queries.GetUsers;

import java.util.ArrayList;

public class FirebaseHelper implements CloudSync {

    private static CloudSync helper;

    public static CloudSync getInstance() {
        if (AuthHelper.getUser() != null) {
            return getHelper();
        } else {
            return new UnimplementedCloud();
        }
    }

    private static CloudSync getHelper() {
        if (helper == null) {
            helper = new FirebaseHelper();
        }
        return helper;
    }

    public enum Node {
        players,
        games,
        playersGames,
        account,
        app,
    }

    public interface PostConfigurationAction {
        void execute();
    }

    public static DatabaseReference games() {
        return getNode(Node.games);
    }

    public static DatabaseReference configurations() {
        return getNode("configurations").child("v1");
    }

    public static DatabaseReference playersGames() {
        return getNode(Node.playersGames);
    }

    public static DatabaseReference account() {
        return getNode(Node.account);
    }

    public static DatabaseReference app() {
        return getNode(Node.app);
    }

    public static DatabaseReference players() {
        return getNode(Node.players);
    }

    private static DatabaseReference getNode(Node node) {
        if (AuthHelper.getUser() == null || AuthHelper.getUserUID().isEmpty()) {
            Log.e("AccountFB", "User is not connected" + AuthHelper.getUser());
            return null;
        }

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        return database.getReference(AuthHelper.getUserUID()).child(node.name());
    }

    private static DatabaseReference getNode(String highLevelPath) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        return database.getReference(highLevelPath);
    }

    public static String sanitizeKey(String key) {
        if (key == null) return null;
        return key.replaceAll("\\.", "");
    }

    //region Sync
    @Override
    public void syncToCloud(Context ctx, SyncProgress progress) {
        switch (Configurations.getUserCloudState(AuthHelper.getUser())) {
            case Allowed:
                TutorialManager.userActionTaken(ctx, TutorialManager.TutorialUserAction.click_sync_to_cloud);
                checkSync(ctx, progress);
                break;
            case RequireAuthentication: // can't because checked previously, no break
                Log.e("User", "Unauthenticated user at syncToCloud " + AuthHelper.getUser());
            case Unknown:
                progress.showSyncProgress(null, 0);
                Toast.makeText(ctx, ctx.getString(R.string.main_connectivity), Toast.LENGTH_SHORT).show();
                break;
            case NotAllowed:
            case Disabled:
                progress.showSyncProgress(null, 0);
                Toast.makeText(ctx, ctx.getString(R.string.main_cloud_not_allowed), Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void checkSync(Context ctx, SyncProgress progress) {
        // Hide progress while showing dialog
        progress.showSyncProgress(null, 0);
        DialogHelper.showApprovalDialog(ctx,
                "Sync players and games to the cloud?", "",
                (dialog, positive) -> {
                    progress.showSyncProgress("Syncing players...", 25);
                    syncData(ctx, progress);
                });
    }

    private void syncData(Context ctx, SyncProgress progress) {
        // Get current max game ID before syncing
        int maxGameId = DbHelper.getMaxGame(ctx);
        
        syncPlayersToCloud(ctx, () -> {
            progress.showSyncProgress("Syncing games...", 50);
            syncGamesToCloud(ctx, () -> {
                progress.showSyncProgress("Syncing player stats...", 75);
                syncPlayersGamesToCloud(ctx, () -> {
                    // Save the last synced game ID
                    PreferenceHelper.setLastSyncedGameId(ctx, maxGameId);
                    Log.i("syncData", "Saved last synced game ID: " + maxGameId);
                    
                    progress.showSyncProgress(null, 100);
                    Toast.makeText(ctx, "Sync completed", Toast.LENGTH_LONG).show();
                });
            });
        });
    }

    private static void syncPlayersToCloud(Context ctx, DataCallback handler) {
        players().removeValue((error, ref) -> {
            Log.i("syncPlayersToCloud", "Deleted error - " + error);
            if (error == null) {

                ArrayList<Player> players = DbHelper.getPlayers(ctx);
                for (Player p : players) {
                    storePlayer(p);
                }

                Log.i("syncPlayersToCloud", "Sync players completed");
                handler.DataChanged();

            } else {
                Log.i("syncPlayersToCloud", "Sync failed " + error);
                Toast.makeText(ctx, "Failed to sync players data " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private static void syncGamesToCloud(Context ctx, DataCallback handler) {
        games().removeValue((error, ref) -> {
            Log.i("syncGamesToCloud", "Deleted error - " + error);
            if (error == null) {

                ArrayList<Game> games = DbHelper.getGames(ctx);

                for (Game g : games) {
                    ArrayList<Player> team1 = DbHelper.getGameTeam(ctx, g.gameId, TeamEnum.Team1, 0);
                    ArrayList<Player> team2 = DbHelper.getGameTeam(ctx, g.gameId, TeamEnum.Team2, 0);
                    g.setTeams(team1, team2);
                    storeGame(g);
                }

                Log.i("syncGamesToCloud", "Sync games completed");
                handler.DataChanged();

            } else {
                Log.i("syncGamesToCloud", "Sync failed " + error);
                Toast.makeText(ctx, "Failed to sync games data " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private static void syncPlayersGamesToCloud(Context ctx, DataCallback handler) {
        playersGames().removeValue((error, ref) -> {
            Log.i("syncPlayersGamesToCloud", "Deleted, error - " + error);
            if (error == null) {
                ArrayList<PlayerGame> pgs = DbHelper.getPlayersGames(ctx);
                for (PlayerGame pg : pgs) {
                    storePlayerGame(pg);
                }
                Log.i("syncPlayersGamesToCloud", "Sync players games completed");
                handler.DataChanged();
            } else {
                Log.i("syncPlayersGamesToCloud", "Sync failed " + error);
                Toast.makeText(ctx, "Failed to sync players games data " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private static void storePlayer(Player p) {
        players().child(sanitizeKey(p.name())).setValue(p);
    }

    private static void storeGame(Game g) {
        games().child(String.valueOf(g.gameId)).setValue(g);
    }

    private static void storePlayerGame(PlayerGame pg) {
        playersGames().child(sanitizeKey(pg.playerName)).child(String.valueOf(pg.gameId)).setValue(pg);
    }
    //endregion

    //region Pull
    @Override
    public void pullFromCloud(Context ctx, SyncProgress handler) {
        switch (Configurations.getUserCloudState(AuthHelper.getUser())) {
            case Allowed:
                if (isAdmin()) {

                    GetUsers.query(ctx, result -> {
                        showUsersDialog(ctx, result, handler);
                    });

                } else {

                    GetLastGame.query(ctx, (game) -> {
                        if (game != null) {
                            Log.d("Date", "Date " + game.dateString);
                            checkPull(ctx, game.getDisplayDate(ctx), handler);
                        } else {
                            handler.showSyncProgress(null, 0);
                            Toast.makeText(ctx, "No Games found - sync to cloud first", Toast.LENGTH_LONG).show();
                        }
                    });

                }
                break;
            case RequireAuthentication: // can't because checked previously, no break
                Log.e("User", "Unauthenticated user at pull from cloud " + AuthHelper.getUser());
            case Unknown:
                handler.showSyncProgress(null, 0);
                Toast.makeText(ctx, ctx.getString(R.string.main_connectivity), Toast.LENGTH_SHORT).show();
                break;
            case NotAllowed:
            case Disabled:
                handler.showSyncProgress(null, 0);
                Toast.makeText(ctx, ctx.getString(R.string.main_cloud_not_allowed), Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void showUsersDialog(Context ctx, ArrayList<AccountData> users, SyncProgress handler) {
        Log.d("showUsersDialog", users.size() + " users ");
        // Hide progress while showing dialog
        handler.showSyncProgress(null, 0);
        
        ArrayList<String> l = new ArrayList<>();
        for (AccountData a : users) {
            if (a != null && a.displayName != null) {
                l.add(a.displayName);
            }
        }

        String[] list = l.toArray(new String[l.size()]);

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle("Pick a user");
        builder.setItems(list, (dialog, which) -> {

            // Fetch data from the selected user
            AuthHelper.fetchFor(users.get(which));
            fetchData(ctx, (status, progress) -> {
                if (status == null) {
                    // Clear selection once done
                    AuthHelper.fetchFor(null);
                    Toast.makeText(ctx, "Pull completed from " + users.get(which).displayName, Toast.LENGTH_LONG).show();
                }
                handler.showSyncProgress(status, progress);
            });
        });
        builder.show();
    }

    private void checkPull(Context ctx, String date, SyncProgress handler) {
        // Hide progress while showing dialog
        handler.showSyncProgress(null, 0);
        
        DialogHelper.showApprovalDialog(ctx,
                "Pull data from cloud? \n" + "Last game - " + date, "",
                (dialog, positive) -> {
                    fetchData(ctx, (status, progress) -> handler.showSyncProgress(status, progress));
                });
    }

    private interface FetchDataProgress {
        void onProgress(String status, int progress);
    }

    private void fetchData(Context ctx, FetchDataProgress handler) {
        DbHelper.deleteTableContents(ctx);
        Log.i("pullFromCloud", "Delete local DB");

        handler.onProgress("Pulling players...", 25);
        pullPlayersFromCloud(ctx, () -> {
            handler.onProgress("Pulling games...", 50);
            pullGamesFromCloud(ctx, () -> {
                handler.onProgress("Pulling player stats...", 75);
                pullPlayersGamesFromCloud(ctx, () -> {
                    // After pulling from cloud, update last synced game ID to current max
                    // since all data is now in sync with the cloud
                    int maxGameId = DbHelper.getMaxGame(ctx);
                    PreferenceHelper.setLastSyncedGameId(ctx, maxGameId);
                    Log.i("fetchData", "Updated last synced game ID after pull: " + maxGameId);
                    
                    handler.onProgress(null, 100);
                });
            });
        });
    }

    private static void pullPlayersFromCloud(Context ctx, DataCallback handler) {
        ValueEventListener playerListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (DataSnapshot snapshotNode : dataSnapshot.getChildren()) {
                    Player p = snapshotNode.getValue(Player.class);
                    boolean created = DbHelper.insertPlayer(ctx, p);
                    if (!created) Log.e("pullPlayersFromCloud", "Failed to insert " + p.mName);
                }

                Log.i("pullPlayersFromCloud", "Local players DB updated from cloud");
                handler.DataChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("pullPlayersFromCloud", "onCancelled", databaseError.toException());
            }
        };

        players().addListenerForSingleValueEvent(playerListener);
    }

    private static void pullGamesFromCloud(Context ctx, DataCallback handler) {
        ValueEventListener gamesListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                int gameCount = 0;
                for (DataSnapshot snapshotNode : dataSnapshot.getChildren()) {
                    Game g = snapshotNode.getValue(Game.class);
                    DbHelper.insertGame(ctx, g);
                    gameCount++;
                }

                Log.i("pullGamesFromCloud", "Local games DB updated from cloud - " + gameCount);
                handler.DataChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("pullGamesFromCloud", "onCancelled", databaseError.toException());
            }
        };

        games().addListenerForSingleValueEvent(gamesListener);
    }

    private static void pullPlayersGamesFromCloud(Context ctx, DataCallback handler) {
        ArrayList<Player> players = DbHelper.getPlayers(ctx);
        if (players.size() == 0) {
            handler.DataChanged();
            return;
        }

        ArrayList<Player> clone = (ArrayList<Player>) players.clone();
        for (Player p : players) {
            ValueEventListener playersGamesListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    int gameCount = 0;
                    for (DataSnapshot snapshotNode : dataSnapshot.getChildren()) {
                        PlayerGame pg = snapshotNode.getValue(PlayerGame.class);
                        DbHelper.insertPlayerGame(ctx, pg);
                        gameCount++;
                    }

                    clone.remove(p);
                    Log.i("pullPlayersGamesFromCloud", "Local players games DB updated from cloud - " + p.mName + " - " + gameCount + " clone " + clone.size());

                    if (clone.size() == 0) {
                        handler.DataChanged();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e("pullPlayersGamesFromCloud", "onCancelled", databaseError.toException());
                }
            };

            playersGames().child(p.mName).addListenerForSingleValueEvent(playersGamesListener);
        }
    }
    //endregion

    //region Remote config
    public static void fetchConfigurations(Context ctx) {
        executePostConfiguration(ctx, true, null);
    }

    public static void executePostConfiguration(Context ctx, boolean forcePull,
                                                PostConfigurationAction action) {
        if (Configurations.remote == null || forcePull) {
            FirebaseHelper.pullRemoteConfiguration(ctx, result -> {
                Configurations.remote = result;

                // Version support check occurs only during new app lunch
                if (!Configurations.isVersionSupported())
                    showForceUpgradeDialog(ctx);
                else if (action != null)
                    action.execute();
            });
        } else {
            if (action != null)
                action.execute();
        }
    }

    private static void showForceUpgradeDialog(Context ctx) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ctx);
        alertDialogBuilder.setTitle("Version Outdated :/")
                .setMessage("This release (" + BuildConfig.VERSION_NAME + ") \n" +
                        "is no longer supported.\n\n")
                .setCancelable(false);

        if (!TextUtils.isEmpty(Configurations.outdatedVersionMessage())) {
            TextView textView = new TextView(ctx);
            textView.setPadding(32, 32, 32, 0);
            textView.setMovementMethod(LinkMovementMethod.getInstance());
            textView.setText(HtmlCompat.fromHtml(Configurations.outdatedVersionMessage(), HtmlCompat.FROM_HTML_MODE_LEGACY));
            alertDialogBuilder.setView(textView);
        }

        alertDialogBuilder.create().show();
    }

    public static void pullRemoteConfiguration(Context ctx, GetConfiguration.ConfigurationResult caller) {
        GetConfiguration.query(ctx, caller);
    }
    //endregion

    @Override
    public void storeAccountData() {
        FirebaseUser user = AuthHelper.getUser();
        if (user != null) account().setValue(new AccountData(user));
        app().setValue(new AppData(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
    }

    public static boolean isAdmin() {
        // return ("T13cmH6prBhDyMeSgYrmKut7sPG3".equals(AuthHelper.getUserUID()));
        return Configurations.isAdmin(AuthHelper.getUser());
    }
}

