package id.co.databiz.awn.model;

import java.sql.ResultSet;
import java.util.Properties;

public class MScheduleLine extends X_Z_ScheduleLine {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2673446255065213612L;

	public MScheduleLine(Properties ctx, int Z_ScheduleLine_ID, String trxName) {
		super(ctx, Z_ScheduleLine_ID, trxName);
	}
	
	public MScheduleLine(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

}
