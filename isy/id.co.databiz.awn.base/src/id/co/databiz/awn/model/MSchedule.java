package id.co.databiz.awn.model;

import java.sql.ResultSet;
import java.util.Properties;

public class MSchedule extends X_Z_Schedule {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5234295951251547015L;

	
	public MSchedule(Properties ctx, int Z_Schedule_ID, String trxName) {
		super(ctx, Z_Schedule_ID, trxName);
	}

	public MSchedule(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
}
