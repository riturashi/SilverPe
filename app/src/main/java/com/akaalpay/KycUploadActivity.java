package com.akaalpay;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import java.util.Iterator;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import org.json.JSONObject;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.akaalpay.utils.StatusBarUtils;
import com.akaalpay.utils.UiUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.*;
import okhttp3.Response;

public class KycUploadActivity extends AppCompatActivity {

    Button btnSubmit;

    ImageView imgAadharFront, imgAadharBack, imgPan, imgBank,imgPhoto,
            imgAddressFront, imgAddressBack,
            imgShopInner, imgShopOuter;

    File aadharFront, aadharBack, panCard, bankProof,
            addressFront, addressBack,photo,
            shopInner, shopOuter;

    String currentDoc = "";

    private ActivityResultLauncher<Intent> cameraLauncher;

    AlertDialog loader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kyc_upload);
        StatusBarUtils.setupStatusBar(this, R.id.header_all);
        btnSubmit = findViewById(R.id.btnSubmit);

        imgAadharFront = findViewById(R.id.imgAadharFront);
        imgAadharBack = findViewById(R.id.imgAadharBack);
        imgPan = findViewById(R.id.imgPan);
        imgBank = findViewById(R.id.imgBank);
        imgAddressFront = findViewById(R.id.imgAddressFront);
        imgAddressBack = findViewById(R.id.imgAddressBack);
        imgShopInner = findViewById(R.id.imgShopInner);
        imgShopOuter = findViewById(R.id.imgShopOuter);
        imgPhoto = findViewById(R.id.imgPhoto);

        setupCamera();

        findViewById(R.id.aadharfront).setOnClickListener(v -> openCamera("aadhar_front"));
        findViewById(R.id.aadharback).setOnClickListener(v -> openCamera("aadhar_back"));
        findViewById(R.id.pancard).setOnClickListener(v -> openCamera("pan_image"));
        findViewById(R.id.bankpassbook).setOnClickListener(v -> openCamera("bank_image"));
        findViewById(R.id.addressproof).setOnClickListener(v -> openCamera("address_front"));
        findViewById(R.id.addressproofback).setOnClickListener(v -> openCamera("address_back"));
        findViewById(R.id.cardShopInner).setOnClickListener(v -> openCamera("shop_inner"));
        findViewById(R.id.cardShopOuter).setOnClickListener(v -> openCamera("shop_outer"));
        findViewById(R.id.photo).setOnClickListener(v -> openCamera("photo"));

        btnSubmit.setOnClickListener(v -> submitKyc());
    }

    /* ================= CAMERA ================= */

    private void setupCamera() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        showCapturedImage();
                    }
                }
        );
    }

    private void openCamera(String type) {

        currentDoc = type;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
            return;
        }

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        File file;
        try {
            file = createImageFile(type);
        } catch (IOException e) {
            showDialog("Error", "Camera Error");
            return;
        }

        Uri uri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".provider",
                file
        );

        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        cameraLauncher.launch(intent);
    }

    private File createImageFile(String type) throws IOException {

        String time = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File file = File.createTempFile(type + "_" + time, ".jpg", dir);

        switch (type) {
            case "aadhar_front": aadharFront = file; break;
            case "aadhar_back": aadharBack = file; break;
            case "pan_image": panCard = file; break;
            case "bank_image": bankProof = file; break;
            case "address_front": addressFront = file; break;
            case "address_back": addressBack = file; break;
            case "photo": photo = file; break;
            case "shop_inner": shopInner = file; break;
            case "shop_outer": shopOuter = file; break;
        }

        return file;
    }

    private void showCapturedImage() {

        if (currentDoc.equals("aadhar_front")) {
            imgAadharFront.setImageURI(Uri.fromFile(aadharFront));
        } else if (currentDoc.equals("aadhar_back")) {
            imgAadharBack.setImageURI(Uri.fromFile(aadharBack));
        } else if (currentDoc.equals("pan_image")) {
            imgPan.setImageURI(Uri.fromFile(panCard));
        } else if (currentDoc.equals("bank_image")) {
            imgBank.setImageURI(Uri.fromFile(bankProof));
        } else if (currentDoc.equals("address_front")) {
            imgAddressFront.setImageURI(Uri.fromFile(addressFront));
        } else if (currentDoc.equals("address_back")) {
            imgAddressBack.setImageURI(Uri.fromFile(addressBack));
        }else if (currentDoc.equals("photo")) {
                imgPhoto.setImageURI(Uri.fromFile(photo));
        } else if (currentDoc.equals("shop_inner")) {
            imgShopInner.setImageURI(Uri.fromFile(shopInner));
        } else if (currentDoc.equals("shop_outer")) {
            imgShopOuter.setImageURI(Uri.fromFile(shopOuter));
        }
    }

    /* ================= LOADER ================= */

    private void showLoader() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(new android.widget.ProgressBar(this));
        builder.setCancelable(false);
        loader = builder.create();
        loader.show();
    }

    private void hideLoader() {
        if (loader != null && loader.isShowing()) {
            loader.dismiss();
        }
    }

    /* ================= SUBMIT ================= */

    private void submitKyc() {

        if (aadharFront == null || aadharBack == null || panCard == null ||
                photo == null ||
                bankProof == null || addressFront == null || addressBack == null ||
                shopInner == null || shopOuter == null) {

            showDialog("Missing", "Please upload all documents");
            return;
        }

        // ✅ Loader Start
        UiUtils.showLoading(this);

        new Thread(() -> {
            try {

                SharedPreferences pref = getSharedPreferences("BP_PREF", MODE_PRIVATE);
                String token = pref.getString("TOKEN", "");

                OkHttpClient client = new OkHttpClient();

                MultipartBody.Builder body = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("upload", "1")
                        .addFormDataPart("aadhar_front", aadharFront.getName(), fileBody(aadharFront))
                        .addFormDataPart("aadhar_back", aadharBack.getName(), fileBody(aadharBack))
                        .addFormDataPart("pan_image", panCard.getName(), fileBody(panCard))
                        .addFormDataPart("bank_image", bankProof.getName(), fileBody(bankProof))
                        .addFormDataPart("address_front", addressFront.getName(), fileBody(addressFront))
                        .addFormDataPart("address_back", addressBack.getName(), fileBody(addressBack))
                        .addFormDataPart("photo", photo.getName(), fileBody(photo))
                        .addFormDataPart("shop_inner", shopInner.getName(), fileBody(shopInner))
                        .addFormDataPart("shop_outer", shopOuter.getName(), fileBody(shopOuter));

                Request request = new Request.Builder()
                        .url("https://akaalpay.com/app/index.php/api/v1/user/submit-kyc")
                        .addHeader("Authorization", "Bearer " + token)
                        .post(body.build())
                        .build();

                Response response = client.newCall(request).execute();

                String res = response.body() != null ? response.body().string() : "";

                runOnUiThread(() -> {

                    // ✅ Loader Stop
                    UiUtils.dismissLoading();

                    try {
                        JSONObject json = new JSONObject(res);

                        int status = json.optInt("status");
                        String message = json.optString("message");

                        if (status == 1) {
                            // ✅ SUCCESS DIALOG WITH REDIRECT
                            new AlertDialog.Builder(this)
                                    .setTitle("Success")
                                    .setMessage("KYC Documents Uploaded Successfully. Please complete Video Verification.")
                                    .setCancelable(false) // User bahar click karke band na kar sake
                                    .setPositiveButton("Start Video KYC", (dialog, which) -> {

                                        // 🔥 Redirect to VideoKycActivity
                                        Intent i = new Intent(KycUploadActivity.this, VideoKycActivity.class);
                                        startActivity(i);
                                        finish(); // KycUploadActivity ko band kar dega taaki user back na aa sake

                                    })
                                    .show();

                        } else {
                            JSONObject errorObj = json.optJSONObject("error");

                            if (errorObj != null) {

                                JSONObject errors = errorObj.optJSONObject("errors");

                                if (errors != null && errors.length() > 0) {

                                    // ❌ VALIDATION ERRORS
                                    StringBuilder errorMsg = new StringBuilder();

                                    Iterator<String> keys = errors.keys();
                                    while (keys.hasNext()) {
                                        String key = keys.next();
                                        errorMsg.append("• ")
                                                .append(errors.optString(key))
                                                .append("\n");
                                    }

                                    showDialog("Validation Error", errorMsg.toString().trim());

                                } else {

                                    // ⚠️ CUSTOM ERROR (Pending / Already Approved)
                                    showDialog("Alert", message);
                                }

                            } else {

                                // ⚠️ FALLBACK ERROR
                                showDialog("Error", message);
                            }
                        }

                    } catch (Exception e) {
                        showDialog("Error", "Response parsing failed");
                    }

                });

            } catch (Exception e) {

                runOnUiThread(() -> {
                    UiUtils.dismissLoading();
                    showDialog("Error", "Network Error");
                });
            }

        }).start();
    }
    private RequestBody fileBody(File file) {
        return RequestBody.create(file, MediaType.parse("image/jpeg"));
    }

    /* ================= DIALOG ================= */

    private void showDialog(String title, String msg) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show();
    }
}