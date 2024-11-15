
package id.co.databiz.awn.callout;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MProduct;

/**
 *	
 *	
 *  @author Anozi Mada
 */
public class CalloutRequisitionLineProduct implements IColumnCallout
{
	
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		Integer M_Product_ID = (Integer)value;
		if (M_Product_ID == null || M_Product_ID.intValue() == 0)
			return "";
		//	Set UOM
		if(M_Product_ID > 0){
			MProduct product = new MProduct(ctx, M_Product_ID, null);
			mTab.setValue("C_UOM_ID", product.getC_UOM_ID());
		}
		 		
		return "";
	}

}