
package id.co.databiz.awn.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.exceptions.DBException;
import org.compiere.model.MAccount;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MClient;
import org.compiere.model.MConversionType;
import org.compiere.model.MFactAcct;
import org.compiere.model.MJournal;
import org.compiere.model.MJournalLine;
import org.compiere.model.MPeriod;
import org.compiere.model.MProductCategory;
import org.compiere.model.MProductCategoryAcct;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.eevolution.model.MPPCostCollector;

import id.co.databiz.awn.model.MVarianceDistribution;
import id.co.databiz.awn.model.Sync;


/**
 *	
 *
 * 	@author 	Anozi Mada
 * 	
 */
public class SYNC_ProductionVarianceProductAdjustment extends SvrProcess
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
		log.info("Start Production Variance Adjustment");
		String info = "";
		int count = 0;
		
		int journalOrgID = 0;
		
		as = MClient.get(getCtx()).getAcctSchema();
		
		List<String> varianceAccounts = new ArrayList<String>();
		varianceAccounts.add("pa.p_ratevariance_acct");
		varianceAccounts.add("pa.p_methodchangevariance_acct");
		varianceAccounts.add("pa.p_mixvariance_acct");
		varianceAccounts.add("pa.p_usagevariance_acct");
		
		List<MProductCategory> productCategoryList = getProductCategoryStandardCosting(as);
		for(String varianceAccount : varianceAccounts){
			StringBuffer sql = new StringBuffer();
			sql.append("SELECT * FROM Fact_Acct fa ");
			sql.append("INNER JOIN M_Product p ON (p.M_Product_ID = fa.M_Product_ID) ");
			sql.append("INNER JOIN M_Product_Acct pa ON (pa.M_Product_ID = p.M_Product_ID AND pa.C_AcctSchema_ID = fa.C_AcctSchema_ID) ");
			sql.append("INNER JOIN C_ValidCombination vc ON (vc.C_ValidCombination_ID = ").append(varianceAccount).append(") ");
			sql.append("INNER JOIN PP_Cost_Collector cc ON (cc.PP_Cost_Collector_ID = fa.Record_ID) ");
			sql.append("INNER JOIN PP_Order mo ON (mo.PP_Order_ID = cc.PP_Order_ID) ");
			sql.append("WHERE 1=1 ");
			sql.append(" AND fa.AD_Table_ID = 53035 "); // PP_Cost_Colector
			sql.append(" AND fa.M_Product_ID <> mo.M_Product_ID ");
			sql.append(" AND fa.Account_ID = vc.Account_ID ");
//			sql.append(" AND p.ProductType = 'I' ");
//			sql.append(" AND p.IsStocked = 'Y' ");
			sql.append(" AND fa.C_AcctSchema_ID = ? "); // #1
			sql.append(" AND (fa.DateAcct BETWEEN ? AND ?) ");			// #2..#3
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
			sql.append(" ORDER BY fa.AD_Org_ID,mo.M_Product_ID ");
			
			List<Object> params = new ArrayList<Object>();
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
			    	  MPPCostCollector cc = new MPPCostCollector(getCtx(), rs.getInt(MFactAcct.COLUMNNAME_Record_ID), get_TrxName());
			    	  
			    	  MVarianceDistribution va = new MVarianceDistribution(getCtx(), 0, get_TrxName());
			    	  va.setLine(new Integer(++count * 10));
			    	  va.setAD_PInstance_ID(getAD_PInstance_ID());
			    	  va.setAD_Org_ID(rs.getInt(MFactAcct.COLUMNNAME_AD_Org_ID));
			    	  va.setAccount_ID(rs.getInt(MFactAcct.COLUMNNAME_Account_ID));
			    	  va.setC_Currency_ID(rs.getInt(MFactAcct.COLUMNNAME_C_Currency_ID));
			    	  va.setAmtSourceDr(rs.getBigDecimal(MFactAcct.COLUMNNAME_AmtSourceCr));
			    	  va.setAmtSourceCr(rs.getBigDecimal(MFactAcct.COLUMNNAME_AmtSourceDr));
			    	  va.setAmtAcctDr(rs.getBigDecimal(MFactAcct.COLUMNNAME_AmtAcctCr));
			    	  va.setAmtAcctCr(rs.getBigDecimal(MFactAcct.COLUMNNAME_AmtAcctDr));
			    	  va.setC_UOM_ID(rs.getInt(MFactAcct.COLUMNNAME_C_UOM_ID));
			    	  va.setQty(rs.getBigDecimal(MFactAcct.COLUMNNAME_Qty).negate());
			    	  va.setM_Product_ID(rs.getInt(MFactAcct.COLUMNNAME_M_Product_ID));
			    	  va.setC_BPartner_ID(rs.getInt(MFactAcct.COLUMNNAME_C_BPartner_ID));
			    	  va.setAD_OrgTrx_ID(rs.getInt(MFactAcct.COLUMNNAME_AD_OrgTrx_ID));
			    	  va.setC_LocFrom_ID(rs.getInt(MFactAcct.COLUMNNAME_C_LocFrom_ID));
			    	  va.setC_LocTo_ID(rs.getInt(MFactAcct.COLUMNNAME_C_LocTo_ID));
			    	  va.setC_SalesRegion_ID(rs.getInt(MFactAcct.COLUMNNAME_C_SalesRegion_ID));
			    	  va.setC_Project_ID(rs.getInt(MFactAcct.COLUMNNAME_C_Project_ID));
			    	  va.setC_Campaign_ID(rs.getInt(MFactAcct.COLUMNNAME_C_Campaign_ID));
			    	  va.setC_Activity_ID(rs.getInt(MFactAcct.COLUMNNAME_C_Activity_ID));
			    	  va.setUser1_ID(rs.getInt(MFactAcct.COLUMNNAME_User1_ID));
			    	  va.setUser2_ID(rs.getInt(MFactAcct.COLUMNNAME_User2_ID));
			    	  va.setA_Asset_ID(rs.getInt(MFactAcct.COLUMNNAME_A_Asset_ID));
			    	  va.setC_SubAcct_ID(rs.getInt(MFactAcct.COLUMNNAME_C_SubAcct_ID));
			    	  va.setUserElement1_ID(rs.getInt(MFactAcct.COLUMNNAME_UserElement1_ID));
			    	  va.setUserElement2_ID(rs.getInt(MFactAcct.COLUMNNAME_UserElement2_ID));
			    	  va.setDescription(rs.getString(MFactAcct.COLUMNNAME_Description));
			    	  va.setDateAcct(dateEnd);
	        		  va.setIsAdjustCOGS(p_IsAdjust);
	        		  va.saveEx();
	        		  
	        		  // create adjustment product
	        		  MVarianceDistribution adj = new MVarianceDistribution(getCtx(), 0, get_TrxName());
	        		  adj.setLine(new Integer(++count * 10));
	        		  adj.setAD_PInstance_ID(getAD_PInstance_ID());
	        		  adj.setAD_Org_ID(rs.getInt(MFactAcct.COLUMNNAME_AD_Org_ID));
	        		  adj.setAccount_ID(rs.getInt(MFactAcct.COLUMNNAME_Account_ID));
	        		  adj.setC_Currency_ID(rs.getInt(MFactAcct.COLUMNNAME_C_Currency_ID));
	        		  adj.setAmtSourceDr(rs.getBigDecimal(MFactAcct.COLUMNNAME_AmtSourceDr));
	        		  adj.setAmtSourceCr(rs.getBigDecimal(MFactAcct.COLUMNNAME_AmtSourceCr));
	        		  adj.setAmtAcctDr(rs.getBigDecimal(MFactAcct.COLUMNNAME_AmtAcctDr));
	        		  adj.setAmtAcctCr(rs.getBigDecimal(MFactAcct.COLUMNNAME_AmtAcctCr));
	        		  adj.setC_UOM_ID(rs.getInt(MFactAcct.COLUMNNAME_C_UOM_ID));
	        		  adj.setQty(rs.getBigDecimal(MFactAcct.COLUMNNAME_Qty));
			    	  adj.setM_Product_ID(cc.getPP_Order().getM_Product_ID());
			    	  adj.setC_BPartner_ID(rs.getInt(MFactAcct.COLUMNNAME_C_BPartner_ID));
			    	  adj.setAD_OrgTrx_ID(rs.getInt(MFactAcct.COLUMNNAME_AD_OrgTrx_ID));
			    	  adj.setC_LocFrom_ID(rs.getInt(MFactAcct.COLUMNNAME_C_LocFrom_ID));
			    	  adj.setC_LocTo_ID(rs.getInt(MFactAcct.COLUMNNAME_C_LocTo_ID));
			    	  adj.setC_SalesRegion_ID(rs.getInt(MFactAcct.COLUMNNAME_C_SalesRegion_ID));
			    	  adj.setC_Project_ID(rs.getInt(MFactAcct.COLUMNNAME_C_Project_ID));
			    	  adj.setC_Campaign_ID(rs.getInt(MFactAcct.COLUMNNAME_C_Campaign_ID));
			    	  adj.setC_Activity_ID(rs.getInt(MFactAcct.COLUMNNAME_C_Activity_ID));
			    	  adj.setUser1_ID(rs.getInt(MFactAcct.COLUMNNAME_User1_ID));
			    	  adj.setUser2_ID(rs.getInt(MFactAcct.COLUMNNAME_User2_ID));
			    	  adj.setA_Asset_ID(rs.getInt(MFactAcct.COLUMNNAME_A_Asset_ID));
			    	  adj.setC_SubAcct_ID(rs.getInt(MFactAcct.COLUMNNAME_C_SubAcct_ID));
			    	  adj.setUserElement1_ID(rs.getInt(MFactAcct.COLUMNNAME_UserElement1_ID));
			    	  adj.setUserElement2_ID(rs.getInt(MFactAcct.COLUMNNAME_UserElement2_ID));
			    	  adj.setDescription(rs.getString(MFactAcct.COLUMNNAME_Description));
			    	  adj.setDateAcct(dateEnd);
			    	  adj.setIsAdjustCOGS(p_IsAdjust);
	        		  adj.saveEx();
			          
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
						 journal.setDescription("Generated from process Production Variance Product Adjustment " + p_DateAcct);
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
