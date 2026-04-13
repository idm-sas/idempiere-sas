package id.co.databiz.sas.callout;

import id.co.databiz.sas.model.MInvoice;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MBankAccount;

public class CalloutPaymentInvoice implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		if(value==null){
			return "";
		}
		Integer paymentID = (Integer) value;

		if (paymentID > 0) {
			Integer invoiceID = (Integer) mTab.getValue("C_Invoice_ID");
			Integer bankAccountID = (Integer) mTab.getValue("C_BankAccount_ID");
			MInvoice invoice = new MInvoice(ctx, invoiceID, null);
			MBankAccount bankAccount = new MBankAccount(ctx, bankAccountID, null);
			
			if(invoice.getC_Activity_ID()>0)
				mTab.setValue("C_Activity_ID", invoice.getC_Activity_ID());
			
			mTab.setValue("C_Currency_ID", bankAccount.getC_Currency_ID());
		}
		return "";
	}
}
