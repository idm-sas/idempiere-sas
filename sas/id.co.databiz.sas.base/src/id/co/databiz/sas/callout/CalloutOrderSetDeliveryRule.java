package id.co.databiz.sas.callout;

import id.co.databiz.sas.SASSystemID;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MOrder;
import org.compiere.util.Env;

public class CalloutOrderSetDeliveryRule implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		if(value == null){
			return "";
		}
		int docTypeID = Env.getContextAsInt(ctx, WindowNo, "C_DocTypeTarget_ID");
		boolean isSOTrx = Env.getContext(ctx, WindowNo, "IsSOTrx").equalsIgnoreCase("Y");
		
		if(docTypeID > 0 && isSOTrx){
			// SAS-149 callout doctype set delivery rule to force if SO Return
			if (docTypeID == SASSystemID.DOCTYPE_SO_RETURN_A || 
					docTypeID == SASSystemID.DOCTYPE_SO_RETURN_B) {
				mTab.setValue("DeliveryRule", MOrder.DELIVERYRULE_Force);
			}
		}
		return "";
	}

}
