package com.teampicker.drorfichman.teampicker.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.teampicker.drorfichman.teampicker.Data.Game;
import com.teampicker.drorfichman.teampicker.Data.ResultEnum;
import com.teampicker.drorfichman.teampicker.R;

import java.util.List;

/**
 * Created by drorfichman on 7/30/16.
 */
public class GameAdapter extends ArrayAdapter<Game> {

    private final Context context;
    private final List<Game> mGames;
    private int mSelectedGame;

    public GameAdapter(Context ctx, List<Game> games, int selectedGameId) {
        super(ctx, -1, games);
        context = ctx;
        mGames = games;
        mSelectedGame = selectedGameId;
    }

    public void setSelectedGameId(int selectedGameId) {
        mSelectedGame = selectedGameId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.game_item, parent, false);
        TextView dateView = view.findViewById(R.id.game_date);
        TextView resultSet = view.findViewById(R.id.game_result_set);
        ImageView res = view.findViewById(R.id.res_1);
        TextView playerGrade = view.findViewById(R.id.game_player_grade);

        Game g = mGames.get(position);

        dateView.setText(g.dateString);
        resultSet.setText(g.getScore());

        setPlayerResult(res, g);
        setPlayerGrade(playerGrade, g);

        view.setTag(R.id.game, g);
        view.setTag(R.id.game_index_id, position);

        view.setBackgroundColor(mSelectedGame == g.gameId ? Color.GRAY : Color.TRANSPARENT);

        return view;
    }

    private void setPlayerGrade(TextView playerGrade, Game g) {
        if (g.playerGrade > 0) { // player's results - player grade at the time of the game
            playerGrade.setVisibility(View.VISIBLE);
            playerGrade.setText(context.getString(R.string.parentheses, g.playerGrade));
        } else { // game history mode
            playerGrade.setVisibility(View.GONE);
        }
    }

    private void setPlayerResult(ImageView starView, Game g) {
        if (g.playerResult != null) { // player's results - green/red
            starView.setImageResource(g.playerResult.drawble);
            starView.setVisibility(View.VISIBLE);
        } else if (g.winningTeam != null) { // winning team - orange/blue/tie
            starView.setImageResource(g.winningTeam.drawable);
            starView.setVisibility(View.VISIBLE);
        } else { // unreachable
            starView.setVisibility(View.GONE);
        }
    }
}