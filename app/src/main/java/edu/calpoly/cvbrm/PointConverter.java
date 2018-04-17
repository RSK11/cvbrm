package edu.calpoly.cvbrm;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.StreetViewPanoramaCamera;

import org.opencv.core.Point;
import java.lang.Math;
import java.util.ArrayList;
import java.util.Arrays;

import static java.lang.Math.atan2;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static java.lang.Math.toRadians;

// A Converter from a list of cartesian points to a list of latitude and longitude points
public class PointConverter {
    // The original points
    private static Point[] points;
    // The used image
    private static Bitmap image;
    // The shortened list of points
    private static ArrayList<Point> shortPoints;
    // The real world distance to cover
    private static double mapDistance;

    // Data structures for conversion
    private static double[] angles;
    private static double[] distances;
    private static int size;

    // The beginning location for the route
    public static LatLng startLocation;


    // Setup the points for conversion
    public static void initialize(Point[] pathPoints, double pathDistance, Bitmap img) {
        points = pathPoints;

        // Shorten the list due to our 100 point restriction
        shortenList();

        mapDistance = pathDistance;
        size = Math.min(points.length, 100);
        angles = new double[size];
        distances = new double[size];
        image = img;
    }

    // The Google API restricts us to 100 points, so optimize the points and shorten the list if needed
    private static void shortenList() {
        // The error threshold for the first shortening pass
        double threshold = 1.0;

        shortPoints = new ArrayList<Point>();
        shortPoints.ensureCapacity(points.length);
        shortPoints.addAll(Arrays.asList(points));

        // Optimize the path
        cutStraight();
        cutDoubles();

        // While there are too many vertices, cut points that cause the smallest effect
        while (shortPoints.size() > 100) {
            cutPoints(threshold++);
        }

        points = shortPoints.toArray(new Point[size]);
    }

    // Remove any unnecessary points along straight segments (within a few degrees)
    private static void cutStraight() {
        double previous, next;
        double x1, x2, y1, y2;
        for (int ind = 1; ind < shortPoints.size() - 1; ind++) {

            // Determine the angle between the two segments connected to the current point
            x1 = shortPoints.get(ind).x - shortPoints.get(ind - 1).x;
            y1 = shortPoints.get(ind).y - shortPoints.get(ind - 1).y;
            x2 = shortPoints.get(ind + 1).x - shortPoints.get(ind).x;
            y2 = shortPoints.get(ind + 1).x - shortPoints.get(ind).x;
            previous = atan2(y1,x1);
            next = atan2(y2, x2);

            // If the segments form a straight line, remove the center point
            if (Math.abs((previous + Math.PI) - (next + Math.PI)) < .05) {
                shortPoints.remove(ind);
                ind--;
            }
        }
    }

    // Due to the Canny algorithm many areas on the path form zig zags, this removes simple zig zags
    private static void cutDoubles() {
        double previous, current, next;
        double x1, x2, x3, y1, y2, y3;
        for (int ind = 1; ind < shortPoints.size() - 2; ind++) {
            // Determine the angles between the current three segments
            x1 = shortPoints.get(ind).x - shortPoints.get(ind - 1).x;
            y1 = shortPoints.get(ind).y - shortPoints.get(ind - 1).y;
            x2 = shortPoints.get(ind + 1).x - shortPoints.get(ind).x;
            y2 = shortPoints.get(ind + 1).x - shortPoints.get(ind).x;
            x3 = shortPoints.get(ind + 2).x - shortPoints.get(ind + 1).x;
            y3 = shortPoints.get(ind + 2).x - shortPoints.get(ind + 1).x;

            previous = atan2(y1,x1);
            current = atan2(y2, x2);
            next = atan2(y3, x3);

            // If the segments form a zig zag, within a few degrees, remove the point that results in the line going backwards
            if (Math.abs((previous + Math.PI) - (next + Math.PI)) < .05 &&
                    Math.abs(((previous + (3 * Math.PI)) % (2 * Math.PI)) - (current + Math.PI)) < .05) {
                shortPoints.remove(ind);
                ind--;
            }
        }
    }

    // Remove points with an error below the given threshold
    // Error is defined as the distance between the current point and the equivalent point on the line segment with the current point removed
    private static void cutPoints(double thresh) {
        double ratio, dist1, error;
        for (int ind = 2; ind < shortPoints.size() && shortPoints.size() > 100; ind++) {
            // Get the distance from each adjacent point
            dist1 = dist(shortPoints.get(ind - 2), shortPoints.get(ind - 1));
            // Find the equivalent point on the segment
            ratio = dist1 / (dist1 + dist(shortPoints.get(ind - 1), shortPoints.get(ind)));
            // Calculate the error
            error = sqrt(pow(dist1, 2.0) - pow(ratio * dist(shortPoints.get(ind - 2),
                    shortPoints.get(ind)), 2.0));

            // Remove the point if the error is small enough
            if (error < thresh) {
                shortPoints.remove(ind - 1);
                ind--;
            }
        }
    }

    // Calculate the distance between two points
    private static double dist(Point a, Point b) {
        double x = b.x - a.x;
        double y = b.y - a.y;

        return Math.abs(sqrt(pow(x, 2.0) + pow(y, 2.0)));
    }

    // Convert the points to Latitude and Longitude
    public static LatLng[] convert(LatLng startPoint) {
        LatLng[] mapPoints = new LatLng[size];

        // Calculate distances and angles
        calculateAttributes();

        // Calculate LatLng coordinates
        mapPoints[0] = startPoint;

        for(int i = 1; i < size; i++) {
            mapPoints[i] = getLatLng(mapPoints[i - 1], angles[i], distances[i]);
        }

        return mapPoints;
    }

    // Calculates the distances and angles between each pair of points and converts the distances to miles.
    private static void calculateAttributes() {
        double totalDistance = 0.0;
        double distanceRatio;
        double x, y;
        float ratio = (float)points.length / size;

        angles[0] = 0;
        distances[0] = 0;

        for (int ind = 1; ind < size; ind++) {
            // Get the x and y components of the path segment
            x = points[ind].x - points[ind - 1].x;
            y = points[ind].y - points[ind - 1].y;

            // Calculate the Cartesian distance
            distances[ind] = sqrt(pow(x, 2.0) + pow(y, 2.0));
            totalDistance += distances[ind];

            // Calculate the angle between the positive x axis and the line to the point
            angles[ind] = atan2(y, x);
        }

        // Get the ratio between the cartesian distance and the distance in miles
        distanceRatio = mapDistance / totalDistance;

        // Convert each distance to miles
        for (int ind = 0; ind < distances.length; ind++) {
            distances[ind] *= distanceRatio;
        }
    }

    // Convert the points to Latitude and Longitude
    private static LatLng getLatLng(LatLng firstPoint, double angle, double distance) {
        // Approximate earth radius
        final double EARTH_RADIUS = 3963.1676;

        // Convert to radians
        double lat1 = Math.toRadians(firstPoint.latitude);
        double long1 = Math.toRadians(firstPoint.longitude);
        double angle1 = Math.toRadians((Math.toDegrees(angle) + 450) % 360);

        // Use the formula to determine latitude and longitude based on the angle and distance from a known point
        double lat2 = Math.asin(Math.sin(lat1) * Math.cos(distance / EARTH_RADIUS)
                + Math.cos(lat1) * Math.sin(distance / EARTH_RADIUS) * Math.cos(angle1));
        double long2 = long1 + Math.atan2(Math.sin(angle1) * Math.sin(distance / EARTH_RADIUS) * Math.cos(lat1),
                Math.cos(distance / EARTH_RADIUS) - Math.sin(lat1) * Math.sin(lat2));

        // Convert back to degrees
        return new LatLng(Math.toDegrees(lat2), Math.toDegrees(long2));
    }

    // Create a String from the path for logging
    public static String makePathString(LatLng[] coordinates) {
        String result = coordinates[0].latitude + "," + coordinates[0].longitude;

        for(int i = 1; i < coordinates.length; i++) {
            result += "|" + coordinates[i].latitude + "," + coordinates[i].longitude;
        }

        return result;
    }

    // Return the input image
    public static Bitmap getImage() {
        return image;
    }

    // Set the input image
    public static void setImage(Bitmap btmp) {
        image = btmp;
    }
}
