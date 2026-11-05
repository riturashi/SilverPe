package com.silverpe.report;

public class BbpsReportModel {

    public String id;

    // 👉 BBPS: canumber (Customer Number / CA Number)
    public String mobile;

    // 👉 BBPS: biller_name
    public String aadhar;

    // 👉 BBPS: amount
    public String amount;

    // 👉 BBPS: category (FASTag, Electricity, etc.)
    public String type;

    // 👉 BBPS: reference_id
    public String rrn;

    // 👉 BBPS: ackno (may be null)
    public String ackno;

    // 👉 BBPS: reference_id (same reuse)
    public String reference;

    // 👉 BBPS: status ("1" / "0" → int me convert Activity me)
    public int status;

    // 👉 BBPS: date
    public String date;

}