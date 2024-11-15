package id.co.databiz.sas.callout;

import id.co.databiz.awn.model.MPayment;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MBankAccount;

public class CalloutPaymentBankAccount implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		if(value==null){
			return "";
		}
		Integer bankAccountID = (Integer) value;
		if(bankAccountID>0){
			// SAS-150
			MBankAccount bankAccount = new MBankAccount(ctx, bankAccountID, null);
			if (bankAccount.getBankAccountType().equals(MBankAccount.BANKACCOUNTTYPE_Cash)) {
				mTab.setValue(MPayment.COLUMNNAME_TenderType, MPayment.TENDERTYPE_Cash);
			} else if (bankAccount.getBankAccountType().equals(MBankAccount.BANKACCOUNTTYPE_Checking)) {
				mTab.setValue(MPayment.COLUMNNAME_TenderType, MPayment.TENDERTYPE_Check);
			} else if (bankAccount.getBankAccountType().equals(MBankAccount.BANKACCOUNTTYPE_Card)) {
				mTab.setValue(MPayment.COLUMNNAME_TenderType, MPayment.TENDERTYPE_CreditCard);
			} else if (bankAccount.getBankAccountType().equals(MBankAccount.BANKACCOUNTTYPE_Savings)) {
				mTab.setValue(MPayment.COLUMNNAME_TenderType, MPayment.TENDERTYPE_Account);
			}
		}
		return "";
	}

}
