package edu.calpoly.cvbrm;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import org.opencv.core.Point;

import java.util.ArrayList;

// The actual canvas for drawing a single stroke path
public class CanvasView extends View {
    // The paint properties of the brush the user will draw with
    private Paint paint;
    // The points in the current stroke
    public static ArrayList<Point> points;
    // The current stroke
    public static Path path;

    // Setup the Canvas
    public CanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // Set the paint to be blue with a width of 10
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.BLUE);paint.setStrokeWidth(10);

        // Setup the data structures
        points = new ArrayList<>();
        path = new Path();
    }


    // Draw the canvas and the stroke, if one exists
    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.WHITE);
        canvas.drawPath(path, paint);
    }

    // Handle canvas input
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        // Get the touch location
        float x = e.getX();
        float y = e.getY();

        // If the user is putting their finger down
        if (e.getAction().equals(MotionEvent.ACTION_DOWN)) {
            // Start a new stroke
            path = new Path();
            points.clear();

            // Add the first point and request a redraw
            path.moveTo(x, y);
            points.add(new Point(x, y));
            invalidate();
        }
        // If the user moves their finger or lifts it
        else if (e.getAction().equals(MotionEvent.ACTION_MOVE) || e.getAction().equals(MotionEvent.ACTION_UP)) {
            // Add the point and request a redraw
            path.lineTo(x, y);
            points.add(new Point(x, y));
            invalidate();
        }
        return true;
    }

    // Clear what the user has drawn
    public void clear() {
        path = new Path();
        points.clear();
        invalidate();
    }

    // Convert the point list to an array
    public Point[] getPoints() {
        Point[] pts = new Point[1];
        pts = points.toArray(pts);
        return pts;
    }

    // Capture the image currently drawn on the canvas
    public Bitmap getBitmap() {
        clearFocus();
        setPressed(false);

        // Enable the drawing cache
        boolean willNotCache = willNotCacheDrawing();
        setWillNotCacheDrawing(false);

        // Set the background
        int color = getDrawingCacheBackgroundColor();
        setDrawingCacheBackgroundColor(0);

        // Clear the cache
        if (color != 0) {
            destroyDrawingCache();
        }

        // Store the current drawing
        buildDrawingCache();
        Bitmap cacheBitmap = getDrawingCache();

        if (cacheBitmap == null) {
            Log.d("CANVAS", "failed getViewBitmap(" + this + ")", new RuntimeException());
            return null;
        }

        // Get the bitmap from the cache
        Bitmap bitmap = Bitmap.createBitmap(cacheBitmap);

        // Restore the view and disable the cache
        destroyDrawingCache();
        setWillNotCacheDrawing(willNotCache);
        setDrawingCacheBackgroundColor(color);

        return bitmap;
    }
}
