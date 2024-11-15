package id.co.databiz.awn.controller;

import id.co.databiz.awn.model.wrapper.ICPaymentAllocate;

import java.math.BigDecimal;

import org.adempiere.model.POWrapper;
import org.compiere.model.MPayment;
import org.compiere.model.MPaymentAllocate;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;


public class PaymentAllocateController {
	
	public static void afterNew(MPaymentAllocate paymentAllocate){
		validate(paymentAllocate);
	}
	
	public static void beforeChange(MPaymentAllocate paymentAllocate){
		validate(paymentAllocate);
	}
	
	public static void validate(MPaymentAllocate paymentAllocate){
		ICPaymentAllocate paymentAllocateCustom = POWrapper.create(paymentAllocate, ICPaymentAllocate.class);
		MPayment payment = new MPayment(Env.getCtx(), paymentAllocate.getC_Payment_ID(), paymentAllocate.get_TrxName());
		if(paymentAllocate.getC_Invoice_ID()>0){
			int chargeCount = DB.getSQLValue(null, "SELECT COUNT(pa.C_PaymentAllocate_ID) FROM C_PaymentAllocate pa WHERE pa.C_Payment_ID = ? AND pa.C_Charge_ID > 0", paymentAllocate.getC_Payment_ID());
			if(chargeCount>0){
				throw new IllegalStateException ("AWN-94\nPayment Allocate should be either invoice only or charge only");
			}
		}
		
		if(paymentAllocateCustom.getC_Charge_ID()>0){
			int invoiceCount = DB.getSQLValue(null, "SELECT COUNT(pa.C_PaymentAllocate_ID) FROM C_PaymentAllocate pa WHERE pa.C_Payment_ID = ? AND pa.C_Invoice_ID > 0", paymentAllocate.getC_Payment_ID());
			if(invoiceCount>0){
				throw new IllegalStateException ("AWN-94\nPayment Allocate should be either invoice only or charge only");
			}
		}
		
		// AWN-97 update PayAmt
		if (!payment.isProcessed()) {
			BigDecimal payAmt = new Query(Env.getCtx(), MPaymentAllocate.Table_Name, "IsActive='Y' AND C_Payment_ID = ? AND C_PaymentAllocate_ID <> ?", paymentAllocate.get_TrxName())
									.setParameters(new Object[]{paymentAllocate.getC_Payment_ID(),paymentAllocate.get_ID()})
									.sum("Amount");
			payment.setPayAmt(payAmt.add(paymentAllocate.getAmount()));
			payment.saveEx();
		}
		
		//ISY-62 set dimension to null if invoice
		if(paymentAllocate.getC_Invoice_ID() > 0){
			StringBuffer sqlUpdate = new StringBuffer();
			sqlUpdate.append("UPDATE C_PaymentAllocate SET User1_ID=NULL,User2_ID=NULL, ");
			sqlUpdate.append("C_Activity_ID=NULL,C_Campaign_ID=NULL,C_Project_ID=NULL,AD_OrgTrx_ID=NULL ");
			sqlUpdate.append("WHERE C_PaymentAllocate_ID = ? ");
			DB.executeUpdateEx(sqlUpdate.toString(), new Object[]{paymentAllocate.get_ID()}, paymentAllocate.get_TrxName());
		}
	}
}
