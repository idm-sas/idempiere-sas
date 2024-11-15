package id.co.databiz.awn.callout;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MOrder;

public class CalloutInvoiceOrder implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		if(value==null){
			return "";
		}
		Integer orderID = (Integer) value;
		if(orderID>0){
			// AWN-30 AWN-32
			MOrder order = new MOrder(ctx, orderID, null);
			mTab.setValue("C_BPartner_ID", order.getC_BPartner_ID());
			if(order.getBill_Location_ID()>0){
				mTab.setValue("C_BPartner_Location_ID", order.getBill_Location_ID());
			} else {
				mTab.setValue("C_BPartner_Location_ID", order.getC_BPartner_Location_ID());
			}
			mTab.setValue("POReference", order.getPOReference());
		}
		return "";
	}

}
