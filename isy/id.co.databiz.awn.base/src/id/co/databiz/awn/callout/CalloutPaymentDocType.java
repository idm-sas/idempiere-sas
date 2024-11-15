package id.co.databiz.awn.callout;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MDocType;

public class CalloutPaymentDocType implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		if(value==null){
			return "";
		}
		Integer docTypeID = (Integer) value;
		if (docTypeID > 0 ) {
			// AWN-251
			MDocType docType = MDocType.get(ctx, docTypeID);
			
			if(docType.getDocBaseType().equalsIgnoreCase(MDocType.DOCBASETYPE_ARReceipt))
				mTab.setValue("IsReceipt", true);
			else if (docType.getDocBaseType().equalsIgnoreCase(MDocType.DOCBASETYPE_APPayment))
				mTab.setValue("IsReceipt", false);
		}
		return "";
	}

}
