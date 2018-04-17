package edu.calpoly.cvbrm;

import android.*;
import android.content.Intent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;


// The main application screen
public class MainActivity extends AppCompatActivity {

    // The view containing previous routes
    private RecyclerView recyclerView;

    // Callback for loading OpenCV
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.d("OpenCV", "OpenCV loaded successfully.");
                } break;
                default:
                {
                    super.onManagerConnected(status);
                    Log.d("OpenCV", "Did NOT load.");
                } break;
            }
        }
    };

    // Setup the Activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Link the Config and Canvas Buttons
        ((Button)findViewById(R.id.config_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Move to the Configuration Screen
                startActivity(new Intent(MainActivity.this, ConfigActivity.class));
            }
        });
        ((Button)findViewById(R.id.canvas_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Move to the Canvas
                startActivity(new Intent(MainActivity.this, CanvasActivity.class));
            }
        });

        // Get Location permissions from the user
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    0);
        }

        // Setup the layout
        recyclerView = (RecyclerView) findViewById(R.id.main_recycler);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        recyclerView.setAdapter(new RouteAdapter());
    }

    // Load OpenCV any time the app becomes active and it has not already been loaded
    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
        }
    }

    // The Adapter for the List of previous routes
    public class RouteAdapter
            extends RecyclerView.Adapter<RouteHolder> {

        public ArrayList<Bitmap> images;
        public ArrayList<String> names;

        public RouteAdapter() {
            images = new ArrayList<>();
            names = new ArrayList<>();

            // Get the stored routes
            File rteDir = getDir("routes", MODE_PRIVATE);
            for (File fi : rteDir.listFiles()) {
                if (fi.getName().endsWith("_image")) {
                    try {
                        // Load the image for the stored route
                        InputStream imageStream = getContentResolver().openInputStream(Uri.fromFile(fi));
                        images.add(BitmapFactory.decodeStream(imageStream));
                        names.add(fi.getName().replace("_image", ""));
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }

        // Link each route so it will display on the map when clicked
        @Override
        public RouteHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_content, parent, false);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Open the map with the selected route loaded
                    Intent mapsint = new Intent(parent.getContext(), MapsActivity.class);
                    String rname = ((TextView)v.findViewById(R.id.list_name)).getText().toString();
                    mapsint.putExtra("routename", rname);
                    PointConverter.setImage(images.get(names.indexOf(rname)));
                    startActivity(mapsint);
                }
            });

            return new RouteHolder(view);
        }

        // Used for changing a list item when scrolling (recycling)
        @Override
        public void onBindViewHolder(final RouteHolder holder, int position) {
            holder.setImage(images.get(position));
            holder.setText(names.get(position));
        }

        @Override
        public int getItemCount() {
            return names.size();
        }
    }

    // A single list item in the Recycler View
    private class RouteHolder extends RecyclerView.ViewHolder {

        ImageView imageView;
        TextView textView;

        public RouteHolder(View itemView) {
            super(itemView);
            imageView = (ImageView)itemView.findViewById(R.id.list_image);
            textView = (TextView)itemView.findViewById(R.id.list_name);
        }

        public void setImage(Bitmap btmp) {
            imageView.setImageBitmap(btmp);
        }

        public void setText(String txt) {
            textView.setText(txt);
        }

        public String getText() {
            return textView.getText().toString();
        }
    }
}
