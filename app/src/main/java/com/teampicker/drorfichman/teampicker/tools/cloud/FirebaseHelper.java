package com.teampicker.drorfichman.teampicker.tools.cloud;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.teampicker.drorfichman.teampicker.Data.AccountData;
import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.Data.Game;
import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.Data.PlayerGame;
import com.teampicker.drorfichman.teampicker.Data.TeamEnum;
import com.teampicker.drorfichman.teampicker.tools.AuthHelper;

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

    private enum Node {
        players,
        games,
        playersGames,
        account
    }

    private static DatabaseReference games() {
        return getNode(Node.games);
    }

    private static DatabaseReference playersGames() {
        return getNode(Node.playersGames);
    }

    private static DatabaseReference account() {
        return getNode(Node.account);
    }

    private static DatabaseReference players() {
        return getNode(Node.players);
    }

    private static DatabaseReference getNode(Node node) {
        if (AuthHelper.getUser() == null || AuthHelper.getUerUID().isEmpty()) {
            Log.e("AccountFB", "User is not connected" + AuthHelper.getUser());
            return null;
        }

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        return database.getReference(AuthHelper.getUerUID()).child(node.name());
    }

    public static String sanitizeKey(String key) {
        if (key == null) return null;
        return key.replaceAll("\\.", "");
    }

    //region Sync

    @Override
    public void syncToCloud(Context ctx, SyncProgress progress) {
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
                    ArrayList<Player> team1 = DbHelper.getCurrTeam(ctx, g.gameId, TeamEnum.Team1, -1);
                    ArrayList<Player> team2 = DbHelper.getCurrTeam(ctx, g.gameId, TeamEnum.Team2, -1);
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

    //endregion

    //region Pull

    @Override
    public void pullFromCloud(Context ctx, SyncProgress handler) {
        DbHelper.deleteTableContents(ctx);
        Log.i("pullFromCloud", "Delete local DB");

        handler.showSyncStatus("Pulling...");
        pullPlayersFromCloud(ctx, () ->
                pullGamesFromCloud(ctx, () ->
                        pullPlayersGamesFromCloud(ctx, () -> {
                            Toast.makeText(ctx, "Pull completed", Toast.LENGTH_LONG).show();
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

                    Log.i("pullPlayersGamesFromCloud", "Local players games DB updated from cloud - " + p.mName + " - " + gameCount);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e("pullPlayersGamesFromCloud", "onCancelled", databaseError.toException());
                }
            };

            playersGames().child(p.mName).addListenerForSingleValueEvent(playersGamesListener);
        }

        handler.DataChanged();
    }

    //endregion

    private static void storePlayer(Player p) {
        players().child(sanitizeKey(p.name())).setValue(p);
    }

    private static void storeGame(Game g) {
        games().child(String.valueOf(g.gameId)).setValue(g);
    }

    private static void storePlayerGame(PlayerGame pg) {
        playersGames().child(sanitizeKey(pg.playerName)).child(String.valueOf(pg.gameId)).setValue(pg);
    }

    @Override
    public void storeAccountData() {
        account().setValue(new AccountData(AuthHelper.getUser()));
    }
}

