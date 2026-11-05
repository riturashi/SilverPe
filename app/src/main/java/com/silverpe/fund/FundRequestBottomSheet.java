package com.silverpe.fund;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.silverpe.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class FundRequestBottomSheet extends BottomSheetDialogFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottomsheet_fund_request, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.cardPg).setOnClickListener(v -> {
            dismiss();
            startActivity(new Intent(getContext(), PgFundRequestActivity.class));
        });

        view.findViewById(R.id.cardManual).setOnClickListener(v -> {
            dismiss();
            startActivity(new Intent(getContext(), FundRequestActivity.class));
        });
    }
}