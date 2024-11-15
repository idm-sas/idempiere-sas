package id.co.databiz.sas.callout;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.util.DB;
import org.compiere.util.Env;

public class CalloutSetSalesRep implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		if(value == null){
			return "";
		}
		boolean isSOTrx = mTab.getValueAsBoolean("IsSOTrx");
		int bpID = Env.getContextAsInt(ctx, WindowNo, "C_BPartner_ID");
		int orgTrxID = Env.getContextAsInt(ctx, WindowNo, "AD_OrgTrx_ID");
		Integer salesRepID = 0;
		
		// SAS-98 SAS-147 set sales rep from bp rule
		if (isSOTrx && bpID > 0 && orgTrxID > 0) {
			String sql = "SELECT MAX(bpr.SalesRep_ID) FROM SAS_BPRule bpr WHERE bpr.AD_OrgTrx_ID=? AND bpr.C_BPartner_ID=?";
			salesRepID = DB.getSQLValue(null, sql, orgTrxID, bpID);
		}
		
		if (salesRepID <= 0) {
			salesRepID = null;
		}
		
		mTab.setValue("SalesRep_ID", salesRepID);
		
		return "";
	}

}
