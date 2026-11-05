package com.akaalpay.payout;

import androidx.annotation.NonNull;

/**
 * BankModel Work Updated as per Server JSON
 */
public class BankModel {
    // Hamne bank_id ko primary rakha hai kyunki server wahi bhej raha hai
    public String bank_name, account_no, ifsc, holder_name, bank_id, bene_id;
    public int acc_status;

    /**
     * Constructor 1: Payout List ke liye (Jo banks already added hain)
     */
    public BankModel(String bank_name, String account_no, String ifsc, String holder_name, String bene_id, int acc_status) {
        this.bank_name = bank_name;
        this.account_no = account_no;
        this.ifsc = ifsc;
        this.holder_name = holder_name;
        this.bene_id = bene_id; // Added bank ki ID yahan bene_id ke naam se aati hai
        this.acc_status = acc_status;
    }

    /**
     * Constructor 2: Bank Selection Dropdown ke liye (Server JSON Response match)
     * JSON mein "bank_id" aa raha hai, isliye hum usey yahan map kar rahe hain.
     */
    public BankModel(String bank_id, String bank_name, String ifsc) {
        this.bank_id = bank_id;
        this.bank_name = bank_name;
        this.ifsc = ifsc;
        // Safety ke liye bank_id ko hi bene_id mein dal dete hain
        this.bene_id = bank_id;
    }

    /**
     * Ye method zaroori hai dropdown mein bank ka naam dikhane ke liye
     */
    @NonNull
    @Override
    public String toString() {
        return bank_name != null ? bank_name : "";
    }
}