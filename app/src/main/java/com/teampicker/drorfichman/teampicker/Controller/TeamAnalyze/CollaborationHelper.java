package com.teampicker.drorfichman.teampicker.Controller.TeamAnalyze;

import android.content.Context;

import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.Data.BuilderPlayerCollaborationStatistics;
import com.teampicker.drorfichman.teampicker.Data.PlayerChemistry;

import java.util.HashMap;
import java.util.List;

/**
 * Created by drorfichman on 9/17/16.
 */
public class CollaborationHelper {

    private static final int RECENT_GAMES = 50;         // games back to look for
    public static final int MIN_GAMES_ANALYSIS = 7;     // games to consider chemistry effect on player
    public static final int MIN_GAMES_TOGETHER = 5;     // games played with another player to consider chemistry
    public static final int WIN_RATE_MARGIN = 0;        // win rate exceeded by to count effect

    public static Collaboration getCollaborationData(Context context, List<Player> team1, List<Player> team2) {
        return getCollaborationData(context, team1, team2, null);
    }

    public static Collaboration getCollaborationData(Context context, List<Player> team1, List<Player> team2, BuilderPlayerCollaborationStatistics params) {
        Collaboration result = new Collaboration();
        processTeam(context, result, team1, team2, params);
        processTeam(context, result, team2, team1, params);

        return result;
    }

    private static void processTeam(Context context, Collaboration result, List<Player> team, List<Player> other, BuilderPlayerCollaborationStatistics params) {

        if (params == null) params = new BuilderPlayerCollaborationStatistics();
        params.setGames(RECENT_GAMES);

        for (Player currPlayer : team) {

            HashMap<String, PlayerChemistry> collaborationMap =
                    DbHelper.getPlayersParticipationStatistics(context, currPlayer.mName, params);

            PlayerCollaboration playerCollaboration = new PlayerCollaboration(currPlayer);
            if (currPlayer.statistics != null) {
                processWithPlayers(team, currPlayer, collaborationMap, playerCollaboration);
                processAgainstPlayers(other, currPlayer, collaborationMap, playerCollaboration);
            }
            result.players.put(currPlayer.mName, playerCollaboration);
        }
    }

    private static void processAgainstPlayers(List<Player> other, Player currPlayer, HashMap<String,
            PlayerChemistry> collaborationMap, PlayerCollaboration playerCollaboration) {
        for (Player against : other) {
            PlayerChemistry collaborationWith = collaborationMap.get(against.mName);
            if (collaborationWith != null) {
                EffectMargin collaboratorEffect = new EffectMargin(currPlayer, collaborationWith);
                playerCollaboration.addOpponent(against.mName, collaboratorEffect);
            }
        }
    }

    private static void processWithPlayers(List<Player> team, Player currPlayer, HashMap<String,
            PlayerChemistry> collaborationMap, PlayerCollaboration playerCollaboration) {
        for (Player with : team) {
            PlayerChemistry collaborationWith = collaborationMap.get(with.mName);
            if (collaborationWith != null) {
                EffectMargin collaboratorEffect = new EffectMargin(currPlayer, collaborationWith);
                playerCollaboration.addCollaborator(with.mName, collaboratorEffect);
            }
        }
    }
}
