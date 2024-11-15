package id.co.databiz.awn.process;

import java.sql.Timestamp;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MMatchPO;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;

public class MatchPOReverse extends SvrProcess {
	private int		p_M_MatchPO_ID = 0;

	@Override
	protected void prepare() {
		p_M_MatchPO_ID = getRecord_ID();
	}

	/**
	 *	@return message
	 *	@throws Exception
	 */
	@Override
	protected String doIt() throws Exception {
		if (log.isLoggable(Level.INFO))
			log.info ("M_MatchPO_ID=" + p_M_MatchPO_ID);

		MMatchPO po = new MMatchPO (getCtx(), p_M_MatchPO_ID, get_TrxName());
		if (po.get_ID() != p_M_MatchPO_ID)
			throw new AdempiereException("@NotFound@ @M_MatchPO_ID@ " + p_M_MatchPO_ID);
		if (po.isProcessed())
		{		
			Timestamp reversalDate = Env.getContextAsDate(getCtx(), "#Date");
			if (reversalDate == null) {
				reversalDate = new Timestamp(System.currentTimeMillis());
			}
			//ISY-420 Delete MatchPO Journal if MR Reverse
			String sqldelete = "delete from fact_acct where ad_table_id = 473 and record_id = ? ";
			DB.executeUpdate(sqldelete, p_M_MatchPO_ID, get_TrxName());
			DB.commit(false, get_TrxName());

			if (!po.reverse(reversalDate))
				throw new AdempiereException("Failed to reverse matching");
		}
		return "@OK@";
	}

}