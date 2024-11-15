package id.co.databiz.sas.callout;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.util.Env;

import id.co.databiz.awn.model.MInOut;
import id.co.databiz.sas.SASSystemID;

public class CalloutRMAShipment implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value, Object oldValue) {
		
		if(mField.getAD_Window_ID()==SASSystemID.WINDOW_CUSTOMER_RMA) {
			if(value==null) return "";
			int M_InOut_ID = Env.getContextAsInt(ctx, WindowNo, mTab.getTabNo(),"InOut_ID");
			
			if(M_InOut_ID > 0) {
				MInOut mInOut = new MInOut(ctx, M_InOut_ID, null);
				mTab.setValue("C_BPartner_ID", mInOut.getC_BPartner_ID());
				mTab.setValue("C_BPartner_Location_ID", mInOut.getC_BPartner_Location_ID());
				mTab.setValue("SalesRep_ID", mInOut.getSalesRep_ID());
			}else {
				mTab.setValue("C_BPartner_ID",null);
				mTab.setValue("C_BPartner_Location_ID", null);
				mTab.setValue("SalesRep_ID",null);
			}
		}else {
			mTab.setValue("C_BPartner_ID",null);
			mTab.setValue("C_BPartner_Location_ID", null);
			mTab.setValue("SalesRep_ID",null);
		}
			return "";
	}

}
