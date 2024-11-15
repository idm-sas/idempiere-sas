package id.co.databiz.awn.model;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;

import org.compiere.model.MAcctSchema;
import org.compiere.model.MClient;
import org.compiere.model.MConversionType;
import org.compiere.model.MCurrency;
import org.compiere.util.Env;

public class MVarianceDistribution extends X_T_VarianceDistribution {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8059966565639144096L;

	MAcctSchema as = null;

	public MVarianceDistribution(Properties ctx, int T_VarianceDistriubtion_ID, String trxName) {
		super(ctx, T_VarianceDistriubtion_ID, trxName);
		if (T_VarianceDistriubtion_ID == 0)
		{
		//	setGL_JournalLine_ID (0);		//	PK
		//	setGL_Journal_ID (0);			//	Parent
		//	setC_Currency_ID (0);
		//	setC_ValidCombination_ID (0);
			setLine (0);
			setAmtAcctCr (Env.ZERO);
			setAmtAcctDr (Env.ZERO);
			setAmtSourceCr (Env.ZERO);
			setAmtSourceDr (Env.ZERO);
			setCurrencyRate (Env.ONE);
			setC_Currency_ID(MClient.get(getCtx()).getC_Currency_ID());
			setC_ConversionType_ID (MConversionType.TYPE_SPOT);
			setDateAcct (new Timestamp(System.currentTimeMillis()));
			setIsGenerated (true);
		}
	}
	
	public MVarianceDistribution (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }
	
	public int getPrecision () {
		int currencyID = MClient.get(Env.getCtx()).getC_Currency_ID();
		return MCurrency.getStdPrecision(getCtx(), currencyID);
	}
	
	public void setAmtAcctCr (BigDecimal amt) {
		super.setAmtAcctCr(amt.setScale(getPrecision(), BigDecimal.ROUND_HALF_UP));
	}
	
	public void setAmtAcctDr (BigDecimal amt) {
		super.setAmtAcctDr(amt.setScale(getPrecision(), BigDecimal.ROUND_HALF_UP));
	}
	
	public void setSourceAcctCr (BigDecimal amt) {
		super.setAmtSourceCr(amt.setScale(getPrecision(), BigDecimal.ROUND_HALF_UP));
	}
	
	public void setAmtSourceDr (BigDecimal amt) {
		super.setAmtSourceDr(amt.setScale(getPrecision(), BigDecimal.ROUND_HALF_UP));
	}
}
