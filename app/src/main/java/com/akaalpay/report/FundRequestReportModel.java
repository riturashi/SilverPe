package com.akaalpay.report;

public class FundRequestReportModel {
    public String requestId;
    public String bankName;
    public String accountNo;
    public String accountHolder;
    public String ifscCode;
    public String depositDate;
    public String paymentMode;
    public String amount;
    public String receiptNo;
    public String receiptImage;
    public String remark;
    public String adminRemark;
    public int status;       // 0 = Pending, 1 = Approved, 2 = Rejected
    public String date;
}