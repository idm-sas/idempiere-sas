package id.co.databiz.sas.callout;

import id.co.databiz.sas.SASSystemID;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MInvoice;
import org.compiere.model.MOrder;
import org.compiere.model.MRequest;
import org.compiere.model.Query;
import org.compiere.util.Env;

public class CalloutRequestInvoice implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		mTab.setValue("BPKNo", null);
		if(value == null){
			return "";
		}
		Integer invoiceiID = (Integer) value;
		int requestTypeID = Env.getContextAsInt(ctx, WindowNo, "R_RequestType_ID");
		
		if(invoiceiID>0){
			// SAS-154
			MInvoice invoice = new MInvoice(ctx, invoiceiID, null);
			mTab.setValue("C_BPartner_ID", invoice.getC_BPartner_ID());
			BigDecimal openAmt = Env.ZERO;
			if (!invoice.isPaid()) {
				openAmt = invoice.getGrandTotal(true);
				BigDecimal allocatedAmt = invoice.getAllocatedAmt();
				if (allocatedAmt != null) {
					openAmt = openAmt.subtract(allocatedAmt);
				}
			}
			
			mTab.setValue("OpenAmt", openAmt);
			mTab.setValue("RemainingAmt", openAmt);
			
			if (invoice.getC_Order_ID() > 0) {
				MOrder order = new MOrder(ctx, invoice.getC_Order_ID(), null);
				mTab.setValue("PickListNote", order.get_Value("PickListNote"));
			}
			
			int prevRequestTypeID = -1;
			if (requestTypeID == SASSystemID.REQUEST_TYPE_SERAH_TERIMA_DELIVERY) {
				prevRequestTypeID = SASSystemID.REQUEST_TYPE_DELIVERY_JOURNAL;
			} else if (requestTypeID == SASSystemID.REQUEST_TYPE_SERAH_TERIMA_BILLING) {
				prevRequestTypeID = SASSystemID.REQUEST_TYPE_SERAH_TERIMA_DELIVERY;
			} else if (requestTypeID == SASSystemID.REQUEST_TYPE_DAFTAR_TAGIH_OFFICE) {
				prevRequestTypeID = SASSystemID.REQUEST_TYPE_SERAH_TERIMA_BILLING;
			}
			
			if (prevRequestTypeID > 0) {
				// set BPKNo
				StringBuilder whereClause = new StringBuilder();
				whereClause.append("Processed='Y' ");
				whereClause.append("AND BPKNo IS NOT NULL ");
				whereClause.append("AND C_Invoice_ID = ? ");
				whereClause.append("AND R_Status_ID IN (?,?,?,?) ");
				whereClause.append("AND R_RequestType_ID = ? ");
				
				List<Object> params = new ArrayList<Object>();
				params.add(invoiceiID);
				params.add(SASSystemID.DJ_STATUS_DELIVERED);
				params.add(SASSystemID.DJ_STATUS_DELIVERED_WITH_NOTE);
				params.add(SASSystemID.STD_STATUS_DELIVERY_TO_BILLING);
				params.add(SASSystemID.STB_STATUS_BILLING);
				params.add(prevRequestTypeID);
				
				MRequest prevRequest = new Query(ctx, MRequest.Table_Name, whereClause.toString(), null)
					.setOnlyActiveRecords(true)
					.setParameters(params)
					.first();
				if (prevRequest != null) {
					mTab.setValue("BPKNo", prevRequest.get_Value("BPKNo"));
				}
			}
		}
		return "";
	}

}
