package id.co.databiz.sas.model;

import java.sql.ResultSet;
import java.util.Properties;

public class MNGKRule extends X_SAS_NGK_Rule{	

	/**
	 * @author AFF
	 */
	private static final long serialVersionUID = -273557369871801880L;
	
	public MNGKRule(Properties ctx, int SAS_NGK_Rule_ID, String trxName) {
		super(ctx, SAS_NGK_Rule_ID, trxName);
		// TODO Auto-generated constructor stub
	}

	public MNGKRule(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
		// TODO Auto-generated constructor stub
	}
	
}