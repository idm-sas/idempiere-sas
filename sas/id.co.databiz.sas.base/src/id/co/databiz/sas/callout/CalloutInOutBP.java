package id.co.databiz.sas.callout;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.acct.Doc;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MDocType;
import org.compiere.util.DB;
import org.compiere.util.Env;

public class CalloutInOutBP implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		if(value == null){
			return "";
		}
		Integer returnID = (Integer) value;
		
		// SAS-99
		if (returnID > 0) {
			int docTypeID = Env.getContextAsInt(ctx, WindowNo, "C_DocType_ID");
			MDocType docType = new MDocType(ctx, docTypeID, null);
			
			if (docTypeID > 0 && docType.getDocBaseType().equals(Doc.DOCTYPE_MatReceipt) && mTab.getValueAsBoolean("IsSOTrx") ){
				int bPartnerID = Env.getContextAsInt(ctx, WindowNo, "C_BPartner_ID");
				int orgTrxID = Env.getContextAsInt(ctx, WindowNo, "AD_OrgTrx_ID");
				
				String sql = "SELECT MAX(bpr.SalesRep_ID) FROM SAS_BPRule bpr WHERE bpr.AD_OrgTrx_ID=? AND bpr.C_BPartner_ID=?";
				int salesRepID = DB.getSQLValue(null, sql, orgTrxID, bPartnerID);
				if(salesRepID>0)
					mTab.setValue("SalesRep_ID", salesRepID);
			}
		}
		
		return "";
	}

}
