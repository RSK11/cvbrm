package edu.calpoly.cvbrm;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Path;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static java.lang.Math.min;

// The configuration screen for generating paths from images
public class ConfigActivity extends AppCompatActivity {

    // The given image
    private Bitmap originalImage;
    // The threshold is a Canny parameter
    private TextView lowThreshText;
    private SeekBar lowThreshBar;
    private int lowVal = 100;
    // The blur prior to running the Canny algorithm
    private double blurVal = 100;
    private TextView blurText;
    // The generated contours
    private List<MatOfPoint> contourList;
    // The buttons corresponding to the longest found contours
    private RadioGroup colorButtons;

    // Setup the activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        // Setup the threshold bar
        lowThreshText = (TextView)findViewById(R.id.thresh_num);
        lowThreshBar = (SeekBar)findViewById(R.id.thresh_val);
        lowThreshBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                lowVal = 255 * i / 100;
                lowThreshText.setText("" + lowVal);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            // When the threshold bar is moved, find contours
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                detectEdges();
            }
        });

        // Setup the blur bar
        blurText = (TextView)findViewById(R.id.blur_num);
        SeekBar blurBar = (SeekBar)findViewById(R.id.blur_val);
        blurBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                blurVal = 255 * i / 100;
                blurText.setText(blurVal + "");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            // When the blur bar is moved, find contours
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                detectEdges();
            }
        });

        // Assign the buttons to colors
        final int blueID = R.id.blue_line;
        final int purpID = R.id.purp_line;
        final int magID = R.id.mag_line;
        final int rubyID = R.id.ruby_line;
        final int redID = R.id.red_line;

        // Setup the distance field
        final EditText distance = (EditText)findViewById(R.id.distance);

        // Setup the submit button
        ((Button)findViewById(R.id.next_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int index = 0;
                
                // Get the line the user selected
                switch (colorButtons.getCheckedRadioButtonId()) {
                    case blueID:
                        index = 0; break;
                    case purpID:
                        index = 1; break;
                    case magID:
                        index = 2; break;
                    case rubyID:
                        index = 3; break;
                    case redID:
                        index = 4; break;
                    default:
                        index = 0; break;
                }

                // Start the point converter and open the map
                PointConverter.initialize(contourList.get(index).toArray(),
                        Double.valueOf(distance.getText().toString()),originalImage);
                Intent pathIntent = new Intent(ConfigActivity.this, MapsActivity.class);
                startActivity(pathIntent);
            }
        });

        // Setup the color buttons
        colorButtons = (RadioGroup)findViewById(R.id.color_buttons);
        colorButtons.check(R.id.blue_line);
    }


    // Find contours within the given image using the Canny algorithm
    private void detectEdges() {
        if (originalImage != null) {
            // Setup image data structures
            Mat rgba = new Mat();
            Mat heirarchy = new Mat();

            // Setup path data structures
            List<MatOfPoint> contours = new ArrayList<>();
            List<MatOfPoint> largest = new ArrayList<>();

            // Convert the loaded image to an OpenCV compatible format
            Utils.bitmapToMat(originalImage, rgba);

            // Create two copies
            Mat edges = new Mat(rgba.size(), CvType.CV_8UC1);
            Mat blur = new Mat(rgba.size(), CvType.CV_8UC1);

            // Convert the image to greyscale
            Imgproc.cvtColor(rgba, edges, Imgproc.COLOR_RGB2GRAY, 4);
            // Blur the image with a Bilateral Filter
            Imgproc.bilateralFilter(edges, blur, 5, blurVal, blurVal);
            // Use the Canny algorithm to detect edges in the image
            Imgproc.Canny(blur, edges, lowVal, 3*lowVal, 3, false);

            // Generate contours from the found edge image
            Imgproc.findContours(edges, contours, heirarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            // Sort the contours by size
            Collections.sort(contours, new Comparator<MatOfPoint>() {
                @Override
                public int compare(MatOfPoint matOfPoint, MatOfPoint t1) {
                    int sizeOne = matOfPoint.toList().size();
                    int sizeTwo = t1.toList().size();
                    int res = 1;
                    if (sizeOne == sizeTwo)
                    {
                        res = 0;
                    }
                    else if (sizeOne > sizeTwo) {
                        res = -1;
                    }
                    return res;
                }
            });
            contourList = contours;

            // Color and draw the five longest contours
            Imgproc.cvtColor(edges, blur, Imgproc.COLOR_GRAY2RGB);
            for (int ind = 0; ind < min(contours.size(), 5); ind++) {
                Imgproc.drawContours(blur, contours, ind, new Scalar(51.0 * (ind + 1), 0.0, 255.0 - 51.0 * (ind + 1)), 2);
            }

            // Display the found contours
            Bitmap edgeImage = Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(blur, edgeImage);
            ((ImageView) findViewById(R.id.edge_img)).setImageBitmap(edgeImage);
        }
    }

    // Setup the toolbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    // React to toolbar buttons
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Perform an action based on the button pressed
        switch (item.getItemId()) {
            case R.id.import_button:
                // Let the user load a photo
                Intent photoPicker = new Intent(Intent.ACTION_GET_CONTENT);
                photoPicker.setType("image/*");
                startActivityForResult(photoPicker, 11);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Handle the photo the user selected
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == 11) {
            try {
                // Load the selected image and find contours
                final Uri imageUri = data.getData();
                final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                originalImage = BitmapFactory.decodeStream(imageStream);
                ((ImageView)findViewById(R.id.edge_img)).setImageBitmap(originalImage);
                detectEdges();
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
