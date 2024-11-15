package id.co.databiz.sas.model;

import java.sql.ResultSet;
import java.util.Properties;

public class MBPNgk extends X_SAS_C_BPartnerNGK{	

	/**
	 * @author AFF
	 */
	private static final long serialVersionUID = 8680235326647287080L;
	
	public MBPNgk(Properties ctx, int SAS_C_BPartnerNGK_ID, String trxName) {
		super(ctx, SAS_C_BPartnerNGK_ID, trxName);
		// TODO Auto-generated constructor stub
	}

	public MBPNgk(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
		// TODO Auto-generated constructor stub
	}
	
}