package id.co.databiz.sas.callout;

import id.co.databiz.sas.model.MDiscountList;

import java.math.BigDecimal;
import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.util.Env;

public class CalloutDiscountSchemaBreakDiscountList implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		BigDecimal discount = Env.ZERO;
		if(value == null) {
			mTab.setValue("BreakDiscount", discount);
			return "";
		}
		Integer discountListID = (Integer) value;

		if (discountListID > 0) {
			MDiscountList dl = new MDiscountList(ctx, discountListID, null);
			discount = dl.getDiscount();
		}
		mTab.setValue("BreakDiscount", discount);
		return "";
	}
}
