package id.co.databiz.sas.model;

import java.sql.ResultSet;
import java.util.Properties;

public class MPromotionBP extends X_SAS_PromotionBP {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6663225871874235475L;

	public MPromotionBP(Properties ctx, int SAS_PromotionBP_ID, String trxName) {
		super(ctx, SAS_PromotionBP_ID, trxName);
	}

	public MPromotionBP(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

}
