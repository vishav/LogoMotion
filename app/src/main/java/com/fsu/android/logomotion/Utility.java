package com.fsu.android.logomotion;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vishav on 3/24/2018.
 */

public class Utility {
    public static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 123;

//    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    protected static boolean checkPermission(final Context context)
    {
        int currentAPIVersion = Build.VERSION.SDK_INT;
        if(currentAPIVersion>=android.os.Build.VERSION_CODES.M)
        {
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

    /* detect borders in an image.
     Assumption: image background is white
     not completed yet
    */
    protected static void detectBorders(ImageView iv){
        Bitmap bmp = ((BitmapDrawable)iv.getDrawable()).getBitmap();
        int height = iv.getHeight();
        int width = iv.getWidth();
        List<Integer> x = new ArrayList<>();
        List<Integer> y = new ArrayList<>();
        for(int i=0;i<height;i++){
            @ColorInt
            int previous_x=0;
            int previous_y=0;
            for(int j=0;j<width;j++){
                @ColorInt
                int currentPixel=bmp.getPixel(i,j);
                if(currentPixel != Color.WHITE && bmp.getPixel(previous_x, previous_y) == Color.WHITE){
                    x.add(j);
                    y.add(i);
                    previous_x=0;
                    previous_y=0;
                }else {
                    previous_x = j;
                    previous_y = i;
                }
            }
        }
    }
}
