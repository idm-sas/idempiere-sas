package org.compiere.model;

import java.sql.ResultSet;
import java.util.Properties;

public class MWFScenarioLine extends X_Z_WFScenarioLine {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4621347323361336038L;

	public MWFScenarioLine(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
	
	public MWFScenarioLine(Properties ctx, int Z_WFScenarioLine_ID,
			String trxName) {
		super(ctx, Z_WFScenarioLine_ID, trxName);
	}
}
