package com.akaalpay.distributer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.akaalpay.R;
import com.akaalpay.utils.ApiClient;
import com.akaalpay.utils.StatusBarUtils;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class UsersAllActivity extends AppCompatActivity {

    private static final String TAG = "UsersAllActivity";
    private static final String PREFS_NAME = "BP_PREF";
    private static final String TOKEN_KEY = "TOKEN";

    // Postman says userType should contain the logged-in user's user_level.
    private static final String USER_LEVEL_KEY = "user_level";
    private static final String USER_TYPE_PREF_KEY = "user_type_id";

    private ImageView btnBack;
    //private TextView tvCount;
    private TextView tvEmpty;
    //private TextView tvSubtitle;
    private ProgressBar progressBar;
    private Button btnRetry;
    private Button btnSearch;
    private TextInputEditText etSearch;
    private RecyclerView recyclerUsers;

    private UserAdapter userAdapter;

    private String token = "";
    private String userTypeLevel = "";
    private String activeSearch = "";

    private boolean requestInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users_all);

        StatusBarUtils.setupStatusBar(this, R.id.header_all);

        btnBack = findViewById(R.id.btnBack);
        //tvCount = findViewById(R.id.tvCount);
        tvEmpty = findViewById(R.id.tvEmpty);
       // tvSubtitle = findViewById(R.id.tvSubtitle);
        progressBar = findViewById(R.id.progressBar);
        btnRetry = findViewById(R.id.btnRetry);
        btnSearch = findViewById(R.id.btnSearch);
        etSearch = findViewById(R.id.etSearch);
        recyclerUsers = findViewById(R.id.recyclerUsers);

        recyclerUsers.setLayoutManager(new LinearLayoutManager(this));
        recyclerUsers.setHasFixedSize(false);

        userAdapter = new UserAdapter(this);
        recyclerUsers.setAdapter(userAdapter);

        btnBack.setOnClickListener(v -> finish());
        btnRetry.setOnClickListener(v -> loadUsers());
        btnSearch.setOnClickListener(v -> performSearch());

        // Keyboard Search button
        etSearch.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        SharedPreferences preferences =
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        token = preferences.getString(TOKEN_KEY, "");

        userTypeLevel = preferences.getString(USER_LEVEL_KEY, "");

        // Compatibility fallback for existing login preferences.
        if (TextUtils.isEmpty(userTypeLevel == null ? "" : userTypeLevel.trim())) {
            userTypeLevel = preferences.getString(USER_TYPE_PREF_KEY, "");
        }

        if (TextUtils.isEmpty(token == null ? "" : token.trim())) {
            showError("Session token missing. Please log in again.");
            return;
        }

        if (TextUtils.isEmpty(userTypeLevel == null ? "" : userTypeLevel.trim())) {
            showError("User level missing. Check login preferences.");
            return;
        }

        // Empty search loads all users.
        loadUsers();
    }

    private void performSearch() {

        if (requestInProgress) {
            return;
        }

        activeSearch = etSearch.getText() == null
                ? ""
                : etSearch.getText().toString().trim();

        // Hide keyboard after Search button click.
        InputMethodManager imm =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        if (imm != null) {
            imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
        }

        etSearch.clearFocus();

        // Call the same API with the entered Search value.
        loadUsers();
    }

    private void loadUsers() {

        if (requestInProgress) {
            return;
        }

        if (TextUtils.isEmpty(token == null ? "" : token.trim())) {
            showError("Session token missing. Please log in again.");
            return;
        }

        if (TextUtils.isEmpty(userTypeLevel == null ? "" : userTypeLevel.trim())) {
            showError("User level missing. Check login preferences.");
            return;
        }

        JSONObject requestBody = new JSONObject();

        try {
            // Keys match the Postman request.
            requestBody.put("userType", userTypeLevel.trim());
            requestBody.put("search", activeSearch);

            Log.d(TAG, "Search parameter = [" + activeSearch + "]");
            Log.d(TAG, "Request body = " + requestBody.toString());

        } catch (JSONException e) {
            Log.e(TAG, "Unable to prepare users/all request", e);
            showError("Unable to prepare users request.");
            return;
        }

        requestInProgress = true;

        progressBar.setVisibility(View.VISIBLE);
        btnRetry.setVisibility(View.GONE);
        btnSearch.setEnabled(false);
        tvEmpty.setVisibility(View.GONE);
        recyclerUsers.setVisibility(View.GONE);

        userAdapter.submitUsers(new ArrayList<>());

//        tvCount.setText(
//                TextUtils.isEmpty(activeSearch)
//                        ? "Loading users..."
//                        : "Searching users..."
//        );

        // Do not log the search string or user personal information.
        Log.d(TAG, "Calling users/all. Search length = "
                + activeSearch.length());

        ApiClient.post(
                this,
                "users/all",
                token,
                requestBody,

                response -> runOnUiThread(() -> {

                    finishRequest();

                    if (response == null) {
                        showError("Invalid server response.");
                        return;
                    }

                    int status = response.optInt("status", 0);

                    Log.d(TAG, "users/all status = " + status);

                    if (status != 1) {
                        String message = decodeHtml(
                                response.optString(
                                        "message",
                                        "Unable to load users."
                                )
                        );

                        showError(
                                TextUtils.isEmpty(message)
                                        ? "Unable to load users."
                                        : message
                        );
                        return;
                    }

                    JSONArray usersArray = extractUsersArray(response);

                    if (usersArray == null) {
                        Log.e(TAG,
                                "Could not find data.users.data array.");

                        showError(
                                "Users list was not found in the server response."
                        );
                        return;
                    }

                    List<JSONObject> userList = new ArrayList<>();

                    for (int i = 0; i < usersArray.length(); i++) {
                        JSONObject user = usersArray.optJSONObject(i);

                        if (user != null) {
                            userList.add(user);
                        }
                    }

                    userAdapter.submitUsers(userList);

                    //tvCount.setText("Total users: " + userList.size());

//                    tvSubtitle.setText(
//                            TextUtils.isEmpty(activeSearch)
//                                    ? "User account details"
//                                    : "Search results"
//                    );

                    if (userList.isEmpty()) {

                        tvEmpty.setText(
                                TextUtils.isEmpty(activeSearch)
                                        ? "No users found."
                                        : "No users match your search."
                        );

                        tvEmpty.setVisibility(View.VISIBLE);
                        recyclerUsers.setVisibility(View.GONE);
                        btnRetry.setVisibility(View.GONE);

                    } else {

                        tvEmpty.setVisibility(View.GONE);
                        recyclerUsers.setVisibility(View.VISIBLE);
                        btnRetry.setVisibility(View.GONE);
                    }
                }),

                error -> runOnUiThread(() -> {

                    finishRequest();

                    Log.e(TAG,
                            "users/all request failed: "
                                    + String.valueOf(error));

                    showError(
                            "Unable to load users. Check connection and try again."
                    );
                })
        );
    }

    private void finishRequest() {
        requestInProgress = false;

        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }

        if (btnSearch != null) {
            btnSearch.setEnabled(true);
        }
    }

    private JSONArray extractUsersArray(JSONObject response) {

        JSONObject data = response.optJSONObject("data");

        if (data == null) {
            return null;
        }

        // Postman response: data -> users -> data -> array
        JSONObject usersObject = data.optJSONObject("users");

        if (usersObject != null) {
            JSONArray nestedUsers = usersObject.optJSONArray("data");

            if (nestedUsers != null) {
                return nestedUsers;
            }
        }


        // Fallback for a different response structure.
        return data.optJSONArray("data");
    }

    private String decodeHtml(String value) {

        if (value == null) {
            return "";
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(
                    value,
                    Html.FROM_HTML_MODE_LEGACY
            ).toString().trim();
        }

        return Html.fromHtml(value).toString().trim();
    }

    private void showError(String message) {

        requestInProgress = false;

        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }

        if (btnSearch != null) {
            btnSearch.setEnabled(true);
        }

        if (recyclerUsers != null) {
            recyclerUsers.setVisibility(View.GONE);
        }

        if (tvEmpty != null) {
            tvEmpty.setText(message);
            tvEmpty.setVisibility(View.VISIBLE);
        }

        if (btnRetry != null) {
            btnRetry.setVisibility(View.VISIBLE);
        }

//        if (tvCount != null) {
//            tvCount.setText("All Users");
//        }
    }
}