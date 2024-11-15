package id.co.databiz.sas.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.Query;
import org.compiere.util.AdempiereUserError;

public class MBPRule extends X_SAS_BPRule {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7984071465863999643L;

	public MBPRule(Properties ctx, int SAS_BPRule_ID, String trxName) {
		super(ctx, SAS_BPRule_ID, trxName);
	}

	public MBPRule(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	/**************************************************************************
	 * 	Before Save
	 *	@param newRecord new
	 *	@return true
	 */
	protected boolean beforeSave (boolean newRecord)
	{
		log.fine("");
		if (isActive()) {
			int count = new Query(getCtx(), Table_Name, 
					"SAS_BPRule_ID <> ? AND C_BPartner_ID = ? AND AD_OrgTrx_ID = ?", get_TrxName())
				.setOnlyActiveRecords(true)
				.setParameters(new Object[]{get_ID(),getC_BPartner_ID(),getAD_OrgTrx_ID()})
				.count();
			if (count > 0) {
				throw new AdempiereUserError("BPartner and Org Trx combination already exists.");
			}
		}
		return true;
	}
}
