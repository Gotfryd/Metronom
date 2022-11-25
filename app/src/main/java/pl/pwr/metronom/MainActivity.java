package pl.pwr.metronom;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Stopwatch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    boolean isPlaying = false;
    boolean importFlag = false;
    boolean firstTap = true;

    String fileName;
    String baseDir;
    String pathDir;
    String line;

    File csvFile;

    FileInputStream fis;
    InputStreamReader isr;
    BufferedReader buff;
    int bpmAmount;
    int currentSongNo = 0;
    int tapCounter = 0;
    private int STORAGE_PERMISSION_CODE = 1;
    final int SETTINGS_ACTIVITY = 1;
    long summedTapsTime;
    long averageTapsTime;

    String effectName;
    String languageCode;

    Intent settingsIntent;

    LinearLayout myLinearLayout;

    MediaPlayer tickSound;;
    Timer tickTimer;
    TimerTask tickTone;

    Stopwatch tapStopwatch;

    TextView tempoMarking;
    TextView songName;

    EditText bpmEditTextInc;

    ImageButton incrementButton;
    ImageButton decrementButton;
    ImageButton pausePlayButton;
    ImageButton recordButton;
    ImageButton tapButton;
    ImageButton importButton;
    ImageButton previousSongButton;
    ImageButton nextSongButton;

    List<SongsList> importedSongs = new ArrayList<>();

    SharedPreferences SP;

    ActivityResultLauncher<Intent> settingsActivityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setAppTheme();
        setLanguage();

        setContentView(R.layout.activity_main);

        tempoMarking = (TextView) findViewById(R.id.tempoMarking);

        songName = (TextView) findViewById(R.id.songName);

        bpmEditTextInc = (EditText) findViewById(R.id.bpmInteger);

        incrementButton = (ImageButton) findViewById(R.id.incrementButton);
        incrementButton.setOnClickListener(this);

        decrementButton = (ImageButton) findViewById(R.id.decrementButton);
        decrementButton.setOnClickListener(this);

        pausePlayButton = (ImageButton) findViewById(R.id.pausePlayButton);
        pausePlayButton.setOnClickListener(this);

        recordButton = (ImageButton) findViewById(R.id.recordButton);
        recordButton.setOnClickListener(this);

        tapButton = (ImageButton) findViewById(R.id.tapButton);
        tapButton.setOnClickListener(this);

        importButton = (ImageButton) findViewById(R.id.importButton);
        importButton.setOnClickListener(this);

        previousSongButton = (ImageButton) findViewById(R.id.previousSongButton);
        previousSongButton.setOnClickListener(this);

        nextSongButton = (ImageButton) findViewById(R.id.nextSongButton);
        nextSongButton.setOnClickListener(this);

        myLinearLayout = (LinearLayout) findViewById(R.id.linearLayout);
        myLinearLayout.setOnClickListener(this);

        nextSongButton.setEnabled(false); // na starcie apki zablokuj przyciski next i previous bo z poziomu activity_main.xml nie dziala a tutaj da sie to zrobic
        previousSongButton.setEnabled(false); //

        bpmAmount = Integer.valueOf(bpmEditTextInc.getText().toString()).intValue(); // pierwsze i jednokrotne przypisanie wartosci do tej zmiennej zeby tempoMarking zadzialalo poprawnie
        setTempoMarking(); // jednokrotne wykonanie funkcji zeby ustawila sie nazwa tempa po wlaczeniu aplikacji (na samym starcie)

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.actionBarTitle);


        settingsActivityResultLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        new ActivityResultCallback<ActivityResult>() {
                            @Override
                            public void onActivityResult(ActivityResult result) {
                                    Intent data = result.getData();
                                    int resultCode = result.getResultCode();
                                    if(resultCode == RESULT_OK) {
                                        recreate();
                                    }
                                    else{
                                        recreate();
                                    }
                            }
                        });

        setTickEffect();

        tickTimer = new Timer("metronomeCounter", true);

        tickTone = new TimerTask(){
            @Override
            public void run(){
               tickSound.start();
            }
        };

        bpmEditTextInc.setOnEditorActionListener(new TextView.OnEditorActionListener() { // akcja wykonujaca sie po kliknieciu przycisku "Enter" z klawiatury
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_DONE){
                    stopTimer();
                    if(!isPlaying) {
                        startTimer();
                    }
                }
                bpmAmount = Integer.valueOf(bpmEditTextInc.getText().toString()).intValue();
                setTempoMarking();
                return false;
            }
        });

        tapButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) { // resetruje dane uzyskane przez przytrzymanie przycisku "Tap"
                Toast.makeText(MainActivity.this, "long click test", Toast.LENGTH_SHORT).show();

                summedTapsTime = 0;
                averageTapsTime = 0;
                tapCounter = 0;
                firstTap = true;
                bpmEditTextInc.setText(String.valueOf((90)));
                stopTimer();

                return true;
            }
        });

    }



// koniec onCreate


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_item, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId())
        {
            case R.id.action_settings:
                stopTimer();
                Toast.makeText(MainActivity.this, "settings selected test", Toast.LENGTH_SHORT).show();
                settingsIntent = new Intent(this, SettingsActivity.class);
               // startActivityForResult(settingsIntent, SETTINGS_ACTIVITY); // tak sie robi po staremu
                settingsActivityResultLauncher.launch(settingsIntent);
                break;

        }
        return super.onOptionsItemSelected(item);
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        if(requestCode == SETTINGS_ACTIVITY){
//            this.recreate();
//        }
//    }

    public void setAppTheme(){
        SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        if(SP.getBoolean("night_mode_switch", false)){
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            System.out.println("wlaczylem tryb nocny");
        }
        else{
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            System.out.println("wlaczylem tryb dzienny");
        }
    }

    private void setTickEffect() {
        SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        effectName = SP.getString("sound_effects_list", "soundone");
        System.out.println("nazwa efektu"+effectName);

        switch(effectName) {
            case "soundone":
                tickSound = MediaPlayer.create(this, R.raw.soundone);
                break;
            case "soundtwo":
                tickSound = MediaPlayer.create(this, R.raw.soundtwo);
                break;
            case "soundthree":
                tickSound = MediaPlayer.create(this, R.raw.soundthree);
                break;
            case "soundfour":
                tickSound = MediaPlayer.create(this, R.raw.soundfour);
                break;
            case "soundfive":
                tickSound = MediaPlayer.create(this, R.raw.soundfive);
                break;
            default:
                tickSound = MediaPlayer.create(this, R.raw.soundone);
        }
    }

    private void setLanguage() {

        SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        languageCode = SP.getString("language_list", "en");

        String languageToLoad  = languageCode; // tutaj podaje skrot jezyka (2 litery)
        Locale locale = new Locale(languageToLoad);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
        this.setContentView(R.layout.activity_main); // przez to nie wczytuje dzwieku
    }

    private boolean isExternalStorageReadable() {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || Environment.MEDIA_MOUNTED_READ_ONLY.equals(Environment.getExternalStorageState())) {
           // Toast.makeText(MainActivity.this, "External storage is readable", Toast.LENGTH_SHORT).show();
            return true;
        }
        else{
          //  Toast.makeText(MainActivity.this, "External storage is not readable", Toast.LENGTH_SHORT).show();
            return false;
        }
    }


    public boolean importAndDataRead() {
            line = "";

            try{

                fileName = "database.csv";
                baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();
                pathDir = baseDir + "/Download/";

                csvFile = new File(pathDir + File.separator + fileName);

                fis = new FileInputStream(csvFile);
                isr = new InputStreamReader(fis);
                buff = new BufferedReader(isr);
                buff.readLine();
                //String line = null; // gdyby String line wyzej nie zadzialal

                    while((line = buff.readLine()) != null){
                        Log.d("MyActivity", "Line: " + line);
                        String[] tokens = line.split(";"); // Oddziel znakiem ;

                        //odczytaj dane
                        SongsList sample = new SongsList();
                        sample.setComposer(tokens[0]);                  //
                        sample.setTitle(tokens[1]);                     // mozna zrobic ostrzezenie, ze rekord jest pusty w pliku csv
                        sample.setTrackBpm(Short.parseShort(tokens[2]));//
                        importedSongs.add(sample);
                        Log.d("My Activity", "Just created: " + sample);
                    }

                    fis.close();
            }

            catch(FileNotFoundException e){
                System.out.println("File doesn't exist!");
                e.printStackTrace();
                return false;
            }
            catch(IOException e){
                Log.wtf("My Activity", "Error reading data file on line" + line, e);
                e.printStackTrace();
                return false;
            }

            return true;
    }

    public void stopTimer(){
        tickTimer.cancel();
        isPlaying=false;
        pausePlayButton.setImageResource(R.drawable.ic_play);
    }

    public void startTimer(){
        tickTimer = new Timer("metronomeCounter", true);
        tickTone = new TimerTask(){
            @Override
            public void run(){
                tickSound.start();
            }
        };
        tickTimer.scheduleAtFixedRate(tickTone, 1000, 60000/Integer.valueOf(bpmEditTextInc.getText().toString()));
        isPlaying=true; //IllegalArgumentException:	if delay < 0, or delay + System.currentTimeMillis() < 0, or period <= 0 (period musi trwac przynajmniej 1 ms)
        pausePlayButton.setImageResource(R.drawable.ic_pause);
    }

    public void setNewBpmAndName(){
        bpmEditTextInc.setText(String.valueOf(importedSongs.get(currentSongNo).getTrackBpm())); // ustaw BPM
        songName.setText(String.valueOf(importedSongs.get(currentSongNo).getComposer()+" - "+importedSongs.get(currentSongNo).getTitle())); // ustaw wykonawce i nazwe utworu
    }

    public static boolean isBetween(int x, int lower, int upper) {
        return lower <= x && x <= upper;
    }

    public void setTempoMarking(){

        bpmAmount = Integer.valueOf(bpmEditTextInc.getText().toString()).intValue();

        if (isBetween(bpmAmount, 1, 29)) {
            tempoMarking.setText("Larghissimo");
        }
        else if (isBetween(bpmAmount, 30, 39)) {
            tempoMarking.setText("Grave");
        }
        else if (isBetween(bpmAmount, 40, 49)) {
            tempoMarking.setText("largo");
        }
        else if (isBetween(bpmAmount, 50, 51)) {
            tempoMarking.setText("lento");
        }
        else if (isBetween(bpmAmount, 52, 54)) {
            tempoMarking.setText("larghetto");
        }
        else if (isBetween(bpmAmount, 55, 59)) {
            tempoMarking.setText("adagio");
        }
        else if (isBetween(bpmAmount, 60, 70)) {
            tempoMarking.setText("andante");
        }
        else if (isBetween(bpmAmount, 88, 92)) {
            tempoMarking.setText("moderato");
        }
        else if (bpmAmount == 96) {
            tempoMarking.setText("andantino");
        }
        else if (isBetween(bpmAmount, 104, 119)) {
            tempoMarking.setText("allegretto");
        }
        else if (isBetween(bpmAmount, 120, 138)) {
            tempoMarking.setText("allegro");
        }
        else if (isBetween(bpmAmount, 160, 167)) {
            tempoMarking.setText("vivo & vivace");
        }
        else if (isBetween(bpmAmount, 168, 175)) {
            tempoMarking.setText("presto");
        }
        else if (bpmAmount == 176) {
            tempoMarking.setText("presto vivacissimo");
        }
        else if (isBetween(bpmAmount, 177, 199)) {
            tempoMarking.setText("presto");
        }
        else if (bpmAmount>=200) {
            tempoMarking.setText("prestissimo");
        }
        else if (bpmAmount<=0){
            tempoMarking.setText("BPM cannot be below 1");
        }
        else{
            tempoMarking.setText("");
        }


    }

    private void requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        }
        else {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage permission granted. Please press the import button again", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "You must grant storage permission to import data", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    public void onClick(View view) {


        switch (view.getId()) {

            case R.id.incrementButton:

                bpmAmount = Integer.valueOf(bpmEditTextInc.getText().toString()).intValue();
                bpmEditTextInc.setText(String.valueOf(bpmAmount+1));
                Toast.makeText(MainActivity.this, String.valueOf(bpmAmount+1), Toast.LENGTH_SHORT).show();
                if(isPlaying) {
                    stopTimer(); // jesli metronom gra to go wylacz, zwieksz bpm i wlacz
                    startTimer();
                }
                else{
                    stopTimer(); // jezeli nie gra to zatrzymaj i zwieksz bpm ale nie wlaczaj
                }
                System.out.println(bpmAmount);
                setTempoMarking();
                break;

            case R.id.decrementButton:

                bpmAmount = Integer.valueOf(bpmEditTextInc.getText().toString()).intValue();
                bpmEditTextInc.setText(String.valueOf(bpmAmount-1));
                Toast.makeText(MainActivity.this, String.valueOf(bpmAmount-1), Toast.LENGTH_SHORT).show();
                if(isPlaying) {
                    stopTimer();
                    startTimer();
                }
                else{
                    stopTimer();
                }
                setTempoMarking();
                break;

            case R.id.pausePlayButton:
                Animation blinkAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.blink);
                tempoMarking.startAnimation(blinkAnimation);

                if(isPlaying){
                    this.stopTimer();
                }
                else{
                    this.startTimer();
                }
                Toast.makeText(MainActivity.this, "pausePlayButton onClick test", Toast.LENGTH_SHORT).show();
                break;

            case R.id.tapButton:
                // ustawic bpm na podstawie tylko 2 klikniec - czas miedzy pierwszym a drugim klinieciem to bpm, po drugim kliknieciu resetuje sie funkcja i przycisk jest gotowy na ponowne wystukanie tempa
                Toast.makeText(MainActivity.this, "tapButton onClick test", Toast.LENGTH_SHORT).show();

                if(firstTap){ // pierwsze inicjujace klikniecie
                    stopTimer();
                    System.out.println("first tap triggered");
                    tapStopwatch = Stopwatch.createUnstarted();
                    tapStopwatch.start();
                    firstTap = false;

                }
                else{
                    stopTimer();
                    tapCounter++;
                    System.out.println("tapCounter++ " + tapCounter);
                    tapStopwatch.stop();
                    System.out.println("time: " + tapStopwatch); // formatted string like "12.3 ms"
                    summedTapsTime =+ tapStopwatch.elapsed(MILLISECONDS);
                    averageTapsTime = summedTapsTime/tapCounter;
                    bpmEditTextInc.setText(String.valueOf((60000/averageTapsTime))); //wylicz srednia w ms
                    tapStopwatch.start();
                    startTimer();
                }

                break;

            case R.id.recordButton:

                Toast.makeText(MainActivity.this, "recordButton onClick test", Toast.LENGTH_SHORT).show();
                break;

            case R.id.importButton: //pierwsze klikniecie importuje baze i wlacza 3 przyciski do sterowania, kolejne klikniecie zwija przyciski

                Toast.makeText(MainActivity.this, "importButton onClick test", Toast.LENGTH_SHORT).show();
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

                    if (isExternalStorageReadable()) {

                        if (importAndDataRead()) {

                            if (!importFlag) {
                                Toast.makeText(MainActivity.this, "Database imported successfully! Tap again to hide imported songs", Toast.LENGTH_SHORT).show();
                                this.stopTimer();
                                importAndDataRead();  // funkcja wczytujaca piosenki z tempem z pliku csv
                                previousSongButton.setEnabled(true);
                                nextSongButton.setEnabled(true);
                                songName.setEnabled(true);
                                importFlag = true;
                                setNewBpmAndName();
                                setTempoMarking();
                            } else {
                                previousSongButton.setEnabled(false);
                                nextSongButton.setEnabled(false);
                                songName.setEnabled(false);
                                importFlag = false;
                                this.stopTimer();
                            }
                        }
                        else {
                            Toast.makeText(MainActivity.this, "Cannot import data. File doesn't exist or it's wrong named or it's wrong formatted", Toast.LENGTH_SHORT).show();
                        }

                    }
                    else{
                        Toast.makeText(MainActivity.this, "Cannot read from external storage", Toast.LENGTH_SHORT).show();
                    }
                }
                else {
                    requestStoragePermission();
                }


                break;

            case R.id.previousSongButton:
                Toast.makeText(MainActivity.this, "previousSongButton onClick test", Toast.LENGTH_SHORT).show();

                this.stopTimer();

                if(currentSongNo == 0){
                    currentSongNo = (importedSongs.toArray().length-1);
                }
                else{
                    currentSongNo--;
                }

                setNewBpmAndName();

                bpmAmount = Integer.valueOf(bpmEditTextInc.getText().toString()).intValue();
                setTempoMarking();

                break;

            case R.id.nextSongButton:
                Toast.makeText(MainActivity.this, "nextSongButton onClick test", Toast.LENGTH_SHORT).show();

                this.stopTimer();

                if(currentSongNo == (importedSongs.toArray().length-1)){
                    currentSongNo = 0;
                }
                else{
                    currentSongNo++;
                }

                setNewBpmAndName();

                bpmAmount = Integer.valueOf(bpmEditTextInc.getText().toString()).intValue();
                setTempoMarking();

                break;

            default:
                break;
        }
    }


}