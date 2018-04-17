package edu.calpoly.cvbrm;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

// The Activity holding the canvas
public class CanvasActivity extends AppCompatActivity {
    
    private CanvasView cView;
    private EditText dText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_canvas);

        // Find the canvas
        cView = (CanvasView)findViewById(R.id.canvas);

        // Setup the clear button
        Button clear = (Button)findViewById(R.id.clear_button);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cView.clear();
            }
        });

        // Setup the distance field
        dText = (EditText)findViewById(R.id.distance_field);

        // Setup the submit button
        Button submit = (Button)findViewById(R.id.submit_button);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the points from the canvas and open the map
                PointConverter.initialize(cView.getPoints(),
                        Double.valueOf(dText.getText().toString()), cView.getBitmap());
                Intent pathIntent = new Intent(CanvasActivity.this, MapsActivity.class);
                startActivity(pathIntent);
            }
        });
    }
}
