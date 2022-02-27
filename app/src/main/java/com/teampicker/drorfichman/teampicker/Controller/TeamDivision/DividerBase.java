package com.teampicker.drorfichman.teampicker.Controller.TeamDivision;

import android.content.Context;
import android.util.Log;

import com.teampicker.drorfichman.teampicker.Data.TeamData;
import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.Data.PlayerAttribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import androidx.annotation.NonNull;

public abstract class DividerBase implements IDivider {

    abstract int gradeOption(Context ctx, OptionalDivision option);

    abstract boolean preferNewOption(int current, int another);

    @Override
    public void divide(Context ctx,
                       @NonNull ArrayList<Player> comingPlayers,
                       @NonNull List<Player> players1, @NonNull List<Player> players2,
                       int divideAttemptsCount,
                       TeamDivision.onTaskInProgress update) {


        ArrayList<Player> GKs = new ArrayList<>();
        ArrayList<Player> Defenders = new ArrayList<>();
        ArrayList<Player> Unbreakable = new ArrayList<>();
        ArrayList<Player> Playmakers = new ArrayList<>();
        ArrayList<Player> Extras = new ArrayList<>();
        ArrayList<Player> Others = new ArrayList<>();
        extractSpecialPlayers(comingPlayers, GKs, Defenders, Unbreakable, Playmakers, Extras, Others);

        Player extraPlayer = null;
        if (comingPlayers.size() % 2 == 1 && Others.size() > 0) {
            extraPlayer = Others.get(Others.size() - 1);
            Others.remove(extraPlayer);
        }

        if (update != null) update.update(0, "--");
        OptionalDivision selected = getDivision(comingPlayers.size() / 2, Others, GKs, Defenders, Playmakers, Unbreakable, Extras);
        int selectedGrade = gradeOption(ctx, selected);

        for (int option = 0; option < divideAttemptsCount; ++option) {
            OptionalDivision another = getDivision(comingPlayers.size() / 2, Others, GKs, Defenders, Playmakers, Unbreakable, Extras);
            int otherGrade = gradeOption(ctx, another);
            if (preferNewOption(selectedGrade, otherGrade)) {
                selected = another;
                selectedGrade = otherGrade;
            }
            if (update != null)
                update.update(getProgress(option, divideAttemptsCount), String.valueOf(selectedGrade));
        }

        players1.addAll(selected.players1.players);
        players2.addAll(selected.players2.players);

        if (extraPlayer != null) {
            Log.d("teams", "adding extra player " + extraPlayer.mName);
            players1.add(extraPlayer);
        }
    }

    private int getProgress(int option, int total) {
        return getSerieOf(option + 1) * 100 / getSerieOf(total);
    }

    private int getSerieOf(int n) {
        return n * (n + 1) / 2;
    }

    static ArrayList<PlayerAttribute> specials =
            new ArrayList<PlayerAttribute>() {{
                add(PlayerAttribute.isDefender);
                add(PlayerAttribute.isGK);
            }};

    private static void extractSpecialPlayers(@NonNull ArrayList<Player> comingPlayers,
                                              List<Player> GKs,
                                              List<Player> defenders,
                                              List<Player> unbreakable,
                                              List<Player> playmakers,
                                              List<Player> extras,
                                              List<Player> others) {

        for (int n = 0; n < comingPlayers.size(); ++n) {
            Player currPlayer = comingPlayers.get(n);
            if (currPlayer.isGK) {
                GKs.add(currPlayer);
            } else if (currPlayer.isDefender) {
                defenders.add(currPlayer);
            } else if (currPlayer.isUnbreakable) {
                unbreakable.add(currPlayer);
            } else if (currPlayer.isPlaymaker) {
                playmakers.add(currPlayer);
            } else if (currPlayer.isExtra) {
                extras.add(currPlayer);
            } else {
                others.add(currPlayer);
            }
        }

        Collections.sort(GKs);
        Collections.sort(defenders);
        Collections.sort(unbreakable);
        Collections.sort(playmakers);
        Collections.sort(extras);
        Collections.sort(others);
    }

    private static TeamData nextTeamToAdd(OptionalDivision option,
                                          int teamMaxSize,
                                          PlayerAttribute attribute) {

        if (option.players1.getCount() == teamMaxSize) return option.players2;
        if (option.players2.getCount() == teamMaxSize) return option.players1;

        int team1Players = option.players1.getCount(attribute);
        int team2Players = option.players2.getCount(attribute);
        if (team1Players > team2Players) { // more players of this type
            return option.players2;
        } else if (team2Players > team1Players) {
            return option.players1;
        } else {
            int team1Specials = option.players1.getCount(specials);
            int team2Specials = option.players2.getCount(specials);
            if (team1Specials > team2Specials) { // more special players
                return option.players2;
            } else if (team2Specials > team1Specials) {
                return option.players1;
            } else { // randomly add
                if (getRandom(new Random(), 2) == 0) {
                    return option.players2;
                } else {
                    return option.players1;
                }
            }
        }
    }

    private static void addSpecialPlayers(OptionalDivision option,
                                          int teamSize, ArrayList<Player> attributePlayers,
                                          PlayerAttribute attribute) {
        Random r = new Random();
        while (attributePlayers.size() > 0) {
            int a = getRandom(r, attributePlayers.size());
            nextTeamToAdd(option, teamSize, attribute).players.add(attributePlayers.get(a));
            attributePlayers.remove(a);
        }
    }

    private static OptionalDivision getDivision(int teamSize, ArrayList<Player> iOthers,
                                                ArrayList<Player> iGKs,
                                                ArrayList<Player> iDefenders,
                                                ArrayList<Player> iPlaymakers,
                                                ArrayList<Player> iUnbreakable,
                                                ArrayList<Player> iExtras) {
        OptionalDivision option = new OptionalDivision();
        ArrayList<Player> Others = cloneList(iOthers);
        ArrayList<Player> Unbreakable = cloneList(iUnbreakable);
        ArrayList<Player> GKs = cloneList(iGKs);
        ArrayList<Player> Defenders = cloneList(iDefenders);
        ArrayList<Player> Playmakers = cloneList(iPlaymakers);
        ArrayList<Player> Extras = cloneList(iExtras);

        addSpecialPlayers(option, teamSize, GKs, PlayerAttribute.isGK);
        addSpecialPlayers(option, teamSize, Defenders, PlayerAttribute.isDefender);
        addSpecialPlayers(option, teamSize, Playmakers, PlayerAttribute.isPlaymaker);
        addSpecialPlayers(option, teamSize, Unbreakable, PlayerAttribute.isUnbreakable);
        addSpecialPlayers(option, teamSize, Extras, PlayerAttribute.isExtra);
        addSpecialPlayers(option, teamSize, Others, null);

        return option;
    }

    static ArrayList<Player> cloneList(List<Player> players) {
        ArrayList<Player> clone = new ArrayList<>(players.size());
        clone.addAll(players);
        return clone;
    }

    static int getRandom(Random r, int value) {
        return r.nextInt(value);
    }
}
