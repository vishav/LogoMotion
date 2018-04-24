package com.fsu.android.logomotion;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
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
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.SeekBar;
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

import com.fsu.android.logomotion.HSLColor;


public class LogoMotionActivity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {

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
    private CheckBox APPLY_SHADING_CHECKBOX;
    private Button RUN_AGAIN_BUTTON;
    private Boolean IVIMAGE_HAS_BITMAP;
    private Spinner emotionSpinner;
    private Spinner paletteSpinner;
    private ArrayList<Integer> NEW_COLORS;
    private Boolean BACKGROUND_CHOSEN;
    private Button RESTART_BUTTON;
    private Button SAVE_BUTTON;
    private CheckBox SHIFT_NEW_COLORS_CHECKBOX;
    private LinearLayout SLIDER_LAYOUT;
    private Boolean SLIDERS_ADJUSTED;
    private ArrayList<Integer> saved_NEW_COLORS;
    private ArrayList<Integer> saved_SLIDER_PROGRESS;
    private int BACKGROUND_COLOR_ID;


    static {
        System.loadLibrary("opencv_java3");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logo_motion);
        imageBtnSelect = (Button) findViewById(R.id.imageBtnSelect);
        RUN_AGAIN_BUTTON = (Button) findViewById(R.id.runAgainButton);
        RESTART_BUTTON = (Button) findViewById(R.id.restartButton);
        SAVE_BUTTON = (Button) findViewById(R.id.saveButton);
        TAKE_PHOTO = getString(R.string.take_photo);
        CHOOSE_FROM_GALLERY = getString(R.string.choose_from_gallery);
        CANCEL = getString(R.string.cancel);
        LOGO_MOTION_IMAGE_NAME = getString(R.string.logo_motion_image_name);
        LOGO_MOTION_IMAGE_EXTENSION = getString(R.string.logo_motion_image_extension);

        MANIPULATE_TYPE_CHECKBOX = (CheckBox) findViewById(R.id.manipulateTypeCheckBox);
        APPLY_SHADING_CHECKBOX = (CheckBox) findViewById(R.id.applyShadingCheckBox);
        SHIFT_NEW_COLORS_CHECKBOX = (CheckBox) findViewById(R.id.shiftNewColorsCheckBox);
        IVIMAGE_HAS_BITMAP = false;
        BACKGROUND_CHOSEN = false;

        //Prepare topColor buttons
        Button topColor1 = (Button) findViewById(R.id.topColor1);
        topColor1.setOnClickListener(this);
        Button topColor2 = (Button) findViewById(R.id.topColor2);
        topColor2.setOnClickListener(this);
        Button topColor3 = (Button) findViewById(R.id.topColor3);
        topColor3.setOnClickListener(this);
        Button topColor4 = (Button) findViewById(R.id.topColor4);
        topColor4.setOnClickListener(this);
        Button topColor5 = (Button) findViewById(R.id.topColor5);
        topColor5.setOnClickListener(this);

        K_COLOR_PICKER = (NumberPicker) findViewById(R.id.kColorPicker);
        K_COLOR_PICKER.setMinValue(2);
        K_COLOR_PICKER.setMaxValue(5);
        K_COLOR_PICKER.setWrapSelectorWheel(false);
        K_COLOR_PICKER.setValue(3);

        NEW_COLORS = new ArrayList<>();
        resetNewColors();
        saved_NEW_COLORS = new ArrayList<>(NEW_COLORS);
        saved_SLIDER_PROGRESS = new ArrayList<Integer>();


        TOP_COLORS_LAYOUT = (LinearLayout) findViewById(R.id.topColorsLayout);
        NEW_COLORS_LAYOUT = (LinearLayout) findViewById(R.id.newColorsLayout);
        SLIDER_LAYOUT = (LinearLayout) findViewById(R.id.sliderLayout);
        for (int i = 0; i < SLIDER_LAYOUT.getChildCount(); i++) {
            SeekBar sb = (SeekBar) SLIDER_LAYOUT.getChildAt(i);
            sb.setOnSeekBarChangeListener(this);
        }

        ivImage = (ImageView) findViewById(R.id.ivImage);
        ivImage2 = (ImageView) findViewById(R.id.ivImage2);

        emotionSpinner = (Spinner) findViewById(R.id.emotion_spinner);
        constructEmotionSpinner();

        emotionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long l) {
                resetTopColorsBackgroundStatus();
                String emotion = parent.getItemAtPosition(position).toString();
                Integer emotion_color = getResources().getColor(Utility.getColorFromEmotions(emotion.toLowerCase())) & 0xFFFFFF;
                String palette_type = paletteSpinner.getSelectedItem().toString();

                ArrayList<Integer> newColors = getNewColors(emotion, palette_type);
                updateNewColorsInView(newColors, emotion_color);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });


        paletteSpinner = (Spinner) findViewById(R.id.palette_spinner);
        constructPaletteSpiner();
        paletteSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long l) {
                resetTopColorsBackgroundStatus();
                String palette_type = parent.getItemAtPosition(position).toString();
                String emotion = emotionSpinner.getSelectedItem().toString();
                Integer emotion_color = getResources().getColor(Utility.getColorFromEmotions(emotion.toLowerCase())) & 0xFFFFFF;

                ArrayList<Integer> newColors = getNewColors(emotion, palette_type);
                updateNewColorsInView(newColors, emotion_color);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        imageBtnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takeImage(LogoMotionActivity.this);
            }
        });
        RUN_AGAIN_BUTTON.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //resetTopColorsBackgroundStatus();
                if (IVIMAGE_HAS_BITMAP) {
                    Bitmap bmp = ((BitmapDrawable) ivImage.getDrawable()).getBitmap();
                    bmp = bmp.copy(Bitmap.Config.ARGB_8888, true);
                    bmp = manipulateBitmap(bmp, K_COLOR_PICKER.getValue());
                    ivImage2.setImageBitmap(bmp);

                    //Hide sliders if value k changes
                    for (int i = K_COLOR_PICKER.getValue(); i < SLIDER_LAYOUT.getChildCount(); i++) {
                        SeekBar slider = (SeekBar) SLIDER_LAYOUT.getChildAt(i);
                        slider.setVisibility(View.GONE);
                    }

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

        RESTART_BUTTON.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                restartAction();
            }
        });

        SAVE_BUTTON.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(IVIMAGE_HAS_BITMAP) {
                    Bitmap bmp = ((BitmapDrawable) ivImage2.getDrawable()).getBitmap();
                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                    String fileName = LOGO_MOTION_IMAGE_NAME + "_colored_" + timeStamp + "." + LOGO_MOTION_IMAGE_EXTENSION;
                    saveImage(bmp, fileName, 100);
                    String msg = fileName + " saved";
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
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
            case Utility.MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
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
            try {
                bm = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), data.getData());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        restartAction();
        ivImage.setImageBitmap(bm);
        IVIMAGE_HAS_BITMAP = true;
        bm = bm.copy(Bitmap.Config.ARGB_8888, true);
        String shape = Utility.findShape(this, bm);
        Log.d("image shape:", String.valueOf(shape));
        bm = manipulateBitmap(bm, K_COLOR_PICKER.getValue());
        ivImage2.setImageBitmap(bm);
    }


    // add photo to gallery
    private void onTakePhotoFromCamera(Intent data) {
        Bitmap bmp = (Bitmap) data.getExtras().get("data");
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = LOGO_MOTION_IMAGE_NAME + "_original_" + timeStamp + "." + LOGO_MOTION_IMAGE_EXTENSION;
        saveImage(bmp, fileName, 60);
        restartAction();
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
                    returnList = Utility.colorDistance(pixel, topColors.get(j));
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
                /*if(APPLY_SHADING_CHECKBOX.isChecked() && !MANIPULATE_TYPE_CHECKBOX.isChecked()){
                    ColorData newColorData = colorDataMatrix[x][y];
                    int shadedColor = Utility.applyShading(pixel, newColorData.getrChange(), newColorData.getgChange(), newColorData.getbChange());
                    bmp.setPixel(x, y, shadedColor);
                } else {*/
                bmp.setPixel(x, y, topColors.get(bestMatchIndex));
                //}
            }
        }

        //Add topColors to Main Screen
        int topColor;
        for (int i = 0; i < k; i++) {
            Button topColorX = (Button) TOP_COLORS_LAYOUT.getChildAt(i);
            topColor = topColors.get(i);
            topColorX.setBackgroundColor(Color.rgb((topColor >> 16) & 0xff, (topColor >> 8) & 0xff, topColor & 0xff));
            topColorX.setVisibility(View.VISIBLE);
        }
        for (int i = k; i < TOP_COLORS_LAYOUT.getChildCount(); i++) {
            Button topColorX = (Button) TOP_COLORS_LAYOUT.getChildAt(i);
            topColorX.setVisibility(View.GONE);
        }

        //Only change colors if Checkbox is selected
        if (MANIPULATE_TYPE_CHECKBOX.isChecked()) {
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    ColorData newColorData = colorDataMatrix[x][y];
                    int topColorId = newColorData.getTopColorId();
                    int newColor = NEW_COLORS.get(topColorId);
                    if (APPLY_SHADING_CHECKBOX.isChecked()) {
                        newColor = Utility.applyShading(newColor, newColorData.getrChange(), newColorData.getgChange(), newColorData.getbChange());
                    }
                    bmp.setPixel(x, y, newColor);
                }
            }
        }


        //Add newColors to Main Screen
        int newColor;
        for (int i = 0; i < k; i++) {
            ImageView newColorX = (ImageView) NEW_COLORS_LAYOUT.getChildAt(i);
            newColor = NEW_COLORS.get(i);
            newColorX.setBackgroundColor(Color.rgb((newColor >> 16) & 0xff, (newColor >> 8) & 0xff, newColor & 0xff));
            newColorX.setVisibility(View.VISIBLE);
        }
        for (int i = k; i < NEW_COLORS_LAYOUT.getChildCount(); i++) {
            ImageView newColorX = (ImageView) NEW_COLORS_LAYOUT.getChildAt(i);
            newColorX.setVisibility(View.GONE);
        }
        return bmp;
    }

    private void constructEmotionSpinner() {
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.emotions_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        emotionSpinner.setAdapter(adapter);
    }

    private void constructPaletteSpiner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.palette_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        paletteSpinner.setAdapter(adapter);
    }

    private ArrayList<Integer> getNewColors(String emotion, String palette) {
        //PALETTES
        ArrayList<Integer> newColors = new ArrayList<>();
        Integer emotion_color = getResources().getColor(Utility.getColorFromEmotions(emotion.toLowerCase())) & 0xFFFFFF;

        if (palette.equals("Monochromatic")) {
            newColors = Utility.getMonochromaticColors(emotion_color, K_COLOR_PICKER.getValue());
        } else if (palette.equals("Complementary")) {
            newColors = Utility.getComplementaryColorPalette(emotion_color, K_COLOR_PICKER.getValue());
        } else if (palette.equals("Analogous")) {
            newColors = Utility.getAnalogousColorPalette(emotion_color, K_COLOR_PICKER.getValue());
        } else if (palette.equals("Triad")) {
            newColors = Utility.getTriadColorPalette(emotion_color, K_COLOR_PICKER.getValue());
        }

        return newColors;
    }

    private void updateNewColorsInView(ArrayList<Integer> newColors, int baseColor) {
        //Set global NEW_COLORS array
        NEW_COLORS.remove(0);
        NEW_COLORS.add(0, baseColor);
        for (int i = 1; i <= newColors.size(); i++) {
            NEW_COLORS.remove(i);
            NEW_COLORS.add(i, newColors.get(i - 1));
        }
        for (int i = newColors.size() + 1; i < 5; i++) {
            NEW_COLORS.remove(i);
            NEW_COLORS.add(i, 0);
        }


        //Set colors in view
        int newColor;
        int k = K_COLOR_PICKER.getValue();
        for (int i = 0; i < k; i++) {
            ImageView newColorX = (ImageView) NEW_COLORS_LAYOUT.getChildAt(i);
            newColor = NEW_COLORS.get(i);
            newColorX.setBackgroundColor(Color.rgb((newColor >> 16) & 0xff, (newColor >> 8) & 0xff, newColor & 0xff));
            newColorX.setVisibility(View.VISIBLE);
        }
        for (int i = k; i < NEW_COLORS_LAYOUT.getChildCount(); i++) {
            ImageView newColorX = (ImageView) NEW_COLORS_LAYOUT.getChildAt(i);
            newColorX.setVisibility(View.GONE);
        }
        setupSliders();
    }

    @Override
    public void onClick(View v) {
        Button b = (Button) findViewById(v.getId());
        //Only allow 1 background color at a time
        if (!BACKGROUND_CHOSEN) {
            BACKGROUND_CHOSEN = true;
            BACKGROUND_COLOR_ID = b.getId();
            b.setTextColor(Color.BLACK);
            if (((ColorDrawable) b.getBackground()).getColor() == Color.BLACK) {
                b.setTextColor(Color.WHITE);
            }

            if (!b.getText().equals("BACKGRD")) {
                b.setText(R.string.BACKGROUND);
                setColorAsBackground(b);
            }
        } else if (BACKGROUND_COLOR_ID == b.getId()) {
            BACKGROUND_CHOSEN = false;
            b.setText("");
            NEW_COLORS = new ArrayList<>(saved_NEW_COLORS);

            //Save slider progress
            saved_SLIDER_PROGRESS.clear();
            for (int i = 0; i < SLIDER_LAYOUT.getChildCount(); i++) {
                SeekBar slider = (SeekBar) SLIDER_LAYOUT.getChildAt(i);
                saved_SLIDER_PROGRESS.add(slider.getProgress());
            }

            //Set colors in view
            int newColor;
            int k = K_COLOR_PICKER.getValue();
            for (int i = 0; i < k; i++) {
                ImageView newColorX = (ImageView) NEW_COLORS_LAYOUT.getChildAt(i);
                newColor = NEW_COLORS.get(i);
                newColorX.setBackgroundColor(Color.rgb((newColor >> 16) & 0xff, (newColor >> 8) & 0xff, newColor & 0xff));
                newColorX.setVisibility(View.VISIBLE);
            }
            for (int i = k; i < NEW_COLORS_LAYOUT.getChildCount(); i++) {
                ImageView newColorX = (ImageView) NEW_COLORS_LAYOUT.getChildAt(i);
                newColorX.setVisibility(View.GONE);
            }

            //Restore sliders
            setupSliders();
            for (int i = 0; i < SLIDER_LAYOUT.getChildCount() && i < saved_SLIDER_PROGRESS.size(); i++) {
                SeekBar slider = (SeekBar) SLIDER_LAYOUT.getChildAt(i);
                slider.setProgress(saved_SLIDER_PROGRESS.get(i));
            }
        }
    }


    public void setColorAsBackground(Button b) {
        ColorDrawable bgColorDrawable = (ColorDrawable) b.getBackground();
        int bgColor = bgColorDrawable.getColor() & 0xFFFFFF;

        if (IVIMAGE_HAS_BITMAP) {
            Bitmap bmp = ((BitmapDrawable) ivImage.getDrawable()).getBitmap();
            bmp = bmp.copy(Bitmap.Config.ARGB_8888, true);

            int target_color = bgColor;
            int child_index = -1;
            if (MANIPULATE_TYPE_CHECKBOX.isChecked()) {
                //Save new colors first so you can go back.
                saved_NEW_COLORS = new ArrayList<>(NEW_COLORS);

                //Based on NEW_COLORS
                LinearLayout linearLayout = (LinearLayout) findViewById(R.id.topColorsLayout);
                for (int i = 0; i < linearLayout.getChildCount(); i++) {
                    Button child = (Button) linearLayout.getChildAt(i);
                    if (child.getId() == b.getId()) {
                        child_index = i;
                        break;
                    }
                }
                //Create new set of NEW_COLORS where Color.White replaces the background color
                //  Also, all colors are shifted to the right so that more important colors
                //  stay in the modified image
                if (SHIFT_NEW_COLORS_CHECKBOX.isChecked()) {
                    for (int i = NEW_COLORS.size() - 1; i >= 0; i--) {
                        NEW_COLORS.remove(i);
                        if (i != child_index) {
                            NEW_COLORS.add(i, NEW_COLORS.get(i - 1));
                        } else {
                            NEW_COLORS.add(i, Color.WHITE);
                            break;
                        }
                    }
                } else {
                    NEW_COLORS.remove(child_index);
                    NEW_COLORS.add(child_index, Color.WHITE);
                }
                manipulateBitmap(bmp, K_COLOR_PICKER.getValue());
            } else {
                for (int x = 0; x < bmp.getWidth(); x++) {
                    for (int y = 0; y < bmp.getHeight(); y++) {
                        int pixel = bmp.getPixel(x, y) & 0xffffff;
                        if (pixel == target_color) {
                            bmp.setPixel(x, y, Color.WHITE);
                        }
                    }
                }
            }


            ivImage2.setImageBitmap(bmp);
        }
    }

    public void resetTopColorsBackgroundStatus() {
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.topColorsLayout);
        for (int i = 0; i < linearLayout.getChildCount(); i++) {
            Button child = (Button) linearLayout.getChildAt(i);
            child.setText("");
        }
        BACKGROUND_CHOSEN = false;

        //Since background status has been cleared, the NEW_COLORS need to be fixed
        //   The colors were shifted to the right at some point and the last color was lost.
        String emotion = emotionSpinner.getSelectedItem().toString();
        Integer emotion_color = getResources().getColor(Utility.getColorFromEmotions(emotion.toLowerCase())) & 0xFFFFFF;
        String palette_type = paletteSpinner.getSelectedItem().toString();
        ArrayList<Integer> newColors = getNewColors(emotion, palette_type);
        updateNewColorsInView(newColors, emotion_color);
    }

    public void resetNewColors() {
        NEW_COLORS.clear();
        NEW_COLORS.add(0);
        NEW_COLORS.add(0);
        NEW_COLORS.add(0);
        NEW_COLORS.add(0);
        NEW_COLORS.add(0);
    }

    public void restartAction() {
        resetTopColorsBackgroundStatus();
        MANIPULATE_TYPE_CHECKBOX.setChecked(false);
        APPLY_SHADING_CHECKBOX.setChecked(false);
        SHIFT_NEW_COLORS_CHECKBOX.setChecked(false);
        BACKGROUND_CHOSEN = false;

        if (IVIMAGE_HAS_BITMAP) {
            //Re-do manipulateBitmap
            Bitmap bmp = ((BitmapDrawable) ivImage.getDrawable()).getBitmap();
            bmp = bmp.copy(Bitmap.Config.ARGB_8888, true);
            bmp = manipulateBitmap(bmp, K_COLOR_PICKER.getValue());
            ivImage2.setImageBitmap(bmp);
        }
    }

    public void setupSliders() {
        int k = NEW_COLORS.size();
        int numSliders = SLIDER_LAYOUT.getChildCount();
        for (int i = 0; i < numSliders && i < k; i++) {
            SeekBar sliderX = (SeekBar) SLIDER_LAYOUT.getChildAt(i);
            ImageView iv = (ImageView) NEW_COLORS_LAYOUT.getChildAt(i);
            HSLColor hslColorX;
            try {
                int newColorX = ((ColorDrawable) iv.getBackground()).getColor();
                hslColorX = new HSLColor(Color.valueOf(newColorX));
            } catch (NullPointerException e) {
                continue;
            }
            sliderX.setProgress((int) hslColorX.getLuminance());
            sliderX.setVisibility(View.VISIBLE);
        }
        for (int i = K_COLOR_PICKER.getValue(); i < SLIDER_LAYOUT.getChildCount(); i++) {
            SeekBar sliderX = (SeekBar) SLIDER_LAYOUT.getChildAt(i);
            sliderX.setVisibility(View.GONE);
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        int id = seekBar.getId();
        int progress = seekBar.getProgress();
        ImageView iv;
        int index = 0;
        switch (id) {
            case R.id.slider1: {
                iv = (ImageView) findViewById(R.id.newColor1);
                index = 0;
                break;
            }
            case R.id.slider2: {
                iv = (ImageView) findViewById(R.id.newColor2);
                index = 1;
                break;
            }
            case R.id.slider3: {
                iv = (ImageView) findViewById(R.id.newColor3);
                index = 2;
                break;
            }
            case R.id.slider4: {
                iv = (ImageView) findViewById(R.id.newColor4);
                index = 3;
                break;
            }
            case R.id.slider5: {
                iv = (ImageView) findViewById(R.id.newColor5);
                index = 4;
                break;
            }
            default: {
                iv = (ImageView) findViewById(R.id.newColor5);
                break;
            }
        }
        //Get Color and adjust luminance
        HSLColor hslColor;
        int intColor = ((ColorDrawable) iv.getBackground()).getColor();
        hslColor = new HSLColor(Color.valueOf(intColor));
        Color adjustedColor = hslColor.adjustLuminance((float) progress);
        int adjustedIntColor = Utility.getIntFromColor(adjustedColor);
        //Set newColor ImageView
        iv.setBackgroundColor(adjustedIntColor);
        //Set NEW_COLORS arraylist value
        NEW_COLORS.remove(index);
        NEW_COLORS.add(index, adjustedIntColor);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {


    }

    private void saveImage(Bitmap bmp, String fileName, int quality) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, quality, bytes);

        File userImage = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), fileName);
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
    }


}
