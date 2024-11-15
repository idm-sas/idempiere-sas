package id.co.databiz.sas.callout;

import id.co.databiz.sas.model.MRMA;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;

public class CalloutInOutRMA implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		if(value == null){
			return "";
		}
		Integer rmaID = (Integer) value;
		
		// SAS-99
		if (rmaID > 0) {
			MRMA rma = new MRMA(ctx, rmaID, null);
			mTab.setValue("AD_OrgTrx_ID", rma.get_Value("AD_OrgTrx_ID"));
			mTab.setValue("C_BPartner_Location_ID", rma.get_Value("C_BPartner_Location_ID"));
			mTab.setValue("SalesRep_ID", rma.getSalesRep_ID());
		}
		
		return "";
	}

}
