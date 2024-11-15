package org.compiere;

import java.math.BigDecimal;
import java.text.DecimalFormat;

import org.compiere.model.MCurrency;
import org.compiere.util.Env;

public class CurrencyUtil {

	public static String formatCurrency(BigDecimal amount, int C_Currency_ID){
		String value = "";
		if(C_Currency_ID>0){
			MCurrency currency = new MCurrency(Env.getCtx(), C_Currency_ID, null);
			int precision = currency.getStdPrecision();
			StringBuffer format = new StringBuffer("#,##0.");
			for(int i=0; i<precision; i++){
				format.append("0");
			}
			DecimalFormat decimal = new DecimalFormat(format.toString());
			value = decimal.format(amount.doubleValue());
		} else {
			DecimalFormat decimal = new DecimalFormat("#,##0");
			value = decimal.format(amount.doubleValue());
		}
		return value;
	}
}
