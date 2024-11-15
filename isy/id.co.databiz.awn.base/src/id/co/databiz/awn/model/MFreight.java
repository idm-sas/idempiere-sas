package id.co.databiz.awn.model;

import java.sql.ResultSet;
import java.util.Properties;

public class MFreight extends  X_M_Freight{

	/**
	 * 
	 */
	private static final long serialVersionUID = -1833052169828004414L;

	public MFreight(Properties ctx, int M_Freight_ID, String trxName) {
		super(ctx, M_Freight_ID, trxName);
	}
	
	public MFreight(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
}
