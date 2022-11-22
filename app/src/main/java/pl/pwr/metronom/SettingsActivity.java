package pl.pwr.metronom;

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
        setAppTheme();

        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        Toast.makeText(this, "onSharedPrefChange", Toast.LENGTH_SHORT).show();
        if (s.equals("dark_theme_switch")){
            this.recreate();
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

        if(SP.getBoolean("dark_theme_switch", false)){
            setTheme(R.style.DarkTheme);
            System.out.println("ustawilem DarkTheme");
        }
        else{
            setTheme(R.style.LightTheme);
            System.out.println("ustawilem LightTheme");
        }
    }
}