package com.akaalpay.fund;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.akaalpay.BaseActivity;
import com.akaalpay.R;
import com.akaalpay.utils.ApiClient;
import com.akaalpay.utils.StatusBarUtils;
import com.akaalpay.utils.UiUtils;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class FundRequestActivity extends BaseActivity {

    TextInputEditText etAmount, etSelectBank, etPaymentMode, etRefNo, etDate;
    TextInputLayout layoutRef;

    ImageView ivReceipt;
    TextView tvUploadHint;
    View btnUpload;

    String selectedBankId="";
    String selectedMode="";

    Uri imageUri;

    int PICK_IMAGE=101;

    ArrayList<String> bankNames=new ArrayList<>();
    ArrayList<String> bankIds=new ArrayList<>();
    ArrayList<String> bankAccount=new ArrayList<>();
    ArrayList<String> bankIfsc=new ArrayList<>();
    ArrayList<String> bankHolder=new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fund_request);
        StatusBarUtils.setupStatusBar(this, R.id.header_all);
        initViews();

//        MaterialToolbar toolbar=findViewById(R.id.toolbar);
//        toolbar.setNavigationOnClickListener(v->finish());

        // Back button click listener
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            onBackPressed(); // or finish();
        });

        // Show real-time error if amount is less than 100
        etAmount.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().isEmpty()) {
                    try {
                        int val = Integer.parseInt(s.toString());
                        if (val < 100) {
                            etAmount.setError("Amount must be ₹100 or more");
                        }
                    } catch (NumberFormatException e) {
                        etAmount.setError("Enter valid amount");
                    }
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        fetchCompanyBanks();

        // Receipt/UTR fields are visible by default for all modes (including Cash).
        layoutRef.setVisibility(View.VISIBLE);
        btnUpload.setVisibility(View.VISIBLE);

        etSelectBank.setOnClickListener(v->showBankDialog());

        etDate.setOnClickListener(v->showDatePicker());

        etPaymentMode.setOnClickListener(v->showModeDialog());

        btnUpload.setOnClickListener(v->openGallery());

        findViewById(R.id.btnSubmitRequest).setOnClickListener(v->validateAndSubmit());
    }


    private void initViews(){

        etAmount=findViewById(R.id.etAmount);
        etSelectBank=findViewById(R.id.etSelectBank);
        etPaymentMode=findViewById(R.id.etPaymentMode);
        etRefNo=findViewById(R.id.etRefNo);
        etDate=findViewById(R.id.etDate);

        layoutRef=findViewById(R.id.layoutRef);

        ivReceipt=findViewById(R.id.ivReceipt);
        tvUploadHint=findViewById(R.id.tvUploadHint);
        btnUpload=findViewById(R.id.btnUploadReceipt);
    }


    private void fetchCompanyBanks(){

        String TOKEN=getSharedPreferences("BP_PREF",MODE_PRIVATE).getString("TOKEN","");

        ApiClient.get(this,"fund-request/company-banks",TOKEN,response -> {

            try{

                JSONArray data=response.getJSONArray("data");

                for(int i=0;i<data.length();i++){

                    JSONObject obj=data.getJSONObject(i);

                    bankNames.add(obj.getString("bank_name"));
                    bankIds.add(obj.getString("id"));
                    bankAccount.add(obj.getString("acc_no"));
                    bankIfsc.add(obj.getString("ifsc_code"));
                    bankHolder.add(obj.getString("holder_name"));
                }

            }catch(Exception e){ }

        },error-> UiUtils.showToast(this,"Bank load failed",false));
    }


    private void showBankDialog(){

        if (bankNames.isEmpty()){
            UiUtils.showToast(this,"Bank list not loaded yet, please wait",false);
            return;
        }

        AlertDialog.Builder builder=new AlertDialog.Builder(this);

        ListView listView=new ListView(this);

        BaseAdapter adapter=new BaseAdapter(){

            public int getCount(){return bankNames.size();}
            public Object getItem(int i){return bankNames.get(i);}
            public long getItemId(int i){return i;}

            public View getView(int i,View v,android.view.ViewGroup parent){

                if(v==null)
                    v=getLayoutInflater().inflate(R.layout.dialog_bank_item,parent,false);

                TextView bank=v.findViewById(R.id.tvBankName);
                TextView acc=v.findViewById(R.id.tvAccount);
                TextView ifsc=v.findViewById(R.id.tvIfsc);
                TextView holder=v.findViewById(R.id.tvHolder);

                bank.setText(bankNames.get(i));
                acc.setText("A/C : "+bankAccount.get(i));
                ifsc.setText("IFSC : "+bankIfsc.get(i));
                holder.setText("Holder : "+bankHolder.get(i));

                return v;
            }
        };

        listView.setAdapter(adapter);

        builder.setView(listView);

        AlertDialog dialog=builder.create();

        listView.setOnItemClickListener((p,v,pos,id)->{

            etSelectBank.setText(bankNames.get(pos)+" ("+bankAccount.get(pos)+")");

            selectedBankId=bankIds.get(pos);

            dialog.dismiss();
        });

        dialog.show();
    }


    private void showDatePicker(){

        Calendar c=Calendar.getInstance();

        new DatePickerDialog(this,(view,year,month,day)->{

            etDate.setText(year+"-"+(month+1)+"-"+day);

        },c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();
    }


    private void showModeDialog(){

        String[] modes={"Cash","NEFT","IMPS","RTGS"};

        new AlertDialog.Builder(this)
                .setItems(modes,(d,i)->{

                    selectedMode=modes[i];

                    etPaymentMode.setText(selectedMode);

                    // Receipt/UTR (reference) fields stay visible for ALL modes, including Cash.
                    layoutRef.setVisibility(View.VISIBLE);
                    btnUpload.setVisibility(View.VISIBLE);

                }).show();
    }


    private void openGallery(){

        Intent intent=new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent,PICK_IMAGE);
    }


    @Override
    protected void onActivityResult(int req,int res,@Nullable Intent data){

        super.onActivityResult(req,res,data);

        if(req==PICK_IMAGE && res==RESULT_OK && data!=null){

            try{

                imageUri=data.getData();

                Bitmap bitmap=MediaStore.Images.Media.getBitmap(getContentResolver(),imageUri);

                ivReceipt.setImageBitmap(bitmap);

                tvUploadHint.setText("Receipt Selected ✓");

            }catch(Exception e){
                UiUtils.showToast(this,"Image select failed: " + e.getMessage(),false);
            }
        }
    }


    private void validateAndSubmit(){

        String amount=etAmount.getText().toString().trim();
        String date=etDate.getText().toString().trim();
        String ref=etRefNo.getText().toString().trim();

        if(amount.isEmpty()){
            etAmount.setError("Enter Amount");
            return;
        }

        if(selectedBankId.isEmpty()){
            UiUtils.showToast(this,"Choose Bank",false);
            return;
        }

        if(selectedMode.isEmpty()){
            UiUtils.showToast(this,"Select Payment Mode",false);
            return;
        }

        if(date.isEmpty()){
            UiUtils.showToast(this,"Select Date",false);
            return;
        }

        // Receipt/UTR mandatory only for non-Cash modes (visible but optional for Cash)
        if(!selectedMode.equals("Cash")){

            if(ref.isEmpty()){
                etRefNo.setError("Enter Reference No");
                return;
            }

            if(imageUri==null){
                UiUtils.showToast(this,"Upload Receipt Image",false);
                return;
            }
        }

        submitRequest(amount,date,ref);
    }


    private void submitRequest(String amount, String date, String ref) {

        String TOKEN = getSharedPreferences("BP_PREF", MODE_PRIVATE).getString("TOKEN", "");

        try {

            UiUtils.showLoading(this);

            MultipartBody.Builder builder = new MultipartBody.Builder();
            builder.setType(MultipartBody.FORM);

            builder.addFormDataPart("bank_id", selectedBankId);
            builder.addFormDataPart("deposit_date", date);
            builder.addFormDataPart("payment_mode", selectedMode);
            builder.addFormDataPart("amount", amount);

            boolean hasRef = ref != null && !ref.trim().isEmpty();
            boolean hasImage = imageUri != null;

            if (hasRef) {
                builder.addFormDataPart("receipt_no", ref);
            }

            if (hasImage) {

                // Reads image via ContentResolver (Uri) instead of raw file path,
                // to avoid EACCES permission issues on newer Android versions.
                byte[] imageBytes;
                String mimeType = getContentResolver().getType(imageUri);
                if (mimeType == null) mimeType = "image/jpeg";

                try (java.io.InputStream inputStream = getContentResolver().openInputStream(imageUri)) {

                    if (inputStream == null) {
                        UiUtils.dismissLoading();
                        UiUtils.showToast(this, "Could not read receipt image, please select it again", false);
                        return;
                    }

                    java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
                    byte[] chunk = new byte[8192];
                    int len;
                    while ((len = inputStream.read(chunk)) != -1) {
                        buffer.write(chunk, 0, len);
                    }
                    imageBytes = buffer.toByteArray();

                } catch (Exception e) {
                    UiUtils.dismissLoading();
                    UiUtils.showToast(this, "Failed to read receipt image: " + e.getMessage(), false);
                    return;
                }

                if (imageBytes.length > 5 * 1024 * 1024) { // more than 5MB
                    UiUtils.dismissLoading();
                    UiUtils.showToast(this, "Receipt image is too large (keep it under 5MB)", false);
                    return;
                }

                String fileName = "receipt_" + System.currentTimeMillis() + ".jpg";

                builder.addFormDataPart(
                        "receipt_image",
                        fileName,
                        RequestBody.create(imageBytes, MediaType.parse(mimeType))
                );
            }

            ApiClient.postMultipart(
                    this,
                    "fund-request/submit-request",
                    TOKEN,
                    builder.build(),

                    response -> runOnUiThread(() -> {

                        UiUtils.dismissLoading();

                        try {

                            int status = response.optInt("status");
                            String message = response.optString("message");

                            if (status == 1) {

                                UiUtils.showToast(FundRequestActivity.this, message, true);

                                // Reset form
                                etAmount.setText("");
                                etRefNo.setText("");
                                etDate.setText("");
                                etPaymentMode.setText("");
                                etSelectBank.setText("");

                                selectedBankId = "";
                                selectedMode = "";
                                imageUri = null;

                                ivReceipt.setImageDrawable(null);
                                tvUploadHint.setText("Upload Receipt");

                            } else {

                                UiUtils.showToast(FundRequestActivity.this, message, false);
                            }

                        } catch (Exception e) {

                            UiUtils.showToast(FundRequestActivity.this, "Response Error: " + e.getMessage(), false);
                        }

                    }),

                    error -> runOnUiThread(() -> {

                        UiUtils.dismissLoading();

                        String errorDetail = (error != null) ? error.toString() : "Unknown error";

                        UiUtils.showToast(FundRequestActivity.this, "Request Failed: " + errorDetail, false);
                    })

            );

        } catch (Exception e) {

            UiUtils.dismissLoading();
            UiUtils.showToast(FundRequestActivity.this, "Something went wrong: " + e.getMessage(), false);
        }
    }
}