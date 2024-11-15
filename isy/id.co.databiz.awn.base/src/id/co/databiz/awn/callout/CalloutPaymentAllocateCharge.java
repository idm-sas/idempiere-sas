package id.co.databiz.awn.callout;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MCharge;

public class CalloutPaymentAllocateCharge implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		if(value==null){
			return "";
		}
		Integer chargeID = (Integer) value;
		if(chargeID>0){
			// AWN-94
			MCharge charge = new MCharge(ctx, chargeID, null);
			mTab.setValue("Amount", charge.getChargeAmt());
		}
		return "";
	}

}
