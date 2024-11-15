package id.co.databiz.sas.callout;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.util.Env;

public class CalloutOrderAffectPromo implements IColumnCallout{
	
	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value, Object oldValue) {
		int doctypeID = Env.getContextAsInt(ctx, WindowNo, "C_DocTypeTarget_ID");
				
		if(doctypeID > 0) {
			if (doctypeID == 550265 || doctypeID == 1000030 || doctypeID == 550269 || 
				doctypeID == 1000026 || doctypeID == 550270 || doctypeID == 1000027) {
			
				mTab.setValue("IsAffectPromo", true);
			}
			else {
				mTab.setValue("IsAffectPromo", false);
			}			
		}
		return "";
	}

}