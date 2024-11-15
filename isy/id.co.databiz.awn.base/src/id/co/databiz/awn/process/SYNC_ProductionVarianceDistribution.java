package id.co.databiz.awn.process;

import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.DBException;
import org.compiere.model.MAccount;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MClient;
import org.compiere.model.MConversionType;
import org.compiere.model.MElementValue;
import org.compiere.model.MFactAcct;
import org.compiere.model.MJournal;
import org.compiere.model.MJournalLine;
import org.compiere.model.MOrg;
import org.compiere.model.MPeriod;
import org.compiere.model.MProduct;
import org.compiere.model.MProductCategory;
import org.compiere.model.MProductCategoryAcct;
import org.compiere.model.MSysConfig;
import org.compiere.model.MTransaction;
import org.compiere.model.MWarehouse;
import org.compiere.model.Query;
import org.compiere.model.X_M_Product_Acct;
import org.compiere.model.X_M_Warehouse_Acct;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.eevolution.model.MPPOrder;

import id.co.databiz.awn.model.MVarianceDistribution;
import id.co.databiz.awn.model.Sync;

/**
 *	
 *
 * 	@author 	Anozi Mada
 * 	
 */
public class SYNC_ProductionVarianceDistribution extends SvrProcess
{
	private Timestamp p_DateAcct = null;	
	private boolean p_IsAdjust = false;
	
	private MAcctSchema as = null;
		
	private MElementValue wipVarianceAllocationAcct = null;
	private MElementValue fgVarianceAllocationAcct = null;
	
	private Timestamp dateStart = null;
	private Timestamp dateEnd = null;
	
	
	/**
	 *  Prepare - e.g., get Parameters.
	 */
	protected void prepare()
	{
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;			
			else if (name.equals("DateAcct"))
				p_DateAcct = (Timestamp)para[i].getParameter();
			else if (name.equals("IsAdjustCOGS"))
				p_IsAdjust = "Y".equals(para[i].getParameter());						
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
		
		dateEnd = p_DateAcct;
		Calendar startCalendar = Calendar.getInstance();
		startCalendar.setTimeInMillis(p_DateAcct.getTime());
		startCalendar.set(Calendar.DAY_OF_MONTH, 1);
		dateStart = new Timestamp(startCalendar.getTimeInMillis());
	}	//	prepare


	/**
	 *  Perform process.
	 *  @return Message
	 *  @throws Exception
	 */
	protected String doIt() throws java.lang.Exception
	{
		log.info("Start Production Variance Distribution");
		String info = "";
		int count = 0;
		int countR = 0;
		int errorCount = 0;
		
		int reversalOrgID = 0;
		int journalOrgID = 0;
		 
//		Cleaned up via house keeping SYNC-545
//		String sqlDelete = "DELETE T_VarianceDistribution";
//		int no = DB.executeUpdate(sqlDelete, null);
//		if (no > 0)
//			log.info("Deleted #" + no);
		
		MJournal reversal = null;
		as = MClient.get(getCtx()).getAcctSchema();
		
		// Accounts to be distributed
		String wipVarianceAllocationValue = MSysConfig.getValue(Sync.SYS_Acct_WIPVarianceAllocation, Env.getAD_Client_ID(Env.getCtx()));
		String fgVarianceAllocationValue = MSysConfig.getValue(Sync.SYS_Acct_FGVarianceAllocation, Env.getAD_Client_ID(Env.getCtx()));
		
		wipVarianceAllocationAcct = new Query(getCtx(), MElementValue.Table_Name, "Value = ?", get_TrxName())
										.setOnlyActiveRecords(true)
										.setParameters(new Object[]{wipVarianceAllocationValue})
										.first();
		fgVarianceAllocationAcct = new Query(getCtx(), MElementValue.Table_Name, "Value = ?", get_TrxName())
										.setOnlyActiveRecords(true)
										.setParameters(new Object[]{fgVarianceAllocationValue})
										.first();
		if(wipVarianceAllocationAcct == null)
			throw new AdempiereException("SYNC-518\nInvalid System Configurator value " + Sync.SYS_Acct_WIPVarianceAllocation + ": " + wipVarianceAllocationValue);
		
		if(fgVarianceAllocationAcct == null)
			throw new AdempiereException("SYNC-518\nInvalid System Configurator value " + Sync.SYS_Acct_FGVarianceAllocation + ": " + fgVarianceAllocationValue);
		
		List<String> varianceAccounts = new ArrayList<String>();
		varianceAccounts.add("pa.p_ratevariance_acct");
		varianceAccounts.add("pa.p_methodchangevariance_acct");
		varianceAccounts.add("pa.p_mixvariance_acct");
		varianceAccounts.add("pa.p_usagevariance_acct");
//		if(wipVarianceAllocationAcct != null)
//			varianceAccounts.add(String.valueOf(wipVarianceAllocationAcct.get_ID()));
		if(fgVarianceAllocationAcct != null)
			varianceAccounts.add(String.valueOf(fgVarianceAllocationAcct.get_ID()));
		
		List<MProductCategory> productCategoryList = getProductCategoryStandardCosting(as);
		for(String varianceAccount : varianceAccounts){
			StringBuffer sql = new StringBuffer();
			sql.append("SELECT SUM(fa.AmtAcctDr - fa.AmtAcctCr) Amt, fa.AD_Org_ID,fa.C_Project_ID,fa.M_Product_ID,fa.Account_ID ");
			sql.append("FROM Fact_Acct fa ");
			sql.append("INNER JOIN M_Product p ON (p.M_Product_ID = fa.M_Product_ID) ");
			sql.append("INNER JOIN M_Product_Acct pa ON (pa.M_Product_ID = p.M_Product_ID AND pa.C_AcctSchema_ID = fa.C_AcctSchema_ID) ");
			if(!varianceAccount.equals(String.valueOf(fgVarianceAllocationAcct.get_ID())))
				sql.append("INNER JOIN C_ValidCombination vc ON (vc.C_ValidCombination_ID = ").append(varianceAccount).append(") ");
			sql.append("WHERE 1=1 ");
			sql.append(" AND fa.Account_ID = ");
			if(varianceAccount.equals(String.valueOf(fgVarianceAllocationAcct.get_ID())))
				sql.append(varianceAccount);
			else
				sql.append("vc.Account_ID");
			sql.append(" AND p.ProductType = 'I' ");
			sql.append("AND p.IsStocked = 'Y' ");
			sql.append("AND fa.C_AcctSchema_ID = ? "); // #1
			sql.append("AND (fa.DateAcct BETWEEN ? AND ?) ");			// #2..#3
			if(productCategoryList.isEmpty()){
				sql.append("AND 1 = 0 ");
			} else {
				sql.append("AND p.M_Product_Category_ID IN (");
				for(MProductCategory pc : productCategoryList){
					if(!productCategoryList.get(0).equals(pc)){
						sql.append(",");
					}
					sql.append(pc.get_ID());
				}
				sql.append(")");
			}
			sql.append("GROUP BY fa.AD_Org_ID,fa.Account_ID,fa.C_Project_ID,fa.M_Product_ID");
			
			List<Object> params = new ArrayList<Object>();
			params.add(as.get_ID());
			if(!varianceAccount.equals(fgVarianceAllocationValue) && !varianceAccount.equals(wipVarianceAllocationValue)){
				params.add(dateStart);
				params.add(dateEnd);
			}
			
			PreparedStatement pstmt = null;
			 ResultSet rs = null;
			 try
			 {			 
			      pstmt = DB.prepareStatement(sql.toString(), get_TrxName());
			      DB.setParameters(pstmt, params);
			      rs = pstmt.executeQuery();
			      while(rs.next())
			      {
			          BigDecimal varianceAmt = rs.getBigDecimal("Amt");
			          varianceAmt = varianceAmt.setScale(as.getC_Currency().getStdPrecision(), BigDecimal.ROUND_HALF_UP);
			          int orgID = rs.getInt("AD_Org_ID");
			          int projectID = rs.getInt("C_Project_ID");
			          int productID = rs.getInt("M_Product_ID");
			          int varianceAccountID = rs.getInt("Account_ID");
			          X_M_Product_Acct pa = new Query(getCtx(), X_M_Product_Acct.Table_Name, "M_Product_ID = ? AND C_AcctSchema_ID = ?", get_TrxName())
			          							.setParameters(new Object[]{productID,as.get_ID()})
			          							.first();
			          log.info("AD_Org_ID=" + orgID + ", Account_ID=" + varianceAccountID + ", M_Product_ID=" + productID + ", Amt=" + varianceAmt);
			          if(varianceAmt.setScale(0).compareTo(Env.ZERO)!=0){
			        	  BigDecimal initialQty;
			        	  BigDecimal finalQty;
			        	  BigDecimal receiptQty;
			        	  BigDecimal orderReceiptQty;
			        	  BigDecimal returnQty;
			        	  BigDecimal denomQty;
			        	  
			        	  BigDecimal productVarianceAmt;
			        	  BigDecimal differenceVarianceAmt;
			        	  BigDecimal cogsVarianceAmt;
			        	  BigDecimal wipVarianceAmt;
			        	  BigDecimal fgVarianceAllocationAmt;
			        	  
			        	  String sqlSelect = "SELECT COALESCE(SUM(MovementQty),0) FROM RV_Transaction WHERE ";
			        	  String sqlWhere = "AD_Org_ID = ? AND COALESCE(C_Project_ID,0) = ? AND M_Product_ID = ? AND MovementDate < ?";
			        	  initialQty = DB.getSQLValueBDEx(get_TrxName(), sqlSelect+sqlWhere, orgID,projectID,productID,dateStart);
//			        	  initialQty = new Query(getCtx(), MTransaction.Table_Name, "AD_Org_ID = ? AND M_Product_ID = ? AND MovementDate < ?", get_TrxName()).setParameters(new Object[]{orgID,productID,dateStart}).sum("MovementQty");
			        	  if(initialQty==null){
			        		  initialQty = Env.ZERO;
			        	  }
			        	  
			        	  sqlWhere = "AD_Org_ID = ? AND COALESCE(C_Project_ID,0) = ? AND M_Product_ID = ? AND MovementDate <= ?";
			        	  finalQty = DB.getSQLValueBDEx(get_TrxName(), sqlSelect+sqlWhere, orgID,projectID,productID,dateEnd);
//			        	  finalQty = new Query(getCtx(), MTransaction.Table_Name, "AD_Org_ID = ? AND M_Product_ID = ? AND MovementDate <= ?", get_TrxName()).setParameters(new Object[]{orgID,productID,dateEnd}).sum("MovementQty");
			        	  if(finalQty==null){
							finalQty = Env.ZERO;
			        	  }
			        	  
			        	  sqlWhere = "AD_Org_ID = ? AND COALESCE(C_Project_ID,0) = ? AND MovementType = 'V+' AND M_Product_ID = ? AND MovementDate BETWEEN ? AND ?";
			        	  receiptQty = DB.getSQLValueBDEx(get_TrxName(), sqlSelect+sqlWhere, orgID,projectID,productID,dateStart,dateEnd);
//			        	  receiptQty = new Query(getCtx(), MTransaction.Table_Name, "AD_Org_ID = ? AND MovementType = 'V+' AND M_Product_ID = ? AND MovementDate BETWEEN ? AND ?", get_TrxName()).setParameters(new Object[]{orgID,productID,dateStart,dateEnd}).sum("MovementQty");
			        	  if(receiptQty==null){
			        		  receiptQty = Env.ZERO;
			        	  }
			        	  
			        	  sqlWhere = "AD_Org_ID = ? AND COALESCE(C_Project_ID,0) = ? AND MovementType = 'V-' AND M_Product_ID = ? AND MovementDate BETWEEN ? AND ?";
			        	  returnQty = DB.getSQLValueBDEx(get_TrxName(), sqlSelect+sqlWhere, orgID,projectID,productID,dateStart,dateEnd);
//			        	  returnQty = new Query(getCtx(), MTransaction.Table_Name, "AD_Org_ID = ? AND MovementType = 'V-' AND M_Product_ID = ? AND MovementDate BETWEEN ? AND ?", get_TrxName()).setParameters(new Object[]{orgID,productID,dateStart,dateEnd}).sum("MovementQty");
			        	  if(returnQty==null){
			        		  returnQty = Env.ZERO;
			        	  }
			        	  
			        	  sqlWhere = "AD_Org_ID = ? AND COALESCE(C_Project_ID,0) = ? AND MovementType = 'W+' AND M_Product_ID = ? AND MovementDate BETWEEN ? AND ?";
			        	  orderReceiptQty = DB.getSQLValueBDEx(get_TrxName(), sqlSelect+sqlWhere, orgID,projectID,productID,dateStart,dateEnd);
//			        	  orderReceiptQty = new Query(getCtx(), MTransaction.Table_Name, "AD_Org_ID = ? AND MovementType = 'W+' AND M_Product_ID = ? AND MovementDate BETWEEN ? AND ?", get_TrxName()).setParameters(new Object[]{orgID,productID,dateStart,dateEnd}).sum("MovementQty");
			        	  if(orderReceiptQty==null){
			        		  orderReceiptQty = Env.ZERO;
			        	  }
			        	  
			        	  denomQty = initialQty.add(receiptQty).add(returnQty);
//			        	  if(varianceAccountID == fgVarianceAllocationAcct.get_ID() || varianceAccountID == wipVarianceAllocationAcct.get_ID())
			        		  denomQty = denomQty.add(orderReceiptQty);
			        		  
			        	  if (denomQty.compareTo(Env.ZERO) == 0) {
			        		  errorCount++;
			        		  addLog("Org=" + MOrg.get(getCtx(), orgID).getValue() +
				        		  		", Account=" +  varianceAccountID +
				        		  		", Amount=" + varianceAmt +
				        		  		", Product=" + MProduct.get(getCtx(), productID).getValue() +
				        		  		", InitialQty=" + initialQty +
				        		  		", FinalQty=" + finalQty +
				        		  		", ReceiptQty=" + receiptQty +
				        		  		", ReturnQty=" + returnQty +
				        		  		", OrderReceiptQty=" + orderReceiptQty);
			        		  continue;
			        	  }
			        	  
			        	  log.info("InitialQty=" + initialQty + ", FinalQty=" + finalQty + ", ReceiptQty=" + receiptQty + ", ReturnQty=" + returnQty + ", OrderReceiptQty" + orderReceiptQty);
			        	  if(finalQty.equals(Env.ZERO) && denomQty.equals(Env.ZERO)){
			        		  log.info("B0. Write off COGS Amt=" + varianceAmt);
			        		  MVarianceDistribution va = new MVarianceDistribution(getCtx(), 0, get_TrxName());
			        		  va.setAD_Org_ID(orgID);
			        		  va.setAD_PInstance_ID(getAD_PInstance_ID());
			        		  va.setAccount_ID(varianceAccountID);
			        		  va.setC_Project_ID(projectID);
			        		  va.setM_Product_ID(productID);
			        		  if(varianceAmt.compareTo(Env.ZERO)>0){
			        			  va.setAmtAcctCr(varianceAmt.abs());
			        		  } else {
			        			  va.setAmtAcctDr(varianceAmt.abs());
			        		  }		        		  
			        		  va.setLine(new Integer(++count * 10));
			        		  va.setDescription("B0. Write off to COGS");
			        		  va.setDateAcct(dateEnd);
			        		  va.setIsAdjustCOGS(p_IsAdjust);
			        		  va.saveEx();
			        		  
			        		  //
			        		  va = new MVarianceDistribution(getCtx(), 0, get_TrxName());
			        		  va.setAD_Org_ID(orgID);
			        		  va.setAD_PInstance_ID(getAD_PInstance_ID());
			        		  va.setAccount_ID(pa.getP_COGS_A().getAccount_ID());
			        		  va.setC_Project_ID(projectID);
			        		  va.setM_Product_ID(productID);
			        		  if(varianceAmt.compareTo(Env.ZERO)>0){
			        			  va.setAmtAcctDr(varianceAmt.abs());
			        		  } else {
			        			  va.setAmtAcctCr(varianceAmt.abs());
			        		  }		        		  
			        		  va.setLine(new Integer(++count * 10));
			        		  va.setDescription("B0. Write off to COGS");
			        		  va.setDateAcct(dateEnd);
			        		  va.setIsAdjustCOGS(p_IsAdjust);
			        		  va.saveEx();
			        		  
			        		  continue;
			        	  }
			        	  
			        	  // B1. Product Asset
			        	  if (finalQty.compareTo(Env.ZERO) == 0) {
			        		  productVarianceAmt = Env.ZERO;
			        	  } else {
			        		  productVarianceAmt = varianceAmt.multiply(finalQty.divide(denomQty, MathContext.DECIMAL128));
			        	  }
			        	 
			        	  if(productVarianceAmt.compareTo(Env.ZERO)!=0){
			        		  log.info("B1. Product Asset Amt=" + productVarianceAmt);
			        		  MVarianceDistribution va = new MVarianceDistribution(getCtx(), 0, get_TrxName());
			        		  va.setAD_Org_ID(orgID);
				        	  va.setAD_PInstance_ID(getAD_PInstance_ID());
				        	  va.setAccount_ID(varianceAccountID);
				        	  va.setC_Project_ID(projectID);
				        	  va.setM_Product_ID(productID);
				        	  if(productVarianceAmt.compareTo(Env.ZERO)>0){
				        		  va.setAmtAcctCr(productVarianceAmt.abs());
				        	  } else {
				        		  va.setAmtAcctDr(productVarianceAmt.abs());
				        	  }
				        	  va.setLine(new Integer(++count * 10));
				        	  va.setDescription("B1. Product Asset");
				        	  va.setDateAcct(dateEnd);
				        	  va.setIsAdjustCOGS(p_IsAdjust);
				        	  va.saveEx();
				        	  
				        	  // 
				        	  va = new MVarianceDistribution(getCtx(), 0, get_TrxName());
				        	  va.setAD_Org_ID(orgID);
				        	  va.setAD_PInstance_ID(getAD_PInstance_ID());
				        	  va.setAccount_ID(pa.getP_Asset_A().getAccount_ID());
				        	  va.setC_Project_ID(projectID);
				        	  va.setM_Product_ID(productID);
				        	  va.setAmtAcctDr(Env.ZERO);
				        	  va.setAmtAcctCr(Env.ZERO);
				        	  if(productVarianceAmt.compareTo(Env.ZERO)>0){
				        		  va.setAmtAcctDr(productVarianceAmt.abs());
				        	  } else {
				        		  va.setAmtAcctCr(productVarianceAmt.abs());
				        	  }
				        	  va.setLine(new Integer(++count * 10));
				        	  va.setDescription("B1. Product Asset");
				        	  va.setDateAcct(dateEnd);
				        	  va.setIsAdjustCOGS(p_IsAdjust);
				        	  va.saveEx();
			        	  }
			        	  
			        	  // reversal for next month
			        	  if(p_IsAdjust){
			        		  if(reversal==null || reversalOrgID != orgID){
			        			  reversalOrgID = orgID;
			        			  Calendar calendar = Calendar.getInstance();
			        			  calendar.setTime(dateEnd);
			        			  calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), 1);
			        			  calendar.add(Calendar.MONTH, 1);
			        			  reversal = new MJournal(getCtx(), 0, get_TrxName());
			        			  reversal.setAD_Org_ID(orgID);
			        			  reversal.setC_AcctSchema_ID(as.get_ID());
			        			  reversal.setDescription(" Reversal >> Generated from process Variance Distribution " + p_DateAcct);
			        			  reversal.setPostingType(MJournal.POSTINGTYPE_Actual);
			        			  reversal.setC_DocType_ID(Sync.DOCTYPE_GL_VarAdj);
			        			  reversal.setGL_Category_ID(Sync.GL_CATEGORY_VarAdj);
			        			  reversal.setDateDoc(new Timestamp(calendar.getTimeInMillis()));
			        			  reversal.setDateAcct(new Timestamp(calendar.getTimeInMillis()));
			        			  reversal.setC_Period_ID(MPeriod.getC_Period_ID(getCtx(), new Timestamp(calendar.getTimeInMillis()),reversal.getAD_Org_ID()));
			        			  reversal.setC_Currency_ID(as.getC_Currency_ID());
			        			  reversal.setC_ConversionType_ID(MConversionType.TYPE_SPOT);
			        			  reversal.saveEx();
			        		  }
			        		 
			        		  MJournalLine line = null;
			        		  MAccount account = null;
							 
			        		 line = new MJournalLine(reversal);
							 line.setLine(++countR * 10);					 
							 line.setDescription("Reversal B1. Product Asset");	
							 if (projectID > 0) {
								 line.setC_Project_ID(projectID);
							 }
							 line.setM_Product_ID(productID);
							 line.setAmtSourceDr(productVarianceAmt.abs());
							 line.setAmtSourceCr(Env.ZERO);
							 line.setAmtAcct(productVarianceAmt.abs(), Env.ZERO);
							 if(productVarianceAmt.compareTo(Env.ZERO)>0){
								 account = getOrCreateCombination(line, varianceAccountID);
							 } else {
								 account = getOrCreateCombination(line, pa.getP_Asset_A().getAccount_ID());
							 }						 
							 if(account!=null){
								 line.setC_ValidCombination_ID(account);
							 }
							 line.setAccount_ID(account.get_ID());
							 line.saveEx();
							 
							 line = new MJournalLine(reversal);
							 line.setLine(++countR * 10);					 
							 line.setDescription("Reversal B1. Product Asset");	
							 if (projectID > 0) {
								 line.setC_Project_ID(projectID);
							 }
							 line.setM_Product_ID(productID);
							 line.setAmtSourceDr(Env.ZERO);
							 line.setAmtSourceCr(productVarianceAmt.abs());
							 line.setAmtAcct(Env.ZERO, productVarianceAmt.abs());	
							 if(productVarianceAmt.compareTo(Env.ZERO)>0){
								 account = getOrCreateCombination(line, pa.getP_Asset_A().getAccount_ID());
							 } else {
								 account = getOrCreateCombination(line, varianceAccountID);
							 }	
							 if(account!=null){
								 line.setC_ValidCombination_ID(account);
							 }
							 line.setAccount_ID(account.get_ID());
							 line.saveEx();			        	  
							
			        	  }
				        	 
			        	  
			        	  // B2A. Warehouse Difference	/ B2B. Internal Use
			        	  List<MWarehouse> whList = new Query(getCtx(), MWarehouse.Table_Name, "AD_Org_ID = ?", get_TrxName())
			        	  								.setParameters(new Object[]{orgID})
			        	  								.setClient_ID()
			        	  								.setOnlyActiveRecords(true)
			        	  								.list();
			        	  for(MWarehouse wh : whList){
			        		  X_M_Warehouse_Acct wa = new Query(getCtx(), X_M_Warehouse_Acct.Table_Name, "M_Warehouse_ID = ? AND C_AcctSchema_ID = ?", get_TrxName())
			        		  								.setParameters(new Object[]{wh.get_ID(),as.get_ID()})
			        		  								.first();
			        		  sqlWhere = "AD_Org_ID = ? AND COALESCE(C_Project_ID,0) = ? AND MovementType LIKE 'I%' AND M_Product_ID = ? AND (MovementDate BETWEEN ? AND ?) AND M_Locator_ID IN (SELECT l.M_Locator_ID FROM M_Locator l WHERE l.M_Warehouse_ID = ?)";
			        		  BigDecimal totalPhysicalInventory = DB.getSQLValueBDEx(get_TrxName(), sqlSelect+sqlWhere, orgID,projectID,productID,dateStart,dateEnd,wh.get_ID());
//			        		  BigDecimal totalPhysicalInventory = new Query(getCtx(), MTransaction.Table_Name, "AD_Org_ID = ? AND MovementType LIKE 'I%' AND M_Product_ID = ? AND (MovementDate BETWEEN ? AND ?) AND M_Locator_ID IN (SELECT l.M_Locator_ID FROM M_Locator l WHERE l.M_Warehouse_ID = ?)", get_TrxName())
//			        		  											.setParameters(new Object[]{orgID,productID,dateStart,dateEnd,wh.get_ID()}).sum("MovementQty");
				        	  totalPhysicalInventory = totalPhysicalInventory.negate();
				        	  differenceVarianceAmt = varianceAmt.multiply(totalPhysicalInventory.divide(denomQty, MathContext.DECIMAL128));
				        	  
				        	  if(differenceVarianceAmt.compareTo(Env.ZERO)!=0){
				        		  log.info("B2A. Warehouse Difference M_Warehouse_ID=" + wh.get_ID() + ", Amt=" + differenceVarianceAmt);
				        		  MVarianceDistribution va = new MVarianceDistribution(getCtx(), 0, get_TrxName());
				        		  va.setAD_Org_ID(orgID);
				        		  va.setAD_PInstance_ID(getAD_PInstance_ID());
				        		  va.setAccount_ID(varianceAccountID);
				        		  va.setC_Project_ID(projectID);
				        		  va.setM_Product_ID(productID);
				        		  if(differenceVarianceAmt.compareTo(Env.ZERO)>0){
				        			  va.setAmtAcctCr(differenceVarianceAmt.abs());
				        		  } else {
				        			  va.setAmtAcctDr(differenceVarianceAmt.abs());
				        		  }		        		  
				        		  va.setLine(new Integer(++count * 10));
				        		  va.setDescription("B2A. Warehouse Difference");
				        		  va.setDateAcct(dateEnd);
				        		  va.setIsAdjustCOGS(p_IsAdjust);
				        		  va.saveEx();
				        		  
				        		  //
				        		  va = new MVarianceDistribution(getCtx(), 0, get_TrxName());
				        		  va.setAD_Org_ID(orgID);
				        		  va.setAD_PInstance_ID(getAD_PInstance_ID());
				        		  va.setAccount_ID(wa.getW_Differences_A().getAccount_ID());
				        		  va.setC_Project_ID(projectID);
				        		  va.setM_Product_ID(productID);
				        		  if(differenceVarianceAmt.compareTo(Env.ZERO)>0){
				        			  va.setAmtAcctDr(differenceVarianceAmt.abs());
				        		  } else {
				        			  va.setAmtAcctCr(differenceVarianceAmt.abs());
				        		  }		        		  
				        		  va.setLine(new Integer(++count * 10));
				        		  va.setDescription("B2A. Warehouse Difference");
				        		  va.setDateAcct(dateEnd);
				        		  va.setIsAdjustCOGS(p_IsAdjust);
				        		  va.saveEx();
				        	  }
			        	  }
			        	  
			        	  // B2C. COGS
			        	  sqlWhere = "AD_Org_ID = ? AND COALESCE(C_Project_ID,0) = ? AND MovementType LIKE 'C%' AND M_Product_ID = ? AND (MovementDate BETWEEN ? AND ?)";
			        	  BigDecimal totalShipment = DB.getSQLValueBDEx(get_TrxName(), sqlSelect+sqlWhere, orgID,projectID,productID,dateStart,dateEnd);
//		        		  BigDecimal totalShipment = new Query(getCtx(), MTransaction.Table_Name, "AD_Org_ID = ? AND MovementType LIKE 'C%' AND M_Product_ID = ? AND (MovementDate BETWEEN ? AND ?)", get_TrxName())
//		        		  							.setParameters(new Object[]{orgID,productID,dateStart,dateEnd})
//		        		  							.sum("MovementQty");
			        	  cogsVarianceAmt = varianceAmt.multiply(totalShipment.divide(denomQty, MathContext.DECIMAL128));
			        	  cogsVarianceAmt = cogsVarianceAmt.negate();
			        	  
			        	  if(cogsVarianceAmt.compareTo(Env.ZERO)!=0){
			        		  log.info("B2C. COGS Amt=" + cogsVarianceAmt);
			        		  MVarianceDistribution va = new MVarianceDistribution(getCtx(), 0, get_TrxName());
			        		  va.setAD_Org_ID(orgID);
			        		  va.setAD_PInstance_ID(getAD_PInstance_ID());
			        		  va.setAccount_ID(varianceAccountID);
			        		  va.setC_Project_ID(projectID);
			        		  va.setM_Product_ID(productID);
			        		  if(cogsVarianceAmt.compareTo(Env.ZERO)>0){
			        			  va.setAmtAcctCr(cogsVarianceAmt.abs());
			        		  } else {
			        			  va.setAmtAcctDr(cogsVarianceAmt.abs());
			        		  }		        		  
			        		  va.setLine(new Integer(++count * 10));
			        		  va.setDescription("B2C. COGS");
			        		  va.setDateAcct(dateEnd);
			        		  va.setIsAdjustCOGS(p_IsAdjust);
			        		  va.saveEx();
			        		  
			        		  //
			        		  va = new MVarianceDistribution(getCtx(), 0, get_TrxName());
			        		  va.setAD_Org_ID(orgID);
			        		  va.setAD_PInstance_ID(getAD_PInstance_ID());
			        		  va.setAccount_ID(pa.getP_COGS_A().getAccount_ID());
			        		  va.setC_Project_ID(projectID);
			        		  va.setM_Product_ID(productID);
			        		  if(cogsVarianceAmt.compareTo(Env.ZERO)>0){
			        			  va.setAmtAcctDr(cogsVarianceAmt.abs());
			        		  } else {
			        			  va.setAmtAcctCr(cogsVarianceAmt.abs());
			        		  }		        		  
			        		  va.setLine(new Integer(++count * 10));
			        		  va.setDescription("B2C. COGS");
			        		  va.setDateAcct(dateEnd);
			        		  va.setIsAdjustCOGS(p_IsAdjust);
			        		  va.saveEx();
			        	  }
		        	  
			        	  // B3. WIP
			        	  StringBuffer sqlWIP = new StringBuffer();
			        	  sqlWIP.append("SELECT COALESCE(SUM(t.MovementQty),0) MovementQty,cc.PP_Order_ID FROM RV_Transaction t ");
			        	  sqlWIP.append("INNER JOIN PP_Cost_Collector cc ON (cc.PP_Cost_Collector_ID = t.PP_Cost_Collector_ID) ");
			        	  sqlWIP.append("WHERE t.MovementType = 'W-' ");
			        	  sqlWIP.append(" AND t.AD_Org_ID = ? ");
			        	  sqlWIP.append(" AND COALESCE(t.C_Project_ID,0) = ? ");
			        	  sqlWIP.append(" AND t.M_Product_ID = ? ");
			        	  sqlWIP.append(" AND t.MovementDate BETWEEN ? AND ? ");
			        	  sqlWIP.append(" GROUP BY cc.PP_Order_ID ");
			        	  PreparedStatement pstmt2 = null;
			        	  ResultSet rs2 = null;
			        	  try
			        	  {
			        	    pstmt2 = DB.prepareStatement(sqlWIP.toString(), get_TrxName());
			        	    DB.setParameters(pstmt2, new Object[]{orgID,projectID,productID,dateStart,dateEnd});
			        	    rs2 = pstmt2.executeQuery();
			        	    while(rs2.next())
			        	    {
			        	    	BigDecimal totalIssue = rs2.getBigDecimal("MovementQty");
			        	    	MPPOrder mo = new MPPOrder(getCtx(), rs2.getInt("PP_Order_ID"), get_TrxName());
			        	    	BigDecimal remainingWIPAmt = new Query(getCtx(), MFactAcct.Table_Name, "Account_ID = ? AND UserElement2_ID IN (SELECT cc.PP_Cost_Collector_ID FROM PP_Cost_Collector cc WHERE cc.PP_Order_ID = ?)", get_TrxName())
			        	    									.setParameters(new Object[]{wipVarianceAllocationAcct.get_ID(),mo.get_ID()})
			        	    									.sum("AmtAcctDr - AmtAcctCr");
			        	    	wipVarianceAmt = varianceAmt.add(remainingWIPAmt).multiply(totalIssue.divide(denomQty, MathContext.DECIMAL128));
			        	    	wipVarianceAmt = wipVarianceAmt.negate();
					        	if(wipVarianceAmt.compareTo(Env.ZERO)!=0){
					        		  log.info("B3. WIP PP_Order_ID=" + mo.get_ID() + ", Amt=" + wipVarianceAmt);
					        		  MVarianceDistribution va = new MVarianceDistribution(getCtx(), 0, get_TrxName());
					        		  va.setAD_Org_ID(orgID);
					        		  va.setAD_PInstance_ID(getAD_PInstance_ID());
					        		  va.setAccount_ID(varianceAccountID);
					        		  va.setC_Project_ID(projectID);
					        		  va.setM_Product_ID(productID);
					        		  if(wipVarianceAmt.compareTo(Env.ZERO)>0){
					        			  va.setAmtAcctCr(wipVarianceAmt.abs());
					        		  } else {
					        			  va.setAmtAcctDr(wipVarianceAmt.abs());
					        		  }		        		  
					        		  va.setLine(new Integer(++count * 10));
					        		  va.setDescription("B3. WIP");
					        		  va.setUserElement2_ID(mo.get_ID());
					        		  va.setRelatedProduct_ID(mo.getM_Product_ID());
					        		  va.setDateAcct(dateEnd);
					        		  va.setIsAdjustCOGS(p_IsAdjust);
					        		  va.saveEx();
					        		  
					        		  //
					        		  va = new MVarianceDistribution(getCtx(), 0, get_TrxName());
					        		  va.setAD_Org_ID(orgID);
					        		  va.setAD_PInstance_ID(getAD_PInstance_ID());
					        		  va.setAccount_ID(wipVarianceAllocationAcct.get_ID());
					        		  va.setC_Project_ID(projectID);
					        		  va.setM_Product_ID(productID);
					        		  if(wipVarianceAmt.compareTo(Env.ZERO)>0){
					        			  va.setAmtAcctDr(wipVarianceAmt.abs());
					        		  } else {
					        			  va.setAmtAcctCr(wipVarianceAmt.abs());
					        		  }		        		  
					        		  va.setLine(new Integer(++count * 10));
					        		  va.setDescription("B3. WIP");
					        		  va.setUserElement2_ID(mo.get_ID());
					        		  va.setRelatedProduct_ID(mo.getM_Product_ID());
					        		  va.setDateAcct(dateEnd);
					        		  va.setIsAdjustCOGS(p_IsAdjust);
					        		  va.saveEx();
					        		  
					        		  // C1. FG Variance Allocation
					        		  StringBuffer sqlFG = new StringBuffer();
					        		  sqlFG.append("SELECT COALESCE(SUM(t.MovementQty),0) FROM RV_Transaction t ");
					        		  sqlFG.append("INNER JOIN PP_Cost_Collector cc ON (cc.PP_Cost_Collector_ID = t.PP_Cost_Collector_ID) ");
					        		  sqlFG.append("WHERE MovementType LIKE 'W+' ");
					        		  sqlFG.append("AND t.AD_Org_ID = ? ");
					        		  sqlFG.append("AND COALESCE(t.C_Project_ID,0) = ? ");
					        		  sqlFG.append("AND t.M_Product_ID = ? ");
					        		  sqlFG.append("AND cc.PP_Order_ID = ? ");
					        		  BigDecimal totalOrderReceipt = DB.getSQLValueBDEx(get_TrxName(), sqlFG.toString(), 
					        				  orgID,projectID,mo.getM_Product_ID(),mo.get_ID());
//					        		  BigDecimal totalOrderReceipt = new Query(getCtx(), MTransaction.Table_Name, "AD_Org_ID = ? AND MovementType LIKE 'W+' AND M_Product_ID = ? AND PP_Cost_Collector_ID IN (SELECT cc.PP_Cost_Collector_ID FROM PP_Cost_Collector cc WHERE cc.PP_Order_ID = ?)", get_TrxName())
//									  		  							.setParameters(new Object[]{orgID,mo.getM_Product_ID(),mo.get_ID()})
//									  		  							.sum("MovementQty");
					        		  if(totalOrderReceipt.compareTo(mo.getQtyEntered()) > 0){
					        			  totalOrderReceipt = mo.getQtyEntered();
					        		  }
					        		  fgVarianceAllocationAmt = wipVarianceAmt.multiply(totalOrderReceipt.divide(mo.getQtyEntered()	, MathContext.DECIMAL128));
					        		  if(fgVarianceAllocationAmt.compareTo(Env.ZERO)!=0){
					        			  log.info("C1. FG Variance Allocation PP_Order_ID=" + mo.get_ID() + ", fgVarianceAllocationAmt=" + wipVarianceAmt);
						        		  va = new MVarianceDistribution(getCtx(), 0, get_TrxName());
						        		  va.setAD_Org_ID(orgID);
						        		  va.setAD_PInstance_ID(getAD_PInstance_ID());
						        		  va.setAccount_ID(wipVarianceAllocationAcct.get_ID());
						        		  va.setC_Project_ID(projectID);
						        		  va.setM_Product_ID(productID);
						        		  if(fgVarianceAllocationAmt.compareTo(Env.ZERO)>0){
						        			  va.setAmtAcctCr(fgVarianceAllocationAmt.abs());
						        		  } else {
						        			  va.setAmtAcctDr(fgVarianceAllocationAmt.abs());
						        		  }		        		  
						        		  va.setLine(new Integer(++count * 10));
						        		  va.setDescription("C1. FG Variance Allocation");
						        		  va.setUserElement2_ID(mo.get_ID());
						        		  va.setRelatedProduct_ID(mo.getM_Product_ID());
						        		  va.setDateAcct(dateEnd);
						        		  va.setIsAdjustCOGS(p_IsAdjust);
						        		  va.saveEx();
						        		  
						        		  //
						        		  va = new MVarianceDistribution(getCtx(), 0, get_TrxName());
						        		  va.setAD_Org_ID(orgID);
						        		  va.setAD_PInstance_ID(getAD_PInstance_ID());
						        		  va.setAccount_ID(fgVarianceAllocationAcct.get_ID());
						        		  va.setC_Project_ID(projectID);
						        		  va.setM_Product_ID(mo.getM_Product_ID());
						        		  if(fgVarianceAllocationAmt.compareTo(Env.ZERO)>0){
						        			  va.setAmtAcctDr(fgVarianceAllocationAmt.abs());
						        		  } else {
						        			  va.setAmtAcctCr(fgVarianceAllocationAmt.abs());
						        		  }		        		  
						        		  va.setLine(new Integer(++count * 10));
						        		  va.setDescription("C1. FG Variance Allocation");
						        		  va.setUserElement2_ID(mo.get_ID());
						        		  va.setRelatedProduct_ID(mo.getM_Product_ID());
						        		  va.setDateAcct(dateEnd);
						        		  va.setIsAdjustCOGS(p_IsAdjust);
						        		  va.saveEx();
						        	  }
					        	  }
			        	    }
			        	  }
			        	  catch (SQLException e)
			        	  {
			        	    throw new DBException(e, sqlWIP.toString());
			        	  }
			        	  finally
			        	  {
			        	    DB.close(rs2, pstmt2);
			        	    rs2 = null; pstmt2 = null;
			        	  }
			          }
			      }
			 }
			 // If your method is not throwing Exception or SQLException you need this block to catch SQLException
			 // and convert them to unchecked DBException
			 catch (SQLException e)
			 {
			      throw new DBException(e, sql.toString());
			 }
			 // '''ALWAYS''' close your ResultSet in a finally statement
			 finally
			 {
			      DB.close(rs, pstmt);
			      rs = null; pstmt = null;
			 }				 		 		 
		}
		
		if (errorCount > 0) {
			throw new AdempiereException("Invalid Qty");
		}
		
		 if(p_IsAdjust){
			 List<MVarianceDistribution> list = new Query(getCtx(), MVarianceDistribution.Table_Name, "AD_PInstance_ID = ?", get_TrxName()).setParameters(new Object[]{getAD_PInstance_ID()}).setOrderBy("AD_Org_ID,Line").list();
			 if(!list.isEmpty()){
				 MJournal journal = null;
				 MJournalLine line = null;
				 MAccount account = null;
				 for(MVarianceDistribution va : list){
					 if(va.getAD_Org_ID() != journalOrgID){
						 journalOrgID = va.getAD_Org_ID();
						 
						 journal = new MJournal(getCtx(), 0, get_TrxName());
						 journal.setAD_Org_ID(journalOrgID);
						 journal.setC_AcctSchema_ID(as.get_ID());
						 journal.setDescription("Generated from process Variance Distribution " + p_DateAcct);
						 journal.setPostingType(MJournal.POSTINGTYPE_Actual);
						 journal.setC_DocType_ID(Sync.DOCTYPE_GL_VarAdj);
						 journal.setGL_Category_ID(Sync.GL_CATEGORY_VarAdj);
						 journal.setDateDoc(p_DateAcct);
						 journal.setDateAcct(p_DateAcct);
						 journal.setC_Period_ID(MPeriod.getC_Period_ID(getCtx(), p_DateAcct,journal.getAD_Org_ID()));
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
//					 line.setUserElement2_ID(va.getUserElement2_ID());
					 line.set_ValueOfColumn("PP_Order_ID", va.getUserElement2_ID());
					 line.setAmtSourceDr(va.getAmtAcctDr());
					 line.setAmtSourceCr(va.getAmtAcctCr());
					 line.setAmtAcct(va.getAmtAcctDr(), va.getAmtAcctCr());	
					 account = getOrCreateCombination(va);
					 if(account!=null){
						 line.setC_ValidCombination_ID(account);
					 }
					 line.setAccount_ID(va.getAccount_ID());
					 line.saveEx();
				 }
			 }
		 }
		 
		return info;
	}	//	doIt
	
	private MAccount getOrCreateCombination(MVarianceDistribution va)
	{
		return 	MAccount.get(getCtx(), va.getAD_Client_ID(), va.getAD_Org_ID(), as.get_ID(), va.getAccount_ID(),
				0, va.getM_Product_ID(), va.getC_BPartner_ID(), va.getAD_OrgTrx_ID(), va.getC_LocFrom_ID(),
				va.getC_LocTo_ID(), va.getC_SalesRegion_ID(), va.getC_Project_ID(), va.getC_Campaign_ID(), 
				va.getC_Activity_ID(), va.getUser1_ID(), va.getUser2_ID(), va.getUserElement1_ID(), va.getUserElement2_ID());
	}	//	getOrCreateCombination
	
	private MAccount getOrCreateCombination(MJournalLine line, int accountID)
	{
		return	 MAccount.get(getCtx(), line.getAD_Client_ID(), line.getAD_Org_ID(), as.get_ID(), accountID,
				line.getC_SubAcct_ID(), line.getM_Product_ID(), line.getC_BPartner_ID(), line.getAD_OrgTrx_ID(), line.getC_LocFrom_ID(),
				line.getC_LocTo_ID(), line.getC_SalesRegion_ID(), line.getC_Project_ID(), line.getC_Campaign_ID(), 
				line.getC_Activity_ID(), line.getUser1_ID(), line.getUser2_ID(), 
//				line.getUserElement1_ID(), line.getUserElement2_ID()
				0,0);
	}	//	getOrCreateCombination
	
	public List<MProductCategory> getProductCategoryStandardCosting(MAcctSchema as)
	{
		List<MProductCategory> results = new ArrayList<MProductCategory>();
		List<MProductCategory> list = new Query(getCtx(), MProductCategory.Table_Name, "", get_TrxName())
										.setClient_ID()
										.setOnlyActiveRecords(true)
										.list();
		for(MProductCategory pc : list){
			MProductCategoryAcct pca = MProductCategoryAcct.get(getCtx(), pc.getM_Product_Category_ID(), as.get_ID(), get_TrxName());
			String costingMethod = pca.getCostingMethod();
			if (costingMethod == null)
			{
				costingMethod = as.getCostingMethod();
			}
			if(costingMethod.equals(MProductCategoryAcct.COSTINGMETHOD_StandardCosting)){
				results.add(pc);
			}
		}
			
		return results;
	}
}
