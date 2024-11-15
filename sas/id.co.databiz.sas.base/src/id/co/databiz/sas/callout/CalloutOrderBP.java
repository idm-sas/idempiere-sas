package id.co.databiz.sas.callout;

import id.co.databiz.sas.SASSystemID;
import id.co.databiz.sas.model.MBPRule;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MBPartner;
import org.compiere.model.MBPartnerLocation;
import org.compiere.model.MOrg;
import org.compiere.model.Query;
import org.compiere.util.Env;

public class CalloutOrderBP implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		if(value == null){
			return "";
		}
		Integer bpID = (Integer) value;
		int docTypeID = Env.getContextAsInt(ctx, WindowNo, "C_DocTypeTarget_ID");
		boolean isSOTrx = Env.getContext(ctx, WindowNo, "IsSOTrx").equalsIgnoreCase("Y");
		
		if(bpID > 0 && docTypeID > 0 && isSOTrx){
			// SAS-149 callout bp location based on istax
			boolean isA = false;
			boolean isB =false;
			if (docTypeID == SASSystemID.DOCTYPE_PK_A ||
					docTypeID == SASSystemID.DOCTYPE_REQUEST_STOCK_A ||
					docTypeID == SASSystemID.DOCTYPE_SO_RETURN_A ||
					docTypeID == SASSystemID.DOCTYPE_SO_A) {
				isA = true;
			}
			
			if (docTypeID == SASSystemID.DOCTYPE_PK_B ||
					docTypeID == SASSystemID.DOCTYPE_REQUEST_STOCK_B ||
					docTypeID == SASSystemID.DOCTYPE_SO_RETURN_B ||
					docTypeID == SASSystemID.DOCTYPE_SO_B) {
				isB = true;
			}
			
			StringBuilder whereClause = new StringBuilder();
			whereClause.append("C_BPartner_ID = ? ");
			whereClause.append("AND IsDefault = 'Y' ");
			if (isA)
				whereClause.append("AND IsTax = 'Y' ");
			if (isB)
				whereClause.append("AND IsTax = 'N' ");
			
			MBPartnerLocation bpLocation = new Query(ctx, MBPartnerLocation.Table_Name, 
					whereClause.toString() + " AND IsShipTo = 'Y'", null)
				.setOnlyActiveRecords(true)
				.setParameters(bpID)
				.first();
			if (bpLocation != null) {
				mTab.setValue("C_BPartner_Location_ID", bpLocation.get_ID());
			}
			
			MBPartnerLocation billLocation = new Query(ctx, MBPartnerLocation.Table_Name, 
					whereClause.toString() + " AND IsBillTo = 'Y'", null)
				.setOnlyActiveRecords(true)
				.setParameters(bpID)
				.first();
			if (billLocation != null) {
				mTab.setValue("Bill_Location_ID", billLocation.get_ID());
			}
		}
		
		// DROPPED PER HELP-4236
//		if (docTypeID == SASSystemID.DOCTYPE_PK_A || docTypeID == SASSystemID.DOCTYPE_PK_B) {
//			MPaymentTerm paymentTerm = new Query(ctx, MPaymentTerm.Table_Name, "NetDays = 3", null)
//				.setClient_ID()
//				.first();
//			if (paymentTerm != null) {
//				mTab.setValue("C_PaymentTerm_ID", paymentTerm.get_ID());
//			}
//		}
		return "";
	}

}
