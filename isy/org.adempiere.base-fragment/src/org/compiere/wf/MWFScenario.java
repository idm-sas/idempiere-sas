package org.compiere.wf;

import id.co.databiz.awn.model.wrapper.ICInvoice;
import id.co.databiz.awn.model.wrapper.ICOrder;
import id.co.databiz.awn.model.wrapper.ICPayment;
import id.co.databiz.awn.model.wrapper.IGLJournal;
import id.co.databiz.awn.model.wrapper.IMInOut;
import id.co.databiz.awn.model.wrapper.IMInventory;
import id.co.databiz.awn.model.wrapper.IMMovement;
import id.co.databiz.awn.model.wrapper.IMRMA;
import id.co.databiz.awn.model.wrapper.IMRequisition;
import id.co.databiz.awn.model.wrapper.ISTimeExpense;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.model.POWrapper;
import org.compiere.model.MConversionRate;
import org.compiere.model.MInOut;
import org.compiere.model.MInventory;
import org.compiere.model.MInvoice;
import org.compiere.model.MJournal;
import org.compiere.model.MMovement;
import org.compiere.model.MOrder;
import org.compiere.model.MPayment;
import org.compiere.model.MRMA;
import org.compiere.model.MRequisition;
import org.compiere.model.MTimeExpense;
import org.compiere.model.PO;
import org.compiere.model.X_C_Payment;
import org.compiere.model.X_Z_WFScenario;
import org.compiere.util.DB;
import org.compiere.util.Env;

public class MWFScenario extends X_Z_WFScenario {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public MWFScenario(Properties ctx, int Z_WFScenario_ID, String trxName) {
		super(ctx, Z_WFScenario_ID, trxName);
	}

	public MWFScenario (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }
	
	/**
	 * Get Z_WFScenario_ID
	 * 
	 * @return Z_WFScenario_ID
	 * @author darwanto ~dar~
	 */
	private static int getScenario(int C_DocType_ID, int AD_Org_ID, int AD_OrgTrx_ID, int C_Project_ID, 
			int M_PriceList_ID, int User1_ID, int C_Activity_ID, int C_Campaign_ID, int M_Warehouse_ID,
			BigDecimal GrandTotal, String scenarioType , String TrxName) {
		int Z_WFScenario_ID = -1;
		StringBuffer sql0 = new StringBuffer("SELECT zw.Z_WFScenario_ID FROM Z_WFScenario zw WHERE zw.C_DocType_ID=")
										.append(C_DocType_ID);
		if( AD_Org_ID > 0 )
			sql0.append(" AND (zw.AD_OrgDoc_ID IS NULL OR zw.AD_OrgDoc_ID=")
				.append(AD_Org_ID).append(")");
		else
			sql0.append(" AND zw.AD_OrgDoc_ID IS NULL");
		
		if( AD_OrgTrx_ID > 0 )
			sql0.append(" AND (zw.AD_OrgTrx_ID IS NULL OR zw.AD_OrgTrx_ID=")
				.append(AD_OrgTrx_ID).append(")");
		else
			sql0.append(" AND zw.AD_OrgTrx_ID IS NULL");
		
		if( C_Project_ID > 0 ) {
			String sql1 = "SELECT isAssetProject(?) FROM DUAL";
			String isAsset = DB.getSQLValueString(TrxName, sql1, C_Project_ID);
			sql0.append(" AND (zw.IsAssetProject = 'N' OR zw.IsAssetProject='")
				.append(isAsset).append("')");
		} else
			sql0.append(" AND zw.IsAssetProject = 'N'");
		
		if( M_PriceList_ID > 0) 
			sql0.append(" AND (zw.M_PriceList_ID IS NULL OR zw.M_PriceList_ID=")
			.append(M_PriceList_ID).append(")");
		else
			sql0.append(" AND zw.M_PriceList_ID IS NULL");

		if( User1_ID > 0) 
			sql0.append(" AND (zw.User1_ID IS NULL OR zw.User1_ID=")
			.append(User1_ID).append(")");
		else
			sql0.append(" AND zw.User1_ID IS NULL");

		if( C_Activity_ID > 0) 
			sql0.append(" AND (zw.C_Activity_ID IS NULL OR zw.C_Activity_ID=")
			.append(C_Activity_ID).append(")");
		else
			sql0.append(" AND zw.C_Activity_ID IS NULL");

		if( C_Campaign_ID > 0) 
			sql0.append(" AND (zw.C_Campaign_ID IS NULL OR zw.C_Campaign_ID=")
			.append(C_Campaign_ID).append(")");
		else
			sql0.append(" AND zw.C_Campaign_ID IS NULL");
		
		if( M_Warehouse_ID > 0) 
			sql0.append(" AND (zw.M_Warehouse_ID IS NULL OR zw.M_Warehouse_ID=")
			.append(M_Warehouse_ID).append(")");
		else
			sql0.append(" AND zw.M_Warehouse_ID IS NULL");
		
		if( GrandTotal != null )
		{
			sql0.append(" AND (zw.GrandTotal IS NULL OR zw.GrandTotal=0 OR zw.GrandTotal>=")
			.append(GrandTotal).append(")");
		}
		else
			sql0.append(" AND (zw.GrandTotal IS NULL OR zw.GrandTotal=0)");
		
		if(scenarioType==null){
			sql0.append(" AND zw.ScenarioType IS NULL");
		} else {
			sql0.append(" AND zw.ScenarioType = '").append(scenarioType).append("'");
		}
		
		sql0.append(" ORDER BY zw.Line DESC");
		Z_WFScenario_ID = DB.getSQLValue(TrxName, sql0.toString());
		
		return Z_WFScenario_ID;
	}
	
	/**
	 * @author darwanto ~dar~
	 */
	public static void setScenario(PO po, String scenarioType) {
		int currencyID = Env.getContextAsInt(Env.getCtx(), "$C_Currency_ID");
		if(currencyID==0) currencyID=303 ; //hardcode to Rupiah for server process (no Currency Context)
		int conversionTypeID = Env.getContextAsInt(Env.getCtx(), "#C_ConversionType_ID");
		String TrxName = po.get_TrxName();
		if( po.get_TableName().equals(MJournal.Table_Name) ) {
			MJournal journal = (MJournal)po;	
			IGLJournal journalCustom = POWrapper.create(journal, IGLJournal.class);
			BigDecimal grandTotal = journal.getApprovalAmt();
			if(journal.getC_Currency_ID()!=currencyID){
				grandTotal = MConversionRate.convert(Env.getCtx(), journal.getApprovalAmt(), journal.getC_Currency_ID(), currencyID, journal.getDateDoc(), journal.getC_ConversionType_ID(), journal.getAD_Client_ID(), journal.getAD_Org_ID());
				if(grandTotal==null){
					throw new AdempiereException("ISY-36\n@NoCurrencyConversion@");
				}
			}	
			int Z_WFScenario_ID = getScenario(journal.getC_DocType_ID(), 
					journal.getAD_Org_ID(), 0, 0, 0, 
					0, 0, 0, 0,
					grandTotal,scenarioType,TrxName);
			if( Z_WFScenario_ID > 0 ) {
				journalCustom.setZ_WFScenario_ID(Z_WFScenario_ID);
				journal.saveEx();
			} else {
				journalCustom.setZ_WFScenario_ID(0);				
				journal.saveEx();
			}
		} else if( po.get_TableName().equals(MInOut.Table_Name) ) {
			MInOut vpo = (MInOut)po;
			IMInOut vpoC = POWrapper.create(vpo, IMInOut.class);
			//	Get PriceList ID
			int M_PriceList_ID = 0;
			MOrder ord = (MOrder)vpo.getC_Order();
			if( ord!=null ) M_PriceList_ID = ord.getM_PriceList_ID();
		
			int Z_WFScenario_ID = getScenario(vpo.getC_DocType_ID(), 
					vpo.getAD_Org_ID(), vpo.getAD_OrgTrx_ID(), vpo.getC_Project_ID(), M_PriceList_ID, 
					vpo.getUser1_ID(), vpo.getC_Activity_ID(), vpo.getC_Campaign_ID(), vpo.getM_Warehouse_ID(),
					vpo.getApprovalAmt(),scenarioType,TrxName);
			if( Z_WFScenario_ID > 0 ) {
				vpoC.setZ_WFScenario_ID(Z_WFScenario_ID);
				vpo.saveEx();
			} else {
				vpoC.setZ_WFScenario_ID(0);				
				vpo.saveEx();
			}
		} else if( po.get_TableName().equals(MInventory.Table_Name) ) {
			MInventory vpo = (MInventory)po;
			IMInventory vpoC = POWrapper.create(vpo, IMInventory.class);
			int Z_WFScenario_ID = getScenario(vpo.getC_DocType_ID(), 
					vpo.getAD_Org_ID(), vpo.getAD_OrgTrx_ID(), vpo.getC_Project_ID(), 0, 
					vpo.getUser1_ID(), vpo.getC_Activity_ID(), vpo.getC_Campaign_ID(), vpo.getM_Warehouse_ID(),
					vpo.getApprovalAmt(),scenarioType,TrxName);
			if( Z_WFScenario_ID > 0 ) {
				vpoC.setZ_WFScenario_ID(Z_WFScenario_ID);
				vpo.saveEx();
			} else {
				vpoC.setZ_WFScenario_ID(0);				
				vpo.saveEx();
			}
		} else if( po.get_TableName().equals(MInvoice.Table_Name) ) {
			MInvoice vpo = (MInvoice)po;
			ICInvoice vpoC = POWrapper.create(vpo, ICInvoice.class);
			BigDecimal grandTotal = vpo.getGrandTotal();
			if(vpo.getC_Currency_ID()!=currencyID){
				grandTotal = MConversionRate.convert(Env.getCtx(), vpo.getGrandTotal(), vpo.getC_Currency_ID(), currencyID, vpo.getDateInvoiced(), vpo.getC_ConversionType_ID(), vpo.getAD_Client_ID(), vpo.getAD_Org_ID());
				if(grandTotal==null){
					throw new AdempiereException("ISY-36\n@NoCurrencyConversion@");
				}
			}	
			int Z_WFScenario_ID = getScenario(vpo.getC_DocTypeTarget_ID(), 
					vpo.getAD_Org_ID(), vpo.getAD_OrgTrx_ID(), vpo.getC_Project_ID(), 0, 
					vpo.getUser1_ID(), vpo.getC_Activity_ID(), vpo.getC_Campaign_ID(),0,
					grandTotal,scenarioType,TrxName);
			if( Z_WFScenario_ID > 0 ) {
				vpoC.setZ_WFScenario_ID(Z_WFScenario_ID);
				vpo.saveEx();
			} else {
				vpoC.setZ_WFScenario_ID(0);				
				vpo.saveEx();
			}
		} else if( po.get_TableName().equals(MMovement.Table_Name) ) {
			MMovement vpo = (MMovement)po;
			IMMovement vpoC = POWrapper.create(vpo, IMMovement.class);			
			int Z_WFScenario_ID = getScenario(vpo.getC_DocType_ID(), 
					vpo.getAD_Org_ID(), vpo.getAD_OrgTrx_ID(), vpo.getC_Project_ID(), 0, 
					vpo.getUser1_ID(), vpo.getC_Activity_ID(), vpo.getC_Campaign_ID(),0,
					vpo.getApprovalAmt(),scenarioType,TrxName);
			if( Z_WFScenario_ID > 0 ) {
				vpoC.setZ_WFScenario_ID(Z_WFScenario_ID);
				vpo.saveEx();
			} else {
				vpoC.setZ_WFScenario_ID(0);				
				vpo.saveEx();
			}
		} else if( po.get_TableName().equals(MOrder.Table_Name) ) {
			MOrder order = (MOrder)po;
			ICOrder ordC = POWrapper.create(order, ICOrder.class);
			BigDecimal grandTotal = null;
			if(scenarioType==null){
				grandTotal = order.getGrandTotal();
				if(order.getC_Currency_ID()!=currencyID){
					grandTotal = MConversionRate.convert(Env.getCtx(), order.getGrandTotal(), order.getC_Currency_ID(), currencyID, order.getDateOrdered(), order.getC_ConversionType_ID(), order.getAD_Client_ID(), order.getAD_Org_ID());
					if(grandTotal==null){
						throw new AdempiereException("ISY-36\n@NoCurrencyConversion@");
					}
				}
			} else if(scenarioType.equals(MWFScenario.SCENARIO_CREDIT_OVER)){
				grandTotal = ordC.getSO_CreditOver();
				if(order.getC_Currency_ID()!=currencyID){
					grandTotal = MConversionRate.convert(Env.getCtx(), ordC.getSO_CreditOver(), order.getC_Currency_ID(), currencyID, order.getDateOrdered(), order.getC_ConversionType_ID(), order.getAD_Client_ID(), order.getAD_Org_ID());
					if(grandTotal==null){
						throw new AdempiereException("ISY-36\n@NoCurrencyConversion@");
					}
				}
			} else if(scenarioType.equals(MWFScenario.SCENARIO_AGING_OVER)){
				grandTotal = ordC.getSO_AgingOver();
			}			
			
			int Z_WFScenario_ID = getScenario(order.getC_DocTypeTarget_ID(), 
					order.getAD_Org_ID(), order.getAD_OrgTrx_ID(), order.getC_Project_ID(), order.getM_PriceList_ID(), 
					order.getUser1_ID(), order.getC_Activity_ID(), order.getC_Campaign_ID(), order.getM_Warehouse_ID(),
					grandTotal,scenarioType,TrxName);
			if( Z_WFScenario_ID > 0 ) {
				if(scenarioType==null){
					if(order.isSOTrx()){
						ordC.setZ_WFScenarioGrandtotal_ID(Z_WFScenario_ID);
					} else {
						ordC.setZ_WFScenario_ID(Z_WFScenario_ID);
					}
				} else if(scenarioType.equals(MWFScenario.SCENARIO_CREDIT_OVER)){
					ordC.setZ_WFScenarioCredit_ID(Z_WFScenario_ID);
				} else if(scenarioType.equals(MWFScenario.SCENARIO_PRICE_LIMIT)){
					ordC.setZ_WFScenarioPrice_ID(Z_WFScenario_ID);
				} else if(scenarioType.equals(MWFScenario.SCENARIO_AGING_OVER)){
					ordC.setZ_WFScenarioAging_ID(Z_WFScenario_ID);
				}								
				order.saveEx();
			} else {
				if(scenarioType==null){
					ordC.setZ_WFScenario_ID(0);	
					ordC.setZ_WFScenarioGrandtotal_ID(0);
				} else if(scenarioType.equals(MWFScenario.SCENARIO_CREDIT_OVER)){
					ordC.setZ_WFScenarioCredit_ID(0);
				} else if(scenarioType.equals(MWFScenario.SCENARIO_PRICE_LIMIT)){
					ordC.setZ_WFScenarioPrice_ID(0);
				} else if(scenarioType.equals(MWFScenario.SCENARIO_AGING_OVER)){
					ordC.setZ_WFScenarioAging_ID(0);
				}
				order.saveEx();
			}
		} else if( po.get_TableName().equals(MPayment.Table_Name) ) {
			X_C_Payment vpo = (X_C_Payment)po;
			ICPayment vpoC = POWrapper.create(vpo, ICPayment.class);
			BigDecimal grandTotal = vpo.getPayAmt();
			if(vpo.getC_Currency_ID()!=currencyID){
				grandTotal = MConversionRate.convert(Env.getCtx(), vpo.getPayAmt(), vpo.getC_Currency_ID(), currencyID, vpo.getDateTrx(), vpo.getC_ConversionType_ID(), vpo.getAD_Client_ID(), vpo.getAD_Org_ID());
				if(grandTotal==null){
					throw new AdempiereException("ISY-36\n@NoCurrencyConversion@");
				}
			}	
			int Z_WFScenario_ID = getScenario(vpo.getC_DocType_ID(), 
					vpo.getAD_Org_ID(), vpo.getAD_OrgTrx_ID(), vpo.getC_Project_ID(), 0, 
					vpo.getUser1_ID(), vpo.getC_Activity_ID(), vpo.getC_Campaign_ID(), 0,
					grandTotal,scenarioType,TrxName);
			if( Z_WFScenario_ID > 0 ) {
				vpoC.setZ_WFScenario_ID(Z_WFScenario_ID);
				vpo.saveEx();
			} else {
				vpoC.setZ_WFScenario_ID(0);				
				vpo.saveEx();
			}
		} else if( po.get_TableName().equals(MRMA.Table_Name) ) {
			MRMA vpo = (MRMA)po;
			IMRMA vpoC = POWrapper.create(vpo, IMRMA.class);
			BigDecimal grandTotal = vpo.getAmt();
			if(vpo.getInOut_ID()>0 && vpo.getInOut().getC_Order_ID()>0 && vpo.getInOut().getC_Order().getC_Currency_ID()!=currencyID){
				grandTotal = MConversionRate.convert(Env.getCtx(), vpo.getAmt(), vpo.getInOut().getC_Order().getC_Currency_ID(), currencyID, vpo.getInOut().getMovementDate(), conversionTypeID, vpo.getAD_Client_ID(), vpo.getAD_Org_ID());
				if(grandTotal==null){
					throw new AdempiereException("ISY-36\n@NoCurrencyConversion@");
				}
			}
			int orgTrxID = 0;
			int projectID = 0;
			int pricelistID = 0;
			int user1ID = 0;
			int activityID = 0;
			int campaignID = 0;
			int warehouseID = 0;
			if (vpo.getInOut_ID() > 0) {
				MInOut io = new MInOut(Env.getCtx(), vpo.getInOut_ID(), null);
				orgTrxID = io.getAD_OrgTrx_ID();
				projectID = io.getC_Project_ID();
				user1ID = io.getUser1_ID();
				activityID = io.getC_Activity_ID();
				campaignID = io.getC_Campaign_ID();
				warehouseID = io.getM_Warehouse_ID();
				if (io.getC_Order_ID() > 0) {
					MOrder order = new MOrder(Env.getCtx(), io.getC_Order_ID(), null);
					pricelistID = order.getM_PriceList_ID();
				}
			}
			int Z_WFScenario_ID = getScenario(vpo.getC_DocType_ID(), 
					vpo.getAD_Org_ID(), orgTrxID, projectID, pricelistID, 
					user1ID, activityID, campaignID, warehouseID,
					grandTotal,scenarioType,TrxName);
			if( Z_WFScenario_ID > 0 ) {
				vpoC.setZ_WFScenario_ID(Z_WFScenario_ID);
				vpo.saveEx();
			} else {
				vpoC.setZ_WFScenario_ID(0);				
				vpo.saveEx();
			}
		} else if( po.get_TableName().equals(MTimeExpense.Table_Name) ) {
			MTimeExpense vpo = (MTimeExpense)po;
			ISTimeExpense vpoC = POWrapper.create(vpo, ISTimeExpense.class);
			BigDecimal grandTotal = vpo.getApprovalAmt();
			if(vpo.getC_Currency_ID()!=currencyID){
				grandTotal = MConversionRate.convert(Env.getCtx(), vpo.getApprovalAmt(), vpo.getC_Currency_ID(), currencyID, vpo.getDateReport(), conversionTypeID, vpo.getAD_Client_ID(), vpo.getAD_Org_ID());
				if(grandTotal==null){
					throw new AdempiereException("ISY-36\n@NoCurrencyConversion@");
				}
			}	
			int doctypeID = vpoC.getC_DocTypeTarget_ID();
			if (vpo.get_ValueAsInt("C_DocType_ID") > 0) {
				doctypeID = vpo.get_ValueAsInt("C_DocType_ID");
			}
			int Z_WFScenario_ID = getScenario(doctypeID, 
					vpo.getAD_Org_ID(), vpoC.getAD_OrgTrx_ID(), vpoC.getC_Project_ID(), vpo.getM_PriceList_ID(), 
					vpoC.getUser1_ID(), vpoC.getC_Activity_ID(), vpoC.getC_Campaign_ID(), vpo.getM_Warehouse_ID(),
					grandTotal,scenarioType,TrxName);
			if( Z_WFScenario_ID > 0 ) {
				vpoC.setZ_WFScenario_ID(Z_WFScenario_ID);
				vpo.saveEx();
			} else {
				vpoC.setZ_WFScenario_ID(0);				
				vpo.saveEx();
			}
		} else if( po.get_TableName().equals(MRequisition.Table_Name) ) {
			MRequisition vpo = (MRequisition)po;
			IMRequisition vpoC = POWrapper.create(vpo, IMRequisition.class);
			BigDecimal grandTotal = vpo.getTotalLines();
			if(vpo.getM_PriceList().getC_Currency_ID()!=currencyID){
				grandTotal = MConversionRate.convert(Env.getCtx(), vpo.getTotalLines(), vpo.getM_PriceList().getC_Currency_ID(), currencyID, vpo.getDateDoc(), conversionTypeID, vpo.getAD_Client_ID(), vpo.getAD_Org_ID());
				if(grandTotal==null){
					throw new AdempiereException("ISY-36\n@NoCurrencyConversion@");
				}
			}
			int Z_WFScenario_ID = getScenario(vpo.getC_DocType_ID(), 
					vpo.getAD_Org_ID(), vpoC.getAD_OrgTrx_ID(), vpoC.getC_Project_ID(), vpo.getM_PriceList_ID(), 
					vpoC.getUser1_ID(), vpoC.getC_Activity_ID(), vpoC.getC_Campaign_ID(), vpo.getM_Warehouse_ID(),
					grandTotal,scenarioType,TrxName);
			if( Z_WFScenario_ID > 0 ) {
				vpoC.setZ_WFScenario_ID(Z_WFScenario_ID);
				vpo.saveEx();
			} else {
				vpoC.setZ_WFScenario_ID(0);				
				vpo.saveEx();
			}
		}
	}

}
