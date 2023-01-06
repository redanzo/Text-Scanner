package com.example.textscannerapp;

import static android.Manifest.permission.CAMERA;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.IOException;

public class ScannerActivity extends AppCompatActivity {

    private ImageView captureIV;
    private TextView resultTV;
    private Button snapBtn,detectBtn, copyBtn;
    private Bitmap imageBitmap;
    private static final int REQUEST_IMAGE_CAPTURE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);
        captureIV = findViewById(R.id.idIVCaptureImg);
        resultTV = findViewById(R.id.idTVDetectedText);
        snapBtn = findViewById(R.id.idBtnSnap);
        detectBtn = findViewById(R.id.idBtnDetect);
        copyBtn = findViewById(R.id.idBtnCopy);

        copyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String scanned_text = resultTV.getText().toString();
                copyToClipboard(scanned_text);
            }
        });

        detectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                detectText();
                copyBtn.setVisibility(View.VISIBLE);
            }
        });

        snapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkPermissions()){
                    CropImage.activity().setGuidelines(CropImageView.Guidelines.ON).start(ScannerActivity.this);
                    detectBtn.setVisibility(View.VISIBLE);
                }else{
                    requestPermission();
                }
            }
        });
    }

    private boolean checkPermissions(){
        int camerPermission = ContextCompat.checkSelfPermission(getApplicationContext(), CAMERA);
        return camerPermission == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission(){
        int PERMISSION_CODE = 200;
        ActivityCompat.requestPermissions(this,new String[]{CAMERA},PERMISSION_CODE);
    }

//     private void captureImage(){
//        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//        if(takePicture.resolveActivity(getPackageManager())!=null){
//            startActivityForResult(takePicture, REQUEST_IMAGE_CAPTURE);
//        }
//    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length > 0){
            boolean cameraPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if(cameraPermission){
                Toast.makeText(this, "Permissions Granted...", Toast.LENGTH_SHORT).show();
                CropImage.activity().setGuidelines(CropImageView.Guidelines.ON).start(ScannerActivity.this);
                detectBtn.setVisibility(View.VISIBLE);
            }else{
                Toast.makeText(this, "Permissions Denied...", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if(resultCode == RESULT_OK){
                Uri resultUri = result.getUri();
                try {
                    imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);
                    captureIV.setImageBitmap(imageBitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void detectText(){
        InputImage image = InputImage.fromBitmap(imageBitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        Task<Text> result = recognizer.process(image).addOnSuccessListener(new OnSuccessListener<Text>() {
            @Override
            public void onSuccess(@NonNull Text text) {
                StringBuilder result = new StringBuilder();
                for(Text.TextBlock block:text.getTextBlocks()){
                    String blockText = block.getText();
                    Point[] blockCornerPoint = block.getCornerPoints();
                    Rect blockFrame = block.getBoundingBox();
                    for(Text.Line line: block.getLines()){
                        String lineText = line.getText();
                        Point[] lineCornerpoint = line.getCornerPoints();
                        Rect lineRect = line.getBoundingBox();
                        for(Text.Element element: line.getElements()){
                            String elementText = element.getText();
                            result.append(elementText);
                        }
                        resultTV.setText(blockText);
                    }
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(ScannerActivity.this, "Fail to detect text from image"+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void copyToClipboard(String text){
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Copied data", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(ScannerActivity.this, "Copied to Clipboard", Toast.LENGTH_SHORT).show();
    }
}