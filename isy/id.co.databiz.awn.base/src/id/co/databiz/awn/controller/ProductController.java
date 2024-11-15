package id.co.databiz.awn.controller;

import id.co.databiz.awn.model.AWNSysConfig;

import org.compiere.model.MPriceListVersion;
import org.compiere.model.MProductPrice;
import org.compiere.model.X_M_Product;

public class ProductController {
	
	public static void afterNew(X_M_Product product){
		// ISY-72 add to default price list version
		int plvID = AWNSysConfig.getIntValue(AWNSysConfig.ISY_STD_PRICELIST_VERSION, 0);
		
		if (plvID > 0) {
			MPriceListVersion plv = new MPriceListVersion(product.getCtx(), plvID, product.get_TrxName());
			MProductPrice pp = new MProductPrice(product.getCtx(), 0, product.get_TrxName());
			pp.setM_PriceList_Version_ID(plv.get_ID());
			pp.setM_Product_ID(product.get_ID());
			pp.setAD_Org_ID(plv.getAD_Org_ID());
			pp.saveEx();
		}
	}
}
