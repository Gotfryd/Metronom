package pl.pwr.metronom;

import androidx.appcompat.app.AppCompatActivity;

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

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    boolean isPlaying = false;
    int bpmAmount;

    MediaPlayer tickSound;;
    Timer tickTimer;
    TimerTask tickTone;

    EditText bpmEditTextInc;

    Button incrementButton;
    Button decrementButton;
    Button pausePlayButton;
    Button recordButton;
    Button tapButton;
    Button importButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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


        tickSound = MediaPlayer.create(this, R.raw.tickeffect);
        tickTimer = new Timer("metronomeCounter", true);


        tickTone = new TimerTask(){
            @Override
            public void run(){
               tickSound.start();
            }
        };

        bpmEditTextInc.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_DONE){
                    stopTimer();
                    startTimer();
                }
                return false;
            }
        });

    }



    public void stopTimer(){
        tickTimer.cancel();
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
    }

    @Override
    public void onClick(View view) {


        switch (view.getId()) {

            case R.id.incrementButton:

                bpmAmount = Integer.valueOf(bpmEditTextInc.getText().toString()).intValue();
                bpmEditTextInc.setText(String.valueOf(bpmAmount+1));
                //Toast.makeText(MainActivity.this, String.valueOf(bpmAmount+1), Toast.LENGTH_SHORT).show();
                stopTimer();
                startTimer();
                break;

            case R.id.decrementButton:

                bpmAmount = Integer.valueOf(bpmEditTextInc.getText().toString()).intValue();
                bpmEditTextInc.setText(String.valueOf(bpmAmount-1));
                //Toast.makeText(MainActivity.this, String.valueOf(bpmAmount-1), Toast.LENGTH_SHORT).show();
                stopTimer();
                startTimer();
                break;

            case R.id.pausePlayButton:
                if(isPlaying){
                    this.stopTimer();
                    isPlaying=false;
                }
                else{
                    this.startTimer();
                    isPlaying=true;
                }
                Toast.makeText(MainActivity.this, "pausePlayButton onClick test", Toast.LENGTH_SHORT).show();
                break;

            case R.id.tapButton:

                Toast.makeText(MainActivity.this, "tapButton onClick test", Toast.LENGTH_SHORT).show();
                break;

            case R.id.recordButton:

                Toast.makeText(MainActivity.this, "recordButton onClick test", Toast.LENGTH_SHORT).show();
                break;

            case R.id.importButton:

                Toast.makeText(MainActivity.this, "importButton onClick test", Toast.LENGTH_SHORT).show();
                break;

            default:
                break;
        }
    }
}