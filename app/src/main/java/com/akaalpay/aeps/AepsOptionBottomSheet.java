package com.akaalpay.aeps;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.akaalpay.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class AepsOptionBottomSheet extends BottomSheetDialogFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Layout load ho raha hai
        return inflater.inflate(R.layout.bottomsheet_aeps_option, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Cut Button (Ab band hoga)
        view.findViewById(R.id.ivClose).setOnClickListener(v -> dismiss());

//        // 2. FINO Click
//        view.findViewById(R.id.layoutFino).setOnClickListener(v -> {
//            startActivity(new Intent(getActivity(), FinoEntryActivity.class));
//            dismiss();
//        });

        // 3. Airtel Click (Ab Activity pe jayega)
        view.findViewById(R.id.layoutAirtel).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), AirtelEntryActivity.class));
            dismiss();
        });
    }
}