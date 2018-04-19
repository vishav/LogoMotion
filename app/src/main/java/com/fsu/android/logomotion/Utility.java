package com.fsu.android.logomotion;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Math.min;
import static java.lang.StrictMath.max;

/**
 * Created by vishav on 3/24/2018.
 */

public class Utility {
    public static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 123;

    private static final Map<String, Integer> emotionColorMap = initializeEmotionColorMap();

    //    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    protected static boolean checkPermission(final Context context) {
        int currentAPIVersion = Build.VERSION.SDK_INT;
        if (currentAPIVersion >= android.os.Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
                    alertBuilder.setCancelable(true);
                    alertBuilder.setTitle("Permission necessary");
                    alertBuilder.setMessage("External storage permission is necessary");
                    alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                        }
                    });
                    AlertDialog alert = alertBuilder.create();
                    alert.show();

                } else {
                    ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                }
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static String getImagePath(Context context, Uri pickedImage) {
        // Let's read picked image path using content resolver
        Cursor cursor = null;
        try {
            String[] filePath = {MediaStore.Images.Media.DATA};
            cursor = context.getContentResolver().query(pickedImage, filePath, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String imagePath = cursor.getString(column_index);
            System.out.println("pickedimage:" + pickedImage.toString());
            System.out.println("imagePath2:" + imagePath);
            return imagePath;
        } finally {
            {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    protected static String findShape(Context context, Bitmap bmp) {

        String shape="";
        Mat edges = new Mat();
        Utils.bitmapToMat(bmp, edges);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        double maxArea = 0;
        double shapeSize=0;
        Imgproc.cvtColor(edges, edges, Imgproc.COLOR_RGB2GRAY, 0);
        Imgproc.GaussianBlur(edges, edges, new Size(3, 3), 0);
        Imgproc.threshold(edges, edges, 60, 255, Imgproc.THRESH_BINARY);
        Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        for (int i=0; i<contours.size();i++) {
            MatOfPoint2f approxCurve2f = new MatOfPoint2f();
            MatOfPoint2f cnt2f = new MatOfPoint2f();
            MatOfPoint approxContour = new MatOfPoint();
            MatOfPoint cnt=contours.get(i);
            cnt.convertTo(cnt2f, CvType.CV_32FC2);
            double epsilon = 0.1 * Imgproc.arcLength(cnt2f, true);
            Imgproc.approxPolyDP(cnt2f, approxCurve2f, epsilon, true);
            approxCurve2f.convertTo(approxContour, CvType.CV_32S);
            double area = Math.abs(Imgproc.contourArea(cnt));
            double size = approxContour.size().height;
            Log.d("image area", String.valueOf(area));
            Log.d("contour size", String.valueOf(size));
            Log.d("shape factor", String.valueOf(epsilon*epsilon/(.04*area*4*3.14)));
            if(area>maxArea && i!=0){
                maxArea=area;
                shapeSize=size;
            }
            Log.d("final contour size", String.valueOf(size));
        }

        if(shapeSize == 3){
            shape= "triangle";
        }else if(shapeSize == 4){
            shape= "square";
        }else if(shapeSize == 5){
            shape= "pentagon";
        }else{
            shape= "circle";
        }
        Log.d("final shape size", String.valueOf(shapeSize));
        return shape;
    }

    private static Map<String, Integer> initializeEmotionColorMap()
    {
        Map<String,Integer> emotionColorMap = new HashMap<>();
        emotionColorMap.put("attention", R.color.ORANGE);
        emotionColorMap.put("creative", R.color.ORANGE);
        emotionColorMap.put("enthusiastic", R.color.ORANGE);

        emotionColorMap.put("urgent", R.color.RED);
        emotionColorMap.put("exciting", R.color.RED);
        emotionColorMap.put("aggressive", R.color.RED);
        emotionColorMap.put("appetite", R.color.RED);
        emotionColorMap.put("love", R.color.RED);

        emotionColorMap.put("impressive", R.color.PURPLE);
        emotionColorMap.put("wisdom", R.color.PURPLE);
        emotionColorMap.put("royal", R.color.PURPLE);

        emotionColorMap.put("calm", R.color.BLUE);
        emotionColorMap.put("intelligence", R.color.BLUE);
        emotionColorMap.put("confidence", R.color.BLUE);

        emotionColorMap.put("growth", R.color.GREEN);
        emotionColorMap.put("peaceful", R.color.GREEN);

        emotionColorMap.put("innocence", R.color.WHITE);
        emotionColorMap.put("pure", R.color.WHITE);

        emotionColorMap.put("professional", R.color.BLACK);
        emotionColorMap.put("power", R.color.BLACK);

        emotionColorMap.put("cheerfulness", R.color.YELLOW);
        emotionColorMap.put("optimism", R.color.YELLOW);


        return emotionColorMap;
    }


    protected static Integer getColorFromEmotions(String emotion){
        return emotionColorMap.get(emotion);
    }

    protected static int getIntFromColor(Color c){
        int argb = c.toArgb() & 0xffffff;
        int[] rgb = {(argb >> 16) & 0xff, (argb >> 8) & 0xff, argb & 0xff };
        return Color.rgb(rgb[0],rgb[1],rgb[2]);
    }

    protected static ArrayList<Integer> getMonochromaticColors(int base, int count){
        ArrayList<Integer> colors = new ArrayList<>();
        HSLColor hslBase = new HSLColor(Color.valueOf(base));

        Color color1 = hslBase.adjustShade(15); //15% darker
        Color color2 = hslBase.adjustTone(15); //15% lighter
        Color color3 = hslBase.adjustShade(25);
        Color color4 = hslBase.adjustTone(25);

        if(count >= 1){ colors.add(getIntFromColor(color1)); }
        if(count >= 2){ colors.add(getIntFromColor(color2)); }
        if(count >= 3){ colors.add(getIntFromColor(color3)); }
        if(count >= 4){ colors.add(getIntFromColor(color4)); }

        return colors;
    }

}