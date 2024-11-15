package id.co.databiz.awn.callout;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MOrderLine;

public class CalloutInOutLineOrderLine implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		if(value==null){
			return "";
		}
		Integer orderLineID = (Integer) value;
		if(orderLineID>0){
			// AWN-29 AWN-31
			MOrderLine orderLine = new MOrderLine(ctx, orderLineID, null);
			mTab.setValue("Description", orderLine.getDescription());
		}
		return "";
	}

}
