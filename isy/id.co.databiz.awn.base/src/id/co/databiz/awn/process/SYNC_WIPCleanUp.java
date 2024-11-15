
package id.co.databiz.awn.process;

import java.math.BigDecimal;
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
import org.compiere.model.MPeriod;
import org.compiere.model.MProductCategory;
import org.compiere.model.MProductCategoryAcct;
import org.compiere.model.MSysConfig;
import org.compiere.model.Query;
import org.compiere.model.X_M_Product_Acct;
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
public class SYNC_WIPCleanUp extends SvrProcess
{
	private Timestamp p_DateAcct = null;	
	private boolean p_IsAdjust = false;
	
	private MAcctSchema as = null;
	
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
		log.info("Start WIP Clean Up");
		String info = "";
		int count = 0;
		
		int journalOrgID = 0;
		
		as = MClient.get(getCtx()).getAcctSchema();
		String wipVarianceAllocationValue = MSysConfig.getValue(Sync.SYS_Acct_WIPVarianceAllocation, Env.getAD_Client_ID(Env.getCtx()));
		MElementValue wipVarianceAllocationAcct = new Query(getCtx(), MElementValue.Table_Name, "Value = ?", get_TrxName())
			.setOnlyActiveRecords(true)
			.setParameters(new Object[]{wipVarianceAllocationValue})
			.first();
		if(wipVarianceAllocationAcct == null)
			throw new AdempiereException("SYNC-634\nInvalid System Configurator value " + Sync.SYS_Acct_WIPVarianceAllocation + ": " + wipVarianceAllocationValue);
		
		List<MProductCategory> productCategoryList = getProductCategoryStandardCosting(as);
		
		StringBuffer sql = new StringBuffer();
		sql.append("SELECT SUM(fa.AmtAcctDr - fa.AmtAcctCr) Amt, fa.AD_Org_ID,fa.C_Project_ID,fa.M_Product_ID,fa.Account_ID, fa.UserElement2_ID ");
		sql.append("FROM Fact_Acct fa ");
		sql.append("INNER JOIN M_Product p ON (p.M_Product_ID = fa.M_Product_ID) ");
		sql.append("INNER JOIN M_Product_Acct pa ON (pa.M_Product_ID = p.M_Product_ID AND pa.C_AcctSchema_ID = fa.C_AcctSchema_ID) ");
		sql.append("INNER JOIN C_ValidCombination vc ON (vc.C_ValidCombination_ID = pa.P_WIP_Acct) ");
		sql.append("INNER JOIN PP_Order mo ON (mo.PP_Order_ID = fa.UserElement2_ID) ");
		sql.append("WHERE 1=1 ");
		sql.append(" AND (fa.Account_ID = vc.Account_ID OR fa.Account_ID = ?) "); // #1
		sql.append(" AND mo.DocStatus = 'CL' ");
//			sql.append(" AND p.ProductType = 'I' ");
//			sql.append(" AND p.IsStocked = 'Y' ");
		sql.append(" AND fa.C_AcctSchema_ID = ? "); // #2
		sql.append(" AND (AddDays(mo.ClosedDate,0) BETWEEN ? AND ?) ");			// #3..#4
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
		sql.append("GROUP BY fa.AD_Org_ID,fa.Account_ID,fa.C_Project_ID,fa.M_Product_ID,fa.UserElement2_ID ");
		sql.append("HAVING SUM(fa.AmtAcctDr - fa.AmtAcctCr) <> 0 ");
		
		List<Object> params = new ArrayList<Object>();
		params.add(wipVarianceAllocationAcct.get_ID());
		params.add(as.get_ID());
		params.add(dateStart);
		params.add(dateEnd);
		
		PreparedStatement pstmt = null;
		 ResultSet rs = null;
		 try
		 {			 
		      pstmt = DB.prepareStatement(sql.toString(), get_TrxName());
		      DB.setParameters(pstmt, params);
		      rs = pstmt.executeQuery();
		      while(rs.next())
		      {
		    	  BigDecimal wipAmt = rs.getBigDecimal("Amt");
		          int orgID = rs.getInt("AD_Org_ID");
		          int projectID = rs.getInt("C_Project_ID");
		          int productID = rs.getInt("M_Product_ID");
		          int accountID = rs.getInt("Account_ID");
		          MPPOrder mo = new MPPOrder(getCtx(), rs.getInt(MFactAcct.COLUMNNAME_UserElement2_ID), get_TrxName());
		          X_M_Product_Acct pa = new Query(getCtx(), X_M_Product_Acct.Table_Name, "M_Product_ID = ? AND C_AcctSchema_ID = ?", get_TrxName())
		          							.setParameters(new Object[]{mo.getM_Product_ID(),as.get_ID()})
		          							.first();
		          
			        //Check Invoice Double Input
			  	  String accountsql ="select cv.account_id from m_product_acct mpa "
			  	  		+ "left join c_validcombination cv on cv.c_validcombination_id = mpa.p_usagevariance_acct "
			  	  		+ "where mpa.m_product_id = ? and mpa.c_acctschema_id = ? fetch first 1 row only ";
			  	  int accountid = DB.getSQLValue(get_TrxName(), accountsql, mo.getM_Product_ID(), as.get_ID());
		    	  
		    	  if(wipAmt.compareTo(Env.ZERO)!=0){
		    		  log.info("AD_Org_ID=" + orgID + ", Account_ID=" + accountID + ", M_Product_ID=" + productID + ", Amt=" + wipAmt + ", MO=" + mo.get_ID());
		    		  MVarianceDistribution va = new MVarianceDistribution(getCtx(), 0, get_TrxName());
	        		  va.setAD_Org_ID(orgID);
		        	  va.setAD_PInstance_ID(getAD_PInstance_ID());
		        	  va.setAccount_ID(accountID);
		        	  va.setC_Project_ID(projectID);
		        	  va.setM_Product_ID(productID);
		        	  va.setUserElement2_ID(mo.get_ID());
		        	  if(wipAmt.compareTo(Env.ZERO)>0){
		        		  va.setAmtAcctCr(wipAmt.abs());
		        	  } else {
		        		  va.setAmtAcctDr(wipAmt.abs());
		        	  }
		        	  va.setLine(new Integer(++count * 10));
		        	  va.setDescription("WIP Clean Up " + mo.getDocumentNo());
		        	  va.setDateAcct(dateEnd);
		        	  va.setIsAdjustCOGS(p_IsAdjust);
		        	  va.saveEx();
		        	  
		        	  // 
		        	  va = new MVarianceDistribution(getCtx(), 0, get_TrxName());
		        	  va.setAD_Org_ID(orgID);
		        	  va.setAD_PInstance_ID(getAD_PInstance_ID());
//		        	  va.setAccount_ID(pa.getP_UsageVariance_A().getAccount_ID());
		        	  va.setAccount_ID(accountid);
		        	  va.setC_Project_ID(projectID);
		        	  va.setM_Product_ID(mo.getM_Product_ID());
		        	  va.setUserElement2_ID(mo.get_ID());
		        	  va.setAmtAcctDr(Env.ZERO);
		        	  va.setAmtAcctCr(Env.ZERO);
		        	  if(wipAmt.compareTo(Env.ZERO)>0){
		        		  va.setAmtAcctDr(wipAmt.abs());
		        	  } else {
		        		  va.setAmtAcctCr(wipAmt.abs());
		        	  }
		        	  va.setLine(new Integer(++count * 10));
		        	  va.setDescription("WIP Clean Up " + mo.getDocumentNo());
		        	  va.setDateAcct(dateEnd);
		        	  va.setIsAdjustCOGS(p_IsAdjust);
		        	  va.saveEx();
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
						 journal.setDescription("Generated from process WIP Clean Up " + p_DateAcct);
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
					 line.setM_Product_ID(va.getM_Product_ID());
					 line.setC_BPartner_ID(va.getC_BPartner_ID());
					 line.setAD_OrgTrx_ID(va.getAD_OrgTrx_ID());
					 line.setC_SalesRegion_ID(va.getC_SalesRegion_ID());
					 line.setC_Project_ID(va.getC_Project_ID());
					 line.setC_Campaign_ID(va.getC_Campaign_ID());
					 line.setC_Activity_ID(va.getC_Activity_ID());
					 line.setUser1_ID(va.getUser1_ID());
					 line.setUser2_ID(va.getUser2_ID());
					 line.setA_Asset_ID(va.getA_Asset_ID());
//					 line.setUserElement1_ID(va.getUserElement1_ID());
//					 line.setUserElement2_ID(va.getUserElement2_ID());
					 line.set_ValueOfColumn("PP_Order_ID", va.getUserElement2_ID());
					 line.setC_UOM_ID(va.getC_UOM_ID());
					 line.setQty(va.getQty());
					 line.setC_Currency_ID(va.getC_Currency_ID());
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
				line.getC_Activity_ID(), line.getUser1_ID(), line.getUser2_ID()
//				, line.getUserElement1_ID(), line.getUserElement2_ID()
				,0,0);
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
