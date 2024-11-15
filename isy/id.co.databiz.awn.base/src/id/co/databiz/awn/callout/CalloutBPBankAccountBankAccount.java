package id.co.databiz.awn.callout;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MBankAccount;

public class CalloutBPBankAccountBankAccount implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		if(value==null){
			return "";
		}
		Integer bankAccountID = (Integer) value;
		if(bankAccountID > 0){
			// ISY-28
			MBankAccount bankAccount = new MBankAccount(ctx, bankAccountID, null);
			mTab.setValue("BankAccountType", bankAccount.getBankAccountType());
			mTab.setValue("AccountNo", bankAccount.getAccountNo());
		}
		return "";
	}

}
