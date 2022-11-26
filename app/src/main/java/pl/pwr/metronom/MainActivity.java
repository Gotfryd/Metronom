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
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.media.Image;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
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
    boolean isImportActive = false;
    boolean isRecordingActive = false;
    boolean firstTap = true;
    boolean isAnimationEnabled = true;

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
    int holdDelay = 80;
    long summedTapsTime;
    long averageTapsTime;
    short currentTact;

    String effectName;
    String languageCode;

    Intent settingsIntent;

    ConstraintLayout myConstraintLayout;

    Animation blinkAnimation;

    Handler handler = new Handler();
    Runnable runnable;

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

    ImageView[] beatDots = new ImageView[4];

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

//        tactOne = (ImageView) findViewById(R.id.tactOne);
//        tactTwo = (ImageView) findViewById(R.id.tactTwo);
//        tactThree = (ImageView) findViewById(R.id.tactThree);
//        tactFour = (ImageView) findViewById(R.id.tactFour);

        myConstraintLayout = (ConstraintLayout) findViewById(R.id.myConstraintLayout);
        myConstraintLayout.setOnClickListener(this);

        nextSongButton.setEnabled(false); // na starcie apki zablokuj przyciski next i previous bo z poziomu activity_main.xml nie dziala a tutaj da sie to zrobic
        previousSongButton.setEnabled(false); //

        beatDots[0] = (ImageView) findViewById(R.id.tactOne);
        beatDots[1] = (ImageView) findViewById(R.id.tactTwo);
        beatDots[2] = (ImageView) findViewById(R.id.tactThree);
        beatDots[3] = (ImageView) findViewById(R.id.tactFour);

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
        setAnimationState();

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
                bpmAmount = Integer.valueOf(bpmEditTextInc.getText().toString()).intValue();
                setTempoMarking();
                if(actionId == EditorInfo.IME_ACTION_DONE){
                    stopTimer();
                    if(!isPlaying) {
                        startTimer();
                    }
                }

                return false;
            }
        });

        tapButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) { // resetruje dane uzyskane przez przytrzymanie przycisku "Tap"
                summedTapsTime = 0;
                averageTapsTime = 0;
                tapCounter = 0;
                firstTap = true;
                bpmEditTextInc.setText(String.valueOf((90)));
                stopTimer();
                return true;
            }
        });

        incrementButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                runnable = new Runnable() {
                    @Override
                    public void run() {
                        if (!incrementButton.isPressed()) return;
                        bpmAmount = Integer.valueOf(bpmEditTextInc.getText().toString()).intValue();
                        bpmEditTextInc.setText(String.valueOf(bpmAmount+1));
                        if(isPlaying) {
                            stopTimer(); // jesli metronom gra to go wylacz, zwieksz bpm i wlacz
                            startTimer();
                        }
                        else{
                            stopTimer(); // jezeli nie gra to zatrzymaj i zwieksz bpm ale nie wlaczaj
                        }
                        setTempoMarking();
                        handler.postDelayed(runnable, holdDelay);
                    }
                };
                handler.postDelayed(runnable, holdDelay);
                return true;
            }
        });

        decrementButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                runnable = new Runnable() {
                    @Override
                    public void run() {
                        if (!decrementButton.isPressed()) return;
                        bpmAmount = Integer.valueOf(bpmEditTextInc.getText().toString()).intValue();
                        bpmEditTextInc.setText(String.valueOf(bpmAmount-1));
                        if(isPlaying) {
                            stopTimer(); // jesli metronom gra to go wylacz, zwieksz bpm i wlacz
                            startTimer();
                        }
                        else{
                            stopTimer(); // jezeli nie gra to zatrzymaj i zwieksz bpm ale nie wlaczaj
                        }
                        setTempoMarking();
                        handler.postDelayed(runnable, holdDelay);
                    }
                };
                handler.postDelayed(runnable, holdDelay);
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
        }
        else{
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void setTickEffect() {
        SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        effectName = SP.getString("sound_effects_list", "soundone");

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

    private void setAnimationState() {
        SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        if(SP.getBoolean("blink_animation_switch", true)){
            isAnimationEnabled = true;
        }
        else{
            isAnimationEnabled = false;
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
              //  baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();
              //  pathDir = baseDir + "/Download/";
                pathDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
                csvFile = new File(pathDir, fileName);

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

        currentTact = 0;
        for(int i=0;i<beatDots.length; i++) {
            beatDots[i].clearColorFilter();
        }
    }

    public void startTimer(){
        checkBpmLimit();
        currentTact = 0;

        if(isAnimationEnabled) {
            blinkAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.blink);
            blinkAnimation.setDuration(60000 / Integer.valueOf(bpmEditTextInc.getText().toString()));
        }

        tickTimer = new Timer("metronomeCounter", true);

        tickTone = new TimerTask(){
            @Override
            public void run(){
                    tactController();
                    tickSound.start();
                    if (isAnimationEnabled) {
                        myConstraintLayout.startAnimation(blinkAnimation);
                    }
            }
        };

        tickTimer.scheduleAtFixedRate(tickTone, 1000, 60000/Integer.valueOf(bpmEditTextInc.getText().toString()));
        isPlaying=true; //IllegalArgumentException:	if delay < 0, or delay + System.currentTimeMillis() < 0, or period <= 0 (period musi trwac przynajmniej 1 ms)
        pausePlayButton.setImageResource(R.drawable.ic_pause);
    }

    private void tactController() {

        switch(currentTact){
            case 0:
//                beatDots[currentTact].setColorFilter(Color.argb(255, 0, 255, 0));
//                beatDots[beatDots.length-1].clearColorFilter();
                setTactDotsColors(currentTact, beatDots.length-1);
                currentTact++;
                break;
            case 1:
            case 2:
//                beatDots[currentTact].setColorFilter(Color.argb(255, 0, 255, 0));
//                beatDots[currentTact-1].clearColorFilter();
                setTactDotsColors(currentTact, currentTact-1);
                currentTact++;
                break;
            case 3:
//                beatDots[currentTact].setColorFilter(Color.argb(255, 0, 255, 0));
//                beatDots[currentTact-1].clearColorFilter();
                setTactDotsColors(currentTact, currentTact-1);
                currentTact = 0;
                break;
            default:
                System.out.println("Tact error");
                break;
        }
    }

    private void setTactDotsColors(int dotToColor , int dotToClear){
        beatDots[dotToColor].setColorFilter(Color.argb(255, 0, 255, 0));
        beatDots[dotToClear].clearColorFilter();
    }



    public void checkBpmLimit() {
        if(bpmAmount < 1){
            System.out.println("BPM cannot be below 1"); // DO DOKONCZENIA
            //stop funkcje
        }
    }

    public void setNewBpmAndNameFromBase(){
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
                Toast.makeText(MainActivity.this, this.getString(R.string.storagePermGranted), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, this.getString(R.string.permissionInfo), Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    public void onClick(View view) {


        switch (view.getId()) {

            case R.id.incrementButton:

                bpmAmount = Integer.valueOf(bpmEditTextInc.getText().toString()).intValue();
                bpmEditTextInc.setText(String.valueOf(bpmAmount+1));
                if(isPlaying) {
                    stopTimer(); // jesli metronom gra to go wylacz, zwieksz bpm i wlacz
                    startTimer();
                }
                else{
                    stopTimer(); // jezeli nie gra to zatrzymaj i zwieksz bpm ale nie wlaczaj
                }
                setTempoMarking();
                break;

            case R.id.decrementButton:

                bpmAmount = Integer.valueOf(bpmEditTextInc.getText().toString()).intValue();
                bpmEditTextInc.setText(String.valueOf(bpmAmount-1));
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
                if(isPlaying){
                    this.stopTimer();
                }
                else{
                    this.startTimer();
                }
                break;

            case R.id.tapButton:
                // ustawic bpm na podstawie tylko 2 klikniec - czas miedzy pierwszym a drugim klinieciem to bpm, po drugim kliknieciu resetuje sie funkcja i przycisk jest gotowy na ponowne wystukanie tempa

                if(firstTap){ // pierwsze inicjujace klikniecie
                    Toast.makeText(MainActivity.this, this.getString(R.string.holdToReset), Toast.LENGTH_SHORT).show();
                    stopTimer();
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
                }

                break;

            case R.id.recordButton:
                System.out.println("Sciezka do tego: " + getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS));
                System.out.println("Sciezka do tego: " +Environment.getExternalStorageDirectory().getAbsolutePath());
                if(!isRecordingActive){
                    recordButton.setColorFilter(Color.argb(255, 0, 255, 0));
                    isRecordingActive = true;
                }
                else{
                    recordButton.clearColorFilter();
                    isRecordingActive = false;
                }

                break;

            case R.id.importButton: //pierwsze klikniecie importuje baze i wlacza 3 przyciski do sterowania, kolejne klikniecie zwija przyciski

                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

                    if (isExternalStorageReadable()) {

                        if (importAndDataRead()) {

                            if (!isImportActive) {
                                Toast.makeText(MainActivity.this, this.getString(R.string.baseImpSuccess), Toast.LENGTH_SHORT).show();
                                this.stopTimer();
                                importAndDataRead();  // funkcja wczytujaca piosenki z tempem z pliku csv
                                previousSongButton.setEnabled(true);
                                nextSongButton.setEnabled(true);
                                songName.setEnabled(true);
                                isImportActive = true;
                                setNewBpmAndNameFromBase();
                                setTempoMarking();
                            } else {
                                previousSongButton.setEnabled(false);
                                nextSongButton.setEnabled(false);
                                songName.setEnabled(false);
                                isImportActive = false;
                                this.stopTimer();
                            }
                        }
                        else {
                            Toast.makeText(MainActivity.this, this.getString(R.string.cantImpData), Toast.LENGTH_SHORT).show();
                        }

                    }
                    else{
                        Toast.makeText(MainActivity.this, this.getString(R.string.cantReadStorage), Toast.LENGTH_SHORT).show();
                    }
                }
                else {
                    requestStoragePermission();
                }


                break;

            case R.id.previousSongButton:

                this.stopTimer();

                if(currentSongNo == 0){
                    currentSongNo = (importedSongs.toArray().length-1);
                }
                else{
                    currentSongNo--;
                }

                setNewBpmAndNameFromBase();

                bpmAmount = Integer.valueOf(bpmEditTextInc.getText().toString()).intValue();
                setTempoMarking();

                break;

            case R.id.nextSongButton:

                this.stopTimer();

                if(currentSongNo == (importedSongs.toArray().length-1)){
                    currentSongNo = 0;
                }
                else{
                    currentSongNo++;
                }

                setNewBpmAndNameFromBase();

                bpmAmount = Integer.valueOf(bpmEditTextInc.getText().toString()).intValue();
                setTempoMarking();

                break;

            default:
                break;
        }
    }


}