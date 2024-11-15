package id.co.databiz.awn.process;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MAccount;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MClient;
import org.compiere.model.MConversionType;
import org.compiere.model.MJournal;
import org.compiere.model.MJournalLine;
import org.compiere.model.MPeriod;
import org.compiere.model.MProductCategory;
import org.compiere.model.MProductCategoryAcct;
import org.compiere.model.MSysConfig;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;

import id.co.databiz.awn.model.AWNSysConfig;
import id.co.databiz.awn.model.MVarianceDistribution;
import id.co.databiz.awn.model.SystemID;

public class ISY388_Var_Dist_Production_Single extends SvrProcess {

	private Timestamp p_DateAcct;
	private boolean p_IsAdjust;
//	private int p_Production_ID;
	private int p_Org_ID;
	private Timestamp dateEnd;
	private Timestamp dateStart;
	private MAcctSchema as;
	private StringBuffer inProductCategory = new StringBuffer("(");
	private MVarianceDistribution va;
	private int count=0;
	private int journalOrgID;


	/**
	 * Prepare - e.g., get Parameters.
	 */
	@Override
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++) {
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals("DateAcct"))
				p_DateAcct = (Timestamp) para[i].getParameter();
			else if (name.equals("IsAdjustCOGS"))
				p_IsAdjust = "Y".equals(para[i].getParameter());
//			else if (name.equals("M_Production_ID"))
//				p_Production_ID = para[i].getParameterAsInt();
			else if (name.equals("AD_Org_ID"))
				p_Org_ID = para[i].getParameterAsInt();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
	
		dateEnd = p_DateAcct;
		Calendar startCalendar = Calendar.getInstance();
		startCalendar.setTimeInMillis(p_DateAcct.getTime());
		startCalendar.set(Calendar.DAY_OF_MONTH, 1);
		dateStart = new Timestamp(startCalendar.getTimeInMillis());
		
	}

	@Override
	protected String doIt() throws Exception {
		log.info("Start Variance Distribution");
		String info = "";
		Trx trx = Trx.get(get_TrxName(), true);
		trx.setTimeout(24 * 60 * 60); //about 24 hour
		as = MClient.get(getCtx()).getAcctSchema();
		
		List<String> varianceAccounts = new ArrayList<String>();
		//P_RateVariance_Acct 
		varianceAccounts.add("pa.P_CostAdjustment_Acct");
		if (MSysConfig.getValue(AWNSysConfig.ISY_STANDARD_COSTING_DISTRIBUTE_PPV_OR_IPV, "N").equalsIgnoreCase("Y"))
			{varianceAccounts.add("pa.P_PurchasePriceVariance_acct");} 
			else {varianceAccounts.add("pa.P_InvoicePriceVariance_Acct");}
		List<MProductCategory> productCategoryList = getProductCategoryStandardCosting(as);
		if (productCategoryList.isEmpty()) {
			addLog("No Product Category");
		} else {
			for (MProductCategory pc : productCategoryList) {
				if (!productCategoryList.get(0).equals(pc)) {
					inProductCategory.append(",");
				}
				inProductCategory.append(pc.get_ID());
			}
			inProductCategory.append(")");
			}
		
		for (String varianceAccount : varianceAccounts) {
			fillAccountBalanceCacheProduction(varianceAccount); // cache dari M_transaction
//			fillAccountBalanceCacheTable(varianceAccount); // cache dari fact_acct
//			fillCacheProductBalanceQtyMassal(); // cache dari rv_transaction
//			writeOffCogsMassal();
			info = varianceAccount;
			distributeProductAssetMassal(info);
//			distributeMassal(info);
//			flushProductBalanceCacheTable(); // flush tables
		}

		if (p_IsAdjust) generate();
		return info;
	} // doIt

	public List<MProductCategory> getProductCategoryStandardCosting(
			MAcctSchema as) {
		List<MProductCategory> results = new ArrayList<MProductCategory>();
		List<MProductCategory> list = new Query(getCtx(),
				MProductCategory.Table_Name, "", get_TrxName()).setClient_ID()
				.setOnlyActiveRecords(true).list();
		for (MProductCategory pc : list) {
			MProductCategoryAcct pca = MProductCategoryAcct.get(getCtx(),
					pc.getM_Product_Category_ID(), as.get_ID(), get_TrxName());
			String costingMethod = pca.getCostingMethod();
			if (costingMethod == null) {
				costingMethod = as.getCostingMethod();
			}
			if (costingMethod
					.equals(MProductCategoryAcct.COSTINGMETHOD_StandardCosting)) {
				results.add(pc);
			}
		}
	
		return results;
	}

	private void fillAccountBalanceCacheProduction(String varianceAccount) throws IllegalStateException, SQLException {
	List<Object> params = new ArrayList<Object>();
	params.add(dateStart);
	params.add(dateEnd);
	params.add(p_Org_ID);
	params.add(dateStart);
	params.add(dateEnd);
	params.add(p_Org_ID);
	
	// insert material or component
	String sql ="insert into t_variancesdistribution_account_cache (ad_pinstance_id,account_id,m_product_id,product_acct,denom_qty,amt) "
			+ "select a.* from ( "
			+ "	with t as ( "
			+ "	select "+ getAD_PInstance_ID()+", vc.account_id , t.m_product_id ,'component', sum(t.movementqty) "
			+ "	from m_transaction t "
			+ "	join m_productionline pl on pl.m_productionline_id = t.m_productionline_id and t.movementtype ='P-' "
			+ "	join m_production p on p.m_production_id = pl.m_production_id and p.docstatus='CO' and p.processed ='Y' "
			+ "	join m_product_acct pa on pa.m_product_id = t.m_product_id and pa.c_acctschema_id = 1000001 "
			+ "	join c_validcombination vc on vc.c_validcombination_id = "+varianceAccount
			+ "	where 1=1  "
			+ "	and t.movementdate between ? and ? "
			+ "	and t.ad_org_id = ? "
			+ "	group by 1,2,3,4 "
			+ "	) , f as ( "
			+ "	SELECT fa.Account_ID, fa.M_Product_ID ,SUM(fa.AmtAcctDr - fa.AmtAcctCr) Amt "
			+ "	FROM Fact_Acct fa  "
			+ "	join t q  "
			+ "	on q.account_id = fa.account_id  "
			+ "	and q.m_product_id = fa.m_product_id  "
			+ "	and fa.dateacct between ? and ? "
			+ "	and fa.ad_org_id = ? "
			+ "	group by 1,2 	 "
			+ "	) "
			+ "	select t.*,f.amt *-1 " //kreditkan variancenya pindah ke rate variance fg 
			+ "	from t join f on f.account_id = t.account_id and f.m_product_id = t.m_product_id  "
			+ ") a";
	DB.executeUpdateEx(sql, params.toArray(), get_TrxName());
	DB.commit(true, get_TrxName());
	log.info("inserted production minus to cache table t_variancesdistribution_account_cache ");
	
	params.clear();
	params.add(dateStart);
	params.add(dateEnd);
	params.add(p_Org_ID);

	//insert FG
	sql ="insert into t_variancesdistribution_account_cache (ad_pinstance_id, account_id , m_product_id,amt,product_acct) "
			+ "select a.*,'fg'  from ( "
			+ "with fg as ( "
			+ "select t.m_product_id , vc.account_id , p.m_product_id fg_id, sum(t.movementqty) movementqty  "
			+ "from m_transaction t "
			+ "join m_productionline pl on pl.m_productionline_id = t.m_productionline_id and t.movementtype ='P-' "
			+ "join m_production p on p.m_production_id = pl.m_production_id and p.docstatus='CO' and p.processed ='Y' "
			+ "join m_product_acct pa on pa.m_product_id = p.m_product_id and pa.c_acctschema_id = 1000001 "
			+ "join c_validcombination vc on vc.c_validcombination_id = pa.p_ratevariance_acct  " //hardcoded to p_ratevariance_acct
			+ "where 1=1  "
			+ "and t.movementdate between ? and ? "
			+ "and t.ad_org_id = ? "
			+ "group by 1,2,3  "
			+ ") "
			+ "select q.ad_pinstance_id , fg.account_id, fg.fg_id m_product_id , round(sum(fg.movementqty / q.denom_qty  * q.amt),2) amt  "
			+ "from t_variancesdistribution_account_cache q "
			+ "join fg on fg.m_product_id = q.m_product_id and q.ad_pinstance_id = "+getAD_PInstance_ID()+" and q.denom_qty <>0 "
			+ "group by 1,2,3 "
			+ "having sum(fg.movementqty / q.denom_qty  * q.amt) <> 0  "
			+ ") a";
	DB.executeUpdateEx(sql, params.toArray(), get_TrxName());
	DB.commit(true, get_TrxName());
	log.info("inserted FG to cache table t_variancesdistribution_account_cache ");
	
	}

	private void distributeProductAssetMassal( String info ) throws AdempiereException, SQLException {
		String sql ="select a.m_product_id, a.account_id variance_account , a.amt "
				+ " from t_variancesdistribution_account_cache a "
				+ " where a.ad_pinstance_id = "+getAD_PInstance_ID();
		ResultSet rs = DB.getRowSet(sql);
		while(rs.next()) {
			int productID = rs.getInt(1);
			int varianceAccountID = rs.getInt(2);
			BigDecimal productVarianceAmt = rs.getBigDecimal(3);
			
			va = new MVarianceDistribution(getCtx(),0, get_TrxName());
			va.setAD_Org_ID(p_Org_ID);
			va.setAD_PInstance_ID(getAD_PInstance_ID());
			va.setAccount_ID(varianceAccountID);
			va.setM_Product_ID(productID);
			if (productVarianceAmt.compareTo(Env.ZERO) < 0) {
				va.setAmtAcctCr(productVarianceAmt.abs());
			} else {
				va.setAmtAcctDr(productVarianceAmt.abs());
			} 
			// 
			va.setLine(new Integer(++count * 10));
//			va.setDescription(info + " A0");
			va.setDateAcct(dateEnd);
			va.setIsAdjustCOGS(p_IsAdjust);
			va.saveEx();			
			}
		}

	private void generate() {
		List<MVarianceDistribution> list = new Query(getCtx(),
				MVarianceDistribution.Table_Name, "AD_PInstance_ID = ?",
				get_TrxName())
				.setParameters(new Object[] { getAD_PInstance_ID() })
				.setOrderBy("AD_Org_ID,Line").list();
		if (!list.isEmpty()) {
			MJournal journal = null;
			MJournalLine line = null;
			MAccount account = null;
			for (MVarianceDistribution va : list) {
				if (va.getAD_Org_ID() != journalOrgID) {
					journalOrgID = va.getAD_Org_ID();
	
					journal = new MJournal(getCtx(), 0, get_TrxName());
					journal.setAD_Org_ID(journalOrgID);
					journal.setC_AcctSchema_ID(as.get_ID());
					journal.setDescription("Generated from process Variance Distribution Production Single ISY-388 "
							+ p_DateAcct);
					journal.setPostingType(MJournal.POSTINGTYPE_Actual);
					journal.setC_DocType_ID(SystemID.DOCTYPE_GL_VarAdj);
					journal.setGL_Category_ID(SystemID.GL_CATEGORY_VarAdj);
					journal.setDateDoc(p_DateAcct);
					journal.setDateAcct(p_DateAcct);
					MPeriod.testPeriodOpen(getCtx(), p_DateAcct,
							journal.getC_DocType_ID(),
							journal.getAD_Org_ID());
					journal.setC_Period_ID(MPeriod.getC_Period_ID(getCtx(),
							p_DateAcct, journal.getAD_Org_ID()));
					journal.setC_Currency_ID(as.getC_Currency_ID());
					journal.setC_ConversionType_ID(MConversionType.TYPE_SPOT);
					journal.saveEx();
					addLog(getProcessInfo().getAD_Process_ID(), p_DateAcct, null, journal.getDocumentNo(), journal.get_Table_ID(), journal.get_ID());
				}
				line = new MJournalLine(journal);
				line.setLine(new Integer(va.getLine()));
				line.setDescription(va.getDescription());
				line.setM_Product_ID(va.getM_Product_ID());
				line.setAmtSourceDr(va.getAmtAcctDr());
				line.setAmtSourceCr(va.getAmtAcctCr());
				line.setAmtAcct(va.getAmtAcctDr(), va.getAmtAcctCr());
				account = getOrCreateCombination(va);
				if (account != null) {
					line.setC_ValidCombination_ID(account);
				}
				line.setAccount_ID(va.getAccount_ID());
				line.saveEx();
			}
		}
	}

	private MAccount getOrCreateCombination(MVarianceDistribution va) {
		return MAccount.get(getCtx(), va.getAD_Client_ID(), va.getAD_Org_ID(),
				as.get_ID(), va.getAccount_ID(), 0, va.getM_Product_ID(),
				va.getC_BPartner_ID(), va.getAD_OrgTrx_ID(),
				va.getC_LocFrom_ID(), va.getC_LocTo_ID(),
				va.getC_SalesRegion_ID(), va.getC_Project_ID(),
				va.getC_Campaign_ID(), va.getC_Activity_ID(), va.getUser1_ID(),
				va.getUser2_ID(), 0, 0,get_TrxName());
	} // getOrCreateCombination
}
