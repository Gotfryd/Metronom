package pl.pwr.metronom;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Stopwatch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
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
    boolean isRecording = false;
    boolean firstTap = true;
    boolean isAnimationEnabled = true;
    boolean hasCameraFlash = false;
    boolean isFlashOn = true;
    boolean areVibrationsOn = true;

    String fileName = "database.csv";
    String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
    String dataBaseContent = "Wykonawca;Tytuł;Tempo (BPM)\n" + "Metallica;The Unforgiven;69\n" + "Red Hot Chili Peppers;Under the Bridge;84";
    String line;
    String audioFileName = "AudioSample";
    String audioFileExtension = ".mp3";
    String effectName;
    String languageCode;

    File csvFile;

    FileInputStream fis;
    InputStreamReader isr;
    BufferedReader buff;

    int bpmAmount;
    int currentSongNo = 0;
    int tapCounter = 0;
    int holdDelay = 80;
    int minBpm = 1; // 1 poniewaz jezeli Timer dostanie wartosc mniejsza niz 1 ms to crash aplikacji
    int maxBpm = 300; // 300 poniewaz jest to juz bardzo wysoka wartosc i jest to prog w ktorym zaczyna sie gatunek muzyki Speedcore czyli ekstremalnie szybkiej. BPM powyzej 200 sa juz rzadko uzywane
    int vibrationTime = 100;

    long summedTapsTime;
    long averageTapsTime;

    short currentTact;

    final double referenceAmplitude = 0.0001;

    Intent settingsIntent;

    ConstraintLayout myConstraintLayout;

    Animation blinkAnimation;

    Handler handler = new Handler();
    Runnable runnable;

    MediaPlayer tickSound;

    MediaRecorder mediaRecorder;

    Timer tickTimer;
    TimerTask tickTone;

    CameraManager cameraManager;

    Vibrator vibratorObj;

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
                                if (resultCode == RESULT_OK) {
                                    recreate();
                                } else {
                                    recreate();
                                }
                            }
                        });
        setTickEffect();
        setAnimationState();
        setVibrationState();
        setFlashlightState();
        tickTimer = new Timer("metronomeCounter", true);

        tickTone = new TimerTask() {
            @Override
            public void run() {
                tickSound.start();
            }
        };

        bpmEditTextInc.setOnEditorActionListener(new TextView.OnEditorActionListener() { // akcja wykonujaca sie po kliknieciu przycisku "Enter" z klawiatury
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                bpmAmount = Integer.valueOf(bpmEditTextInc.getText().toString()).intValue();
                setTempoMarking();
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    stopTimer();
                    if (!isPlaying) {
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
                //bpmEditTextInc.setText(String.valueOf((90)));
                setNewBpm(90);
                setTempoMarking();
                stopTimer();
                Toast.makeText(MainActivity.this, R.string.tapReset, Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        importButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                generateCsvFile();
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
                        //bpmEditTextInc.setText(String.valueOf(bpmAmount+1));
                        incOrDecBpm(1);
                        if (isPlaying) {
                            stopTimer(); // jesli metronom gra to go wylacz, zwieksz bpm i wlacz
                            startTimer();
                        } else {
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
                        //bpmEditTextInc.setText(String.valueOf(bpmAmount-1));
                        incOrDecBpm(-1);
                        if (isPlaying) {
                            stopTimer(); // jesli metronom gra to go wylacz, zwieksz bpm i wlacz
                            startTimer();
                        } else {
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
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_lock_power_off)
                .setTitle(R.string.exitApplicationTitle)
                .setMessage(R.string.exitApplicationMessage)
                .setPositiveButton(R.string.exitApplicationYes, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                        System.exit(0);
                    }

                })
                .setNegativeButton(R.string.exitApplicationNo, null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_item, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_settings:
                stopTimer();
                settingsIntent = new Intent(this, SettingsActivity.class);
                // startActivityForResult(settingsIntent, SETTINGS_ACTIVITY); // tak sie robi po staremu
                settingsActivityResultLauncher.launch(settingsIntent);
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    public void setAppTheme() {
        SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        if (SP.getBoolean("night_mode_switch", false)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void setTickEffect() {
        SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        effectName = SP.getString("sound_effects_list", "soundone");

        switch (effectName) {
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

        if (SP.getBoolean("blink_animation_switch", true)) {
            isAnimationEnabled = true;
        } else {
            isAnimationEnabled = false;
        }
    }

    private void setVibrationState() {
        SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        if (SP.getBoolean("vibrations_switch", true)) {
            areVibrationsOn = true;
        } else {
            areVibrationsOn = false;
        }
    }

    private void setFlashlightState() {
        SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        if (SP.getBoolean("flashlight_switch", true)) {
            isFlashOn = true;
        } else {
            isFlashOn = false;
        }
    }


    private void setLanguage() {

        SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        languageCode = SP.getString("language_list", "en");

        String languageToLoad = languageCode; // tutaj podaje skrot jezyka (2 litery)
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
        } else {
              Toast.makeText(MainActivity.this, (R.string.storageNotRead), Toast.LENGTH_SHORT).show();
            return false;
        }
    }


    public boolean importAndDataRead() {
        line = "";

        try {
            csvFile = new File(filePath, fileName);
            fis = new FileInputStream(csvFile);
            isr = new InputStreamReader(fis);
            buff = new BufferedReader(isr);
            buff.readLine();
            //String line = null; // gdyby String line wyzej nie zadzialal

            while ((line = buff.readLine()) != null) {
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
        } catch (FileNotFoundException e) {
             System.out.println("File not found!");
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            Log.wtf("My Activity", "Error reading data file on line" + line, e);
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private void generateCsvFile() {
        csvFile = new File(filePath, fileName);
        if(csvFile.exists()){
            Toast.makeText(MainActivity.this, (R.string.databaseExists), Toast.LENGTH_SHORT).show();
        }
        else{
            csvFile = new File(filePath, fileName);
            try {
                FileWriter writer = new FileWriter(csvFile);
                writer.append(dataBaseContent);
                writer.flush();
                writer.close();
                Toast.makeText(MainActivity.this, (R.string.databaseGenerated), Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void stopTimer() {
        tickTimer.cancel();
        isPlaying = false;
        pausePlayButton.setImageResource(R.drawable.ic_play);

        currentTact = 0;
        for (int i = 0; i < beatDots.length; i++) {
            beatDots[i].clearColorFilter();
        }
    }

    public void startTimer() {

        if (isBpmInRange(Integer.valueOf(bpmEditTextInc.getText().toString()))) {

            currentTact = 0;
            if (isAnimationEnabled) {
                blinkAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.blink);
                blinkAnimation.setDuration(60000 / Integer.valueOf(bpmEditTextInc.getText().toString()));
            }

            tickTimer = new Timer("metronomeCounter", true);

            tickTone = new TimerTask() {
                @Override
                public void run() {
                    tactController();
                    tickSound.start();
                    if(areVibrationsOn) {
                        vibrate();
                    }
                    if (isAnimationEnabled) {
                        myConstraintLayout.startAnimation(blinkAnimation);
                    }
                    if(isFlashOn){
                        blinkFlashLight();
                    }
                }
            };

            tickTimer.scheduleAtFixedRate(tickTone, 1000, 60000 / Integer.valueOf(bpmEditTextInc.getText().toString()));
            isPlaying = true; //IllegalArgumentException:	if delay < 0, or delay + System.currentTimeMillis() < 0, or period <= 0 (period musi trwac przynajmniej 1 ms)
            pausePlayButton.setImageResource(R.drawable.ic_pause);

        } else {
            Toast.makeText(MainActivity.this, (R.string.exceededBpmRange), Toast.LENGTH_SHORT).show();
            return;
        }

    }

    private void tactController() {
        switch (currentTact) {
            case 0:
                setTactDotsColors(currentTact, beatDots.length - 1);
                currentTact++;
                break;
            case 1:
            case 2:
                setTactDotsColors(currentTact, currentTact - 1);
                currentTact++;
                break;
            case 3:
                setTactDotsColors(currentTact, currentTact - 1);
                currentTact = 0;
                break;
            default:
                System.out.println("Tact error");
                break;
        }

    }

    private void setTactDotsColors(int dotToColor, int dotToClear) {
        beatDots[dotToColor].setColorFilter(Color.argb(255, 0, 255, 0));
        beatDots[dotToClear].clearColorFilter();
    }


    private void setNewBpm(int newBpm) {
        if (isBpmInRange(newBpm)) {
            bpmEditTextInc.setText(String.valueOf(newBpm));
        } else {
            Toast.makeText(MainActivity.this, (R.string.exceededBpmRange), Toast.LENGTH_SHORT).show();
        }
    }

    private void incOrDecBpm(int newBpm) {
        if (isBpmInRange(bpmAmount + newBpm)) {
            bpmEditTextInc.setText(String.valueOf(bpmAmount + newBpm));
        } else {
            Toast.makeText(MainActivity.this, (R.string.exceededBpmRange), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isBpmInRange(int newBpm) {
        return newBpm >= minBpm && newBpm <= maxBpm;
    }


    public void setNewBpmAndNameFromBase() {
        //bpmEditTextInc.setText(String.valueOf(importedSongs.get(currentSongNo).getTrackBpm()));
        setNewBpm(importedSongs.get(currentSongNo).getTrackBpm()); // ustaw BPM
        songName.setText(String.valueOf(importedSongs.get(currentSongNo).getComposer() + " - " + importedSongs.get(currentSongNo).getTitle())); // ustaw wykonawce i nazwe utworu
    }

    public static boolean isBetween(int x, int lower, int upper) {
        return lower <= x && x <= upper;
    }


    public void setTempoMarking() {

        bpmAmount = Integer.valueOf(bpmEditTextInc.getText().toString()).intValue();

        if (isBetween(bpmAmount, 1, 29)) {
            tempoMarking.setText("Larghissimo");
        } else if (isBetween(bpmAmount, 30, 39)) {
            tempoMarking.setText("Grave");
        } else if (isBetween(bpmAmount, 40, 49)) {
            tempoMarking.setText("largo");
        } else if (isBetween(bpmAmount, 50, 51)) {
            tempoMarking.setText("lento");
        } else if (isBetween(bpmAmount, 52, 54)) {
            tempoMarking.setText("larghetto");
        } else if (isBetween(bpmAmount, 55, 59)) {
            tempoMarking.setText("adagio");
        } else if (isBetween(bpmAmount, 60, 70)) {
            tempoMarking.setText("andante");
        } else if (isBetween(bpmAmount, 88, 92)) {
            tempoMarking.setText("moderato");
        } else if (bpmAmount == 96) {
            tempoMarking.setText("andantino");
        } else if (isBetween(bpmAmount, 104, 119)) {
            tempoMarking.setText("allegretto");
        } else if (isBetween(bpmAmount, 120, 138)) {
            tempoMarking.setText("allegro");
        } else if (isBetween(bpmAmount, 160, 167)) {
            tempoMarking.setText("vivo & vivace");
        } else if (isBetween(bpmAmount, 168, 175)) {
            tempoMarking.setText("presto");
        } else if (bpmAmount == 176) {
            tempoMarking.setText("presto vivacissimo");
        } else if (isBetween(bpmAmount, 177, 199)) {
            tempoMarking.setText("presto");
        } else if (isBetween(bpmAmount, 200, 300)) {
            tempoMarking.setText("prestissimo");
        } else {
            tempoMarking.setText("");
        }


    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private void requestStoragePermission() {
        Toast.makeText(MainActivity.this, (R.string.allowPermission), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    private final int MY_PERMISSIONS_RECORD_AUDIO = 1;

    private void requestAudioPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            //When permission is not granted by user, show them message why this permission is needed.
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                Toast.makeText(this, (R.string.permToRecord), Toast.LENGTH_LONG).show();
                //Give user option to still opt-in the permissions
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS_RECORD_AUDIO);
           }
            else {
                // Show user dialog to grant permission to record audio
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS_RECORD_AUDIO);
            }
        }
        //If permission is granted, then go ahead recording audio
        else if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            //Go ahead with recording audio now

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(getRecordingFilePath());
            startRecording();

            final double maxAmplitude = mediaRecorder.getMaxAmplitude();
            final double amplitude = 20 * Math.log10(maxAmplitude / referenceAmplitude);
            System.out.println("amplitude equals: " + amplitude);
            System.out.println("media recorder started");
        }
            System.out.println("code executed");
        }

    private void blinkFlashLight(){
        hasCameraFlash = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

        if(hasCameraFlash) {
            cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            try {
                assert cameraManager != null;
                String cameraId = cameraManager.getCameraIdList()[0];
                cameraManager.setTorchMode(cameraId, true);
                cameraManager.setTorchMode(cameraId, false);
            } catch (CameraAccessException e) {
                Log.e("Camera Problem", "Cannot turn off camera flashlight");
            }
        }
        else{
            Toast.makeText(MainActivity.this, (R.string.noFlashOnDevice), Toast.LENGTH_SHORT).show();
        }
    }

    private void vibrate(){
        vibratorObj = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        vibratorObj.vibrate(vibrationTime);
    }

    private void startRecording() {
        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaRecorder.start();
        recordButton.setColorFilter(Color.argb(255, 0, 255, 0));
        isRecording = true;
    }

    private void stopRecording() {
        mediaRecorder.stop();
        mediaRecorder.release();
        mediaRecorder = null;
        recordButton.clearColorFilter();
        isRecording = false;
    }

    private String getRecordingFilePath() {
        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
        File music = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        File file = new File(music, audioFileName + audioFileExtension);
        return file.getPath();
    }

    //Handling callback
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_RECORD_AUDIO: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, (R.string.audioPermDenied), Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }


            @RequiresApi(api = Build.VERSION_CODES.R)
            @Override
            public void onClick (View view){

                switch (view.getId()) {

                    case R.id.incrementButton:
                        bpmAmount = Integer.valueOf(bpmEditTextInc.getText().toString()).intValue();
                        //bpmEditTextInc.setText(String.valueOf(bpmAmount+1));
                        incOrDecBpm(1);
                        if (isPlaying) {
                            stopTimer(); // jesli metronom gra to go wylacz, zwieksz bpm i wlacz
                            startTimer();
                        } else {
                            stopTimer(); // jezeli nie gra to zatrzymaj i zwieksz bpm ale nie wlaczaj
                        }
                        setTempoMarking();
                        break;

                    case R.id.decrementButton:

                        bpmAmount = Integer.valueOf(bpmEditTextInc.getText().toString()).intValue();
                        //bpmEditTextInc.setText(String.valueOf(bpmAmount-1));
                        incOrDecBpm(-1);
                        if (isPlaying) {
                            stopTimer();
                            startTimer();
                        } else {
                            stopTimer();
                        }
                        setTempoMarking();
                        break;

                    case R.id.pausePlayButton:

                        if (isPlaying) {
                            this.stopTimer();
                        } else {
                            this.startTimer();
                        }
                        break;

                    case R.id.tapButton:
                        // ustawic bpm na podstawie tylko 2 klikniec - czas miedzy pierwszym a drugim klinieciem to bpm, po drugim kliknieciu resetuje sie funkcja i przycisk jest gotowy na ponowne wystukanie tempa
                        if (firstTap) { // pierwsze inicjujace klikniecie
                            Toast.makeText(MainActivity.this, (R.string.holdToReset), Toast.LENGTH_SHORT).show();
                            stopTimer();
                            tapStopwatch = Stopwatch.createUnstarted();
                            tapStopwatch.start();
                            setTempoMarking();
                            firstTap = false;

                        } else {
                            stopTimer();
                            tapCounter++;
                            System.out.println("tapCounter++ " + tapCounter);
                            tapStopwatch.stop();
                            System.out.println("time: " + tapStopwatch); // formatted string like "12.3 ms"
                            summedTapsTime = +tapStopwatch.elapsed(MILLISECONDS);
                            averageTapsTime = summedTapsTime / tapCounter;
                            //bpmEditTextInc.setText(String.valueOf((60000/averageTapsTime))); //wylicz srednia w bpm
                            setNewBpm((int) (60000 / averageTapsTime));
                            setTempoMarking();
                            tapStopwatch.start();
                        }

                        break;

                    case R.id.recordButton:

                        if (!isRecording) {
                            requestAudioPermissions(); // rowniez wywolywana jest metoda startRecording();
                        }
                        else {
                            stopRecording();
                        }

                        break;

                    case R.id.importButton: //pierwsze klikniecie importuje baze i wlacza 3 przyciski do sterowania, kolejne klikniecie zwija przyciski

                        //   if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        if (Environment.isExternalStorageManager()) {
                            if (isExternalStorageReadable()) {

                                if (importAndDataRead()) {

                                    if (!isImportActive) {
                                        Toast.makeText(MainActivity.this, (R.string.baseImpSuccess), Toast.LENGTH_SHORT).show();
                                        this.stopTimer();
                                        importAndDataRead();  // funkcja wczytujaca piosenki z tempem z pliku csv
                                        previousSongButton.setEnabled(true);
                                        nextSongButton.setEnabled(true);
                                        songName.setEnabled(true);
                                        importButton.setColorFilter(Color.argb(255, 0, 255, 0));
                                        isImportActive = true;
                                        setNewBpmAndNameFromBase();
                                        setTempoMarking();
                                    } else {
                                        previousSongButton.setEnabled(false);
                                        nextSongButton.setEnabled(false);
                                        songName.setEnabled(false);
                                        importButton.clearColorFilter();
                                        isImportActive = false;
                                        this.stopTimer();
                                        Toast.makeText(MainActivity.this, R.string.baseHidden, Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(MainActivity.this, (R.string.cantImpData), Toast.LENGTH_SHORT).show();
                                }

                            } else {
                                Toast.makeText(MainActivity.this, (R.string.cantReadStorage), Toast.LENGTH_SHORT).show();
                            }
                            //  }
                        } else {
                            requestStoragePermission();
                        }


                        break;

                    case R.id.previousSongButton:

                        this.stopTimer();

                        if (currentSongNo == 0) {
                            currentSongNo = (importedSongs.toArray().length - 1);
                        } else {
                            currentSongNo--;
                        }

                        setNewBpmAndNameFromBase();

                        bpmAmount = Integer.valueOf(bpmEditTextInc.getText().toString()).intValue();
                        setTempoMarking();

                        break;

                    case R.id.nextSongButton:

                        this.stopTimer();

                        if (currentSongNo == (importedSongs.toArray().length - 1)) {
                            currentSongNo = 0;
                        } else {
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