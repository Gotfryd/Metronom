package pl.pwr.metronom;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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


    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.incrementButton:

                Toast.makeText(MainActivity.this, "incrementButton onClick test", Toast.LENGTH_SHORT).show();
                EditText bpmEditText = (EditText) findViewById(R.id.bpmInteger);
                int bpmAmount = Integer.valueOf(bpmEditText.getText().toString()).intValue();
                bpmEditText.setText(String.valueOf(bpmAmount+1));
                Toast.makeText(MainActivity.this, String.valueOf(bpmAmount+1), Toast.LENGTH_SHORT).show();
                break;

            case R.id.decrementButton:

                Toast.makeText(MainActivity.this, "decrementButton onClick test", Toast.LENGTH_SHORT).show();
                break;

            case R.id.pausePlayButton:

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