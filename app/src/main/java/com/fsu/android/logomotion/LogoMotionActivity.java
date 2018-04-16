package com.fsu.android.logomotion;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class LogoMotionActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private int REQUEST_CAMERA = 0;
    private int SELECT_FILE = 1;
    private String userChosenTask = "";
    private ImageView ivImage, ivImage2;
    private Button imageBtnSelect;
    private String TAKE_PHOTO;
    private String CHOOSE_FROM_GALLERY;
    private String CANCEL;
    private String LOGO_MOTION_IMAGE_NAME;
    private String LOGO_MOTION_IMAGE_EXTENSION;
    private NumberPicker K_COLOR_PICKER;
    private LinearLayout TOP_COLORS_LAYOUT;
    private LinearLayout NEW_COLORS_LAYOUT;
    private CheckBox MANIPULATE_TYPE_CHECKBOX;
    private Button RUN_AGAIN_BUTTON;
    private Boolean IVIMAGE_HAS_BITMAP;
    private Spinner emotionSpinner;

    static {
        System.loadLibrary("opencv_java3");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logo_motion);
        imageBtnSelect = (Button) findViewById(R.id.imageBtnSelect);
        RUN_AGAIN_BUTTON = (Button) findViewById(R.id.runAgainButton);
        TAKE_PHOTO = getString(R.string.take_photo);
        CHOOSE_FROM_GALLERY = getString(R.string.choose_from_gallery);
        CANCEL = getString(R.string.cancel);
        LOGO_MOTION_IMAGE_NAME = getString(R.string.logo_motion_image_name);
        LOGO_MOTION_IMAGE_EXTENSION = getString(R.string.logo_motion_image_extension);

        MANIPULATE_TYPE_CHECKBOX = (CheckBox) findViewById(R.id.manipulateTypeCheckBox);
        IVIMAGE_HAS_BITMAP = false;

        K_COLOR_PICKER = (NumberPicker) findViewById(R.id.kColorPicker);
        K_COLOR_PICKER.setMinValue(2);
        K_COLOR_PICKER.setMaxValue(5);
        K_COLOR_PICKER.setWrapSelectorWheel(false);
        K_COLOR_PICKER.setValue(3);

        TOP_COLORS_LAYOUT = (LinearLayout) findViewById(R.id.topColorsLayout);
        NEW_COLORS_LAYOUT = (LinearLayout) findViewById(R.id.newColorsLayout);

        ivImage = (ImageView) findViewById(R.id.ivImage);
        ivImage2 = (ImageView) findViewById(R.id.ivImage2);

        emotionSpinner = (Spinner) findViewById(R.id.emotion_spinner);

        constructEmotionSpinner();

        emotionSpinner.setOnItemSelectedListener(this);

        imageBtnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takeImage(LogoMotionActivity.this);
            }
        });
        RUN_AGAIN_BUTTON.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (IVIMAGE_HAS_BITMAP) {
                    Bitmap bmp = ((BitmapDrawable) ivImage.getDrawable()).getBitmap();
                    bmp = bmp.copy(Bitmap.Config.ARGB_8888, true);
                    bmp = manipulateBitmap(bmp, K_COLOR_PICKER.getValue());

                    ivImage2.setImageBitmap(bmp);
                } else {
                    AlertDialog alertDialog = new AlertDialog.Builder(LogoMotionActivity.this).create();
                    alertDialog.setTitle("Alert");
                    alertDialog.setMessage("Please choose an image first.");
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();
                }
            }
        });
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
                    userChosenTask = TAKE_PHOTO;
                    if (result)
                        cameraIntent();
                } else if (items[item].equals(CHOOSE_FROM_GALLERY)) {
                    userChosenTask = CHOOSE_FROM_GALLERY;
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
        intent.setAction(Intent.ACTION_PICK);
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
                    if (userChosenTask.equals("Take Photo"))
                        cameraIntent();
                    else if (userChosenTask.equals("Choose from Library"))
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
            // First decode with inJustDecodeBounds=true to check dimensions
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            options.inMutable = true;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            Uri pickedImage = data.getData();
            String imagePath = Utility.getImagePath(LogoMotionActivity.this, pickedImage);
            BitmapFactory.decodeFile(imagePath, options);

           /* even if theimage width and height are small
            manipulateBitmap() is taking a lot of time*/
            int reqWidth = 100;
            int reqHeight = 100;
            /* calculate inSamplesize
                Multiplying the inSampleSize by 4 to reduce
                the time taken by manipulatebitmap()
            */
            options.inSampleSize = 4 * Utility.calculateInSampleSize(options, ivImage.getWidth(), ivImage.getHeight());

            // resize options
            if (options.outWidth > reqWidth) {
                options.outWidth = reqWidth;
            }
            if (options.outHeight > reqHeight) {
                options.outHeight = reqHeight;
            }
            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;

            bm = BitmapFactory.decodeFile(imagePath, options);
            ivImage.setImageBitmap(bm);
            IVIMAGE_HAS_BITMAP = true;
            String shape = Utility.findShape(this, bm);
            Log.d("image shape:", String.valueOf(shape));

            bm = manipulateBitmap(bm, K_COLOR_PICKER.getValue());

            ivImage2.setImageBitmap(bm);
        }
    }


    // add photo to gallery
    private void onTakePhotoFromCamera(Intent data) {
        Bitmap bmp = (Bitmap) data.getExtras().get("data");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 60, bytes);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File userImage = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                LOGO_MOTION_IMAGE_NAME + "_" + timeStamp + "." + LOGO_MOTION_IMAGE_EXTENSION);
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

        bmp = bmp.copy(Bitmap.Config.ARGB_8888, true);
        ivImage.setImageBitmap(bmp);
        IVIMAGE_HAS_BITMAP = true;
        // this method returns the shape present in the image.
        String shape = Utility.findShape(this, bmp);
        Log.d("image shape:", String.valueOf(shape));

        bmp = manipulateBitmap(bmp, K_COLOR_PICKER.getValue());
        ivImage2.setImageBitmap(bmp);
    }

    private void makeImageAvailableToOthers(File userImage) {

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


    public double[] colorDistance(int x, int y) {
        //3D Cartesian Distance formula
        int[] rgb_x = {((x >> 16) & 0xff), ((x >> 8) & 0xff), (x & 0xff)};
        int[] rgb_y = {((y >> 16) & 0xff), ((y >> 8) & 0xff), (y & 0xff)};
        double[] returnList = {0.0, 0.0, 0.0, 0.0};

        double sum = 0.0;
        int diff;
        for (int i = 0; i < 3; i++) {
            diff = rgb_y[i] - rgb_x[i];
            returnList[i + 1] = diff;
            sum = sum + Math.pow(diff, 2);

        }
        returnList[0] = Math.sqrt(sum);
        return returnList;
    }


    public int roundColor(int color) {
        int[] rgb = {((color >> 16) & 0xff), ((color >> 8) & 0xff), (color & 0xff)};

        // if colors are not relatively close enough, assign to new color
        for (int i = 0; i < rgb.length; i++) {
            if (rgb[i] >= 0 && rgb[i] < 32) {
                rgb[i] = 0;
            } else if (rgb[i] >= 32 && rgb[i] < 96) {
                rgb[i] = 64;
            } else if (rgb[i] >= 96 && rgb[i] < 160) {
                rgb[i] = 128;
            } else if (rgb[i] >= 160 && rgb[i] < 224) {
                rgb[i] = 192;
            } else if (rgb[i] >= 224) {
                rgb[i] = 255;
            }

        }
        return (rgb[0] << 16) + (rgb[1] << 8) + (rgb[2]);
    }

    public Bitmap manipulateBitmap(Bitmap bmp, int k) {
        int height = bmp.getHeight();
        int width = bmp.getWidth();
        SparseIntArray colorAssignments = new SparseIntArray();
        ArrayList<Integer> topColors = new ArrayList<>();
        int pixel, pixel_assignment;
        int value;

        //Init and construct colorData
        ColorData[][] colorDataMatrix = new ColorData[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                colorDataMatrix[x][y] = new ColorData();
            }
        }

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                //Get pixel color
                pixel = bmp.getPixel(x, y) & 0xFFFFFF; //isolate the last 3 bytes

                //Round pixel color
                pixel_assignment = roundColor(pixel);

                //Record assigned value
                value = colorAssignments.get(pixel_assignment);
                if (value == 0) {
                    value = 1;
                } else {
                    value++;
                }
                colorAssignments.put(pixel_assignment, value);
            }
        }

        //Get top k colors
        int highestValue, highestIndex;
        for (int i = 0; i < k; i++) {
            highestValue = -1;
            highestIndex = 0;
            for (int j = 0; j < colorAssignments.size(); j++) {
                value = colorAssignments.valueAt(j);
                if (value > highestValue) {
                    highestValue = value;
                    highestIndex = j;
                }
            }
            topColors.add(colorAssignments.keyAt(highestIndex));
            Log.d("jcs12c", String.format("Position #%d ... color: #%06X ... count = %d", i + 1, colorAssignments.keyAt(highestIndex), highestValue));
            colorAssignments.removeAt(highestIndex);
        }

        //Reassign pixels using top 3 colors
        double minDistance;
        double distance;
        int bestMatchIndex = -1;
        double[] returnList;
        int[] changeValues = {0, 0, 0};

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                minDistance = (double) (0xFF * 3 + 1);
                pixel = bmp.getPixel(x, y) & 0xFFFFFF;

                for (int j = 0; j < k; j++) {
                    //distance, change1, change2, change3
                    returnList = colorDistance(pixel, topColors.get(j));
                    //Get Cartesian distance between pixels
                    distance = returnList[0];

                    if (distance < minDistance) {
                        minDistance = distance;
                        bestMatchIndex = j;
                        for (int i = 0; i < 3; i++) {
                            changeValues[i] = (int) returnList[i];
                        }
                    }
                }
                //Record data in colorDat matrix
                colorDataMatrix[x][y].set(bestMatchIndex, changeValues);

                //Change Pixel to assignment
                bmp.setPixel(x, y, topColors.get(bestMatchIndex));
            }
        }

        //Add topColors to Main Screen
        int topColor;
        for (int i = 0; i < k; i++) {
            ImageView topColorX = (ImageView) TOP_COLORS_LAYOUT.getChildAt(i);
            topColor = topColors.get(i);
            topColorX.setBackgroundColor(Color.rgb((topColor >> 16) & 0xff, (topColor >> 8) & 0xff, topColor & 0xff));
            topColorX.setVisibility(View.VISIBLE);
        }
        for (int i = k; i < TOP_COLORS_LAYOUT.getChildCount(); i++) {
            ImageView topColorX = (ImageView) TOP_COLORS_LAYOUT.getChildAt(i);
            topColorX.setVisibility(View.GONE);
        }


        //Primitive method for changing colors of image
        ArrayList<Integer> newColors = new ArrayList<>();
        newColors.add(Color.WHITE);
        newColors.add(Color.RED);
        newColors.add(Color.BLUE);
        newColors.add(Color.BLACK);
        newColors.add(Color.GREEN);

        if (MANIPULATE_TYPE_CHECKBOX.isChecked()) {
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, newColors.get(colorDataMatrix[x][y].getTopColorId()));
                }
            }
        }


        //Add newColors to Main Screen
        int newColor;
        for (int i = 0; i < k; i++) {
            ImageView newColorX = (ImageView) NEW_COLORS_LAYOUT.getChildAt(i);
            newColor = newColors.get(i);
            newColorX.setBackgroundColor(Color.rgb((newColor >> 16) & 0xff, (newColor >> 8) & 0xff, newColor & 0xff));
            newColorX.setVisibility(View.VISIBLE);
        }
        for (int i = k; i < NEW_COLORS_LAYOUT.getChildCount(); i++) {
            ImageView newColorX = (ImageView) NEW_COLORS_LAYOUT.getChildAt(i);
            newColorX.setVisibility(View.GONE);
        }

        return bmp;
    }

    private void constructEmotionSpinner(){
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.emotions_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        emotionSpinner.setAdapter(adapter);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // On selecting a spinner item
        String item = parent.getItemAtPosition(position).toString();

        // Showing selected spinner item
        Toast.makeText(parent.getContext(), "Selected: " + item, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
}
