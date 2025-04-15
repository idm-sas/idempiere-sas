package id.co.databiz.sas.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.util.List;
import java.util.Properties;

import org.compiere.model.Query;
import org.compiere.util.Env;

public class MDiscountList extends X_SAS_DiscountList{

	/**
	 * 
	 */
	private static final long serialVersionUID = -2952085538150527137L;

	public MDiscountList(Properties ctx, int SAS_DiscountList_ID, String trxName) {
		super(ctx, SAS_DiscountList_ID, trxName);
	}

	public MDiscountList(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
	
	public static BigDecimal getCalculatedPrice(int SAS_DiscountList_ID, BigDecimal price, int precision) {
		return getCalculatedPrice(SAS_DiscountList_ID, Env.ZERO, price, precision);
	}
	
	public static BigDecimal getCalculatedPrice(int SAS_DiscountList_ID, BigDecimal additionalDiscount, BigDecimal price, int precision) {
		if (price.compareTo(Env.ZERO) == 0) {
			return Env.ZERO;
		}
		
		if (additionalDiscount == null) {
			additionalDiscount = Env.ZERO;
		}
		
		BigDecimal calculatedPrice = price;
		List<MDiscountSchemaBreakLine> lineList = new Query(Env.getCtx(), MDiscountSchemaBreakLine.Table_Name, 
				"SAS_DiscountList_ID = ?", null)
			.setOnlyActiveRecords(true)
			.setParameters(SAS_DiscountList_ID)
			.setOrderBy("SeqNo")
			.list();
		for (MDiscountSchemaBreakLine line : lineList) {
			calculatedPrice = calculatedPrice.subtract(calculatedPrice.multiply(line.getDiscount()).divide(Env.ONEHUNDRED, 8, RoundingMode.HALF_UP));
		}
		
		// add additional discount if any
		if (additionalDiscount.signum() > 0) {
			calculatedPrice = calculatedPrice.subtract(calculatedPrice.multiply(additionalDiscount).divide(Env.ONEHUNDRED, 8, RoundingMode.HALF_UP));
		}
		
		calculatedPrice = calculatedPrice.setScale(precision,RoundingMode.HALF_UP);
		return calculatedPrice;
	}
}
