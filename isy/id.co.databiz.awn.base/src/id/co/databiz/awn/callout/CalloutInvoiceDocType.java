package id.co.databiz.awn.callout;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MDocType;

public class CalloutInvoiceDocType implements IColumnCallout {

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
			mTab.setValue("CreateTaxInvoice", docType.get_Value("CreateTaxInvoice"));
		}
		return "";
	}

}
