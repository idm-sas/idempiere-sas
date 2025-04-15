package id.co.databiz.sas.callout;

import id.co.databiz.sas.model.MBPRule;
import id.co.databiz.sas.model.MDiscountList;
import id.co.databiz.sas.model.MDiscountSchemaBreakLine;

import java.math.BigDecimal;
import java.util.List;
import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MBPartner;
import org.compiere.model.MCurrency;
import org.compiere.model.MDiscountSchema;
import org.compiere.model.MDiscountSchemaBreak;
import org.compiere.model.MPriceList;
import org.compiere.model.MProduct;
import org.compiere.model.Query;
import org.compiere.util.Env;

public class CalloutOrderLineProduct implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		if(value == null){
			return "";
		}
		Integer productID = (Integer) value;
		int orgTrxID = Env.getContextAsInt(ctx, WindowNo, "AD_OrgTrx_ID");
		int bpID = Env.getContextAsInt(ctx, WindowNo, "C_BPartner_ID");
		int currencyID = Env.getContextAsInt(ctx, WindowNo, "C_Currency_ID");
		int priceListID = Env.getContextAsInt(ctx, WindowNo, "M_PriceList_ID");
		String soTrx = Env.getContext(ctx, WindowNo, "IsSOTrx");
		
		// reset standard discount
		mTab.setValue("Std_Discount", Env.ZERO);
		mTab.setValue("SAS_DiscountList_ID", null);
		mTab.setValue("AdditionalDiscount", Env.ZERO);
					
		if(productID > 0 && bpID > 0 && orgTrxID > 0 && soTrx.equals("Y")){
			// SAS-64 Discount Matrix
//			MBPartner bp = MBPartner.get(Env.getCtx(), bpID);
			
			MProduct product = MProduct.get(Env.getCtx(), productID);
			BigDecimal price = (BigDecimal)mTab.getValue("PriceActual");
			MBPRule bpRule = new Query(ctx, MBPRule.Table_Name, "C_BPartner_ID = ? AND AD_OrgTrx_ID = ?", null)
				.setOnlyActiveRecords(true)
				.setParameters(bpID, orgTrxID)
				.first();
			
			if (bpRule != null && bpRule.getM_DiscountSchema_ID() > 0) {
				MDiscountSchema schema = new MDiscountSchema(Env.getCtx(), bpRule.getM_DiscountSchema_ID(), null);
				List<MDiscountSchemaBreak> breakList = new Query(Env.getCtx(), MDiscountSchemaBreak.Table_Name, 
						"M_DiscountSchema_ID = ?", null)
					.setOnlyActiveRecords(true)
					.setParameters(schema.get_ID())
					.setOrderBy(MDiscountSchemaBreak.COLUMNNAME_SeqNo)
					.list();
				BigDecimal discount = Env.ZERO;
				MDiscountSchemaBreak discountSchemaBreak = null;
				for (MDiscountSchemaBreak dsb : breakList) {
					if (dsb.get_ValueAsString("Group1") != null && !dsb.get_ValueAsString("Group1").isEmpty()) {
						if (dsb.get_ValueAsString("Group1").equals(product.getGroup1())) {
							discount = dsb.getBreakDiscount();
							discountSchemaBreak = dsb;
							break;
						}
					}
				}
				if (discountSchemaBreak != null) {
					int discountListID = discountSchemaBreak.get_ValueAsInt("SAS_DiscountList_ID");
					if (discountListID > 0) {
						MCurrency cur = MCurrency.get(ctx, currencyID) ;
						MPriceList pl = MPriceList.get(ctx, priceListID, null);
						int precision = cur.getStdPrecision();
						if (pl.getStandardPrecision() > precision) {
							precision = pl.getStandardPrecision();
						}
						BigDecimal additionalDiscount = (BigDecimal) mTab.getValue("AdditionalDiscount"); 
						BigDecimal calculatedPrice = MDiscountList.getCalculatedPrice(discountListID, additionalDiscount, price, precision);
						mTab.setValue("PriceActual", calculatedPrice);
						mTab.setValue("PriceEntered", calculatedPrice);
						mTab.setValue("Discount", discount);
						mTab.setValue("Std_Discount", discount);
						mTab.setValue("SAS_DiscountList_ID", discountListID);
					}
				}
			}
		}
		return "";
	}

}
