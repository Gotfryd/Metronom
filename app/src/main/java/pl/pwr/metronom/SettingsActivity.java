package pl.pwr.metronom;

import static androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.widget.Toast;

public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PreferenceManager.getDefaultSharedPreferences(getBaseContext()).registerOnSharedPreferenceChangeListener(this);

        setAppTheme(); // nie dziala zmiana motywu ekranu preferences
        System.out.println("Default night mode = " + getDefaultNightMode());
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        Toast.makeText(this, "onSharedPrefChange", Toast.LENGTH_SHORT).show();
       if (s.equals("night_mode_switch")){
            this.recreate();
           Toast.makeText(this, "recreated", Toast.LENGTH_SHORT).show();
        }
       else if(s.equals("sound_effects_list")){
           this.recreate();
           Toast.makeText(this, "recreated", Toast.LENGTH_SHORT).show();
       }
    }

    public static class MyPreferenceFragment extends PreferenceFragment {

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

        }
    }

    public void setAppTheme(){
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        if(SP.getBoolean("night_mode_switch", false)){
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            System.out.println("wlaczylem tryb nocny");
        }
        else{
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            System.out.println("wlaczylem tryb dzienny");
        }
    }
}