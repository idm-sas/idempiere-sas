
package id.co.databiz.awn.callout;

import java.math.BigDecimal;
import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MUOMConversion;
import org.compiere.util.Env;

/**
 *	
 *	
 *  @author Anozi Mada
 */
public class CalloutUOMConvert implements IColumnCallout
{

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		BigDecimal qtyEntered = (BigDecimal) mTab.getValue("QtyEntered");
		Integer productID = (Integer) mTab.getValue("M_Product_ID");
		Integer uomID = (Integer) mTab.getValue("C_UOM_ID");
		
		if (productID != null && qtyEntered.compareTo(Env.ZERO) > 0 && uomID !=null ) {
			BigDecimal qty = MUOMConversion.convertProductFrom(ctx, productID, uomID, qtyEntered);
			mTab.setValue("Qty", qty);
			mTab.setValue("MovementQty", qty);
			mTab.setValue("QtyCount", qty);
	}
		return "";
	}

}