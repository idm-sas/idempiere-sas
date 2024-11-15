package id.co.databiz.sas.model;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.List;
import java.util.Properties;

import org.compiere.model.MDiscountSchemaBreak;
import org.compiere.model.Query;
import org.compiere.util.Env;

public class MDiscountSchemaBreakLine extends X_SAS_DiscountSchemaBreakLine {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1269935547988984882L;

	public MDiscountSchemaBreakLine(Properties ctx,
			int SAS_DiscountSchemaBreakLine_ID, String trxName) {
		super(ctx, SAS_DiscountSchemaBreakLine_ID, trxName);
	}

	public MDiscountSchemaBreakLine(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
	
	public boolean updateHeaderDiscount () {
		MDiscountList dl = new MDiscountList(getCtx(), getSAS_DiscountList_ID(), get_TrxName());
		if (dl != null) {
			BigDecimal remainingPercent = Env.ONEHUNDRED;
			BigDecimal discount = Env.ZERO;
			BigDecimal discount2 = Env.ZERO;
			String name = "";
			String name2 = "";
			List<MDiscountSchemaBreakLine> lineList = new Query(getCtx(), MDiscountSchemaBreakLine.Table_Name, 
					"SAS_DiscountList_ID = ? AND Discount > 0", get_TrxName())
				.setOnlyActiveRecords(true)
				.setParameters(dl.get_ID())
				.setOrderBy("SeqNo")
				.list();
			for (MDiscountSchemaBreakLine line : lineList) {
				discount = discount.add(line.getDiscount().divide(Env.ONEHUNDRED, 4, BigDecimal.ROUND_HALF_UP).multiply(remainingPercent));
				if (lineList.indexOf(line) < lineList.size()-1) {
					discount2 = discount2.add(line.getDiscount().divide(Env.ONEHUNDRED, 4, BigDecimal.ROUND_HALF_UP).multiply(remainingPercent));
				}
				remainingPercent = Env.ONEHUNDRED.subtract(discount);
				if (lineList.indexOf(line) == 0) {
					name = String.valueOf(line.getDiscount());
					name2 = String.valueOf(line.getDiscount());
				} else {
					name += "+" + String.valueOf(line.getDiscount());
					if (lineList.indexOf(line) < lineList.size()-1) {
						name2 += "+" + String.valueOf(line.getDiscount());
					}
				}
			}
			
			if (lineList.size() > 0) {
				discount = discount.setScale(2, BigDecimal.ROUND_HALF_UP);
				dl.setDiscount(discount);
				
				discount2 = discount2.setScale(2, BigDecimal.ROUND_HALF_UP);
				
				if (lineList.size() == 1) {
					name = discount.stripTrailingZeros().toPlainString() + "%";
					name2 = discount2.stripTrailingZeros().toPlainString() + "%";
				} else {
					name += " (" + discount.stripTrailingZeros().toPlainString() + "%)";
					name2 += " (" + discount2.stripTrailingZeros().toPlainString() + "%)";
				}
				dl.setName(name);
				dl.setName2(name2);
			} else {
				dl.setDiscount(Env.ZERO);
				dl.setName("0%");
				dl.setName2("0%");
			}
			dl.saveEx();
			
		}
		return true;
	}
	
	protected boolean afterSave (boolean newRecord, boolean success) {
		if (!success)
			return success;
		return updateHeaderDiscount();
	}
	
	protected boolean afterDelete (boolean success) {
		if (!success)
			return success;
		return updateHeaderDiscount();
	}
}
