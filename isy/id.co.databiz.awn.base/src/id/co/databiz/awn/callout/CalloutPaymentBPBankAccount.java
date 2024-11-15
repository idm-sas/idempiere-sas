package id.co.databiz.awn.callout;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MBPBankAccount;

public class CalloutPaymentBPBankAccount implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		if(value==null){
			return "";
		}
		Integer bpBankAccountID = (Integer) value;
		if(bpBankAccountID > 0){
			// ISY-62
			MBPBankAccount bpBankAccount = new MBPBankAccount(ctx, bpBankAccountID, null);
			mTab.setValue("AccountNo", bpBankAccount.getAccountNo());
			mTab.setValue("A_Name", bpBankAccount.getA_Name());
		} else {
			mTab.setValue("AccountNo", "");
			mTab.setValue("A_Name", "");
		}
		return "";
	}

}
