package id.co.databiz.awn.callout;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MDocType;
import org.compiere.model.MOrder;
import org.compiere.util.Env;

public class CalloutInOutDocType implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		if(value==null){
			return "";
		}
		
		int docTypeID = Env.getContextAsInt(ctx, WindowNo, "C_DocType_ID");
		int orderID = Env.getContextAsInt(ctx, WindowNo, "C_Order_ID");
		
		if(docTypeID == 0 & orderID > 0){
			MOrder order = new MOrder(ctx, orderID, null);
			
			// AWN-295
	        int orderDocTypeID = order.getC_DocType_ID();
	        int relatedDocTypeId = 0;
	        
	        if (orderDocTypeID == 0)
	        {
	            orderDocTypeID = order.getC_DocTypeTarget_ID();
	        }
	        
	        relatedDocTypeId = MDocType.getShipmentReceiptDocType(orderDocTypeID);
	        if (relatedDocTypeId == 0) {
	        	MDocType docType = MDocType.get(Env.getCtx(), orderDocTypeID);
	        	relatedDocTypeId = docType.getC_DocTypeShipment_ID();
	        }
	        if (relatedDocTypeId > 0) {
	        	mTab.setValue("C_DocType_ID", relatedDocTypeId);
	        }
		}
		return "";
	}

}
