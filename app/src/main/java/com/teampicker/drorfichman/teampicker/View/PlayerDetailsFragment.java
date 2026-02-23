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
import android.widget.Button;
import android.widget.Toast;

import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.teampicker.drorfichman.teampicker.Controller.Broadcast.LocalNotifications;
import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.DbAsync;
import com.teampicker.drorfichman.teampicker.tools.cloud.FirebaseHelper;

import java.util.Calendar;

public class PlayerDetailsFragment extends Fragment {

    private static final String ARG_PLAYER = "player";
    private static final String ARG_IDENTIFIER = "identifier";

    public interface PlayerUpdated {
        void onUpdate(String name);
    }

    private Player player;
    private String createFromIdentifier;
    private PlayerUpdated updateListener = null;

    private TextInputEditText vName;
    private TextInputEditText vGrade;
    private Button vBirth;
    private Chip isGK;
    private Chip isDefender;
    private Chip isPlaymaker;
    private Chip isUnbreakable;
    private Chip isExtra;
    private Chip isInjured;
    private View injuredHelpIcon;

    public PlayerDetailsFragment() {
        super(R.layout.player_crad_fragment);
    }

    public static PlayerDetailsFragment newInstance(Player p, String identifier, PlayerUpdated update) {
        PlayerDetailsFragment playerCardFragment = new PlayerDetailsFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PLAYER, p);
        args.putString(ARG_IDENTIFIER, identifier);
        playerCardFragment.setArguments(args);
        playerCardFragment.updateListener = update;
        return playerCardFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            player = (Player) getArguments().getSerializable(ARG_PLAYER);
            createFromIdentifier = getArguments().getString(ARG_IDENTIFIER);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);

        vName = root.findViewById(R.id.edit_player_name);
        vGrade = root.findViewById(R.id.edit_player_grade);
        vBirth = root.findViewById(R.id.edit_player_birthday);
        vBirth.setOnClickListener(this::showBirthdayPicker);

        isGK = root.findViewById(R.id.player_is_gk);
        isDefender = root.findViewById(R.id.player_is_defender);
        isPlaymaker = root.findViewById(R.id.player_is_playmaker);
        isUnbreakable = root.findViewById(R.id.player_is_unbreaking);
        isExtra = root.findViewById(R.id.player_is_extra);
        isInjured = root.findViewById(R.id.player_is_injured);
        injuredHelpIcon = root.findViewById(R.id.injured_help_icon);
        injuredHelpIcon.setOnClickListener(v -> showInjuredHelpDialog());

        if (player != null) {
            vName.setText(player.mName);
            vGrade.setText(String.valueOf(player.mGrade));
            initBirthdayView(player);
            initPlayerAttributesView();
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

        hideKeyboard();

        // Capture birthday tag before going off-thread
        String birthdayTag = vBirth.getTag() != null ? (String) vBirth.getTag() : null;

        // Capture attribute state
        boolean gk = isGK.isChecked(), defender = isDefender.isChecked(),
                playmaker = isPlaymaker.isChecked(), unbreakable = isUnbreakable.isChecked(),
                extra = isExtra.isChecked(), injured = isInjured.isChecked();

        android.content.Context ctx = getContext();
        DbAsync.run(
                () -> {
                    // setNameAndGrade
                    if (player != null) {
                        if (!player.mName.equals(newName)) {
                            if (!DbHelper.updatePlayerName(ctx, player, newName)) return false;
                        }
                        DbHelper.updatePlayerGrade(ctx, player.mName, newGrade);
                    } else {
                        Player p = new Player(newName, newGrade);
                        if (!DbHelper.insertPlayer(ctx, p)) return false;
                        player = p;
                        if (createFromIdentifier != null) {
                            DbHelper.setPlayerIdentifier(ctx, newName, createFromIdentifier);
                        }
                    }

                    // setBirthday
                    if (birthdayTag != null) {
                        int newYear = Integer.parseInt(birthdayTag.split("/")[2]);
                        int newMonth = Integer.parseInt(birthdayTag.split("/")[1]);
                        int newDay = Integer.parseInt(birthdayTag.split("/")[0]);
                        if (newYear < 1900 || newYear > java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)) {
                            return false;
                        }
                        DbHelper.updatePlayerBirth(ctx, player.mName, newYear, newMonth, newDay);
                    }

                    // setAttributes
                    player.isGK = gk; player.isDefender = defender; player.isPlaymaker = playmaker;
                    player.isUnbreakable = unbreakable; player.isExtra = extra; player.isInjured = injured;
                    DbHelper.updatePlayerAttributes(ctx, player);
                    return true;
                },
                success -> {
                    if (!isAdded()) return;
                    if (!success) {
                        if (birthdayTag != null) {
                            int year = Integer.parseInt(birthdayTag.split("/")[2]);
                            if (year < 1900 || year > java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)) {
                                Toast.makeText(ctx, getString(R.string.toast_validation_year_range),
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                        Toast.makeText(ctx, getString(R.string.toast_validation_player_name_in_use),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    LocalNotifications.sendNotification(ctx, LocalNotifications.PLAYER_UPDATE_ACTION);
                    Toast.makeText(ctx, getString(R.string.toast_success_player_saved), Toast.LENGTH_SHORT).show();
                    if (updateListener != null) {
                        updateListener.onUpdate(player.mName);
                    } else if (getActivity() != null) {
                        getActivity().finish();
                    }
                });
    };

    private String verifyName() {
        String newName = FirebaseHelper.sanitizeKey(vName.getText().toString().trim());
        if (TextUtils.isEmpty(newName)) {
            Toast.makeText(getContext(), getString(R.string.toast_validation_player_name_required), Toast.LENGTH_SHORT).show();
            return "";
        }
        return newName;
    }

    private int verifyGrade() {
        String stringGrade = vGrade.getText().toString();
        if (TextUtils.isEmpty(stringGrade)) {
            Toast.makeText(getContext(), getString(R.string.toast_validation_player_grade_required), Toast.LENGTH_SHORT).show();
            return -1;
        }

        int newGrade = Integer.parseInt(stringGrade);
        if (newGrade > 99 || newGrade < 0) {
            Toast.makeText(getContext(), getString(R.string.toast_validation_grade_range), Toast.LENGTH_SHORT).show();
            return -1;
        }

        return newGrade;
    }


    //region init
    private void initPlayerAttributesView() {
        isGK.setChecked(player.isGK);
        isDefender.setChecked(player.isDefender);
        isPlaymaker.setChecked(player.isPlaymaker);
        isUnbreakable.setChecked(player.isUnbreakable);
        isExtra.setChecked(player.isExtra);
        // Temporarily remove listener to avoid triggering dialog during init
        isInjured.setOnCheckedChangeListener(null);
        isInjured.setChecked(player.isInjured);
        isInjured.setOnCheckedChangeListener((buttonView, isChecked) -> showGradeChangeDialog(isChecked));
    }

    private void showInjuredHelpDialog() {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.injured_help_title)
                .setMessage(R.string.injured_help_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showGradeChangeDialog(boolean markedAsInjured) {
        if (getContext() == null) return;

        EditText gradeInput = new EditText(getContext());
        gradeInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        gradeInput.setText(vGrade.getText());
        gradeInput.setSelection(gradeInput.getText().length());
        gradeInput.setPadding(50, 30, 50, 30);

        int messageRes = markedAsInjured ? R.string.injured_marked_message : R.string.injury_cleared_message;

        new AlertDialog.Builder(getContext())
                .setTitle(R.string.injured_grade_change_title)
                .setMessage(messageRes)
                .setView(gradeInput)
                .setPositiveButton(R.string.update_grade, (dialog, which) -> {
                    String newGradeStr = gradeInput.getText().toString();
                    if (!TextUtils.isEmpty(newGradeStr)) {
                        try {
                            int newGrade = Integer.parseInt(newGradeStr);
                            if (newGrade >= 0 && newGrade <= 99) {
                                vGrade.setText(String.valueOf(newGrade));
                            } else {
                                Toast.makeText(getContext(), getString(R.string.toast_validation_grade_range), Toast.LENGTH_SHORT).show();
                            }
                        } catch (NumberFormatException e) {
                            Toast.makeText(getContext(), getString(R.string.toast_validation_invalid_grade), Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(R.string.skip, null)
                .show();
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
    //endregion

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(vGrade.getWindowToken(), 0);
    }
}
