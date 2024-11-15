package id.co.databiz.sas.callout;

import id.co.databiz.sas.model.MBPRule;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MBPartner;
import org.compiere.model.MOrg;
import org.compiere.model.Query;
import org.compiere.util.Env;

public class CalloutOrderBPRule implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		if(value == null){
			return "";
		}
		int bpID = Env.getContextAsInt(ctx, WindowNo, "C_BPartner_ID");
		int orgTrxID = Env.getContextAsInt(ctx, WindowNo, "AD_OrgTrx_ID");
		boolean isSOTrx = Env.getContext(ctx, WindowNo, "IsSOTrx").equalsIgnoreCase("Y");
		
		if(bpID > 0 && orgTrxID > 0 && isSOTrx){
			MBPRule bprule = new Query(ctx, MBPRule.Table_Name, "C_BPartner_ID = ? AND AD_OrgTrx_ID = ?", null)
				.setOnlyActiveRecords(true)
				.setParameters(bpID, orgTrxID)
				.first();
			if (bprule != null) {
				if (bprule.getSalesRep_ID() > 0) {
					mTab.setValue("SalesRep_ID", bprule.getSalesRep_ID());
				}
				if (bprule.getC_PaymentTerm_ID() > 0) {
					mTab.setValue("C_PaymentTerm_ID", bprule.getC_PaymentTerm_ID());
				}
			} else {
				mTab.setValue("SalesRep_ID", null);
				mTab.fireDataStatusEEvent ("BP Rule not found", 
						MBPartner.get(ctx, bpID).getValue() + " - " + MOrg.get(ctx,orgTrxID).getName(), true);
			}
		}
		
		return "";
	}

}
