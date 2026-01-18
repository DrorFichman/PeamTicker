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
import java.util.HashMap;
import java.util.Map;

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
        // Staging nodes for transactional sync
        players_staging,
        games_staging,
        playersGames_staging,
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

    // Staging nodes for transactional sync
    public static DatabaseReference stagingPlayers() {
        return getNode(Node.players_staging);
    }

    public static DatabaseReference stagingGames() {
        return getNode(Node.games_staging);
    }

    public static DatabaseReference stagingPlayersGames() {
        return getNode(Node.playersGames_staging);
    }

    /**
     * Get the user's root reference for atomic multi-path updates.
     */
    private static DatabaseReference getUserRoot() {
        if (AuthHelper.getUser() == null || AuthHelper.getUserUID().isEmpty()) {
            Log.e("AccountFB", "User is not connected" + AuthHelper.getUser());
            return null;
        }
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        return database.getReference(AuthHelper.getUserUID());
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

    /**
     * Holds data being prepared for staging upload.
     */
    private static class StagingData {
        Map<String, Object> playersMap = new HashMap<>();
        Map<String, Object> gamesMap = new HashMap<>();
        Map<String, Object> playersGamesMap = new HashMap<>();
        int maxGameId = 0;
        String error = null;
    }

    /**
     * Sync data to cloud using a staging approach for atomicity.
     * 1. First clear any leftover staging data
     * 2. Write all data to staging nodes
     * 3. Perform atomic swap from staging to production using updateChildren()
     * 
     * If the app crashes during staging writes, production data remains intact.
     */
    private void syncData(Context ctx, SyncProgress progress) {
        progress.showSyncProgress("Preparing data...", 10);
        
        // Prepare all data for upload
        StagingData stagingData = new StagingData();
        
        // Get current max game ID before syncing
        stagingData.maxGameId = DbHelper.getMaxGame(ctx);
        
        // Prepare players data
        ArrayList<Player> players = DbHelper.getPlayers(ctx);
        for (Player p : players) {
            stagingData.playersMap.put(sanitizeKey(p.name()), p);
        }
        Log.i("syncData", "Prepared " + players.size() + " players for sync");
        
        // Prepare games data
        ArrayList<Game> games = DbHelper.getGames(ctx);
        for (Game g : games) {
            ArrayList<Player> team1 = DbHelper.getGameTeam(ctx, g.gameId, TeamEnum.Team1, 0);
            ArrayList<Player> team2 = DbHelper.getGameTeam(ctx, g.gameId, TeamEnum.Team2, 0);
            g.setTeams(team1, team2);
            stagingData.gamesMap.put(String.valueOf(g.gameId), g);
        }
        Log.i("syncData", "Prepared " + games.size() + " games for sync");
        
        // Prepare player games data
        ArrayList<PlayerGame> playerGames = DbHelper.getPlayersGames(ctx);
        for (PlayerGame pg : playerGames) {
            String playerKey = sanitizeKey(pg.playerName);
            // Build nested structure: playersGames/{playerName}/{gameId}
            @SuppressWarnings("unchecked")
            Map<String, Object> playerGamesForPlayer = (Map<String, Object>) stagingData.playersGamesMap.get(playerKey);
            if (playerGamesForPlayer == null) {
                playerGamesForPlayer = new HashMap<>();
                stagingData.playersGamesMap.put(playerKey, playerGamesForPlayer);
            }
            playerGamesForPlayer.put(String.valueOf(pg.gameId), pg);
        }
        Log.i("syncData", "Prepared " + playerGames.size() + " player games for sync");
        
        // Step 1: Clear any leftover staging data first
        progress.showSyncProgress("Clearing staging...", 20);
        clearStagingData(() -> {
            // Step 2: Write to staging nodes
            progress.showSyncProgress("Writing players to staging...", 30);
            writeStagingPlayers(stagingData, ctx, () -> {
                if (stagingData.error != null) {
                    progress.showSyncProgress(null, 0);
                    Toast.makeText(ctx, "Failed to stage players: " + stagingData.error, Toast.LENGTH_LONG).show();
                    return;
                }
                
                progress.showSyncProgress("Writing games to staging...", 45);
                writeStagingGames(stagingData, ctx, () -> {
                    if (stagingData.error != null) {
                        progress.showSyncProgress(null, 0);
                        Toast.makeText(ctx, "Failed to stage games: " + stagingData.error, Toast.LENGTH_LONG).show();
                        return;
                    }
                    
                    progress.showSyncProgress("Writing player stats to staging...", 60);
                    writeStagingPlayersGames(stagingData, ctx, () -> {
                        if (stagingData.error != null) {
                            progress.showSyncProgress(null, 0);
                            Toast.makeText(ctx, "Failed to stage player stats: " + stagingData.error, Toast.LENGTH_LONG).show();
                            return;
                        }
                        
                        // Step 3: Atomic swap from staging to production
                        progress.showSyncProgress("Finalizing sync...", 80);
                        swapStagingToProduction(stagingData, ctx, progress);
                    });
                });
            });
        });
    }

    /**
     * Clear any leftover staging data from a previous incomplete sync.
     */
    private static void clearStagingData(DataCallback handler) {
        stagingPlayers().removeValue((error1, ref1) -> {
            stagingGames().removeValue((error2, ref2) -> {
                stagingPlayersGames().removeValue((error3, ref3) -> {
                    Log.i("clearStagingData", "Staging data cleared");
                    handler.DataChanged();
                });
            });
        });
    }

    /**
     * Write players data to staging node.
     */
    private static void writeStagingPlayers(StagingData stagingData, Context ctx, DataCallback handler) {
        if (stagingData.playersMap.isEmpty()) {
            Log.i("writeStagingPlayers", "No players to stage");
            handler.DataChanged();
            return;
        }
        
        stagingPlayers().setValue(stagingData.playersMap, (error, ref) -> {
            if (error != null) {
                Log.e("writeStagingPlayers", "Failed: " + error.getMessage());
                stagingData.error = error.getMessage();
            } else {
                Log.i("writeStagingPlayers", "Staged " + stagingData.playersMap.size() + " players");
            }
            handler.DataChanged();
        });
    }

    /**
     * Write games data to staging node.
     */
    private static void writeStagingGames(StagingData stagingData, Context ctx, DataCallback handler) {
        if (stagingData.gamesMap.isEmpty()) {
            Log.i("writeStagingGames", "No games to stage");
            handler.DataChanged();
            return;
        }
        
        stagingGames().setValue(stagingData.gamesMap, (error, ref) -> {
            if (error != null) {
                Log.e("writeStagingGames", "Failed: " + error.getMessage());
                stagingData.error = error.getMessage();
            } else {
                Log.i("writeStagingGames", "Staged " + stagingData.gamesMap.size() + " games");
            }
            handler.DataChanged();
        });
    }

    /**
     * Write player games data to staging node.
     */
    private static void writeStagingPlayersGames(StagingData stagingData, Context ctx, DataCallback handler) {
        if (stagingData.playersGamesMap.isEmpty()) {
            Log.i("writeStagingPlayersGames", "No player games to stage");
            handler.DataChanged();
            return;
        }
        
        stagingPlayersGames().setValue(stagingData.playersGamesMap, (error, ref) -> {
            if (error != null) {
                Log.e("writeStagingPlayersGames", "Failed: " + error.getMessage());
                stagingData.error = error.getMessage();
            } else {
                Log.i("writeStagingPlayersGames", "Staged player games for " + stagingData.playersGamesMap.size() + " players");
            }
            handler.DataChanged();
        });
    }

    /**
     * Atomically swap staging data to production using updateChildren().
     * This operation is atomic - either all paths are updated or none are.
     */
    private static void swapStagingToProduction(StagingData stagingData, Context ctx, SyncProgress progress) {
        Map<String, Object> updates = new HashMap<>();
        
        // Set production paths to staging data (or null if empty to clear existing)
        updates.put("/" + Node.players.name(), stagingData.playersMap.isEmpty() ? null : stagingData.playersMap);
        updates.put("/" + Node.games.name(), stagingData.gamesMap.isEmpty() ? null : stagingData.gamesMap);
        updates.put("/" + Node.playersGames.name(), stagingData.playersGamesMap.isEmpty() ? null : stagingData.playersGamesMap);
        
        // Clear staging paths
        updates.put("/" + Node.players_staging.name(), null);
        updates.put("/" + Node.games_staging.name(), null);
        updates.put("/" + Node.playersGames_staging.name(), null);
        
        Log.i("swapStagingToProduction", "Performing atomic swap with " + updates.size() + " path updates");
        
        getUserRoot().updateChildren(updates, (error, ref) -> {
            if (error != null) {
                Log.e("swapStagingToProduction", "Atomic swap failed: " + error.getMessage());
                progress.showSyncProgress(null, 0);
                Toast.makeText(ctx, "Failed to finalize sync: " + error.getMessage(), Toast.LENGTH_LONG).show();
            } else {
                // Save the last synced game ID after successful sync
                PreferenceHelper.setLastSyncedGameId(ctx, stagingData.maxGameId);
                Log.i("swapStagingToProduction", "Atomic swap completed, saved last synced game ID: " + stagingData.maxGameId);
                
                progress.showSyncProgress(null, 100);
                Toast.makeText(ctx, "Sync completed", Toast.LENGTH_LONG).show();
            }
        });
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

    /**
     * Holds all data fetched from the cloud before committing to local database.
     * This allows us to fetch all data first, then insert atomically.
     */
    private static class CloudData {
        ArrayList<Player> players = new ArrayList<>();
        ArrayList<Game> games = new ArrayList<>();
        ArrayList<PlayerGame> playerGames = new ArrayList<>();
        String error = null;
    }

    /**
     * Fetch all data from cloud first, then insert into local DB in a single transaction.
     * This ensures atomicity - if anything fails or the app crashes during fetch,
     * the local database remains untouched.
     */
    private void fetchData(Context ctx, FetchDataProgress handler) {
        handler.onProgress("Pulling players...", 20);
        
        CloudData cloudData = new CloudData();
        
        // Fetch players from cloud
        fetchPlayersFromCloud(cloudData, () -> {
            if (cloudData.error != null) {
                handler.onProgress(null, 0);
                Toast.makeText(ctx, "Failed to pull players: " + cloudData.error, Toast.LENGTH_LONG).show();
                return;
            }
            
            handler.onProgress("Pulling games...", 40);
            
            // Fetch games from cloud
            fetchGamesFromCloud(cloudData, () -> {
                if (cloudData.error != null) {
                    handler.onProgress(null, 0);
                    Toast.makeText(ctx, "Failed to pull games: " + cloudData.error, Toast.LENGTH_LONG).show();
                    return;
                }
                
                handler.onProgress("Pulling player stats...", 60);
                
                // Fetch all player games from cloud
                fetchAllPlayerGamesFromCloud(cloudData, () -> {
                    if (cloudData.error != null) {
                        handler.onProgress(null, 0);
                        Toast.makeText(ctx, "Failed to pull player stats: " + cloudData.error, Toast.LENGTH_LONG).show();
                        return;
                    }
                    
                    handler.onProgress("Saving data...", 80);
                    
                    // All data fetched successfully - now insert atomically
                    try {
                        DbHelper.replaceDataInTransaction(ctx, 
                                cloudData.players, 
                                cloudData.games, 
                                cloudData.playerGames);
                        
                        // After successful transaction, update last synced game ID
                        int maxGameId = DbHelper.getMaxGame(ctx);
                        PreferenceHelper.setLastSyncedGameId(ctx, maxGameId);
                        Log.i("pullFromCloud", "Transaction completed, updated last synced game ID: " + maxGameId);
                        
                        handler.onProgress(null, 100);
                    } catch (Exception e) {
                        Log.e("pullFromCloud", "Transaction failed", e);
                        handler.onProgress(null, 0);
                        Toast.makeText(ctx, "Failed to save data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            });
        });
    }

    /**
     * Fetch players from cloud into memory (does not write to local DB).
     */
    private static void fetchPlayersFromCloud(CloudData cloudData, DataCallback handler) {
        ValueEventListener playerListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshotNode : dataSnapshot.getChildren()) {
                    Player p = snapshotNode.getValue(Player.class);
                    if (p != null) {
                        cloudData.players.add(p);
                    }
                }
                Log.i("fetchPlayersFromCloud", "Fetched " + cloudData.players.size() + " players from cloud");
                handler.DataChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("fetchPlayersFromCloud", "onCancelled", databaseError.toException());
                cloudData.error = databaseError.getMessage();
                handler.DataChanged();
            }
        };

        players().addListenerForSingleValueEvent(playerListener);
    }

    /**
     * Fetch games from cloud into memory (does not write to local DB).
     */
    private static void fetchGamesFromCloud(CloudData cloudData, DataCallback handler) {
        ValueEventListener gamesListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshotNode : dataSnapshot.getChildren()) {
                    Game g = snapshotNode.getValue(Game.class);
                    if (g != null) {
                        cloudData.games.add(g);
                    }
                }
                Log.i("fetchGamesFromCloud", "Fetched " + cloudData.games.size() + " games from cloud");
                handler.DataChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("fetchGamesFromCloud", "onCancelled", databaseError.toException());
                cloudData.error = databaseError.getMessage();
                handler.DataChanged();
            }
        };

        games().addListenerForSingleValueEvent(gamesListener);
    }

    /**
     * Fetch all player games from cloud into memory (does not write to local DB).
     * Fetches the entire playersGames node at once instead of per-player.
     */
    private static void fetchAllPlayerGamesFromCloud(CloudData cloudData, DataCallback handler) {
        ValueEventListener playersGamesListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // playersGames is structured as: playersGames/{playerName}/{gameId}
                for (DataSnapshot playerNode : dataSnapshot.getChildren()) {
                    for (DataSnapshot gameNode : playerNode.getChildren()) {
                        PlayerGame pg = gameNode.getValue(PlayerGame.class);
                        if (pg != null) {
                            cloudData.playerGames.add(pg);
                        }
                    }
                }
                Log.i("fetchAllPlayerGamesFromCloud", "Fetched " + cloudData.playerGames.size() + " player games from cloud");
                handler.DataChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("fetchAllPlayerGamesFromCloud", "onCancelled", databaseError.toException());
                cloudData.error = databaseError.getMessage();
                handler.DataChanged();
            }
        };

        playersGames().addListenerForSingleValueEvent(playersGamesListener);
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

