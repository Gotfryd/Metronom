package pl.pwr.metronom;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    boolean isPlaying = false;
    boolean importFlag = false;
    int bpmAmount;
    int currentSongNo = 0;
    String tempoName;

    MediaPlayer tickSound;;
    Timer tickTimer;
    TimerTask tickTone;

    TextView tempoMarking;
    TextView songName;

    EditText bpmEditTextInc;

    Button incrementButton;
    Button decrementButton;
    Button pausePlayButton;
    Button recordButton;
    Button tapButton;
    Button importButton;
    Button previousSongButton;
    Button nextSongButton;

    List<SongsList> importedSongs = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tempoMarking = (TextView) findViewById(R.id.tempoMarking);

        songName = (TextView) findViewById(R.id.songName);

        bpmEditTextInc = (EditText) findViewById(R.id.bpmInteger);

        incrementButton = (Button) findViewById(R.id.incrementButton);
        incrementButton.setOnClickListener(this);

        decrementButton = (Button) findViewById(R.id.decrementButton);
        decrementButton.setOnClickListener(this);

        pausePlayButton = (Button) findViewById(R.id.pausePlayButton);
        pausePlayButton.setOnClickListener(this);

        recordButton = (Button) findViewById(R.id.recordButton);
        recordButton.setOnClickListener(this);

        tapButton = (Button) findViewById(R.id.tapButton);
        tapButton.setOnClickListener(this);

        importButton = (Button) findViewById(R.id.importButton);
        importButton.setOnClickListener(this);

        previousSongButton = (Button) findViewById(R.id.previousSongButton);
        previousSongButton.setOnClickListener(this);

        nextSongButton = (Button) findViewById(R.id.nextSongButton);
        nextSongButton.setOnClickListener(this);

        bpmAmount = Integer.valueOf(bpmEditTextInc.getText().toString()).intValue(); // pierwsze i jednokrotne przypisanie wartosci do tej zmiennej zeby tempoMarking zadzialalo poprawnie
        setTempoMarking(); // jednokrotne wykonanie funkcji zeby ustawila sie nazwa tempa po wlaczeniu aplikacji (na samym starcie)

        tickSound = MediaPlayer.create(this, R.raw.tickeffect);
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

    }


    public void readSongsData() {
        InputStream istream = getResources().openRawResource(R.raw.songsdata);
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(istream, Charset.forName("UTF-8"))
        );

        String line = "";

        try {
            reader.readLine();    //pomin naglowek pliku csv (Wykonawca, Tytul, Tempo(BPM)

            while ((line = reader.readLine()) != null) {
                Log.d("MyActivity", "Line: " + line);
                String[] tokens = line.split(","); // Oddziel znakiem ,

                //odczytaj dane
                SongsList sample = new SongsList();
                sample.setComposer(tokens[0]);                  //
                sample.setTitle(tokens[1]);                     // mozna zrobic ostrzezenie, ze rekord jest pusty w pliku csv
                sample.setTrackBpm(Short.parseShort(tokens[2]));//
                importedSongs.add(sample);

                Log.d("My Activity", "Just created: " + sample);
            }
        } catch (IOException e) {
            Log.wtf("My Activity", "Error reading data file on line" + line, e);
            e.printStackTrace();
        }
    }


    public void stopTimer(){
        tickTimer.cancel();
        isPlaying=false;
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
        isPlaying=true;
    }

    public void setNewBpmAndName(){
        bpmEditTextInc.setText(String.valueOf(importedSongs.get(currentSongNo).getTrackBpm())); // ustaw BPM
        songName.setText(String.valueOf(importedSongs.get(currentSongNo).getComposer()+" - "+importedSongs.get(currentSongNo).getTitle())); // ustaw wykonawce i nazwe utworu
    }

    public static boolean isBetween(int x, int lower, int upper) {
        return lower <= x && x <= upper;
    }

    public void setTempoMarking(){
        if (isBetween(bpmAmount+1, 1, 29)) {
            tempoMarking.setText("Larghissimo");
        }
        else if (isBetween(bpmAmount+1, 30, 39)) {
            tempoMarking.setText("Grave");
        }
        else if (isBetween(bpmAmount+1, 40, 49)) {
            tempoMarking.setText("largo");
        }
        else if (isBetween(bpmAmount+1, 50, 51)) {
            tempoMarking.setText("lento");
        }
        else if (isBetween(bpmAmount+1, 52, 54)) {
            tempoMarking.setText("larghetto");
        }
        else if (isBetween(bpmAmount+1, 55, 59)) {
            tempoMarking.setText("adagio");
        }
        else if (isBetween(bpmAmount+1, 60, 70)) {
            tempoMarking.setText("andante");
        }
        else if (isBetween(bpmAmount+1, 88, 92)) {
            tempoMarking.setText("moderato");
        }
        else if (bpmAmount+1 == 96) {
            tempoMarking.setText("andantino");
        }
        else if (isBetween(bpmAmount+1, 104, 119)) {
            tempoMarking.setText("allegretto");
        }
        else if (isBetween(bpmAmount+1, 120, 138)) {
            tempoMarking.setText("allegro");
        }
        else if (isBetween(bpmAmount+1, 160, 167)) {
            tempoMarking.setText("vivo & vivace");
        }
        else if (isBetween(bpmAmount+1, 168, 175)) {
            tempoMarking.setText("presto");
        }
        else if (bpmAmount+1 == 176) {
            tempoMarking.setText("presto vivacissimo");
        }
        else if (isBetween(bpmAmount+1, 177, 199)) {
            tempoMarking.setText("presto");
        }
        else if (bpmAmount+1>=200) {
            tempoMarking.setText("prestissimo");
        }
        else if (bpmAmount+1<=0){
            tempoMarking.setText("BPM cannot be below 1");
        }
        else{
            tempoMarking.setText("");
        }


    }

    @Override
    public void onClick(View view) {


        switch (view.getId()) {

            case R.id.incrementButton:

                bpmAmount = Integer.valueOf(bpmEditTextInc.getText().toString()).intValue();
                bpmEditTextInc.setText(String.valueOf(bpmAmount+1));
                Toast.makeText(MainActivity.this, String.valueOf(bpmAmount+1), Toast.LENGTH_SHORT).show();
                stopTimer();
                if(isPlaying) {
                    startTimer();
                }
                System.out.println(bpmAmount);
                setTempoMarking();
                break;

            case R.id.decrementButton:

                bpmAmount = Integer.valueOf(bpmEditTextInc.getText().toString()).intValue();
                bpmEditTextInc.setText(String.valueOf(bpmAmount-1));
                Toast.makeText(MainActivity.this, String.valueOf(bpmAmount-1), Toast.LENGTH_SHORT).show();
                stopTimer();
                if(isPlaying) {
                    startTimer();
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
                Toast.makeText(MainActivity.this, "pausePlayButton onClick test", Toast.LENGTH_SHORT).show();
                break;

            case R.id.tapButton:
                // ustawic bpm na podstawie tylko 2 klikniec - czas miedzy pierwszym a drugim klinieciem to bpm, po drugim kliknieciu resetuje sie funkcja i przycisk jest gotowy na ponowne wystukanie tempa
                Toast.makeText(MainActivity.this, "tapButton onClick test", Toast.LENGTH_SHORT).show();
                break;

            case R.id.recordButton:

                Toast.makeText(MainActivity.this, "recordButton onClick test", Toast.LENGTH_SHORT).show();
                break;

            case R.id.importButton: //pierwsze klikniecie importuje baze i wlacza 3 przyciski do sterowania, kolejne klikniecie zwija przyciski
                Toast.makeText(MainActivity.this, "importButton onClick test", Toast.LENGTH_SHORT).show();
                if(!importFlag) {

                    this.stopTimer();
                    readSongsData(); // funkcja wczytujaca piosenki z tempem z pliku csv
                    previousSongButton.setEnabled(true);
                    nextSongButton.setEnabled(true);
                    songName.setEnabled(true);
                    songName.setTextColor(Color.BLACK);
                    importFlag = true;

                    setNewBpmAndName();

                    bpmAmount = Integer.valueOf(bpmEditTextInc.getText().toString()).intValue();
                    setTempoMarking();

                }
                else{

                    previousSongButton.setEnabled(false);
                    nextSongButton.setEnabled(false);
                    songName.setEnabled(false);
                    songName.setTextColor(Color.rgb(136,136,136));
                    importFlag = false;
                    this.stopTimer();
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