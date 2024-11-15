package id.co.databiz.awn.callout;

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
			// AWN-93
			MBankAccount bankAccount = new MBankAccount(ctx, bankAccountID, null);
			mTab.setValue("C_Currency_ID", bankAccount.getC_Currency_ID());
		}
		return "";
	}

}
