package com.fsu.android.logomotion;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogoMotionActivity extends AppCompatActivity {

    private int REQUEST_CAMERA = 0;
    private int SELECT_FILE = 1;
    private String userChoosenTask = "";
    private ImageView ivImage;
    private Button imageBtnSelect;
    private String TAKE_PHOTO;
    private String CHOOSE_FROM_GALLERY;
    private String CANCEL;
    private String LOGO_MOTION_IMAGE_NAME;
    private String LOGO_MOTION_IMAGE_EXTENSION;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logo_motion);
        imageBtnSelect = (Button) findViewById(R.id.imageBtnSelect);
        TAKE_PHOTO = getString(R.string.take_photo);
        CHOOSE_FROM_GALLERY = getString(R.string.choose_from_gallery);
        CANCEL = getString(R.string.cancel);
        LOGO_MOTION_IMAGE_NAME = getString(R.string.logo_motion_image_name);
        LOGO_MOTION_IMAGE_EXTENSION = getString(R.string.logo_motion_image_extension);
        imageBtnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takeImage(LogoMotionActivity.this);
            }
        });
        ivImage = (ImageView) findViewById(R.id.ivImage);
    }

    /*when the button is clicked, it will give option to either
    take a photo or select a photo or cancel the task*/
    private void takeImage(Context context) {

        final CharSequence[] items = {TAKE_PHOTO, CHOOSE_FROM_GALLERY, CANCEL};
        AlertDialog.Builder builder = new AlertDialog.Builder(LogoMotionActivity.this);
        builder.setTitle("Add Photo!");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                boolean result = Utility.checkPermission(LogoMotionActivity.this);
                if (items[item].equals(TAKE_PHOTO)) {
                    userChoosenTask = TAKE_PHOTO;
                    if (result)
                        cameraIntent();
                } else if (items[item].equals(CHOOSE_FROM_GALLERY)) {
                    userChoosenTask = CHOOSE_FROM_GALLERY;
                    if (result)
                        galleryIntent();
                } else if (items[item].equals(CANCEL)) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    private void cameraIntent() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    private void galleryIntent() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select File"), SELECT_FILE);
    }

    /*onRequestPermissionsResult() is inbuilt method which receives a callback of the dialog
    action (ActivityCompat.requestPermissions) for particular activity from which checkPermssion()
    (inside Utility.java) has been called*/
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case Utility.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (userChoosenTask.equals("Take Photo"))
                        cameraIntent();
                    else if (userChoosenTask.equals("Choose from Library"))
                        galleryIntent();
                } else {
                    //code for deny
                }
                break;
        }
    }

    /*handle the result we have received by calling startActivityForResult() Method*/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SELECT_FILE) {
                onSelectImageFromGallery(data);
            } else if (requestCode == REQUEST_CAMERA) {
                onTakePhotoFromCamera(data);
            }
        }
    }

    private void onSelectImageFromGallery(Intent data) {
        Bitmap bm = null;
        if (data != null) {
            try {
                bm = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), data.getData());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ivImage.setImageBitmap(bm);
    }

    // add photo to gallery
    private void onTakePhotoFromCamera(Intent data) {
        Bitmap bmp = (Bitmap) data.getExtras().get("data");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File userImage = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                LOGO_MOTION_IMAGE_NAME + "_" + timeStamp + "."+ LOGO_MOTION_IMAGE_EXTENSION);
        FileOutputStream fo;
        try {
            userImage.createNewFile();
            fo = new FileOutputStream(userImage);
            fo.write(bytes.toByteArray());
            fo.close();

            makeImageAvailableToOthers(userImage);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ivImage.setImageBitmap(bmp);
    }

    private void makeImageAvailableToOthers(File userImage){

        // Tell the media scanner about the new file so that it is
        // immediately available to the user.
        MediaScannerConnection.scanFile(this,
                new String[]{userImage.toString()}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });
    }
}
