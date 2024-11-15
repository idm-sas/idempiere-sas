package id.co.databiz.awn.callout;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MOrder;

public class CalloutInOutOrder implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		if(value==null){
			return "";
		}
		Integer orderID = (Integer) value;
		if(orderID>0){
			// AWN-29 AWN-31
			MOrder order = new MOrder(ctx, orderID, null);
			mTab.setValue("Description", order.getDescription());
			mTab.setValue("POReference", order.getPOReference());
		}
		return "";
	}

}
