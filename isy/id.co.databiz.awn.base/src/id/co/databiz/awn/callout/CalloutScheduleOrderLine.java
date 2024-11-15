package id.co.databiz.awn.callout;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MOrderLine;

public class CalloutScheduleOrderLine implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		if(value==null){
			return "";
		}
		Integer orderLineID = (Integer) value;
		if(orderLineID > 0){
			MOrderLine orderLine = new MOrderLine(ctx, orderLineID, null);
			mTab.setValue("M_Product_ID", orderLine.getM_Product_ID());
			mTab.setValue("C_UOM_ID", orderLine.getC_UOM_ID());
			mTab.setValue("QtyEntered", orderLine.getQtyEntered());
			mTab.setValue("Qty", orderLine.getQtyOrdered());
			mTab.setValue("DatePromised", orderLine.getDatePromised());
		}
		return "";
	}

}
