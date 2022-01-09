package com.teampicker.drorfichman.teampicker.View;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.teampicker.drorfichman.teampicker.Data.DbHelper;
import com.teampicker.drorfichman.teampicker.Data.Player;
import com.teampicker.drorfichman.teampicker.R;
import com.teampicker.drorfichman.teampicker.tools.cloud.FirebaseHelper;

import java.util.Calendar;

import androidx.appcompat.app.AppCompatActivity;

public class NewPlayerActivity extends AppCompatActivity {

    private EditText vGrade;
    private EditText vName;
    private TextView vBirth;
    private CheckBox isGK;
    private CheckBox isDefender;
    private CheckBox isPlaymaker;
    private CheckBox isUnbreakable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_crad_fragment);

        vName = findViewById(R.id.edit_player_name);
        vGrade = findViewById(R.id.edit_player_grade);
        vBirth = findViewById(R.id.edit_player_birthday);
        isGK = findViewById(R.id.player_is_gk);
        isDefender = findViewById(R.id.player_is_defender);
        isPlaymaker = findViewById(R.id.player_is_playmaker);
        isUnbreakable = findViewById(R.id.player_is_unbreaking);

        findViewById(R.id.save).setOnClickListener(view -> {
            setResult(1);

            String stringGrade = vGrade.getText().toString();
            if (TextUtils.isEmpty(stringGrade)) {
                Toast.makeText(getApplicationContext(), "Fill player's grade", Toast.LENGTH_LONG).show();
                return;
            }

            int newGrade = Integer.parseInt(stringGrade);

            if (newGrade > 99 || newGrade < 0) {
                Toast.makeText(getApplicationContext(), "Grade must be between 0-99", Toast.LENGTH_LONG).show();
                return;
            }

            String newName = FirebaseHelper.sanitizeKey(vName.getText().toString().trim());
            if (TextUtils.isEmpty(newName)) {
                Toast.makeText(getApplicationContext(), "Fill player's name", Toast.LENGTH_LONG).show();
                return;
            }

            Player p = new Player(newName, newGrade);
            boolean isCreated = DbHelper.insertPlayer(NewPlayerActivity.this, p);

            if (isCreated) {
                setPlayerBirthday(p);
                setAttributes(p);

                Toast.makeText(getApplicationContext(), "Player added", Toast.LENGTH_LONG).show();

                finishNow(1);
            } else {
                Toast.makeText(getApplicationContext(), "Player name is already taken", Toast.LENGTH_LONG).show();
            }
        });

        vBirth.setOnClickListener(this::showDatePicker);

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    private void setAttributes(Player p) {
        p.isGK = isGK.isChecked();
        p.isDefender = isDefender.isChecked();
        p.isPlaymaker = isPlaymaker.isChecked();
        p.isUnbreakable = isUnbreakable.isChecked();
        DbHelper.updatePlayerAttributes(this, p);
    }

    private void setPlayerBirthday(Player p) {
        if (vBirth.getTag() != null) {
            Calendar date =  (Calendar) vBirth.getTag();
            int newYear = date.get(Calendar.YEAR);
            int newMonth = date.get(Calendar.MONTH);

            if (newYear < 1900 || newYear > Calendar.getInstance().get(Calendar.YEAR)) {
                Toast.makeText(getApplicationContext(), "Year must be between 1900-now", Toast.LENGTH_LONG).show();
                return;
            }

            // TODO birth day
            DbHelper.updatePlayerBirth(getApplicationContext(), p.mName, newYear, newMonth, 1);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        vName.setFocusableInTouchMode(true);
        vName.requestFocus();
    }

    private void finishNow(int result) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(vGrade.getWindowToken(), 0);

        setResult(result);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public void showDatePicker(View view) {
        DatePickerDialog d = new DatePickerDialog(this, (datePicker, year, month, i2) -> {
            month++;
            setBirthday(year, month);
        }, 2005, 4, 25);
        d.show();
    }

    private void setBirthday(int year, int month) {
        vBirth.setText(month + "/" + year);
        Calendar date = Calendar.getInstance();
        date.set(Calendar.YEAR, year);
        date.set(Calendar.MONTH, month);
        vBirth.setTag(date);
    }
}
