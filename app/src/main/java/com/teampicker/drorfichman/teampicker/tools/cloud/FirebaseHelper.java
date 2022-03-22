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
import com.teampicker.drorfichman.teampicker.Data.AccountData;
import com.teampicker.drorfichman.teampicker.Data.AppData;
import com.teampicker.drorfichman.teampicker.Data.Configurations;
import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.Data.Game;
import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.Data.PlayerGame;
import com.teampicker.drorfichman.teampicker.Data.TeamEnum;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.AuthHelper;
import com.teampicker.drorfichman.teampicker.tools.DialogHelper;
import com.teampicker.drorfichman.teampicker.tools.TutorialManager;
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

        if (!Configurations.isCloudFeaturesAllowed()) {
            if (Configurations.remote == null)
                Toast.makeText(ctx, ctx.getString(R.string.main_connectivity), Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(ctx, ctx.getString(R.string.main_cloud_not_allowed), Toast.LENGTH_SHORT).show();
            return;
        }

        TutorialManager.userActionTaken(ctx, TutorialManager.TutorialUserAction.click_sync_to_cloud);

        progress.showSyncStatus("Syncing...");
        syncPlayersToCloud(ctx, () ->
                syncGamesToCloud(ctx, () ->
                        syncPlayersGamesToCloud(ctx, () -> {
                            progress.showSyncStatus(null);
                            Toast.makeText(ctx, "Sync completed", Toast.LENGTH_LONG).show();
                        })));
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
                    ArrayList<Player> team1 = DbHelper.getCurrTeam(ctx, g.gameId, TeamEnum.Team1, 0);
                    ArrayList<Player> team2 = DbHelper.getCurrTeam(ctx, g.gameId, TeamEnum.Team2, 0);
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

        if (!Configurations.isCloudFeaturesAllowed()) {
            if (Configurations.remote == null)
                Toast.makeText(ctx, ctx.getString(R.string.main_connectivity), Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(ctx, ctx.getString(R.string.main_cloud_not_allowed), Toast.LENGTH_SHORT).show();
            return;
        }

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
                    Toast.makeText(ctx, "No Games found - sync to cloud first", Toast.LENGTH_LONG).show();
                }
            });

        }
    }

    private void showUsersDialog(Context ctx, ArrayList<AccountData> users, SyncProgress handler) {
        Log.d("showUsersDialog", users.size() + " users ");
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
            fetchData(ctx, status -> {
                if (status == null) {
                    // Clear selection once done
                    AuthHelper.fetchFor(null);
                    Toast.makeText(ctx, "Pull completed from " + users.get(which).displayName, Toast.LENGTH_LONG).show();
                }
                handler.showSyncStatus(status);
            });
        });
        builder.show();
    }

    private void checkPull(Context ctx, String date, SyncProgress handler) {

        DialogHelper.showApprovalDialog(ctx,
                "Pull data from cloud? \n" + "Last game - " + date, "",
                (dialog, which) -> {
                    fetchData(ctx, handler);
                });
    }

    private void fetchData(Context ctx, SyncProgress handler) {
        DbHelper.deleteTableContents(ctx);
        Log.i("pullFromCloud", "Delete local DB");

        handler.showSyncStatus("Pulling...");
        pullPlayersFromCloud(ctx, () ->
                pullGamesFromCloud(ctx, () ->
                        pullPlayersGamesFromCloud(ctx, () -> {
                            handler.showSyncStatus(null);
                        })));
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
    public static void fetchConfigurations(Context ctx, boolean forcePull, GetConfiguration.Results caller) {
        if (Configurations.remote == null || forcePull) {
            FirebaseHelper.pullRemoteConfiguration(ctx, result -> {
                Configurations.remote = result;
                if (!Configurations.isVersionSupported()) {
                    showForceUpgradeDialog(ctx);
                }
                if (caller != null) caller.queryResults(result);
            });
        } else {
            if (caller != null) caller.queryResults(Configurations.remote);
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

    public static void pullRemoteConfiguration(Context ctx, GetConfiguration.Results caller) {
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
        return ("T13cmH6prBhDyMeSgYrmKut7sPG3".equals(AuthHelper.getUserUID()));
    }
}

