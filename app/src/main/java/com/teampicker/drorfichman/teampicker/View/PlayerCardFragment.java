package com.teampicker.drorfichman.teampicker.View;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.cloud.FirebaseHelper;

import java.util.Calendar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class PlayerCardFragment extends Fragment {

    private PlayerUpdated updateListener = null;

    public interface PlayerUpdated {
        void onUpdate(String name);
    }

    private Player pPlayer;
    private EditText vName;
    private EditText vGrade;
    private TextView vBirth;
    private CheckBox isGK;
    private CheckBox isDefender;
    private CheckBox isPlaymaker;
    private CheckBox isUnbreakable;

    public PlayerCardFragment() {
        super(R.layout.player_crad_fragment);
    }

    public static PlayerCardFragment newInstance(Player p, PlayerUpdated update) {
        PlayerCardFragment playerCardFragment = new PlayerCardFragment();
        playerCardFragment.pPlayer = p;
        playerCardFragment.updateListener = update;
        return playerCardFragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);

        vName = root.findViewById(R.id.edit_player_name);
        vGrade = root.findViewById(R.id.edit_player_grade);

        vBirth = root.findViewById(R.id.edit_player_birthday);
        vBirth.setOnClickListener(this::showDatePicker);

        isGK = root.findViewById(R.id.player_is_gk);
        isDefender = root.findViewById(R.id.player_is_defender);
        isPlaymaker = root.findViewById(R.id.player_is_playmaker);
        isUnbreakable = root.findViewById(R.id.player_is_unbreaking);

        vName.setText(pPlayer.mName);
        vGrade.setText(String.valueOf(pPlayer.mGrade));

        if (pPlayer.mBirthYear > 0) setBirthday();

        initPlayerAttributes();

        root.findViewById(R.id.save).setOnClickListener(saveClick);

        return root;
    }

    View.OnClickListener saveClick = view -> {
        Context ctx = getContext();
        if (ctx == null) return;

        getActivity().setResult(1);
        boolean playerUpdated = false;

        String newGradeNumber = vGrade.getText().toString();
        String newName = FirebaseHelper.sanitizeKey(vName.getText().toString());

        if (!TextUtils.isEmpty(newGradeNumber)) { // update grade
            int newGradeString = Integer.parseInt(newGradeNumber);

            if (newGradeString > 99 || newGradeString < 0) {
                Toast.makeText(ctx, "Score must be between 0-99", Toast.LENGTH_LONG).show();
                return;
            }
            DbHelper.updatePlayerGrade(ctx, pPlayer.mName, newGradeString);
            playerUpdated = true;
        }

        if (!TextUtils.isEmpty(newName) && !pPlayer.mName.equals(newName)) { // update name
            boolean updated = DbHelper.updatePlayerName(ctx, pPlayer, newName);
            if (updated) {
                playerUpdated = true;
            } else {
                Toast.makeText(getContext(), "Player name is already taken", Toast.LENGTH_LONG).show();
                return;
            }
        }

        if (vBirth.getTag() != null) { // update birth
            String date = (String) vBirth.getTag();
            int newYear = Integer.parseInt(date.split("/")[1]);
            int newMonth = Integer.parseInt(date.split("/")[0]);

            if (newYear < 1900 || newYear > Calendar.getInstance().get(Calendar.YEAR)) {
                Toast.makeText(getContext(), "Year must be between 1900-now", Toast.LENGTH_LONG).show();
                return;
            }

            Log.i("AGE", "Year " + newYear + " month " + newMonth);
            DbHelper.updatePlayerBirth(ctx, pPlayer.mName, newYear, newMonth);
            playerUpdated = true;
        }

        // update attributes
        setAttributes(pPlayer);

        if (playerUpdated) {
            if (updateListener != null) updateListener.onUpdate(pPlayer.mName);
            Toast.makeText(ctx, "Player updated", Toast.LENGTH_LONG).show();
        }
    };

    private void initPlayerAttributes() {
        isGK.setChecked(pPlayer.isGK);
        isDefender.setChecked(pPlayer.isDefender);
        isPlaymaker.setChecked(pPlayer.isPlaymaker);
        isUnbreakable.setChecked(pPlayer.isUnbreakable);
    }

    private void setAttributes(Player p) {
        p.isGK = isGK.isChecked();
        p.isDefender = isDefender.isChecked();
        p.isPlaymaker = isPlaymaker.isChecked();
        p.isUnbreakable = isUnbreakable.isChecked();
        DbHelper.updatePlayerAttributes(getContext(), p);
    }

    public void showDatePicker(View view) {
        int year = pPlayer.mBirthYear > 0 ? pPlayer.mBirthYear : 2000;
        int month = pPlayer.mBirthMonth > 0 ? pPlayer.mBirthMonth - 1 : 0;
        DatePickerDialog d = new DatePickerDialog(getContext(), 0, (datePicker, newYear, newMonth, i2) -> {
        Log.i("Birth", "set " + pPlayer.mBirthDay);
            newMonth++; // starts at 0...
            pPlayer.mBirthMonth = newMonth;
            pPlayer.mBirthYear = newYear;
            Log.i("Birth", "set " + newDay);
            setBirthday();
        }, year, month, 1);
        d.show();
    }

    private void setBirthday() {
        vBirth.setText(String.valueOf(pPlayer.getAge()));
        vBirth.setTag(pPlayer.mBirthMonth + "/" + pPlayer.mBirthYear);
    }
}
