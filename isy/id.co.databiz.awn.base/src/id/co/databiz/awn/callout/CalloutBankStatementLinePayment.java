package id.co.databiz.awn.callout;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MPayment;

public class CalloutBankStatementLinePayment implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		if(value==null){
			return "";
		}
		Integer paymentID = (Integer) value;
		if(paymentID>0){
			// AWN-246
			MPayment payment = new MPayment(ctx, paymentID, null);
			mTab.setValue("C_ConversionType_ID", payment.getC_ConversionType_ID());
		}
		return "";
	}

}
