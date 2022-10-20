package pl.pwr.metronom;

import androidx.appcompat.app.AppCompatActivity;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Button incrementButton = (Button) findViewById(R.id.incrementButton);
        incrementButton.setOnClickListener(this);

        Button decrementButton = (Button) findViewById(R.id.decrementButton);
        decrementButton.setOnClickListener(this);

        Button pausePlayButton = (Button) findViewById(R.id.pausePlayButton);
        pausePlayButton.setOnClickListener(this);

        Button recordButton = (Button) findViewById(R.id.recordButton);
        recordButton.setOnClickListener(this);

        Button tapButton = (Button) findViewById(R.id.tapButton);
        tapButton.setOnClickListener(this);

        Button importButton = (Button) findViewById(R.id.importButton);
        importButton.setOnClickListener(this);

        boolean isPlaying = false;
        final MediaPlayer tickSound = MediaPlayer.create(this, R.raw.tickeffect);
        Timer tickTimer = new Timer("metronomeCounter", true);
        TimerTask tickTone = new TimerTask(){
            @Override
            public void run(){
                tickSound.start();
            }
        };
        if(isPlaying) {
            tickTimer.scheduleAtFixedRate(tickTone, 500, 500); //120 BPM. Executes every 500 ms. //zamiast suchego delaya pobrac aktualna wartosc z bpmInteger
        }

//        tickTone.cancel();
//        tickTone = new TimerTask(){
//            @Override
//            public void run(){
//                tickSound.start();
//            }
//        };
//        tickTimer.scheduleAtFixedRate(tickTone, 1000, 1000); //new tempo, 60 BPM. Executes every 1000 ms.

    }

    @Override
    public void onClick(View view) {


        switch (view.getId()) {

            case R.id.incrementButton:

                Toast.makeText(MainActivity.this, "incrementButton onClick test", Toast.LENGTH_SHORT).show();
                EditText bpmEditTextInc = (EditText) findViewById(R.id.bpmInteger);
                int bpmAmountInc = Integer.valueOf(bpmEditTextInc.getText().toString()).intValue();
                bpmEditTextInc.setText(String.valueOf(bpmAmountInc+1));
                Toast.makeText(MainActivity.this, String.valueOf(bpmAmountInc+1), Toast.LENGTH_SHORT).show();
                break;

            case R.id.decrementButton:

                Toast.makeText(MainActivity.this, "decrementButton onClick test", Toast.LENGTH_SHORT).show();
                EditText bpmEditTextDec = (EditText) findViewById(R.id.bpmInteger);
                int bpmAmountDec = Integer.valueOf(bpmEditTextDec.getText().toString()).intValue();
                bpmEditTextDec.setText(String.valueOf(bpmAmountDec-1));
                Toast.makeText(MainActivity.this, String.valueOf(bpmAmountDec-1), Toast.LENGTH_SHORT).show();
                break;

            case R.id.pausePlayButton:
                isPlaying = !isPlaying; // zmienia stan z pauzy na play i na odwrot 
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