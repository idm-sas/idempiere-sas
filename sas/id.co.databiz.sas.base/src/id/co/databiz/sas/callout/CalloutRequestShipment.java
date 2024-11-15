package id.co.databiz.sas.callout;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MInOut;
import org.compiere.model.MOrder;

public class CalloutRequestShipment implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		if(value == null){
			return "";
		}
		Integer inOutID = (Integer) value;
		if(inOutID>0){
			// ISY-154
			MInOut inOut = new MInOut(ctx, inOutID, null);
			mTab.setValue("C_BPartner_ID", inOut.getC_BPartner_ID());
			
			if (inOut.getC_Order_ID() > 0) {
				MOrder order = new MOrder(ctx, inOut.getC_Order_ID(), null);
				mTab.setValue("PickListNote", order.get_Value("PickListNote"));
			}
		}
		return "";
	}

}
