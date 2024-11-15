package id.co.databiz.awn.callout;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MProduct;

public class CalloutOrderLineProduct implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		if(value==null){
			return "";
		}
		Integer productID = (Integer) value;
		if(productID>0){
			// AWN-33
			MProduct product = new MProduct(ctx, productID, null);
			StringBuffer sb = new StringBuffer();
			sb.append(product.getName());
			if(product.getDescription()!=null && !product.getDescription().isEmpty()){
				sb.append(", ");
				sb.append(product.getDescription());
			}
			mTab.setValue("Description", sb.toString());
		}
		return "";
	}

}
