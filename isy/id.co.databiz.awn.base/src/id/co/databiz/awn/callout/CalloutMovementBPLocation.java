package id.co.databiz.awn.callout;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MBPartner;
import org.compiere.model.MBPartnerLocation;
import org.compiere.util.DB;

public class CalloutMovementBPLocation implements IColumnCallout{

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value, Object oldValue) {
		if (value == null) {
			return "";
		}
		Integer C_BPartner_ID = (Integer) value;
		String sql = "SELECT C_BPartner_Location_ID FROM C_BPartner_Location WHERE "
				+ "C_BPartner_Location.C_BPartner_ID=? AND "
				+ "C_BPartner_Location.IsShipTo='Y' AND "
				+ "C_BPartner_Location.IsActive='Y'";
		Integer C_BPartner_Location_ID = DB.getSQLValue(null, sql, C_BPartner_ID);
		System.out.println(C_BPartner_ID);
		if (C_BPartner_ID > 0) {
			//ISY-361
			MBPartnerLocation mBPartnerLocation = new MBPartnerLocation(ctx, C_BPartner_Location_ID, null);
			mTab.setValue("C_BPartner_Location_ID", mBPartnerLocation.getC_BPartner_Location_ID());
		}
		return "";
	}

}
