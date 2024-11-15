package id.co.databiz.awn.callout;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MInOut;
import org.compiere.model.MRMA;

public class CalloutInOutRMA implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		if(value==null){
			return "";
		}
		Integer rmaID = (Integer) value;
		if(rmaID>0){
			// ISY-22
			MRMA rma = new MRMA(ctx, rmaID, null);
			MInOut originalReceipt = rma.getShipment();
			
			mTab.setValue("C_BPartner_ID", new Integer(originalReceipt.getC_BPartner_ID()));
			mTab.setValue("C_Project_ID", new Integer(originalReceipt.getC_Project_ID()));
		}
		return "";
	}

}
