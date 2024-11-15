package id.co.databiz.awn.callout;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MConversionRate;
import org.compiere.model.MTimeExpenseLine;
import org.compiere.util.Env;

public class CalloutTimeExpenseLineQty implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		if(value==null){
			return "";
		}
		BigDecimal expenseLineID = (BigDecimal) value;
		if(expenseLineID.intValue()>0){
			// ISY-275 -- DROPPED, removed from extension
			BigDecimal expenseAmt = (BigDecimal)mTab.getValue("ExpenseAmt");
			BigDecimal qty = (BigDecimal)mTab.getValue("Qty");
			BigDecimal ConvertedAmt = expenseAmt.multiply(qty);
			
			Integer C_Currency_From_ID = (Integer)mTab.getValue("C_Currency_ID");
			int C_Currency_To_ID = Env.getContextAsInt(ctx, "$C_Currency_ID");
			Timestamp DateExpense = Env.getContextAsDate(ctx, WindowNo, "DateExpense");
			//	convert if required
			if (ConvertedAmt.compareTo(Env.ZERO)!=0 && C_Currency_To_ID != C_Currency_From_ID.intValue())
			{
				int AD_Client_ID = Env.getContextAsInt (ctx, WindowNo, "AD_Client_ID");
				int AD_Org_ID = Env.getContextAsInt (ctx, WindowNo, "AD_Org_ID");
				ConvertedAmt = MConversionRate.convert (ctx,
					ConvertedAmt, C_Currency_From_ID.intValue(), C_Currency_To_ID, 
					DateExpense, 0, AD_Client_ID, AD_Org_ID);
			}
			mTab.setValue("ConvertedAmt", ConvertedAmt);
		}
		return "";
	}

}
