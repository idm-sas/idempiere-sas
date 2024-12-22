package id.co.databiz.sas.callout;

import id.co.databiz.sas.model.MDiscountList;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MCurrency;
import org.compiere.model.MPriceList;
import org.compiere.util.Env;

public class CalloutOrderLineDiscountList implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		if (isCalloutActive(mTab)) {
			return "";
		}
		
		// skip if PO
		String isSOTrx = Env.getContext(ctx, WindowNo, "IsSOTrx");
		if (isSOTrx.equals("N")) {
			return "";
		}
		
		BigDecimal calculatedPrice = Env.ZERO;
		if(value == null) {
			mTab.setValue("PriceActual", calculatedPrice);
			mTab.setValue("PriceEntered", calculatedPrice);
			return "";
		}
		Integer discountListID = (Integer) mTab.getValue("SAS_DiscountList_ID");
		BigDecimal additionalDiscount = (BigDecimal) mTab.getValue("AdditionalDiscount");
		BigDecimal priceList = (BigDecimal) mTab.getValue("PriceList");
		int currencyID = Env.getContextAsInt(ctx, WindowNo, "C_Currency_ID");
		int priceListID = Env.getContextAsInt(ctx, WindowNo, "M_PriceList_ID");

		MCurrency cur = MCurrency.get(ctx, currencyID) ;
		MPriceList pl = MPriceList.get(ctx, priceListID, null);
		int precision = cur.getStdPrecision();
		if (pl.getStandardPrecision() > precision) {
			precision = pl.getStandardPrecision();
		}
		
		if ((discountListID != null && discountListID > 0) || (additionalDiscount != null && additionalDiscount.signum() > 0)) {
			calculatedPrice = MDiscountList.getCalculatedPrice(discountListID, additionalDiscount, priceList, precision);
		}
		mTab.setValue("PriceActual", calculatedPrice);
		mTab.setValue("PriceEntered", calculatedPrice);
		
		if (priceList.compareTo(Env.ZERO) != 0) {
			BigDecimal discount = priceList.subtract(calculatedPrice)
					.multiply(Env.ONEHUNDRED)
					.divide(priceList, precision, RoundingMode.HALF_UP);
			mTab.setValue("Discount", discount);
		}
		return "";
	}
	
	protected boolean isCalloutActive(GridTab mTab)
	{
		//greater than 1 instead of 0 to discount this callout instance
		return mTab != null ? mTab.getActiveCallouts().length > 1 : false;
	}
}
