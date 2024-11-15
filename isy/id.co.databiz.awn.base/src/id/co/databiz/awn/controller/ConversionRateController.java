package id.co.databiz.awn.controller;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MConversionRate;
import org.compiere.model.Query;
import org.compiere.util.Env;


public class ConversionRateController {
	
	public static void beforeNew(MConversionRate conversionRate){
		validate(conversionRate);
	}
	
	public static void beforeChange(MConversionRate conversionRate){
		validate(conversionRate);
	}
	
	public static void validate(MConversionRate conversionRate){
		// AWN-230
		StringBuffer whereClause = new StringBuffer();
		whereClause.append("AD_Client_ID = ? ");
		whereClause.append("AND AD_Org_ID = ? ");
		whereClause.append("AND C_Currency_ID = ? ");
		whereClause.append("AND C_Currency_ID_To = ? ");
		whereClause.append("AND C_ConversionType_ID = ? ");
		whereClause.append("AND ? <= COALESCE(ValidTo,ValidFrom) ");
		whereClause.append("AND ? >= ValidFrom ");
		whereClause.append("AND C_Conversion_Rate_ID <> ? ");
		int count = new Query(Env.getCtx(), MConversionRate.Table_Name, whereClause.toString(), conversionRate.get_TrxName())
							.setParameters(new Object[]{conversionRate.getAD_Client_ID(),
														conversionRate.getAD_Org_ID(),
														conversionRate.getC_Currency_ID(),
														conversionRate.getC_Currency_ID_To(),
														conversionRate.getC_ConversionType_ID(),
														conversionRate.getValidFrom(),
														conversionRate.getValidTo()!=null?conversionRate.getValidTo():conversionRate.getValidFrom(),
														conversionRate.get_ID()})
							.setOnlyActiveRecords(true)
							.count();
		if(count > 0){
			throw new AdempiereException("AWN-230\nOverlap date");
		}
	}
}
