package id.co.databiz.awn.process;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.compiere.model.MPeriod;
import org.compiere.model.MSysConfig;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.TimeUtil;

/**
 * @author yg
 * isy-395 reval inventory ke org trx sesuai posisi stok akhirnya
 */
public class ISY395_RevalInventory extends SvrProcess {

	private Timestamp today;
	private MPeriod period;
	private int p_C_Period_ID = 0;
	private int p_AD_Org_ID = 0;
	private int p_AD_OrgTrx_ID = 0;
	private boolean p_UpdateBalances = true; //default to refresh table
	private String MvTableName;
	private String sql;

	@Override
	protected void prepare() {
		today = TimeUtil.getDay(0);
		period = MPeriod.get(getCtx(), today, 0, get_TrxName()); //get current period
		p_C_Period_ID = period.getC_Period_ID();
		MvTableName = MSysConfig.getValue("ISY_ISY395_MV_TABLENAME", "t_isy395_reval_inventory");
		 
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++) {
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals("AD_Org_ID"))
				p_AD_Org_ID  = para[i].getParameterAsInt();
			else if (name.equals("AD_OrgTrx_ID"))
				p_AD_OrgTrx_ID  = para[i].getParameterAsInt();
			else if (name.equals("C_Period_ID")) {
				p_C_Period_ID  = para[i].getParameterAsInt();
				period  = MPeriod.get(getCtx(), p_C_Period_ID);
			}
			else if (name.equals("UpdateBalances"))
				p_UpdateBalances  = para[i].getParameterAsBoolean();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}	
	}

	@Override
	protected String doIt() throws Exception {
		// https://databiz.atlassian.net/browse/ISY-395
		// check eligible product to reval
		
		int AD_PInstance_ID = getAD_PInstance_ID();
		int AD_User_ID = getAD_User_ID();
		
		List<MPeriod> periodes = new Query(getCtx(),MPeriod.Table_Name , "AD_Client_ID ="+getAD_Client_ID() , get_TrxName())
				.setOnlyActiveRecords(true).setOrderBy("StartDate").list();
		
		//truncate table at start of process
		statusUpdate("Temp Table Truncating... ");
		DB.executeUpdateEx("truncate "+ MvTableName, get_TrxName());
		DB.commit(false, get_TrxName());
		String acccounts_ID = DB.getSQLValueString(get_TrxName()
				, "select string_agg(distinct vc.account_id::text, ',') "
						+ "from m_product p  "
						+ "join m_product_acct pa on pa.m_product_id = p.m_product_id "
						+ "join c_validcombination vc on vc.c_validcombination_id = pa.p_asset_acct  "
						+ "where p.ad_client_id = ? and p.isstocked ='Y' and p.issummary ='N'"
				, getAD_Client_ID());
		
		for (MPeriod per : periodes) {
			
			// param C_Period_ID
			List<Object> params = new ArrayList<Object>();
			params.add(per.getC_Period_ID()); // param #1
		    sql = 
					"with f as  "
					+ "	(select per.startdate::date created ,f.ad_org_id ,f.ad_orgtrx_id ,f.m_product_id ,sum(f.amtsourcedr-f.amtsourcecr) amount , sum(f.qty) qty "
					+ "	from fact_acct f "
					+ "	join c_period per on per.c_period_id = f.c_period_id  "
					+ "	where f.account_id in ("+acccounts_ID+") "
					+ "	and f.c_period_id = ? " //param #1
					+ "	group by 1,2,3,4)	 "
					+ "insert into "+MvTableName+" (ad_org_id, ad_orgtrx_id ,m_product_id, amount, actualqty , created , updated) " //param #2
					+ "select f.ad_org_id ,f.ad_orgtrx_id ,f.m_product_id  "
					+ ", f.amount amount  "
					+ ", f.qty actualqty "
					+ ", f.created created  "
					+ ", now() updated "
					+ "from f "
					+ "ON CONFLICT(ad_org_id,ad_orgtrx_id,m_product_id) "
					+ "do update "
					+ "	set "
					+ "	amount = "+MvTableName+".amount + EXCLUDED.amount " //param #3
					+ "	,actualqty = "+MvTableName+".actualqty + EXCLUDED.actualqty " //param #4
					+ "	,created = EXCLUDED.created "
					+ "	,updated = now()";
			statusUpdate(" Processing Inventory Journals for period:"+per.getName()+" and p_asset_acct ");
			DB.executeUpdateEx(sql, params.toArray(), get_TrxName());
			DB.commit(false, get_TrxName());
			params.clear();
			// stop apabila sudah ketemu periode di param / periode terkini
			if (per.getC_Period_ID() == period.getC_Period_ID()) {
				statusUpdate(" Stop processing temp table ");
				break;			
			}
		}
		
		if (p_UpdateBalances) {
			statusUpdate(" Calculating average cost per Client ");
			calculateAverage();
			statusUpdate(" Insert Product Asset Account ");
			insertPassetAcct();
		}
		//set createdby , adpisntance
		finallysign();
		
	return "";
	}
	
	private void calculateAverage() throws IllegalStateException, SQLException {
		sql = "with a as ( "
				+ "	select t.m_product_id , sum(t.amount) / sum(t.actualqty) avgcost "
				+ "	from "+MvTableName+" t "
				+ "	group by 1 "
				+ "	having sum(t.actualqty) <> 0 "
				+ ") "
				+ "update "+MvTableName+" t "
				+ "set costaverage = a.avgcost "
				+ "from a  "
				+ "where a.m_product_id = t.m_product_id";
		DB.executeUpdateEx(sql, get_TrxName());
		DB.commit(false, get_TrxName());
	}
	private void insertPassetAcct() throws IllegalStateException, SQLException {
		sql = "update "+MvTableName+" t "
				+ "set p_asset_acct = pa.p_asset_acct  "
				+ "from m_product_acct pa "
				+ "where pa.m_product_id = t.m_product_id ";
		DB.executeUpdateEx(sql, get_TrxName());
		DB.commit(false, get_TrxName());
	}

	private void finallysign() throws IllegalStateException, SQLException {
		List<Object> params = new ArrayList<Object>();
		params.add(getAD_PInstance_ID()); // param #2
		params.add(getAD_Client_ID()); // param #3
		params.add(getAD_User_ID()); // param #4
		params.add(getAD_User_ID()); // param #5
		sql = 
				"update "+MvTableName+" " 
				+ "set ad_pinstance_id = ? " //param #2
				+ ",ad_client_id  =  ? " //param #3
				+ ",createdby = ? " //param #4
				+ ",updatedby = ? "; //param #5
		statusUpdate(" Updating AD_Pinstance and user field ");
		DB.executeUpdateEx(sql, params.toArray(), get_TrxName());
		DB.commit(false, get_TrxName());
		params.clear();
	}
}
