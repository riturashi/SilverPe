package com.silverpe.distributer;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.silverpe.R;
import com.silverpe.utils.ApiClient;
import com.silverpe.utils.StatusBarUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddUserBYDistributer extends AppCompatActivity {

    private static final String TAG = "AddUserBYDistributer";
    private static final String PREFS_NAME = "BP_PREF";
    private static final String TOKEN_KEY = "TOKEN";
    private static final String USER_TYPE_PREF_KEY = "user_type_id";

    private MaterialAutoCompleteTextView actUserType;
    private MaterialAutoCompleteTextView actPlan;
    private MaterialAutoCompleteTextView etState;
    private MaterialAutoCompleteTextView etCity;

    private TextInputEditText etName;
    private TextInputEditText etFatherName;
    private TextInputEditText etEmail;
    private TextInputEditText etMobile;
    private TextInputEditText etDob;
    private TextInputEditText etAadhar;
    private TextInputEditText etPan;
    private TextInputEditText etAddress;
    private TextInputEditText etPincode;
    private TextInputEditText etFirmName;
   // private TextInputEditText etMinBalance;

    private MaterialButtonToggleGroup genderToggle;
    private MaterialButton btnSubmit;
    private ImageView btnBack;
    private FrameLayout loadingOverlay;

    private String token = "";

    private final List<DropdownItem> userTypeItems = new ArrayList<>();
    private final List<DropdownItem> planItems = new ArrayList<>();
    private final List<DropdownItem> stateItems = new ArrayList<>();
    private final List<DropdownItem> cityItems = new ArrayList<>();

    private String selectedUserTypeId = "";
    // The users/save API's userType field expects user_level, not downline_type.id.
    private String selectedUserLevel = "";
    private String selectedPlanId = "";
    private String selectedStateId = "";
    private String selectedCityId = "";
    private boolean submitting = false;

    private static class DropdownItem {
        final String id;
        final String label;
        final String userLevel;

        DropdownItem(String id, String label) {
            this(id, label, "");
        }

        DropdownItem(String id, String label, String userLevel) {
            this.id = id == null ? "" : id;
            this.label = label == null ? "" : label;
            this.userLevel = userLevel == null ? "" : userLevel;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private interface DropdownResultCallback {
        void onSuccess(List<DropdownItem> items);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_user_bydistributer);
        StatusBarUtils.setupStatusBar(this, R.id.header_all);
        bindViews();
        if (!hasRequiredViews()) {
            Log.e(TAG, "One or more required views were not found. Check activity_add_user_bydistributer.xml IDs.");
            Toast.makeText(this, "Form layout error. Please check the layout IDs.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        SharedPreferences pref = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        token = pref.getString(TOKEN_KEY, "");
        Log.d(TAG, "onCreate: "+token);

        if (TextUtils.isEmpty(token.trim())) {
            Log.e(TAG, "Session token is missing in SharedPreferences: " + PREFS_NAME);
            Toast.makeText(this, "Session token missing. Please log in again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        actPlan.setAdapter(makeAdapter(planItems));
        etCity.setAdapter(makeAdapter(cityItems));
        setupDropdownListeners();
        setupFormActions();
        loadUserTypes();
        loadStates();
    }

    private void bindViews() {
        actUserType = findViewById(R.id.actUserType);
        actPlan = findViewById(R.id.actPlan);
        etState = findViewById(R.id.etState);
        etCity = findViewById(R.id.etCity);

        etName = findViewById(R.id.etName);
        etFatherName = findViewById(R.id.etFatherName);
        etEmail = findViewById(R.id.etEmail);
        etMobile = findViewById(R.id.etMobile);
        etDob = findViewById(R.id.etDob);
        etAadhar = findViewById(R.id.etAadhar);
        etPan = findViewById(R.id.etPan);
        etAddress = findViewById(R.id.etAddress);
        etPincode = findViewById(R.id.etPincode);
        etFirmName = findViewById(R.id.etFirmName);

        genderToggle = findViewById(R.id.genderToggle);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnBack = findViewById(R.id.btnBack);
        loadingOverlay = findViewById(R.id.loadingOverlay);
    }

    private boolean hasRequiredViews() {
        return actUserType != null && actPlan != null && etState != null && etCity != null
                && etName != null && etFatherName != null && etEmail != null && etMobile != null
                && etDob != null && etAadhar != null && etPan != null && etAddress != null && etFirmName !=null
                && etPincode != null && genderToggle != null
                && btnSubmit != null && btnBack != null;
    }

    private void setupFormActions() {
        btnBack.setOnClickListener(v -> finish());
        btnSubmit.setOnClickListener(v -> submitAddUser());
        etDob.setOnClickListener(v -> showDatePicker());
    }

    private void setupDropdownListeners() {
        actUserType.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= userTypeItems.size()) return;

            DropdownItem selected = userTypeItems.get(position);
            selectedUserTypeId = selected.id;
            selectedUserLevel = selected.userLevel;

            Log.d(TAG, "Selected user type id=" + selectedUserTypeId
                    + ", user_level=" + selectedUserLevel);

            selectedPlanId = "";
            planItems.clear();
            actPlan.setText("", false);
            actPlan.setAdapter(makeAdapter(planItems));
            loadPlans(selectedUserLevel);
        });
        actUserType.setOnClickListener(v -> actUserType.showDropDown());

        actPlan.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < planItems.size()) {
                selectedPlanId = planItems.get(position).id;
                Log.d(TAG, "Selected plan id=" + selectedPlanId);
            }
        });
        actPlan.setOnClickListener(v -> {
            if (TextUtils.isEmpty(selectedUserLevel)) {
                Toast.makeText(this, "Please select a user type first.", Toast.LENGTH_SHORT).show();
            } else if (planItems.isEmpty()) {
                Toast.makeText(this, "No plans available for this user type.", Toast.LENGTH_SHORT).show();
            } else {
                actPlan.showDropDown();
            }
        });

        etState.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= stateItems.size()) return;

            selectedStateId = stateItems.get(position).id;
            selectedCityId = "";
            cityItems.clear();
            etCity.setText("", false);
            etCity.setAdapter(makeAdapter(cityItems));
            loadCities(selectedStateId);
        });
        etState.setOnClickListener(v -> etState.showDropDown());

        etCity.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < cityItems.size()) {
                selectedCityId = cityItems.get(position).id;
                Log.d(TAG, "Selected city id=" + selectedCityId);
            }
        });
        etCity.setOnClickListener(v -> {
            if (TextUtils.isEmpty(selectedStateId)) {
                Toast.makeText(this, "Please select a state first.", Toast.LENGTH_SHORT).show();
            } else if (cityItems.isEmpty()) {
                Toast.makeText(this, "No cities available for this state.", Toast.LENGTH_SHORT).show();
            } else {
                etCity.showDropDown();
            }
        });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        String current = getText(etDob);
        if (!current.isEmpty()) {
            try {
                String[] parts = current.split("-");
                if (parts.length == 3) {
                    calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parts[0]));
                    calendar.set(Calendar.MONTH, Integer.parseInt(parts[1]) - 1);
                    calendar.set(Calendar.YEAR, Integer.parseInt(parts[2]));
                }
            } catch (Exception ignored) {
                // Use today's date if the existing field text isn't a valid date.
            }
        }

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (picker, year, month, dayOfMonth) -> etDob.setText(
                        String.format(Locale.getDefault(), "%02d-%02d-%04d", dayOfMonth, month + 1, year)),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();
    }

    private void loadUserTypes() {
        SharedPreferences pref = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String loggedInUserType = pref.getString(USER_TYPE_PREF_KEY, "");
        if (TextUtils.isEmpty(loggedInUserType.trim())) {
            Log.e(TAG, "Missing logged-in user_type_id in SharedPreferences.");
            Toast.makeText(this, "User type missing in session. Please log in again.", Toast.LENGTH_LONG).show();
            return;
        }

        JSONObject body = new JSONObject();
        try {
            // The downline endpoint takes the current logged-in user's level in userType.
            body.put("userType", loggedInUserType);
        } catch (Exception e) {
            Log.e(TAG, "Failed to create downline user type request body", e);
            return;
        }

        postDropdownRequest("users/downline-user-level", body, "downline_type", items -> {
            userTypeItems.clear();
            userTypeItems.addAll(items);
            actUserType.setAdapter(makeAdapter(userTypeItems));
            Log.d(TAG, "User types loaded: " + userTypeItems.size());
        });
    }

    private void loadPlans(String userLevel) {
        if (TextUtils.isEmpty(userLevel == null ? "" : userLevel.trim())) {
            Log.w(TAG, "Not loading plans: selected user_level is empty.");
            return;
        }

        JSONObject body = new JSONObject();
        try {
            // Plan filtering is based on the selected downline item user_level.
            body.put("user_type", userLevel);
        } catch (Exception e) {
            Log.e(TAG, "Failed to create plans request body", e);
            return;
        }

        postDropdownRequest("users/get-plans", body, "plans", items -> {
            planItems.clear();
            planItems.addAll(items);
            actPlan.setAdapter(makeAdapter(planItems));
            Log.d(TAG, "Plans loaded for user_level " + userLevel + ": " + planItems.size());
        });
    }

    private void loadStates() {
        postDropdownRequest("users/get-states", new JSONObject(), "states", items -> {
            stateItems.clear();
            stateItems.addAll(items);
            etState.setAdapter(makeAdapter(stateItems));
            Log.d(TAG, "States loaded: " + stateItems.size());
        });
    }

    private void loadCities(String stateId) {
        if (TextUtils.isEmpty(stateId == null ? "" : stateId.trim())) return;

        JSONObject body = new JSONObject();
        try {
            body.put("state", stateId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to create city request body", e);
            Toast.makeText(this, "Unable to prepare city request.", Toast.LENGTH_SHORT).show();
            return;
        }

        postDropdownRequest("users/get-cities", body, "cities", items -> {
            cityItems.clear();
            cityItems.addAll(items);
            etCity.setAdapter(makeAdapter(cityItems));
            Log.d(TAG, "Cities loaded for state " + stateId + ": " + cityItems.size());
        });
    }

    private void postDropdownRequest(String endpoint, JSONObject body,
                                     String expectedListName, DropdownResultCallback callback) {
        if (TextUtils.isEmpty(token == null ? "" : token.trim())) {
            Toast.makeText(this, "Session token missing. Please log in again.", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            Log.d(TAG, "Calling endpoint: " + endpoint + ", body: " + body);
            ApiClient.post(
                    this,
                    endpoint,
                    token,
                    body,
                    response -> runOnUiThread(() -> {
                        Log.d(TAG, endpoint + " response: " + response);
                        List<DropdownItem> items = parseDropdownItems(response, expectedListName);
                        callback.onSuccess(items);
                        if (items.isEmpty()) {
                            Toast.makeText(this, "No options found for " + expectedListName + ".",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }),
                    err -> runOnUiThread(() -> {
                        Log.e(TAG, "API request failed: " + endpoint + " | " + String.valueOf(err));
                        Toast.makeText(this, "Request failed for " + expectedListName + ". Check Logcat.",
                                Toast.LENGTH_LONG).show();
                    })
            );
        } catch (Exception e) {
            Log.e(TAG, "Request failed: " + endpoint, e);
            Toast.makeText(this, "Request failed: " + endpoint, Toast.LENGTH_SHORT).show();
        }
    }

    private List<DropdownItem> parseDropdownItems(JSONObject response, String expectedListName) {
        List<DropdownItem> result = new ArrayList<>();
        if (response == null) return result;

        JSONArray array = null;
        Object data = response.opt("data");

        if (data instanceof JSONArray) {
            array = (JSONArray) data;
        } else if (data instanceof JSONObject) {
            JSONObject dataObject = (JSONObject) data;
            String[] keys;

            if ("states".equals(expectedListName)) {
                keys = new String[]{"states", "state", "list", "result", "items"};
            } else if ("cities".equals(expectedListName)) {
                keys = new String[]{"city", "cities", "list", "result", "items"};
            } else if ("plans".equals(expectedListName)) {
                keys = new String[]{"plans", "plan", "get_plans", "list", "result", "items"};
            } else {
                keys = new String[]{"downline_type", "levels", "user_levels", "downline_user_level",
                        "user_types", "user_type", "level", "list", "result", "items"};
            }

            for (String key : keys) {
                Object candidate = dataObject.opt(key);
                if (candidate instanceof JSONArray) {
                    array = (JSONArray) candidate;
                    break;
                }
            }
        }

        if (array == null) {
            String[] keys = {expectedListName, "states", "city", "cities", "downline_type",
                    "plans", "plan", "levels", "list", "items", "result"};
            for (String key : keys) {
                Object candidate = response.opt(key);
                if (candidate instanceof JSONArray) {
                    array = (JSONArray) candidate;
                    break;
                }
            }
        }

        if (array == null) {
            Log.e(TAG, "No array found for " + expectedListName + " in response: " + response);
            return result;
        }

        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item == null) continue;

            String itemId = firstNonEmpty(item, "id", "state_id", "city_id", "plan_id",
                    "user_type_id", "user_level_id", "level_id", "value");
            String label;
            String userLevel = item.optString("user_level", "");

            if ("states".equals(expectedListName)) {
                label = firstNonEmpty(item, "state_name", "name", "label", "title", "display_name");
            } else if ("cities".equals(expectedListName)) {
                label = firstNonEmpty(item, "name", "city_name", "label", "title", "display_name");
            } else if ("plans".equals(expectedListName)) {
                label = firstNonEmpty(item, "plan_name", "name", "title", "label", "display_name");
            } else {
                label = firstNonEmpty(item, "user_type_name", "level_name", "user_type",
                        "name", "title", "label", "display_name", "type");
            }

            if (label.isEmpty()) continue;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                label = Html.fromHtml(label, Html.FROM_HTML_MODE_LEGACY).toString().trim();
            } else {
                label = Html.fromHtml(label).toString().trim();
            }
            if (itemId.isEmpty()) itemId = label;

            if ("downline_type".equals(expectedListName)) {
                result.add(new DropdownItem(itemId, label, userLevel));
            } else {
                result.add(new DropdownItem(itemId, label));
            }
        }

        return result;
    }

    private void submitAddUser() {
        if (submitting) return;

        String name = getText(etName);
        String fatherName = getText(etFatherName);
        String email = getText(etEmail);
        String mobile = getText(etMobile);
        String dob = getText(etDob);
        String aadhar = getText(etAadhar);
        String pan = getText(etPan).toUpperCase(Locale.ROOT);
        String address = getText(etAddress);
        String pincode = getText(etPincode);
        String firmname = getText(etFirmName);
        String gender = getSelectedGender();

        if (TextUtils.isEmpty(selectedUserLevel)) {
            showValidationError("Please select a user type.");
            return;
        }
        if (TextUtils.isEmpty(selectedPlanId)) {
            showValidationError("Please select a plan.");
            return;
        }
        if (TextUtils.isEmpty(name)) {
            showValidationError("Please enter full name.");
            return;
        }
        // Father Name validation
        if (TextUtils.isEmpty(fatherName)) {
            showValidationError("Please enter father's name.");
            return;
        }
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showValidationError("Please enter a valid email address.");
            return;
        }
        if (!mobile.matches("[0-9]{10}")) {
            showValidationError("Please enter a valid 10-digit mobile number.");
            return;
        }

        // Gender validation
        if (TextUtils.isEmpty(gender)) {
            showValidationError("Please select gender.");
            return;
        }

// Date of Birth validation
        if (TextUtils.isEmpty(dob)) {
            showValidationError("Please select date of birth.");
            return;
        }

// Aadhaar validation
        if (TextUtils.isEmpty(aadhar)) {
            showValidationError("Please enter Aadhaar number.");
            return;
        }

        if (!aadhar.matches("[0-9]{12}")) {
            showValidationError("Aadhaar number must be 12 digits.");
            return;
        }

// PAN validation
        if (TextUtils.isEmpty(pan)) {
            showValidationError("Please enter PAN number.");
            return;
        }

        if (!pan.matches("[A-Z]{5}[0-9]{4}[A-Z]")) {
            showValidationError("Please enter a valid PAN number.");
            return;
        }

// Address validation
        if (TextUtils.isEmpty(address)) {
            showValidationError("Please enter full address.");
            return;
        }

// State validation
        if (TextUtils.isEmpty(selectedStateId)) {
            showValidationError("Please select a state.");
            return;
        }

// City validation
        if (TextUtils.isEmpty(selectedCityId)) {
            showValidationError("Please select a city.");
            return;
        }

// Pincode validation
        if (TextUtils.isEmpty(pincode)) {
            showValidationError("Please enter pincode.");
            return;
        }

        if (!pincode.matches("[0-9]{6}")) {
            showValidationError("Pincode must be 6 digits.");
            return;
        }




        Log.d(TAG, "users/save - userType: " + selectedUserLevel);
        Log.d(TAG, "users/save - name entered: " + !name.isEmpty());
        Log.d(TAG, "users/save - email entered: " + !email.isEmpty());
        Log.d(TAG, "users/save - mobile length: " + mobile.length());
        Log.d(TAG, "users/save - plan_id: " + selectedPlanId);
        Log.d(TAG, "users/save - aadhar length: " + aadhar.length());
        Log.d(TAG, "users/save - pan entered: " + !pan.isEmpty());
        Log.d(TAG, "users/save - father_name entered: " + !fatherName.isEmpty());
        Log.d(TAG, "users/save - gender: " + gender);
        Log.d(TAG, "users/save - dob entered: " + !dob.isEmpty());
        Log.d(TAG, "users/save - address entered: " + !address.isEmpty());
        Log.d(TAG, "users/save - address entered: " + !firmname.isEmpty());
        Log.d(TAG, "users/save - state: " + selectedStateId);
        Log.d(TAG, "users/save - city: " + selectedCityId);
        Log.d(TAG, "users/save - pincode length: " + pincode.length());

        JSONObject body = new JSONObject();
        try {
            // Names match the users/save fields shown in your Postman screenshot.
            // userType must contain the selected dropdown item's user_level.
            body.put("user_type", selectedUserLevel);
            body.put("name", name);
            body.put("email", email);
            body.put("mobile", mobile);
            body.put("plan_id", selectedPlanId);
            body.put("aadhar", aadhar);
            body.put("pan", pan);
            body.put("father_name", fatherName);
            body.put("gender", gender);
            body.put("dob", dob);
            body.put("address", address);
            body.put("firm_name", etFirmName);
            body.put("state", selectedStateId);
            body.put("city", selectedCityId);
            body.put("pincode", pincode);
            body.put("min_balance", "0");


        } catch (Exception e) {
            Log.e(TAG, "Failed to prepare users/save request body", e);
            Toast.makeText(this, "Unable to prepare user details.", Toast.LENGTH_SHORT).show();
            return;
        }

        setSubmitting(true);
        // Do not log the request body; it contains personal and KYC information.
        Log.d(TAG, "Submitting users/save request.");

        // Uses the project's existing ApiClient helper and relative endpoint.
        // Check ApiClient.post's Content-Type: Postman shows x-www-form-urlencoded for users/save.
        ApiClient.post(
                this,
                "users/save",
                token,
                body,
                response -> runOnUiThread(() -> {
                    setSubmitting(false);
                    Log.d(TAG, "users/save response: " + response);
                    int status = response == null ? 0 : response.optInt("status", 0);
                    String message = response == null
                            ? "Invalid server response"
                            : response.optString("message", "");

                    if (status == 1) {
                        Toast.makeText(this,
                                TextUtils.isEmpty(message) ? "User added successfully." : message,
                                Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(this,
                                TextUtils.isEmpty(message) ? "Unable to add user." : message,
                                Toast.LENGTH_LONG).show();
                    }
                }),
                err -> runOnUiThread(() -> {
                    setSubmitting(false);
                    Log.e(TAG, "users/save failed: " + String.valueOf(err));
                    Toast.makeText(this, "Unable to add user. Check Logcat.", Toast.LENGTH_LONG).show();
                })
        );
    }

    private String getSelectedGender() {
        int checkedId = genderToggle.getCheckedButtonId();

        if (checkedId == R.id.btnMale) return "Male";
        if (checkedId == R.id.btnFemale) return "Female";
        if (checkedId == R.id.btnOther) return "Other";

        return "";
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private void showValidationError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void setSubmitting(boolean value) {
        submitting = value;
        if (btnSubmit != null) btnSubmit.setEnabled(!value);
        if (loadingOverlay != null) loadingOverlay.setVisibility(value ? View.VISIBLE : View.GONE);
    }

    private String firstNonEmpty(JSONObject object, String... keys) {
        for (String key : keys) {
            String value = object.optString(key, "");
            if (value != null && !value.trim().isEmpty() && !"null".equalsIgnoreCase(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private ArrayAdapter<DropdownItem> makeAdapter(List<DropdownItem> items) {
        return new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line,
                new ArrayList<>(items));
    }
}
