package com.teampicker.drorfichman.teampicker.View;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.teampicker.drorfichman.teampicker.Controller.Broadcast.LocalNotifications;
import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.Data.StreakInfo;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.cloud.FirebaseHelper;

import java.util.Calendar;

public class PlayerDetailsFragment extends Fragment {

    public interface PlayerUpdated {
        void onUpdate(String name);
    }

    private Player player;
    private String createFromIdentifier;
    private PlayerUpdated updateListener = null;

    private EditText vName;
    private EditText vGrade;
    private TextView vBirth;
    private TextView vLongestUnbeatenRun;
    private TextView vConsecutiveAttendance;
    private CheckBox isGK;
    // private CheckBox isInjured;
    private CheckBox isDefender;
    private CheckBox isPlaymaker;
    private CheckBox isUnbreakable;
    private CheckBox isExtra;

    public PlayerDetailsFragment() {
        super(R.layout.player_crad_fragment);
    }

    public static PlayerDetailsFragment newInstance(Player p, String identifier, PlayerUpdated update) {
        PlayerDetailsFragment playerCardFragment = new PlayerDetailsFragment();
        playerCardFragment.player = p;
        playerCardFragment.createFromIdentifier = identifier;
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
        vLongestUnbeatenRun = root.findViewById(R.id.longest_unbeaten_run);
        vConsecutiveAttendance = root.findViewById(R.id.consecutive_attendance);
        vBirth.setOnClickListener(this::showBirthdayPicker);

        // isInjured = root.findViewById(R.id.player_is_injured);
        isGK = root.findViewById(R.id.player_is_gk);
        isDefender = root.findViewById(R.id.player_is_defender);
        isPlaymaker = root.findViewById(R.id.player_is_playmaker);
        isUnbreakable = root.findViewById(R.id.player_is_unbreaking);
        isExtra = root.findViewById(R.id.player_is_extra);

        if (player != null) {
            vName.setText(player.mName);
            vGrade.setText(String.valueOf(player.mGrade));
            initBirthdayView(player);
            initPlayerAttributesView();
            updateLongestUnbeatenRun();
            updateConsecutiveAttendance();
        } else if (createFromIdentifier != null) {
            vName.setText(createFromIdentifier);
        } else {
            vName.requestFocus();
        }

        root.findViewById(R.id.save).setOnClickListener(saveClick);

        return root;
    }

    View.OnClickListener saveClick = view -> {
        if (getContext() == null) return;

        String newName = verifyName();
        if (TextUtils.isEmpty(newName)) return;

        int newGrade = verifyGrade();
        if (newGrade < 0) return;

        if (!setNameAndGrade(newName, newGrade)) return;

        if (!setBirthday()) return;

        setAttributes();

        hideKeyboard();

        LocalNotifications.sendNotification(getContext(), LocalNotifications.PLAYER_UPDATE_ACTION);

        Toast.makeText(getContext(), "Player saved", Toast.LENGTH_SHORT).show();
        updateListener.onUpdate(player.mName);
    };

    private String verifyName() {
        String newName = FirebaseHelper.sanitizeKey(vName.getText().toString().trim());
        if (TextUtils.isEmpty(newName)) {
            Toast.makeText(getContext(), "Fill player's name", Toast.LENGTH_SHORT).show();
            return "";
        }
        return newName;
    }

    private int verifyGrade() {
        String stringGrade = vGrade.getText().toString();
        if (TextUtils.isEmpty(stringGrade)) {
            Toast.makeText(getContext(), "Fill player's grade", Toast.LENGTH_SHORT).show();
            return -1;
        }

        int newGrade = Integer.parseInt(stringGrade);
        if (newGrade > 99 || newGrade < 0) {
            Toast.makeText(getContext(), "Grade must be between 0-99", Toast.LENGTH_SHORT).show();
            return -1;
        }

        return newGrade;
    }

    private boolean setNameAndGrade(String name, int grade) {
        if (player != null) {
            if (!player.mName.equals(name)) {
                if (!DbHelper.updatePlayerName(getContext(), player, name)) {
                    Toast.makeText(getContext(), "Player name is already in use", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
            DbHelper.updatePlayerGrade(getContext(), player.mName, grade);
        } else {
            Player p = new Player(name, grade);
            if (!DbHelper.insertPlayer(getContext(), p)) {
                Toast.makeText(getContext(), "Player name is already in use", Toast.LENGTH_SHORT).show();
                return false;
            }
            player = p;
            if (createFromIdentifier != null) {
                DbHelper.setPlayerIdentifier(getContext(), name, createFromIdentifier);
            }
        }

        return true;
    }

    private boolean setBirthday() {
        if (vBirth.getTag() != null) { // update birth
            String date = (String) vBirth.getTag();
            int newYear = Integer.parseInt(date.split("/")[2]);
            int newMonth = Integer.parseInt(date.split("/")[1]);
            int newDay = Integer.parseInt(date.split("/")[0]);

            if (newYear < 1900 || newYear > Calendar.getInstance().get(Calendar.YEAR)) {
                Toast.makeText(getContext(), "Year must be between 1900-now", Toast.LENGTH_SHORT).show();
                return false;
            }

            Log.i("AGE", "Year " + newYear + " month " + newMonth);
            DbHelper.updatePlayerBirth(getContext(), player.mName, newYear, newMonth, newDay);
        }
        return true;
    }

    private void setAttributes() {
        player.isGK = isGK.isChecked();
        player.isDefender = isDefender.isChecked();
        player.isPlaymaker = isPlaymaker.isChecked();
        player.isUnbreakable = isUnbreakable.isChecked();
        player.isExtra = isExtra.isChecked();
        // player.isInjured = isInjured.isChecked();
        DbHelper.updatePlayerAttributes(getContext(), player);
    }

    //region init
    private void initPlayerAttributesView() {
        isGK.setChecked(player.isGK);
        isDefender.setChecked(player.isDefender);
        isPlaymaker.setChecked(player.isPlaymaker);
        isUnbreakable.setChecked(player.isUnbreakable);
        isExtra.setChecked(player.isExtra);
        // isInjured.setChecked(player.isInjured);
    }

    public void showBirthdayPicker(View view) {
        int year = player != null && player.mBirthYear > 0 ? player.mBirthYear : Calendar.getInstance().get(Calendar.YEAR) - 20;
        int month = player != null && player.mBirthMonth > 0 ? player.mBirthMonth - 1 : 0;
        int day = player != null && player.mBirthDay > 0 ? player.mBirthDay : 1;
        DatePickerDialog d = new DatePickerDialog(getContext(), 0, (datePicker, newYear, newMonth, newDay) -> {
            newMonth++; // starts at 0...
            Player p = new Player();
            p.mBirthMonth = newMonth;
            p.mBirthYear = newYear;
            p.mBirthDay = newDay;
            initBirthdayView(p);
        }, year, month, day);
        d.show();
    }

    private void initBirthdayView(Player p) {
        if (p.getAge() > 0) {
            vBirth.setText(String.valueOf(p.getAge()));
            vBirth.setTag(p.mBirthDay + "/" + p.mBirthMonth + "/" + p.mBirthYear);
        }
    }

    private void updateLongestUnbeatenRun() {
        if (player != null && getContext() != null) {
            StreakInfo streak = DbHelper.getLongestUnbeatenRun(getContext(), player.mName);
            if (streak.length > 0) {
                vLongestUnbeatenRun.setText(String.format("%d games\n(%d days)", streak.length, streak.days));
                vLongestUnbeatenRun.setVisibility(View.VISIBLE);
            } else {
                vLongestUnbeatenRun.setVisibility(View.GONE);
            }
        }
    }

    private void updateConsecutiveAttendance() {
        if (player != null && getContext() != null) {
            StreakInfo streak = DbHelper.getConsecutiveAttendance(getContext(), player.mName);
            if (streak.length > 0) {
                vConsecutiveAttendance.setText(String.format("%d games\n(%d days)", streak.length, streak.days));
                vConsecutiveAttendance.setVisibility(View.VISIBLE);
            } else {
                vConsecutiveAttendance.setVisibility(View.GONE);
            }
        }
    }
    //endregion

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(vGrade.getWindowToken(), 0);
    }
}
