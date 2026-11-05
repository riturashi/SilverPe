package com.akaalpay;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;

import com.akaalpay.utils.ApiClient;
import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationServices;

import org.json.JSONObject;

public class ProfileFragment extends Fragment {

    private TextView tvName, tvMobile, tvUserId, tvEmail, tvAddress, tvFirm;
    private ImageView imgProfile;
//    private MaterialButton btnFinoOnboarding;

    private String TOKEN = "";
    private double currentLat = 0.0;
    private double currentLng = 0.0;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        initViews(view);
        setupStatusBarInset(view);

        if (getActivity() != null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());
        }

        SharedPreferences pref = requireActivity().getSharedPreferences("BP_PREF", Context.MODE_PRIVATE);
        TOKEN = pref.getString("TOKEN", "");


        loadProfile();

//        btnFinoOnboarding.setOnClickListener(v -> handleOnboardingClick());

        return view;
    }

    // ---------- STATUS BAR ----------
    @Override
    public void onResume() {
        super.onResume();
        applyStatusBar();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) applyStatusBar();
    }

    private void applyStatusBar() {
        if (!isAdded() || getActivity() == null) return;

        Window window = requireActivity().getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.feno_primary));

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());
        controller.show(WindowInsetsCompat.Type.statusBars());
        // feno_primary yellow hai, isliye status bar ke icons dark (true) rakhe hain
        controller.setAppearanceLightStatusBars(true);
    }

    // Android 15+ (edge-to-edge) mein status bar ka colour set nahi hota.
    // Tab header khud status bar ke peeche tak failta hai, aur andar ka content niche khisakta hai.
    private void setupStatusBarInset(View root) {
        final View header = root.findViewById(R.id.profileHeader);
        if (header == null) return;

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            header.setPadding(header.getPaddingLeft(), top,
                    header.getPaddingRight(), header.getPaddingBottom());
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    private void initViews(View view) {
        imgProfile        = view.findViewById(R.id.imgProfile);
        tvName            = view.findViewById(R.id.tvName);
        tvMobile          = view.findViewById(R.id.tvMobile);
        tvUserId          = view.findViewById(R.id.tvUserId);
        tvEmail           = view.findViewById(R.id.tvEmail);
        tvAddress         = view.findViewById(R.id.tvAddress);
        tvFirm            = view.findViewById(R.id.tvFirm);
        //  btnFinoOnboarding = view.findViewById(R.id.btnFinoOnboarding);
    }

    private void loadProfile() {
        ApiClient.get(getActivity(), "user/my-profile", TOKEN, response -> {
            try {
                JSONObject data = response.getJSONObject("data");

                String name    = data.optString("name", "N/A");
                String mobile  = data.optString("mobile", "N/A");
                String userId  = data.optString("username", "N/A");
                String email   = data.optString("email_id", "N/A");
                String firm    = data.optString("firm_name", "N/A");
                String image   = data.optString("profile_image", "");
                int onboardingStatus = data.optInt("onboarding_status", 0);

                String address = "Address not updated";
                if (data.has("profile_details")) {
                    JSONObject pd = data.optJSONObject("profile_details");
                    if (pd != null) address = pd.optString("address", address);
                }

                String finalAddress = address;

                if (!isAdded() || getActivity() == null) return;

                getActivity().runOnUiThread(() ->
                        updateUI(name, mobile, userId, email, firm, finalAddress, image, onboardingStatus)
                );

            } catch (Exception ignored) {}

        }, error -> {
            if (!isAdded() || getActivity() == null) return;
            getActivity().runOnUiThread(() ->
                    Toast.makeText(getContext(), "Server error. Please try later.", Toast.LENGTH_SHORT).show()
            );
        });
    }

    private void updateUI(String name, String mobile, String userId, String email,
                          String firm, String address, String image, int status) {

        tvName.setText(name);
        tvMobile.setText("+91 " + mobile);
        tvUserId.setText(userId);
        tvEmail.setText(email);
        tvFirm.setText(firm);
        tvAddress.setText(address);

        if (!image.isEmpty() && isAdded() && getContext() != null) {
            Glide.with(getContext())
                    .load(image)
                    .circleCrop()
                    .placeholder(R.drawable.ic_user)
                    .into(imgProfile);
        }

        // btnFinoOnboarding.setVisibility(status == 1 ? View.GONE : View.VISIBLE);
    }

//    private void handleOnboardingClick() {
////        btnFinoOnboarding.setEnabled(false);
////        btnFinoOnboarding.setText("Fetching Location...");
//        checkLocationPermission();
//    }

//    private void checkLocationPermission() {
//        if (!isAdded()) return;
//
//        if (ContextCompat.checkSelfPermission(requireContext(),
//                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
//            resetButton();
//        } else {
//            checkGpsEnabled();
//        }
//    }
//
//    private void checkGpsEnabled() {
//        if (!isAdded()) return;
//
//        LocationManager lm = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);
//
//        if (lm != null && lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
//            fetchLocation();
//        } else {
//            resetButton();
//            Toast.makeText(getContext(), "Please enable GPS", Toast.LENGTH_SHORT).show();
//            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
//        }
//    }

//    @SuppressLint("MissingPermission")
//    private void fetchLocation() {
//        if (!isAdded()) return;
//
//        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
//            if (!isAdded()) return;
//
//            if (location != null) {
//                currentLat = location.getLatitude();
//                currentLng = location.getLongitude();
//                callOnboardingAPI();
//            } else {
//                requestFreshLocation();
//            }
//        });
//    }

//    @SuppressLint("MissingPermission")
//    private void requestFreshLocation() {
//        if (!isAdded()) return;
//
////        requireActivity().runOnUiThread(() ->
////                btnFinoOnboarding.setText("Getting GPS Fix...")
////        );
//
//        LocationRequest locationRequest = LocationRequest.create()
//                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
//                .setInterval(1000)
//                .setNumUpdates(1);
//
//        locationCallback = new LocationCallback() {
//            @Override
//            public void onLocationResult(@NonNull LocationResult result) {
//                if (!isAdded()) return;
//
//                fusedLocationClient.removeLocationUpdates(this);
//
//                android.location.Location loc = result.getLastLocation();
//
//                if (loc != null) {
//                    currentLat = loc.getLatitude();
//                    currentLng = loc.getLongitude();
//                    callOnboardingAPI();
//                } else {
//                    resetButton();
//                    if (getActivity() != null) {
//                        getActivity().runOnUiThread(() ->
//                                Toast.makeText(getContext(),
//                                        "Location fix failed. Please try again.",
//                                        Toast.LENGTH_SHORT).show()
//                        );
//                    }
//                }
//            }
//        };
//
//        fusedLocationClient.requestLocationUpdates(
//                locationRequest,
//                locationCallback,
//                requireActivity().getMainLooper()
//        );
//    }

//    private void callOnboardingAPI() {
//        if (!isAdded() || getActivity() == null) return;
//
////        getActivity().runOnUiThread(() ->
////                btnFinoOnboarding.setText("Please wait...")
////        );
//
//        try {
//            JSONObject body = new JSONObject();
//            body.put("lat", currentLat);
//            body.put("lng", currentLng);
//
//            ApiClient.post(getActivity(), "user/onboarding", TOKEN, body, response -> {
//                try {
//                    if (!isAdded() || getActivity() == null) return;
//
//                    int status = response.optInt("status", 0);
//
//                    if (status == 1) {
//                        JSONObject data = response.optJSONObject("data");
//
//                        String merchantId = "";
//                        if (data != null) {
//                            merchantId = data.optString("merchant_id", "");
//                        }
//                        if (merchantId.isEmpty()) {
//                            merchantId = tvUserId.getText().toString();
//                        }
//
//                        final String finalMerchantId = merchantId;
//                        final String message = response.optString("message", "Proceeding...");
//
//                        getActivity().runOnUiThread(() ->
//                                showDialog("Success", message, () ->
//                                        openSDK(finalMerchantId)
//                                )
//                        );
//
//                    } else {
//                        getActivity().runOnUiThread(() -> {
//                            resetButton();
//                            showDialog("Failed", response.optString("message", "Onboarding failed"));
//                        });
//                    }
//
//                } catch (Exception e) {
//                    if (!isAdded() || getActivity() == null) return;
//                    getActivity().runOnUiThread(() -> {
//                        resetButton();
//                        Toast.makeText(getContext(), "Something went wrong.", Toast.LENGTH_SHORT).show();
//                    });
//                }
//
//            }, error -> {
//                if (!isAdded() || getActivity() == null) return;
//                getActivity().runOnUiThread(() -> {
//                    resetButton();
//                    Toast.makeText(getContext(), "Server error. Try again.", Toast.LENGTH_SHORT).show();
//                });
//            });
//
//        } catch (Exception e) {
//            resetButton();
//        }
//    }

//    private void openSDK(String merchantId) {
//        if (!isAdded()) return;
//
//        Intent intent = new Intent(requireActivity(), HostActivity.class);
//        intent.putExtra("pId",     "PS00443");
//        intent.putExtra("pApiKey", "UFMwMDQ0MzdmMjViMTljODk2MmY0ZTI5MDIwNjE3NGVlYzVhNzdk");
//        intent.putExtra("mCode",   merchantId);
//        intent.putExtra("mobile",  tvMobile.getText().toString().replace("+91 ", ""));
//        intent.putExtra("email",   tvEmail.getText().toString());
//        intent.putExtra("firm",    tvFirm.getText().toString());
//        intent.putExtra("pipe",    "bank2");
//        intent.putExtra("lat",     String.valueOf(currentLat));
//        intent.putExtra("lng",     String.valueOf(currentLng));
//
//        startActivityForResult(intent, 1111); //
//        resetButton();
//    }
//    @Override
//    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        if (requestCode == 1111) {
//            if (resultCode == Activity.RESULT_OK && data != null) {
//
//                boolean status = data.getBooleanExtra("status", false);
//                String message = data.getStringExtra("message");
//
//                if (!isAdded()) return;
//
//                new AlertDialog.Builder(requireContext())
//                        .setTitle(status ? "Success" : "Failed")
//                        .setMessage(message)
//                        .setPositiveButton("OK", (d, w) -> {
//                            d.dismiss();
////                            if (status) {
////                                btnFinoOnboarding.setVisibility(View.GONE);
////                            }
//                        })
//                        .setCancelable(false)
//                        .show();
//
//            } else {
//                if (!isAdded()) return;
//                Toast.makeText(getContext(), "Onboarding incomplete. Try again.", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }
//    private void showDialog(String title, String message) {
//        if (!isAdded()) return;
//        new AlertDialog.Builder(requireContext())
//                .setTitle(title)
//                .setMessage(message)
//                .setPositiveButton("OK", null)
//                .show();
//    }
//
//    private void showDialog(String title, String message, Runnable action) {
//        if (!isAdded()) return;
//        new AlertDialog.Builder(requireContext())
//                .setTitle(title)
//                .setMessage(message)
//                .setCancelable(false)
//                .setPositiveButton("Continue", (d, w) -> {
//                    d.dismiss();
//                    action.run();
//                })
//                .show();
//    }

//    private void resetButton() {
//        if (!isAdded() || getActivity() == null) return;
////        getActivity().runOnUiThread(() -> {
////            btnFinoOnboarding.setEnabled(true);
////            btnFinoOnboarding.setText("Complete Fino AEPS Onboarding");
////        });
//    }

//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//        if (fusedLocationClient != null && locationCallback != null) {
//            fusedLocationClient.removeLocationUpdates(locationCallback);
//        }
//    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
//                                           @NonNull int[] grantResults) {
//        if (!isAdded()) return;
//
//        if (requestCode == 101 && grantResults.length > 0
//                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//            checkGpsEnabled();
//        } else {
//            resetButton();
//            Toast.makeText(getContext(), "Location permission denied.", Toast.LENGTH_SHORT).show();
//        }
//    }
}