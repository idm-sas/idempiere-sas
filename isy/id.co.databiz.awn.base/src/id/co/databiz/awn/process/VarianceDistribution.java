package id.co.databiz.awn.process;

import id.co.databiz.awn.model.AWNSysConfig;
import id.co.databiz.awn.model.MVarianceDistribution;
import id.co.databiz.awn.model.SystemID;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.crypto.spec.RC2ParameterSpec;
import javax.sql.RowSet;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MAccount;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MClient;
import org.compiere.model.MConversionType;
import org.compiere.model.MJournal;
import org.compiere.model.MJournalLine;
import org.compiere.model.MPeriod;
import org.compiere.model.MProduct;
import org.compiere.model.MProductCategory;
import org.compiere.model.MProductCategoryAcct;
import org.compiere.model.MSysConfig;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;

/**
 * 
 * Distribution variance ver 2.0
 * @author Anozi Mada , yg
 * 
 */
public class VarianceDistribution extends SvrProcess {
	private Timestamp p_DateAcct = null;
	private boolean p_IsAdjust = false;
	private int p_Product =0;
	private BigDecimal p_WriteOffAmt = Env.ONEHUNDRED.multiply(Env.ONEHUNDRED); // Rp 10,000-

	private MAcctSchema as = null;
	private Timestamp dateStart = null;
	private Timestamp dateEnd = null;

	int count = 0;
	int countR = 0;

	int journalOrgID = 0;
	
	MJournal reversal = null;
	Map<Integer,MJournal> reversalMap;
	
	StringBuffer inProductCategory = new StringBuffer("(");
	MVarianceDistribution va;

	/**
	 * Prepare - e.g., get Parameters.
	 */
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
			else if (name.equals("M_Product_ID"))
				p_Product = para[i].getParameterAsInt();
			else if (name.equals("WriteOffAmt"))
				p_WriteOffAmt = para[i].getParameterAsBigDecimal();

			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
	
		dateEnd = p_DateAcct;
		Calendar startCalendar = Calendar.getInstance();
		startCalendar.setTimeInMillis(p_DateAcct.getTime());
		startCalendar.set(Calendar.DAY_OF_MONTH, 1);
		dateStart = new Timestamp(startCalendar.getTimeInMillis());
	} // prepare

	/**
	 * Perform process.
	 * 
	 * @return Message
	 * @throws Exception
	 */
	protected String doIt() throws java.lang.Exception {
		log.info("Start Variance Distribution");
		String info = "";
		Trx trx = Trx.get(get_TrxName(), true);
		trx.setTimeout(24 * 60 * 60); //about 24 hour
		as = MClient.get(getCtx()).getAcctSchema();
		
		List<String> varianceAccounts = new ArrayList<String>();
		//P_RateVariance_Acct 
		varianceAccounts.add("pa.P_RateVariance_Acct"); //https://databiz.atlassian.net/browse/ISY-61?focusedCommentId=105725
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
			fillAccountBalanceCacheTable(varianceAccount); // cache dari fact_acct
			fillCacheProductBalanceQtyMassal(); // cache dari rv_transaction
			writeOffCogsMassal();
			info = varianceAccount;
			distributeProductAssetMassal(info);
			distributeMassal(info);
			flushProductBalanceCacheTable(); // flush tables
		}

		if (p_IsAdjust) generate();
		return info;
	} // doIt
	
	private void flushProductBalanceCacheTable() throws IllegalStateException, SQLException {
		if (p_WriteOffAmt.compareTo(new BigDecimal(66)) != 0) //secret number to debug cache table
			{
			//flush cube table
			DB.executeUpdateEx("delete from t_variancesdistribution_cache where ad_pinstance_id="+getAD_PInstance_ID(), get_TrxName());	
			DB.executeUpdateEx("delete from t_variancesdistribution_account_cache where ad_pinstance_id="+getAD_PInstance_ID(), get_TrxName());
//			DB.executeUpdateEx("delete from t_variancedistribution where ad_pinstance_id="+getAD_PInstance_ID(), get_TrxName());
			DB.commit(true, get_TrxName());
			log.info("t_variancesdistribution% tables flushed");			
			}
		}

	private void fillAccountBalanceCacheTable(String varianceAccount) throws IllegalStateException, SQLException {
	List<Object> params = new ArrayList<Object>();
	params.add(dateStart);
	params.add(dateEnd);
	String sql ="INSERT INTO t_variancesdistribution_account_cache (ad_pinstance_id,account_id, m_product_id,amt)"
			+ " SELECT "+ getAD_PInstance_ID() +
			",fa.Account_ID, fa.M_Product_ID" +
			", SUM(fa.AmtAcctDr - fa.AmtAcctCr) Amt"+
			" FROM Fact_Acct fa " + 
			" INNER JOIN M_Product p ON (p.M_Product_ID = fa.M_Product_ID)" + 
			" INNER JOIN M_Product_Acct pa ON (pa.M_Product_ID = p.M_Product_ID AND pa.C_AcctSchema_ID = fa.C_AcctSchema_ID)" + 
			" INNER JOIN C_ValidCombination vc ON vc.C_ValidCombination_ID ="+varianceAccount +  //param variances account
			" WHERE 1=1" + 
			" AND fa.Account_ID = vc.Account_ID" + 
			" AND p.ProductType = 'I'" + 
			" AND p.IsStocked = 'Y'" + 
			" AND fa.C_AcctSchema_ID ="+ as.get_ID() +
			" AND (fa.DateAcct BETWEEN ? AND ?)" + // param dates use params.toArray()
			" AND fa.M_Product_ID ="+(p_Product == 0 ? "fa.M_Product_ID" : p_Product) + //param product
			" AND p.M_Product_Category_ID IN " +  inProductCategory +
			" GROUP BY fa.Account_ID,fa.M_Product_ID" + 
			" ORDER BY fa.Account_ID,fa.M_Product_ID";
	DB.executeUpdateEx(sql, params.toArray(), get_TrxName());
	DB.commit(true, get_TrxName());
	log.info("inserted cache table t_variancesdistribution_account_cache ");
	}

	private void fillCacheProductBalanceQtyMassal() throws IllegalStateException, SQLException {

		//fill temp table per produk for qty detail
		List<Object> params = new ArrayList<Object>();
		params.add(dateStart);
		params.add(dateEnd);
		params.add(dateStart);
		String sql ="insert into t_variancesdistribution_cache" + 
				" select "+getAD_PInstance_ID()+
				",t.ad_org_id , t.ad_orgtrx_id , t.c_project_id , t.c_campaign_id ,t.m_warehouse_id, t.m_product_id, t.m_inventoryline_c_charge_id" + 
				",sum(t.saldo_awal) as saldo_awal" + 
				",sum(t.V_PLUS_QTY) V_PLUS_QTY" + 
				",sum(V_MINUS_QTY) V_MINUS_QTY" + 
				",sum(C_PLUS_QTY) C_PLUS_QTY" + 
				",sum(C_MINUS_QTY) C_MINUS_QTY" + 
				",sum(I_PLUS_QTY) I_PLUS_QTY" + 
				",sum(I_MINUS_QTY) I_MINUS_QTY" + 
				",sum(M_PLUS_QTY) M_PLUS_QTY" + 
				",sum(M_MINUS_QTY) M_MINUS_QTY" + 
				",sum(W_PLUS_QTY) W_PLUS_QTY" + 
				",sum(W_MINUS_QTY) W_MINUS_QTY" + 
				",sum(P_PLUS_QTY) P_PLUS_QTY" + 
				",sum(P_MINUS_QTY) P_MINUS_QTY" + 
				",sum(t.saldo_awal+t.V_PLUS_QTY+V_MINUS_QTY+C_PLUS_QTY+C_MINUS_QTY+I_PLUS_QTY"
				+ "+I_MINUS_QTY+M_PLUS_QTY+M_MINUS_QTY+W_PLUS_QTY+W_MINUS_QTY+P_PLUS_QTY+P_MINUS_QTY) as saldo_akhir" + 
				" from (SELECT "
				+ " t.ad_org_id , t.ad_orgtrx_id , t.c_project_id , t.c_campaign_id ,t.m_warehouse_id, t.m_product_id, t.m_inventoryline_c_charge_id" + 
				",0 as saldo_awal" + 
				",sum(case when t.MovementType = 'V+' then t.movementqty else 0 end) as V_PLUS_QTY" + 
				",sum(case when t.MovementType = 'V-' then t.movementqty else 0 end) as V_MINUS_QTY" + 
				",sum(case when t.MovementType = 'C+' then t.movementqty else 0 end) as C_PLUS_QTY" + 
				",sum(case when t.MovementType = 'C-' then t.movementqty else 0 end) as C_MINUS_QTY" + 
				",sum(case when t.MovementType = 'I+' then t.movementqty else 0 end) as I_PLUS_QTY" + 
				",sum(case when t.MovementType = 'I-' then t.movementqty else 0 end) as I_MINUS_QTY" + 
				",sum(case when t.MovementType = 'M+' then t.movementqty else 0 end) as M_PLUS_QTY" + 
				",sum(case when t.MovementType = 'M-' then t.movementqty else 0 end) as M_MINUS_QTY" + 
				",sum(case when t.MovementType = 'W+' then t.movementqty else 0 end) as W_PLUS_QTY" + 
				",sum(case when t.MovementType = 'W-' then t.movementqty else 0 end) as W_MINUS_QTY" + 
				",sum(case when t.MovementType = 'P+' then t.movementqty else 0 end) as P_PLUS_QTY" + 
				",sum(case when t.MovementType = 'P-' then t.movementqty else 0 end) as P_MINUS_QTY" + 
				" FROM rv_transaction t "
				+ "left join m_inout io on io.m_inout_id = t.m_inout_id "
				+ "left join c_doctype dt on dt.c_doctype_id = io.c_doctype_id " + 
				" where 1=1 "
				+ "and coalesce(dt.isinternal,'N')<>'Y' " + 
				" and M_Product_ID IN (select distinct m_product_id from t_variancesdistribution_account_cache where ad_pinstance_id ="+getAD_PInstance_ID()
				+" ) " + 
				" AND t.docstatus='CO' " + //only completed calculated 20220708 ISY-61
				" AND t.MovementDate between ? and ? " + 
				" group by" + 
				" t.ad_org_id , t.ad_orgtrx_id , t.c_project_id , t.c_campaign_id ,t.m_warehouse_id, t.m_product_id, t.m_inventoryline_c_charge_id " + 
				" union all " + 
				" SELECT t.ad_org_id , t.ad_orgtrx_id , t.c_project_id , t.c_campaign_id ,t.m_warehouse_id, t.m_product_id ,t.m_inventoryline_c_charge_id " + 
				",sum(t.movementqty) as saldo_awal" + 
				",0 as V_PLUS_QTY" + 
				",0 as V_MINUS_QTY" + 
				",0 as C_PLUS_QTY" + 
				",0 as C_MINUS_QTY" + 
				",0 as I_PLUS_QTY" + 
				",0 as I_MINUS_QTY" + 
				",0 as M_PLUS_QTY" + 
				",0 as M_MINUS_QTY" + 
				",0 as W_PLUS_QTY" + 
				",0 as W_MINUS_QTY" + 
				",0 as P_PLUS_QTY" + 
				",0 as P_MINUS_QTY" + 
				" FROM rv_transaction t " +
//				"left join m_inout io on io.m_inout_id = t.m_inout_id " +
//				"left join c_doctype dt on dt.c_doctype_id = io.c_doctype_id " + 
				" where 1=1 " +
//				isInternal di cabut 20191018 oleh spek dy
//				"and coalesce(dt.isiternal,'N')<>'Y' " +  
				" and M_Product_ID IN (select distinct m_product_id from t_variancesdistribution_account_cache where ad_pinstance_id ="+ getAD_PInstance_ID()+
				" ) " + 
				" AND t.MovementDate < ? " + 
				" group by" + 
				" t.ad_org_id , t.ad_orgtrx_id , t.c_project_id , t.c_campaign_id ,t.m_warehouse_id, t.m_product_id, t.m_inventoryline_c_charge_id ) t" + 
				" group by t.ad_org_id , t.ad_orgtrx_id , t.c_project_id , t.c_campaign_id ,t.m_warehouse_id, t.m_product_id, t.m_inventoryline_c_charge_id" + 
				" order by t.ad_org_id , t.ad_orgtrx_id , t.c_project_id , t.c_campaign_id ,t.m_warehouse_id, t.m_product_id, t.m_inventoryline_c_charge_id";
		int no = DB.executeUpdateEx(sql, params.toArray(), get_TrxName());
		DB.commit(true, get_TrxName());
		log.info(no + " records inserted to table t_variancesdistribution_cache ");

		//insert denomQty , finalQty , 
		sql = "update t_variancesdistribution_account_cache a " + 
				" set denom_qty = q.denom_qty + (-1 * least(q.saldo_akhir,0)) " + //kalau final_qty minus, add to denom_qty
				" ,final_qty = q.saldo_akhir" + 
				" from (select m_product_id " + 
				"	, sum(saldo_awal +v_plus +c_plus +i_plus +w_plus +p_plus) as denom_qty " + //saldo awal minus diabaikan utk denom_qty 
				"	, sum(saldo_akhir) as saldo_akhir" + 
				"	from t_variancesdistribution_cache where ad_pinstance_id =" +getAD_PInstance_ID()+ 
				"	group by m_product_id) q" + 
				" where q.m_product_id = a.m_product_id"+
				" and a.ad_pinstance_id ="+getAD_PInstance_ID();
		no = DB.executeUpdateEx(sql, get_TrxName());
		DB.commit(true, get_TrxName());
		log.info(no + " denomqty and saldo_akhir updated qty on t_variancesdistribution_account_cache ");
		
		//update validcombination account id for : cogs, asset, variances
		sql = "update t_variancesdistribution_account_cache a" + 
				" set cogs_account_id = q.cogs_account_id , asset_account_id = q.asset_account_id , ppv_account_id = q.ppv_account_id" + 
				" from ("+
					" select pa.m_product_id,cvc.account_id cogs_account_id , avc.account_id asset_account_id , ppvvc.account_id ppv_account_id" + 
					" from m_product_acct pa , c_validcombination cvc ,c_validcombination avc ,c_validcombination ppvvc " + 
					" where 1=1" + 
					" and pa.p_cogs_acct = cvc.c_validcombination_id" + 
					" and pa.p_asset_acct = avc.c_validcombination_id" + 
					" and pa.p_purchasepricevariance_acct = ppvvc.c_validcombination_id" + 
					" and pa.c_acctschema_id ="+as.get_ID() +
				") q" + 
				" where q.m_product_id = a.m_product_id"+
				" and a.ad_pinstance_id ="+getAD_PInstance_ID();
		DB.executeUpdateEx(sql.toString(), get_TrxName());
		DB.commit(false, get_TrxName());
		log.info("update accountings accounts_ID for each product, cogs, asset , ppv ");
	}

	/**
	 * B0 WiteOff COGS
	 * @throws SQLException 
	 * @throws IllegalStateException 
	 */
	private void writeOffCogsMassal() throws IllegalStateException, SQLException {
		
		//remove 0 amt records
		String sql = " delete from t_variancesdistribution_account_cache a" + 
				" where abs(round(a.amt,0))=0 and a.ad_pinstance_id ="+getAD_PInstance_ID();
		DB.executeUpdateEx(sql.toString(), get_TrxName());
		DB.commit(false, get_TrxName());
		log.info("deleted recordw with 0 amt (nothing to distribute) ");
		
		//remove writed-off records
		sql = " delete from t_variancesdistribution_account_cache a" + 
				" where abs(a.amt) <= "+p_WriteOffAmt.intValue()+
				" and a.ad_pinstance_id ="+getAD_PInstance_ID();
		DB.executeUpdateEx(sql.toString(), get_TrxName());
		DB.commit(false, get_TrxName());
		log.info("deleted recordw after Write off with certain criteria ");

	}

	private void distributeProductAssetMassal( String info) throws AdempiereException, SQLException {
	String sql ="select wh.ad_org_id as org_id ,q.m_product_id, a.account_id variance_account "
			+ " , coalesce(sum(q.saldo_akhir)/nullif(max(a.denom_qty),0)*max(a.amt),0) var_org_amt,a.asset_account_id" + 
			" from t_variancesdistribution_cache q " + 
			" join m_warehouse wh on wh.m_warehouse_id = q.m_warehouse_id" + 
			" join t_variancesdistribution_account_cache a on a.ad_pinstance_id = q.ad_pinstance_id and a.m_product_id = q.m_product_id" + 
			" where q.ad_pinstance_id = "+getAD_PInstance_ID() + 
			" group by wh.ad_org_id , q.m_product_id, a.account_id ,a.asset_account_id " + 
//			" having coalesce(sum(q.saldo_akhir),0) >= 0 and  coalesce(sum(q.saldo_akhir),0) <= coalesce(max(a.denom_qty),0) " + //stok akhir harus positif dan tidak boleh lebih besar dari denomqty
			" order by q.m_product_id , a.account_id , wh.ad_org_id";
	ResultSet rs = DB.getRowSet(sql);
	while(rs.next()) {
		int orgID = rs.getInt(1);
		int productID = rs.getInt(2);
		int varianceAccountID = rs.getInt(3);
		BigDecimal productVarianceAmt = rs.getBigDecimal(4);
		int asset_account_id = rs.getInt(5);
		
		va = new MVarianceDistribution(getCtx(),0, get_TrxName());
		va.setAD_Org_ID(orgID);
		va.setAD_PInstance_ID(getAD_PInstance_ID());
		va.setAccount_ID(varianceAccountID);
		va.setM_Product_ID(productID);
		if (productVarianceAmt.compareTo(Env.ZERO) > 0) {
			va.setAmtAcctCr(productVarianceAmt.abs());
		} else if (productVarianceAmt.compareTo(Env.ZERO) < 0){
			va.setAmtAcctDr(productVarianceAmt.abs());
		} else {
			MProduct product = new MProduct(getCtx(), productID, get_TrxName());
			addLog(productID, p_DateAcct, productVarianceAmt, info +" product ="+product.getName(),product.get_Table_ID(), productID);
		}
		// A0 
		va.setLine(new Integer(++count * 10));
		va.setDescription(info + " A0");
		va.setDateAcct(dateEnd);
		va.setIsAdjustCOGS(p_IsAdjust);
		va.saveEx();
	
		//
		va = new MVarianceDistribution(getCtx(), 0, get_TrxName());
		va.setAD_Org_ID(orgID);
		va.setAD_PInstance_ID(getAD_PInstance_ID());
		va.setAccount_ID(asset_account_id);
		va.setM_Product_ID(productID);
		va.setAmtAcctDr(Env.ZERO);
		va.setAmtAcctCr(Env.ZERO);
		if (productVarianceAmt.compareTo(Env.ZERO) > 0) {
			va.setAmtAcctDr(productVarianceAmt.abs());
		} else {
			va.setAmtAcctCr(productVarianceAmt.abs());
		}
		va.setLine(new Integer(++count * 10));
		va.setDescription(info + " B1. Product Asset");
		va.setDateAcct(dateEnd);
		va.setIsAdjustCOGS(p_IsAdjust);
		va.saveEx();
		
		// reversal for next month
		if (p_IsAdjust) {
			
			if (reversalMap == null) {
				reversalMap = new HashMap<Integer, MJournal>();
			}
			
			if (reversal == null || reversalMap.get(orgID) == null) {
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(dateEnd);
				calendar.set(calendar.get(Calendar.YEAR),
						calendar.get(Calendar.MONTH), 1);
				calendar.add(Calendar.MONTH, 1);
				reversal = new MJournal(getCtx(), 0, get_TrxName());
				reversal.setAD_Org_ID(orgID);
				reversal.setC_AcctSchema_ID(as.get_ID());
				reversal.setDescription(" Reversal >> Generated from process Variance Distribution "
						+ p_DateAcct);
				reversal.setPostingType(MJournal.POSTINGTYPE_Actual);
				reversal.setC_DocType_ID(SystemID.DOCTYPE_GL_VarAdj);
				reversal.setGL_Category_ID(SystemID.GL_CATEGORY_VarAdj);
				reversal.setDateDoc(new Timestamp(calendar
						.getTimeInMillis()));
				reversal.setDateAcct(new Timestamp(calendar
						.getTimeInMillis()));
				MPeriod.testPeriodOpen(getCtx(),
						new Timestamp(calendar.getTimeInMillis()),
						reversal.getC_DocType_ID(), reversal.getAD_Org_ID());
				reversal.setC_Period_ID(MPeriod.getC_Period_ID(getCtx(),
						new Timestamp(calendar.getTimeInMillis()),
						reversal.getAD_Org_ID()));
				reversal.setC_Currency_ID(as.getC_Currency_ID());
				reversal.setC_ConversionType_ID(MConversionType.TYPE_SPOT);
				reversal.saveEx();
				reversalMap.put(orgID, reversal);
			}
			
			reversal = reversalMap.get(orgID);
			MJournalLine line = null;
			MAccount account = null;
	
			line = new MJournalLine(reversal);
			line.setLine(++countR * 10);
			line.setDescription(info + "Reversal B1. Product Asset");
			line.setM_Product_ID(productID);
			line.setAmtSourceDr(productVarianceAmt.abs());
			line.setAmtSourceCr(Env.ZERO);
			line.setAmtAcct(productVarianceAmt.abs(), Env.ZERO);
			if (productVarianceAmt.compareTo(Env.ZERO) > 0) {
				account = getOrCreateCombination(line, varianceAccountID);
			} else {
				account = getOrCreateCombination(line, asset_account_id);
			}
			if (account != null) {
				line.setC_ValidCombination_ID(account);
			}
			line.setAccount_ID(account.get_ID());
			line.saveEx();
	
			line = new MJournalLine(reversal);
			line.setLine(++countR * 10);
			line.setDescription(info + " Reversal B1. Product Asset");
			line.setM_Product_ID(productID);
			line.setAmtSourceDr(Env.ZERO);
			line.setAmtSourceCr(productVarianceAmt.abs());
			line.setAmtAcct(Env.ZERO, productVarianceAmt.abs());
			if (productVarianceAmt.compareTo(Env.ZERO) > 0) {
				account = getOrCreateCombination(line, asset_account_id);
			} else {
				account = getOrCreateCombination(line, varianceAccountID);
			}
			if (account != null) {
				line.setC_ValidCombination_ID(account);
			}
			line.setAccount_ID(account.get_ID());
			line.saveEx();
			}
		}
	}

	private void distributeMassal(String info) throws AdempiereException, SQLException {
		BigDecimal cogsVarianceAmt;
		BigDecimal vendorReturnAmt;
		BigDecimal productionMinusAmt;
		BigDecimal whDifferenceVarianceAmt;

		// B2A. Warehouse Difference / B2B. Internal Use
		String sql="select q.ad_org_id,q.ad_orgtrx_id,q.c_project_id,q.c_campaign_id,a.m_product_id,a.account_id,coalesce(cvc.account_id,whc.account_id) diff_account_id" + 
				" , least(SUM(q.i_minus)*-1/max(a.denom_qty),1)*max(a.amt) diff_amt" +  // qty * -1 (negate)  //20210427 ratio tdk bisa lebih dari 1, tambah least( #,1)
				" from t_variancesdistribution_cache q " + 
				" join m_warehouse wh on wh.m_warehouse_id = q.m_warehouse_id" + 
				" join m_warehouse_acct wa on wa.m_warehouse_id = wh.m_warehouse_id and wa.c_acctschema_id ="+as.get_ID() + 
				" left join c_validcombination whc on whc.c_validcombination_id = wa.w_differences_acct and whc.c_acctschema_id ="+as.get_ID() +
				" left join c_charge_acct ca on ca.c_charge_id = q.m_inventoryline_c_charge_id" + 
				" left join c_validcombination cvc on cvc.c_validcombination_id = ca.ch_expense_acct and cvc.c_acctschema_id ="+as.get_ID() +
				" join t_variancesdistribution_account_cache a on a.ad_pinstance_id = q.ad_pinstance_id and a.m_product_id = q.m_product_id" + 
				" where q.ad_pinstance_id ="+getAD_PInstance_ID() +
				" group by q.ad_org_id,q.ad_orgtrx_id,q.c_project_id,q.c_campaign_id,a.m_product_id,a.account_id,coalesce(cvc.account_id,whc.account_id)" + 
				" having SUM(q.i_minus) < 0"+
				" and max(a.denom_qty) > 0" + //20210427denom_qty harus positif
				" order by m_product_id , ad_org_id ,ad_orgtrx_id ,c_project_id ,c_campaign_id,a.account_id";
		ResultSet rs = DB.getRowSet(sql);
		while(rs.next()) {
			int orgID = rs.getInt(1);
			int orgTrxID = rs.getInt(2);
			int projectID = rs.getInt(3);
			int campaignID = rs.getInt(4);
			int productID = rs.getInt(5);
			int varianceAccountID = rs.getInt(6);
			int accountID = rs.getInt(7);
			whDifferenceVarianceAmt = rs.getBigDecimal(8);
			if (whDifferenceVarianceAmt == null) whDifferenceVarianceAmt=Env.ZERO;
			
			va = new MVarianceDistribution(getCtx(), 0, get_TrxName());
			va.setAD_Org_ID(orgID);
			va.setAD_PInstance_ID(getAD_PInstance_ID());
			va.setAccount_ID(varianceAccountID);
			va.setM_Product_ID(productID);
			if (whDifferenceVarianceAmt.compareTo(Env.ZERO) > 0) {
				va.setAmtAcctCr(whDifferenceVarianceAmt.abs());
				} else {
				va.setAmtAcctDr(whDifferenceVarianceAmt.abs());
				}
			va.setLine(new Integer(++count * 10));
			va.setDescription(info + " B2A. ( I+ , I- )");
			va.setDateAcct(dateEnd);
			va.setIsAdjustCOGS(p_IsAdjust);
			va.saveEx();
			
			va = new MVarianceDistribution(getCtx(), 0, get_TrxName());
			va.setAD_Org_ID(orgID);
			va.setAD_OrgTrx_ID(orgTrxID);
			va.setAD_PInstance_ID(getAD_PInstance_ID());
			va.setAccount_ID(accountID);
			va.setC_Project_ID(projectID);
			va.setC_Campaign_ID(campaignID);
			va.setM_Product_ID(productID);
			if (whDifferenceVarianceAmt.compareTo(Env.ZERO) > 0) {
				va.setAmtAcctDr(whDifferenceVarianceAmt.abs());
			} else {
				va.setAmtAcctCr(whDifferenceVarianceAmt.abs());
			}
			va.setLine(new Integer(++count * 10));
			va.setDescription(info + " B2A. I+ I- ");
			va.setDateAcct(dateEnd);
			va.setIsAdjustCOGS(p_IsAdjust);
			va.saveEx();
		} 
	
	// B2D. Vendor Return
		sql="select q.ad_org_id,q.ad_orgtrx_id,q.c_project_id,q.c_campaign_id,a.m_product_id,a.account_id,a.ppv_account_id" + 
				" , least(SUM(q.v_minus)*-1/max(a.denom_qty),1)*max(a.amt) diff_amt" +  // qty * -1 (negate)
				" from t_variancesdistribution_cache q " + 
				" join m_warehouse wh on wh.m_warehouse_id = q.m_warehouse_id" + 
				" join m_warehouse_acct wa on wa.m_warehouse_id = wh.m_warehouse_id and wa.c_acctschema_id ="+as.get_ID() + 
				" join c_validcombination vc on vc.c_validcombination_id = wa.w_differences_acct" + 
				" left join c_charge_acct ca on ca.c_charge_id = q.m_inventoryline_c_charge_id" + 
				" left join c_validcombination cvc on cvc.c_validcombination_id = ca.ch_expense_acct and cvc.c_acctschema_id ="+as.get_ID() +
				" join t_variancesdistribution_account_cache a on a.ad_pinstance_id = q.ad_pinstance_id and a.m_product_id = q.m_product_id" + 
				" where q.ad_pinstance_id ="+getAD_PInstance_ID() +
				" group by q.ad_org_id,q.ad_orgtrx_id,q.c_project_id,q.c_campaign_id,a.m_product_id,a.account_id,a.ppv_account_id" + 
				" having SUM(q.v_minus) < 0"+
				" and max(a.denom_qty) > 0" + //20210427denom_qty harus positif
				" order by m_product_id , ad_org_id ,ad_orgtrx_id ,c_project_id ,c_campaign_id,a.account_id";
		rs = DB.getRowSet(sql);
		while(rs.next()) {
			
			int orgID = rs.getInt(1);
			int orgTrxID = rs.getInt(2);
			int projectID = rs.getInt(3);
			int campaignID = rs.getInt(4);
			int productID = rs.getInt(5);
			int varianceAccountID = rs.getInt(6);
			int ppv_accountID = rs.getInt(7);
			vendorReturnAmt = rs.getBigDecimal(8);

			va = new MVarianceDistribution(getCtx(),0, get_TrxName());
			va.setAD_Org_ID(orgID);
			va.setAD_PInstance_ID(getAD_PInstance_ID());
			va.setAccount_ID(varianceAccountID);
			va.setM_Product_ID(productID);
			if (vendorReturnAmt.compareTo(Env.ZERO) > 0) {
				va.setAmtAcctCr(vendorReturnAmt.abs());
			} else {
				va.setAmtAcctDr(vendorReturnAmt.abs());
			}
			va.setLine(new Integer(++count * 10));
			va.setDescription(info + " B2D. (V-)");
			va.setDateAcct(dateEnd);
			va.setIsAdjustCOGS(p_IsAdjust);
			va.saveEx();
			
			// counter journal
			va = new MVarianceDistribution(getCtx(), 0, get_TrxName());
			va.setAD_Org_ID(orgID);
			va.setAD_OrgTrx_ID(orgTrxID);
			va.setAD_PInstance_ID(getAD_PInstance_ID());
			va.setAccount_ID(ppv_accountID);
			va.setC_Project_ID(projectID);
			va.setC_Campaign_ID(campaignID);
			va.setM_Product_ID(productID);
			if (vendorReturnAmt.compareTo(Env.ZERO) > 0) {
				va.setAmtAcctDr(vendorReturnAmt.abs());
			} else {
				va.setAmtAcctCr(vendorReturnAmt.abs());
			}
			va.setLine(new Integer(++count*10));
			va.setDescription(info + " B2D. Vendor Return");
			va.setDateAcct(dateEnd);
			va.setIsAdjustCOGS(p_IsAdjust);
			va.saveEx();
			}

		
		// B2E. Production Minus
		sql="select q.ad_org_id,a.m_product_id,a.account_id" + 
				" , least(SUM(q.p_minus)*-1/max(a.denom_qty),1)*max(a.amt) diff_amt" +  // qty * -1 (negate)
				" from t_variancesdistribution_cache q " + 
				" join t_variancesdistribution_account_cache a on a.ad_pinstance_id = q.ad_pinstance_id and a.m_product_id = q.m_product_id" + 
				" where q.ad_pinstance_id ="+getAD_PInstance_ID() +
				" group by q.ad_org_id,a.m_product_id,a.account_id" + 
				" having SUM(q.p_minus) <> 0"+
				" and max(a.denom_qty) > 0" + //20210427denom_qty harus positif
				" order by m_product_id , ad_org_id ,a.account_id";
		rs = DB.getRowSet(sql);
		while(rs.next()) {
			
			int orgID = rs.getInt(1);
			int productID = rs.getInt(2);
			int varianceAccountID = rs.getInt(3);
			productionMinusAmt = rs.getBigDecimal(4);
			
			String sqlProduction = "select pl.m_product_id, p.m_product_id fg_id , fgvc.account_id fg_account_id "
					+ ", least (sum(f.qty) / sum(sum(f.qty)) over w , 1) as ratio  "
					+ " from fact_acct f  "
					+ " join m_production p on p.m_production_id = f.record_id and f.ad_table_id= 325 and p.docstatus not in ('RE','VO') "
					+ " join m_productionline pl on pl.m_productionline_id = f.line_id and pl.m_production_id = p.m_production_id  "
					+ " join m_product_acct fgpa on fgpa.m_product_id = p.m_product_id and fgpa.c_acctschema_id = f.c_acctschema_id  "
					+ " join c_validcombination fgvc on fgvc.c_validcombination_id = fgpa.p_ratevariance_acct  "
					+ " where f.ad_org_id = " +orgID
					+ " and f.dateacct between "+DB.TO_DATE(dateStart)+" and " +DB.TO_DATE(dateEnd)
					+ " and p.m_product_id <> pl.m_product_id  "
					+ " and pl.m_product_id = " + productID
					+ " group by 1,2,3 "
					+ " having sum(f.qty) <> 0 "
					+ " window w as (partition by pl.m_product_id)";
			
			ResultSet rsp = DB.getRowSet(sqlProduction);
			while (rsp.next()) {
				int fgID = rsp.getInt(2);
				int rateVarAccountID = rsp.getInt(3);
				BigDecimal fgRatio = rsp.getBigDecimal(4);
				BigDecimal fgAmt = fgRatio.multiply(productionMinusAmt).setScale(4, BigDecimal.ROUND_HALF_UP);
					va = new MVarianceDistribution(getCtx(),0, get_TrxName());
					va.setAD_Org_ID(orgID);
					va.setAD_PInstance_ID(getAD_PInstance_ID());
					va.setAccount_ID(varianceAccountID);
					va.setM_Product_ID(productID);
					if (fgAmt.compareTo(Env.ZERO) > 0) {
						va.setAmtAcctCr(fgAmt.abs());
					} else {
						va.setAmtAcctDr(fgAmt.abs());
					}
					va.setLine(new Integer(++count * 10));
					va.setDescription(info + " B2E. (P-)");
					va.setDateAcct(dateEnd);
					va.setIsAdjustCOGS(p_IsAdjust);
					va.saveEx();
					
					// counter journal (geser ke fg)
					va = new MVarianceDistribution(getCtx(), 0, get_TrxName());
					va.setAD_Org_ID(orgID);
					va.setAD_PInstance_ID(getAD_PInstance_ID());
					va.setAccount_ID(rateVarAccountID);
					va.setM_Product_ID(fgID);
					if (fgAmt.compareTo(Env.ZERO) > 0) {
						va.setAmtAcctDr(fgAmt.abs());
					} else {
						va.setAmtAcctCr(fgAmt.abs());
					}
					va.setLine(new Integer(++count*10));
					va.setDescription(info + " B2E. Production Minus");
					va.setDateAcct(dateEnd);
					va.setIsAdjustCOGS(p_IsAdjust);
					va.saveEx();
				}
			}
			
		// B2C. COGS
		sql="select q.ad_org_id,q.ad_orgtrx_id,q.c_project_id,q.c_campaign_id,a.m_product_id,a.account_id,a.cogs_account_id" + 
				" , least(SUM(q.c_minus)*-1/max(a.denom_qty),1)*max(a.amt) diff_amt" +  // qty * -1 (negate)
				" from t_variancesdistribution_cache q " + 
				" join m_warehouse wh on wh.m_warehouse_id = q.m_warehouse_id" + 
				" join m_warehouse_acct wa on wa.m_warehouse_id = wh.m_warehouse_id and wa.c_acctschema_id ="+as.get_ID() + 
				" join c_validcombination vc on vc.c_validcombination_id = wa.w_differences_acct" + 
				" left join c_charge_acct ca on ca.c_charge_id = q.m_inventoryline_c_charge_id" + 
				" left join c_validcombination cvc on cvc.c_validcombination_id = ca.ch_expense_acct and cvc.c_acctschema_id ="+as.get_ID() +
				" join t_variancesdistribution_account_cache a on a.ad_pinstance_id = q.ad_pinstance_id and a.m_product_id = q.m_product_id" + 
				" where q.ad_pinstance_id ="+getAD_PInstance_ID() +
				" group by q.ad_org_id,q.ad_orgtrx_id,q.c_project_id,q.c_campaign_id,a.m_product_id,a.account_id,a.cogs_account_id" + 
				" having SUM(q.c_minus) < 0"+
				" and max(a.denom_qty) > 0" + //20210427denom_qty harus positif
				" order by m_product_id , ad_org_id ,ad_orgtrx_id ,c_project_id ,c_campaign_id,a.account_id";
		rs = DB.getRowSet(sql);
		while(rs.next()) {
			MVarianceDistribution va = new MVarianceDistribution(getCtx(),0, get_TrxName());
			
			int orgID = rs.getInt(1);
			int orgTrxID = rs.getInt(2);
			int projectID = rs.getInt(3);
			int campaignID = rs.getInt(4);
			int productID = rs.getInt(5);
			int varianceAccountID = rs.getInt(6);
			int cogs_accountID = rs.getInt(7);
			cogsVarianceAmt = rs.getBigDecimal(8);

			va.setAD_Org_ID(orgID);
			va.setAD_PInstance_ID(getAD_PInstance_ID());
			va.setAccount_ID(varianceAccountID);
			va.setM_Product_ID(productID);
			if (cogsVarianceAmt.compareTo(Env.ZERO) > 0) {
				va.setAmtAcctCr(cogsVarianceAmt.abs());
			} else {
				va.setAmtAcctDr(cogsVarianceAmt.abs());
			}
			va.setLine(new Integer(++count * 10));
			va.setDescription(info + " B2C. COGS");
			va.setDateAcct(dateEnd);
			va.setIsAdjustCOGS(p_IsAdjust);
			va.saveEx();
	
			//
			va = new MVarianceDistribution(getCtx(), 0, get_TrxName());
			va.setAD_Org_ID(orgID);
			va.setAD_OrgTrx_ID(orgTrxID);
			va.setAD_PInstance_ID(getAD_PInstance_ID());
			va.setAccount_ID(cogs_accountID);
			va.setC_Project_ID(projectID);
			va.setC_Campaign_ID(campaignID);
			va.setM_Product_ID(productID);
			if (cogsVarianceAmt.compareTo(Env.ZERO) > 0) {
				va.setAmtAcctDr(cogsVarianceAmt.abs());
			} else {
				va.setAmtAcctCr(cogsVarianceAmt.abs());
			}
			va.setLine(new Integer(++count * 10));
			va.setDescription(info + " B2C. COGS C-");
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
					journal.setDescription("Generated from process Variance Distribution "
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
				}
				line = new MJournalLine(journal);
				line.setLine(new Integer(va.getLine()));
				line.setDescription(va.getDescription());
				if (va.getC_Project_ID() > 0) {
					line.setC_Project_ID(va.getC_Project_ID());
				}
				line.setM_Product_ID(va.getM_Product_ID());
				line.setC_Campaign_ID(va.getC_Campaign_ID());
				line.setAmtSourceDr(va.getAmtAcctDr());
				line.setAmtSourceCr(va.getAmtAcctCr());
				line.setAmtAcct(va.getAmtAcctDr(), va.getAmtAcctCr());
				account = getOrCreateCombination(va);
				if (account != null) {
					line.setC_ValidCombination_ID(account);
				}
				line.setAccount_ID(va.getAccount_ID());
				line.setAD_OrgTrx_ID(va.getAD_OrgTrx_ID());
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

	private MAccount getOrCreateCombination(MJournalLine line, int accountID) {
		return MAccount.get(getCtx(), line.getAD_Client_ID(),
				line.getAD_Org_ID(), as.get_ID(), accountID,
				line.getC_SubAcct_ID(), line.getM_Product_ID(),
				line.getC_BPartner_ID(), line.getAD_OrgTrx_ID(),
				line.getC_LocFrom_ID(), line.getC_LocTo_ID(),
				line.getC_SalesRegion_ID(), line.getC_Project_ID(),
				line.getC_Campaign_ID(), line.getC_Activity_ID(),
				line.getUser1_ID(), line.getUser2_ID(), 0, 0,get_TrxName());
	} // getOrCreateCombination

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
}
