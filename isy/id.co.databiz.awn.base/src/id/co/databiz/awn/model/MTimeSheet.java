package id.co.databiz.awn.model;

import java.sql.ResultSet;
import java.util.Properties;

public class MTimeSheet extends X_ISY_TimeSheet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 821387113574125987L;

	public MTimeSheet(Properties ctx, int ISY_TimeSheet_ID, String trxName) {
		super(ctx, ISY_TimeSheet_ID, trxName);
	}

	public MTimeSheet(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
	
}
