package id.co.databiz.awn.model;

import java.sql.ResultSet;
import java.util.Properties;

public class MOrderRef extends X_Z_OrderRef {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5513858821188266040L;

	public MOrderRef(Properties ctx, int Z_OrderRef_ID, String trxName) {
		super(ctx, Z_OrderRef_ID, trxName);
	}
	
	public MOrderRef(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
}
