package kr.co.gstech.smarttimer.ui;

import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import kr.co.gstech.smarttimer.R;
import kr.co.gstech.smarttimer.utils.InputFilterMinMax;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);

        EditTextPreference angleEditTextPreference = findPreference("angle");
        assert angleEditTextPreference != null;
        angleEditTextPreference.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
            @Override
            public void onBindEditText(@NonNull EditText editText) {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                editText.setMaxEms(2);
                editText.setFilters(new InputFilter[]{ new InputFilterMinMax(0, 30)});
            }
        });
        EditTextPreference breakTimeEditTextPreference = findPreference("break_time");
        assert breakTimeEditTextPreference != null;
        breakTimeEditTextPreference.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
            @Override
            public void onBindEditText(@NonNull EditText editText) {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                editText.setMaxEms(2);
                editText.setFilters(new InputFilter[]{ new InputFilterMinMax(3, 60)});
            }
        });
        EditTextPreference learningGoalTimeEditTextPreference = findPreference("learning_goal_time");
        assert learningGoalTimeEditTextPreference != null;
        learningGoalTimeEditTextPreference.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
            @Override
            public void onBindEditText(@NonNull EditText editText) {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                editText.setMaxEms(2);
                editText.setFilters(new InputFilter[]{ new InputFilterMinMax(1, 24)});
            }
        });
    }

}