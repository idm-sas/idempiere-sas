package id.co.databiz.awn.controller;

import java.util.Arrays;
import java.util.List;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MPayment;
import org.compiere.model.MPaymentAllocate;
import org.compiere.model.Query;
import org.compiere.model.X_C_Payment;
import org.compiere.util.Env;

public class PaymentController {
	
	public static void afterComplete(X_C_Payment payment){
		// AWN-94
		if(payment.getC_Invoice_ID()==0 && payment.getC_Charge_ID()==0){
			int count = new Query(Env.getCtx(), MPaymentAllocate.Table_Name, "C_Payment_ID = ? AND C_Charge_ID > 0", null)
							.setParameters(new Object[]{payment.get_ID()})
							.count();
			if(count>0){
				payment.setIsAllocated(true);
				payment.saveEx();
			}
		}
	}
	
	public static void reverse(MPayment payment) {
		// ISY-57
//		if (payment.getTenderType().equals(MPayment.TENDERTYPE_Check) && payment.get_ValueAsInt("C_PaymentCash_ID") > 0) {
//			MPayment paymentCash = new MPayment(payment.getCtx(), payment.get_ValueAsInt("C_PaymentCash_ID"), payment.get_TrxName());
//			if (!paymentCash.getDocStatus().equals(MPayment.DOCSTATUS_Voided) && !paymentCash.getDocStatus().equals(MPayment.DOCSTATUS_Reversed)) {
//				throw new AdempiereException("ISY-57 This document is already referenced to " + paymentCash.getDocumentNo());
//			}
//		}
		
		List<MPayment> giroList = new Query(Env.getCtx(), MPayment.Table_Name, "C_PaymentCash_ID = ?", payment.get_TrxName())
			.setParameters(payment.get_ID())
			.list();
		for (MPayment giro : giroList) {
			giro.set_ValueOfColumn("C_PaymentCash_ID", null);
			giro.set_ValueOfColumn("IsInTransit", false);
			giro.set_ValueOfColumn("IsCashed", false);
			giro.saveEx();
		}
		
		List<MPaymentAllocate> paList = Arrays.asList(MPaymentAllocate.get(payment));
		for (MPaymentAllocate pa : paList) {
			pa.set_ValueOfColumn("C_PaymentGiro_ID", null);
			pa.set_ValueOfColumn("CheckNo", null);
			pa.saveEx();
		}
	}
}
