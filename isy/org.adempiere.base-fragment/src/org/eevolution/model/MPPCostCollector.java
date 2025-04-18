/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * Copyright (C) 2003-2007 e-Evolution,SC. All Rights Reserved.               *
 * Contributor(s): Victor Perez www.e-evolution.com                           *
 *                 Teo Sarca, www.arhipac.ro                                  *
 *****************************************************************************/
package org.eevolution.model;


import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.DocTypeNotFoundException;
import org.adempiere.exceptions.FillMandatoryException;
import org.adempiere.exceptions.NoVendorForProductException;
import org.adempiere.model.engines.CostEngineFactory;
import org.adempiere.model.engines.IDocumentLine;
import org.adempiere.model.engines.StorageEngine;
import org.compiere.model.I_C_UOM;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MBPartner;
import org.compiere.model.MCost;
import org.compiere.model.MCostDetail;
import org.compiere.model.MCostElement;
import org.compiere.model.MDocType;
import org.compiere.model.MLocator;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MPeriod;
import org.compiere.model.MProduct;
import org.compiere.model.MProductPO;
import org.compiere.model.MTransaction;
import org.compiere.model.MUOM;
import org.compiere.model.MWarehouse;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.Query;
import org.compiere.print.ReportEngine;
import org.compiere.process.DocAction;
import org.compiere.process.DocumentEngine;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.TimeUtil;
import org.libero.exceptions.ActivityProcessedException;

import id.co.databiz.awn.model.AWNSysConfig;

import org.eevolution.model.I_PP_Cost_Collector;
import org.eevolution.model.X_PP_Cost_Collector;

/**
 *	PP Cost Collector Model
 *	
 *  @author victor.perez@e-evolution.com, e-Evolution http://www.e-evolution.com
 *			<li> Original contributor of Manufacturing Standard Cost
 * 			<li> FR [ 2520591 ] Support multiples calendar for Org 
 *			@see http://sourceforge.net/tracker2/?func=detail&atid=879335&aid=2520591&group_id=176962 
 *  
 *  @author Teo Sarca, www.arhipac.ro 
 *  @version $Id: MPPCostCollector.java,v 1.1 2004/06/19 02:10:34 vpj-cd Exp $
 */
public class MPPCostCollector extends X_PP_Cost_Collector implements DocAction , IDocumentLine
{
	private static final long serialVersionUID = 1L;
	
    /**
     * Create & Complete Cost Collector 
     * @param order
     * @param M_Product_ID
     * @param M_Locator_ID
     * @param M_AttributeSetInstance_ID
     * @param S_Resource_ID
     * @param PP_Order_BOMLine_ID
     * @param PP_Order_Node_ID
     * @param C_DocType_ID
     * @param CostCollectorType
     * @param movementdate
     * @param qty
     * @param scrap
     * @param reject
     * @param durationSetup
     * @param duration
     * @param trxName
     * @return completed cost collector
     */
	public static MPPCostCollector createCollector (MPPOrder order,
			int M_Product_ID,
			int M_Locator_ID,
			int M_AttributeSetInstance_ID,
			int S_Resource_ID,
			int PP_Order_BOMLine_ID,
			int PP_Order_Node_ID,
			int C_DocType_ID,
			String CostCollectorType,
			Timestamp movementdate,
			BigDecimal qty,
			BigDecimal scrap,
			BigDecimal reject,
			int durationSetup,
			BigDecimal duration
		)
	{
//		int id = getDocType(CostCollectorType);
//		if(id>0){
//			C_DocType_ID = id;
//		}
		MPPCostCollector cc = new MPPCostCollector(order);
		cc.setPP_Order_BOMLine_ID(PP_Order_BOMLine_ID);
		cc.setPP_Order_Node_ID(PP_Order_Node_ID);
		cc.setC_DocType_ID(C_DocType_ID);
		cc.setC_DocTypeTarget_ID(C_DocType_ID);
		cc.setCostCollectorType(CostCollectorType);
		cc.setDocAction(MPPCostCollector.DOCACTION_Complete);
		cc.setDocStatus(MPPCostCollector.DOCSTATUS_Drafted);
		cc.setIsActive(true);
		cc.setM_Locator_ID(M_Locator_ID);
		cc.setM_AttributeSetInstance_ID(M_AttributeSetInstance_ID);
		cc.setS_Resource_ID(S_Resource_ID);
		cc.setMovementDate(movementdate);
		cc.setDateAcct(movementdate);
		cc.setMovementQty(qty);
		cc.setScrappedQty(scrap);
		cc.setQtyReject(reject);
		cc.setSetupTimeReal(new BigDecimal(durationSetup));
		cc.setDurationReal(duration);
		cc.setPosted(false);
		cc.setProcessed(false);
		cc.setProcessing(false);
		cc.setUser1_ID(order.getUser1_ID());
		cc.setUser2_ID(order.getUser2_ID());
		cc.setM_Product_ID(M_Product_ID);
		if(PP_Order_Node_ID > 0)
		{	
			cc.setIsSubcontracting(PP_Order_Node_ID);
		}
		// If this is an material issue, we should use BOM Line's UOM
		if (PP_Order_BOMLine_ID > 0)
		{
			cc.setC_UOM_ID(0); // we set the BOM Line UOM on beforeSave
		}
		cc.saveEx(order.get_TrxName());
		if (!cc.processIt(MPPCostCollector.DOCACTION_Complete))
		{
			throw new AdempiereException(cc.getProcessMsg());
		}
		cc.saveEx(order.get_TrxName());
		return cc;
	}
	
	public static void setPP_Order(I_PP_Cost_Collector cc, MPPOrder order)
	{
		cc.setPP_Order_ID(order.getPP_Order_ID());
		cc.setPP_Order_Workflow_ID(order.getMPPOrderWorkflow().get_ID());
		cc.setAD_Org_ID(order.getAD_Org_ID());
		cc.setM_Warehouse_ID(order.getM_Warehouse_ID());
		cc.setAD_OrgTrx_ID(order.getAD_OrgTrx_ID());
		cc.setC_Activity_ID(order.getC_Activity_ID());
		cc.setC_Campaign_ID(order.getC_Campaign_ID());
		cc.setC_Project_ID(order.getC_Project_ID());
		cc.setDescription(order.getDescription());
		cc.setS_Resource_ID(order.getS_Resource_ID());
		cc.setM_Product_ID(order.getM_Product_ID());
		cc.setC_UOM_ID(order.getC_UOM_ID());
		cc.setM_AttributeSetInstance_ID(order.getM_AttributeSetInstance_ID());
		cc.setMovementQty(order.getQtyOrdered());
	}
	


	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param PP_Cost_Collector id
	 */
	public MPPCostCollector(Properties ctx, int PP_Cost_Collector_ID, String trxName)
	{
		super (ctx, PP_Cost_Collector_ID,trxName);
		if (PP_Cost_Collector_ID == 0)
		{
			//setC_DocType_ID(0);
			setDocStatus (DOCSTATUS_Drafted);	// DR
			setDocAction (DOCACTION_Complete);	// CO
			setMovementDate (new Timestamp(System.currentTimeMillis()));	// @#Date@
			setIsActive(true);
			setPosted (false);
			setProcessing (false);
			setProcessed (false);
		}	
	}	//	MPPCostCollector

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 */
	public MPPCostCollector(Properties ctx, ResultSet rs,String trxName)
	{
		super (ctx, rs, trxName);
	}	//	MPPCostCollector
	
	/**
	 * 	Load Constructor
	 *	@param MPPOrder
	 */
	public MPPCostCollector(MPPOrder order)
	{
		this(order.getCtx(), 0 , order.get_TrxName());
		setPP_Order(this, order);
		m_order = order;	
	}	//	MPPCostCollector


	/**
	 * 	Add to Description
	 *	@param description text
	 */
	public void addDescription (String description)
	{
		String desc = getDescription();
		if (desc == null)
			setDescription(description);
		else
			setDescription(desc + " | " + description);
	}	//	addDescription
	
	
	public void setC_DocTypeTarget_ID(String docBaseType)
	{
		MDocType[] doc = MDocType.getOfDocBaseType(getCtx(), docBaseType);	
		if(doc == null)
		{
			throw new DocTypeNotFoundException(docBaseType, "");
		}
		else
		{	
			setC_DocTypeTarget_ID(doc[0].get_ID());
		}
	}

//	@Override
	public void setProcessed (boolean processed)
	{
		super.setProcessed (processed);
		if (get_ID() == 0)
			return;
		final String sql = "UPDATE PP_Cost_Collector SET Processed=? WHERE PP_Cost_Collector_ID=?";
		int noLine = DB.executeUpdateEx(sql, new Object[]{processed, get_ID()}, get_TrxName());
		log.fine("setProcessed - " + processed + " - Lines=" + noLine);
	}	//	setProcessed


//	@Override
	public boolean processIt (String processAction)
	{
		m_processMsg = null;
		DocumentEngine engine = new DocumentEngine (this, getDocStatus());
		return engine.processIt (processAction, getDocAction());
	}	//	processIt

	/**	Process Message 			*/
	private String		m_processMsg = null;
	/**	Just Prepared Flag			*/
	private boolean		m_justPrepared = false;
	
	/** Manufacturing Order **/
	private MPPOrder m_order = null;
	
	/** Manufacturing Order Activity **/
	private MPPOrderNode m_orderNode = null;
	
	/** Manufacturing Order BOM Line **/
	private MPPOrderBOMLine m_bomLine = null;

//	@Override
	public boolean unlockIt()
	{
		log.info("unlockIt - " + toString());
		setProcessing(false);
		return true;
	}	//	unlockIt

//	@Override
	public boolean invalidateIt()
	{
		log.info("invalidateIt - " + toString());
		setDocAction(DOCACTION_Prepare);
		return true;
	}	//	invalidateIt

//	@Override
	public String prepareIt()
	{
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_PREPARE);
		if (m_processMsg != null)
		{
			return DocAction.STATUS_Invalid;
		}
		
		MPeriod.testPeriodOpen(getCtx(), getDateAcct(), getC_DocTypeTarget_ID(), getAD_Org_ID());
		//	Convert/Check DocType
		setC_DocType_ID(getC_DocTypeTarget_ID());
		
		//
		// Operation Activity
		if(isActivityControl())
		{
			MPPOrderNode activity = getPP_Order_Node();
			if(MPPOrderNode.DOCACTION_Complete.equals(activity.getDocStatus()))
			{	
				throw new ActivityProcessedException(activity);
			}
			
			if (activity.isSubcontracting())
			{
				if(MPPOrderNode.DOCSTATUS_InProgress.equals(activity.getDocStatus())
						&& MPPCostCollector.DOCSTATUS_InProgress.equals(getDocStatus()))
				{			
					return MPPOrderNode.DOCSTATUS_InProgress;
				}
				else if(MPPOrderNode.DOCSTATUS_InProgress.equals(activity.getDocStatus())
						&& MPPCostCollector.DOCSTATUS_Drafted.equals(getDocStatus()))
				{
					throw new ActivityProcessedException(activity);
				}				
				m_processMsg = createPO(activity);
				m_justPrepared = false;
				activity.setInProgress(this);
				activity.saveEx(get_TrxName());
				return DOCSTATUS_InProgress;
			}
			
			activity.setInProgress(this);
			activity.setQtyDelivered(activity.getQtyDelivered().add(getMovementQty()));
			activity.setQtyScrap(activity.getQtyScrap().add(getScrappedQty()));
			activity.setQtyReject(activity.getQtyReject().add(getQtyReject()));
			activity.setDurationReal(activity.getDurationReal()+getDurationReal().intValueExact());
			activity.setSetupTimeReal(activity.getSetupTimeReal()+getSetupTimeReal().intValueExact());
			activity.saveEx(get_TrxName());

			// report all activity previews to milestone activity
			if(activity.isMilestone())
			{
				MPPOrderWorkflow order_workflow = activity.getMPPOrderWorkflow();
				order_workflow.closeActivities(activity, getMovementDate(), true);
			}
		}
		// Issue
		else if (isIssue())
		{
			MProduct product = getM_Product();
			if (getM_AttributeSetInstance_ID() == 0 && product.isASIMandatory(false))
			{
				throw new AdempiereException("@M_AttributeSet_ID@ @IsMandatory@ @M_Product_ID@=" + product.getValue());
			}
		}
		// Receipt
		else if (isReceipt())
		{
			MProduct product = getM_Product();
			if (getM_AttributeSetInstance_ID() == 0 && product.isASIMandatory(true))
			{
				throw new AdempiereException("@M_AttributeSet_ID@ @IsMandatory@ @M_Product_ID@=" + product.getValue());
			}
		}
		
		m_justPrepared = true;
		setDocAction(DOCACTION_Complete);
		
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_PREPARE);
		if (m_processMsg != null)
		{
			return DocAction.STATUS_Invalid;
		}

		return DocAction.STATUS_InProgress;
	}	//	prepareIt

//	@Override
	public boolean  approveIt()
	{
		log.info("approveIt - " + toString());
		//setIsApproved(true);
		return true;
	}	//	approveIt

//	@Override
	public boolean rejectIt()
	{
		log.info("rejectIt - " + toString());
		//setIsApproved(false);
		return true;
	}	//	rejectIt

//	@Override
	public String completeIt()
	{
		//	Re-Check
		if (!m_justPrepared)
		{
			String status = prepareIt();
			if (!DocAction.STATUS_InProgress.equals(status))
				return status;
		}

		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_COMPLETE);
		if (m_processMsg != null)
			return DocAction.STATUS_Invalid;
		
		//
		// Material Issue (component issue, method change variance, mix variance)
		// Material Receipt
		if(isIssue() || isReceipt())
		{
			//	Stock Movement 
			MProduct product = getM_Product();
			if (product != null	&& product.isStocked() && !isVariance())
			{
				StorageEngine.createTransaction(
						this,
						getMovementType() , 
						getMovementDate() , 
						getMovementQty() , 
						false,											// IsReversal=false
						getM_Warehouse_ID(), 
						getPP_Order().getM_AttributeSetInstance_ID(),	// Reservation ASI
						getPP_Order().getM_Warehouse_ID(),				// Reservation Warehouse
						false											// IsSOTrx=false
						);
			}	//	stock movement
			
			// MRI-153 MRI-188
						// -> if service & expense then create costDetail ~dar~
			else if( product != null ) {
							//get accounting schema
					MAcctSchema as = MAcctSchema.getClientAcctSchema(getCtx(), getAD_Client_ID())[0];
					String CostingLevel = product.getCostingLevel(as);
					int AD_Org_ID = getAD_Org_ID();
					int M_AttributeSetInstance_ID = getM_AttributeSetInstance_ID();
					if (MAcctSchema.COSTINGLEVEL_Client.equals(CostingLevel))
						{
							AD_Org_ID = 0;
							M_AttributeSetInstance_ID = 0;
						}
					else if (MAcctSchema.COSTINGLEVEL_Organization.equals(CostingLevel))
							M_AttributeSetInstance_ID = 0;
					else if (MAcctSchema.COSTINGLEVEL_BatchLot.equals(CostingLevel))
							AD_Org_ID = 0;
							
					BigDecimal price = Env.ZERO;

					List<MCostElement> costElementList = MCostElement.getByCostingMethod(getCtx(), as.getCostingMethod());
					for (MCostElement costElement : costElementList) {
						MCost cost = MCost.get(getCtx(), getAD_Client_ID(), AD_Org_ID, getM_Product_ID(), 
											as.getM_CostType_ID(), as.get_ID(), costElement.get_ID(), 
											M_AttributeSetInstance_ID, get_TrxName());
						price = cost.getCurrentCostPrice();
						price = roundCost(price, as.getC_AcctSchema_ID());
				
							//create cost detail
							BigDecimal amt = roundCost(price.multiply(getMovementQty()), as.getC_AcctSchema_ID());
							MCostDetail cdv = new MCostDetail(getCtx(), 0, get_TrxName());
								if (product != null)
								{
									cdv.setM_Product_ID(product.getM_Product_ID());
									cdv.setM_AttributeSetInstance_ID(M_AttributeSetInstance_ID);
								}
								if (as != null)
								{
									cdv.setC_AcctSchema_ID(as.getC_AcctSchema_ID());
								}
								cdv.setM_CostElement_ID(costElement.get_ID());
								cdv.setPP_Cost_Collector_ID(getPP_Cost_Collector_ID());
								cdv.setAmt(amt.negate());
								cdv.setQty(getMovementQty().negate());
								cdv.saveEx();
							}
							//processCostDetail(cdv);
						}
						// <- if service & expense then create costDetail ~dar~

			
			if (isIssue())				
			{
				//	Update PP Order Line
				MPPOrderBOMLine obomline = getPP_Order_BOMLine();
				obomline.setQtyDelivered(obomline.getQtyDelivered().add(getMovementQty()));
				obomline.setQtyScrap(obomline.getQtyScrap().add(getScrappedQty()));
				obomline.setQtyReject(obomline.getQtyReject().add(getQtyReject()));  
				obomline.setDateDelivered(getMovementDate());	//	overwrite=last	
 				log.fine("OrderLine - Reserved=" + obomline.getQtyReserved() + ", Delivered=" + obomline.getQtyDelivered());				
				obomline.saveEx(get_TrxName());
				log.fine("OrderLine -> Reserved="+obomline.getQtyReserved()+", Delivered="+obomline.getQtyDelivered());
			}
			if (isReceipt())
			{
				//	Update PP Order Qtys 
				final MPPOrder order = getPP_Order();
				order.setQtyDelivered(order.getQtyDelivered().add(getMovementQty()));                
				order.setQtyScrap(order.getQtyScrap().add(getScrappedQty()));
				order.setQtyReject(order.getQtyReject().add(getQtyReject()));                				
				//
				// Update PP Order Dates
				order.setDateDelivered(getMovementDate()); //	overwrite=last
				if (order.getDateStart() == null)
				{
					order.setDateStart(getDateStart());
				}
				if (order.getQtyOpen().signum() <= 0)
				{
					order.setDateFinish(getDateFinish());
				}
				order.saveEx(get_TrxName());
			}
		}
		//
		// Activity Control
		else if(isActivityControl())
		{
			MPPOrderNode activity = getPP_Order_Node();
			if(activity.isProcessed())
			{
				throw new ActivityProcessedException(activity);
			}
			
			if(isSubcontracting())
			{	
				String whereClause = MOrderLine.COLUMNNAME_PP_Cost_Collector_ID+"=?";
				Collection<MOrderLine> olines = new Query(getCtx(), MOrderLine.Table_Name, whereClause, get_TrxName())
													.setParameters(new Object[]{get_ID()})
													.list();
				String DocStatus = MPPOrderNode.DOCSTATUS_Completed;
				StringBuffer msg = new StringBuffer("The quantity do not is complete for next Purchase Order : ");
				for (MOrderLine oline : olines)
				{
					if(oline.getQtyDelivered().compareTo(oline.getQtyOrdered()) < 0)
					{
						DocStatus = MPPOrderNode.DOCSTATUS_InProgress;
					}
					msg.append(oline.getParent().getDocumentNo()).append(",");
				}
				
				if(MPPOrderNode.DOCSTATUS_InProgress.equals(DocStatus))
				{	
					m_processMsg = msg.toString();
					return DocStatus;
				}
				setProcessed(true);
				setDocAction(MPPOrderNode.DOCACTION_Close);
				setDocStatus(MPPOrderNode.DOCSTATUS_Completed);
				activity.completeIt();
				activity.saveEx(get_TrxName());
				m_processMsg = Msg.translate(getCtx(), "PP_Order_ID")
				+": "+ getPP_Order().getDocumentNo()
				+" "+ Msg.translate(getCtx(),"PP_Order_Node_ID")
				+": "+getPP_Order_Node().getValue();
				return DocStatus;
			}
			else
			{
//				CostEngineFactory.getCostEngine(getAD_Client_ID()).createActivityControl(this);
//				if(activity.getQtyDelivered().compareTo(activity.getQtyRequired()) >= 0)
//				{
//					activity.closeIt();
//					activity.saveEx(get_TrxName());									
//				}
			}
		}
		//
		// Usage Variance (material)
		else if (isCostCollectorType(COSTCOLLECTORTYPE_UsegeVariance) && getPP_Order_BOMLine_ID() > 0)
		{
			MPPOrderBOMLine obomline = getPP_Order_BOMLine();
			obomline.setQtyDelivered(obomline.getQtyDelivered().add(getMovementQty()));
			obomline.setQtyScrap(obomline.getQtyScrap().add(getScrappedQty()));
			obomline.setQtyReject(obomline.getQtyReject().add(getQtyReject()));  
			//obomline.setDateDelivered(getMovementDate());	//	overwrite=last	
 			log.fine("OrderLine - Reserved=" + obomline.getQtyReserved() + ", Delivered=" + obomline.getQtyDelivered());				
			obomline.saveEx(get_TrxName());
			log.fine("OrderLine -> Reserved="+obomline.getQtyReserved()+", Delivered="+obomline.getQtyDelivered());
			CostEngineFactory.getCostEngine(getAD_Client_ID()).createUsageVariances(this);
		}
		//
		// Usage Variance (resource)
		else if (isCostCollectorType(COSTCOLLECTORTYPE_UsegeVariance) && getPP_Order_Node_ID() > 0)
		{
			MPPOrderNode activity = getPP_Order_Node();
			activity.setDurationReal(activity.getDurationReal()+getDurationReal().intValueExact());
			activity.setSetupTimeReal(activity.getSetupTimeReal()+getSetupTimeReal().intValueExact());
			activity.saveEx(get_TrxName());
			CostEngineFactory.getCostEngine(getAD_Client_ID()).createUsageVariances(this);
		}
		else
		{
			; // nothing
		}
		//
		CostEngineFactory.getCostEngine(getAD_Client_ID()).createRateVariances(this);
		CostEngineFactory.getCostEngine(getAD_Client_ID()).createMethodVariances(this);

		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_COMPLETE);
		if (m_processMsg != null)
			return DocAction.STATUS_Invalid;

		//	
		setProcessed(true);
		setDocAction(DOCACTION_Close);
		setDocStatus(DOCSTATUS_Completed);
		
		return DocAction.STATUS_Completed;
	}	//	completeIt

//	@Override
	public boolean voidIt()
	{
		return false;
	}	//	voidIt

//	@Override
	public boolean closeIt()
	{
		log.info("closeIt - " + toString());
		setDocAction(DOCACTION_None);
		return true;
	}	//	closeIt

//	@Override
	public boolean reverseCorrectIt()
	{
		return false;
	}

//	@Override
	public boolean reverseAccrualIt()
	{
		return false;
	}

//	@Override
	public boolean reActivateIt()
	{
		return false;
	}

//	@Override
	public String getSummary()
	{
		StringBuffer sb = new StringBuffer();
		sb.append(getDescription());
		return sb.toString();
	}

//	@Override
	public String getProcessMsg()
	{
		return m_processMsg;
	}

//	@Override
	public int getDoc_User_ID()
	{
		return getCreatedBy();
	}

//	@Override
	public int getC_Currency_ID()
	{
		return 0;
	}

//	@Override
	public BigDecimal getApprovalAmt()
	{
		return Env.ZERO;
	}

//	@Override
	public File createPDF ()
	{
		try
		{
			File temp = File.createTempFile(get_TableName()+get_ID()+"_", ".pdf");
			return createPDF (temp);
		}
		catch (Exception e)
		{
			log.severe("Could not create PDF - " + e.getMessage());
		}
		return null;
	}	//	getPDF

	/**
	 * 	Create PDF file
	 *	@param file output file
	 *	@return file if success
	 */
	public File createPDF (File file)
	{
		ReportEngine re = ReportEngine.get (getCtx(), ReportEngine.ORDER, getPP_Order_ID());
		if (re == null)
			return null;
		return re.getPDF(file);
	}	//	createPDF

//	@Override
	public String getDocumentInfo()
	{
		MDocType dt = MDocType.get(getCtx(), getC_DocType_ID());
		return dt.getName() + " " + getDocumentNo();
	}	//	getDocumentInfo

	@Override
	protected boolean beforeSave(boolean newRecord)
	{
		// Set default locator, if not set and we have the warehouse:
		if (getM_Locator_ID() <= 0 && getM_Warehouse_ID() > 0)
		{
			MWarehouse wh = MWarehouse.get(getCtx(), getM_Warehouse_ID());
			MLocator loc = wh.getDefaultLocator();
			if (loc != null)
			{
				setM_Locator_ID(loc.get_ID());
			}
		}
		//
		if (isIssue())
		{
			if (getPP_Order_BOMLine_ID() <= 0)
			{
				throw new FillMandatoryException(COLUMNNAME_PP_Order_BOMLine_ID);
			}
			// If no UOM, use the UOM from BOMLine
			if (getC_UOM_ID() <= 0)
			{
				setC_UOM_ID(getPP_Order_BOMLine().getC_UOM_ID());
			}
			// If Cost Collector UOM differs from BOM Line UOM then throw exception because this conversion is not supported yet
			if (getC_UOM_ID() != getPP_Order_BOMLine().getC_UOM_ID())
			{
				throw new AdempiereException("@PP_Cost_Collector_ID@ @C_UOM_ID@ <> @PP_Order_BOMLine_ID@ @C_UOM_ID@");
			}
		}
		//
		if (isActivityControl() && getPP_Order_Node_ID() <= 0)
		{
			throw new FillMandatoryException(COLUMNNAME_PP_Order_Node_ID);
		}
		return true;
	}

	@Override
	public MPPOrderNode getPP_Order_Node()
	{
		int node_id = getPP_Order_Node_ID();
		if (node_id <= 0)
		{
			m_orderNode = null;
			return null;
		}
		if (m_orderNode == null || m_orderNode.get_ID() != node_id)
		{
			m_orderNode = new MPPOrderNode(getCtx(), node_id, get_TrxName());
		}
		return m_orderNode;
	}

	@Override
	public MPPOrderBOMLine getPP_Order_BOMLine()
	{
		int id = getPP_Order_BOMLine_ID();
		if (id <= 0)
		{
			m_bomLine = null;
			return null;
		}
		if (m_bomLine == null || m_bomLine.get_ID() != id)
		{
			m_bomLine = new MPPOrderBOMLine(getCtx(), id, get_TrxName());
		}
		m_bomLine.set_TrxName(get_TrxName());
		return m_bomLine;
	}
	
	@Override
	public MPPOrder getPP_Order()
	{
		int id = getPP_Order_ID();
		if (id <= 0)
		{
			m_order = null;
			return null;
		}
		if (m_order == null || m_order.get_ID() != id)
		{
			m_order = new MPPOrder(getCtx(), id, get_TrxName());
		}
		return m_order;
	}
	
	/**
	 * Get Duration Base in Seconds
	 * @return duration unit in seconds
	 * @see MPPOrderWorkflow#getDurationBaseSec()
	 */
	public long getDurationBaseSec()
	{
		return getPP_Order().getMPPOrderWorkflow().getDurationBaseSec();
	}

	/**
	 * @return Activity Control Report Start Date
	 */
	public Timestamp getDateStart()
	{
		double duration = getDurationReal().doubleValue();
		if (duration != 0)
		{
			long durationMillis = (long)(getDurationReal().doubleValue() * getDurationBaseSec() * 1000.0);
			return new Timestamp(getMovementDate().getTime() - durationMillis);
		}
		else
		{
			return getMovementDate();
		}
	}
	
	/**
	 * @return Activity Control Report End Date
	 */
	public Timestamp getDateFinish()
	{
		return getMovementDate();
	}

	
	/**
	 * Create Purchase Order (in case of Subcontracting)
	 * @param activity
	 */
	private String createPO(MPPOrderNode activity)
	{
		String msg = "";
		HashMap<Integer,MOrder> orders = new HashMap<Integer,MOrder>();
		//
		String whereClause = MPPOrderNodeProduct.COLUMNNAME_PP_Order_Node_ID+"=?"
							+" AND "+MPPOrderNodeProduct.COLUMNNAME_IsSubcontracting+"=?";
		Collection<MPPOrderNodeProduct> subcontracts = new Query(getCtx(), MPPOrderNodeProduct.Table_Name, whereClause, get_TrxName())
				.setParameters(new Object[]{activity.get_ID(), true})
				.setOnlyActiveRecords(true)
				.list();
		
		for (MPPOrderNodeProduct subcontract : subcontracts)
		{
			//
			// If Product is not Purchased or is not Service, then it is not a subcontracting candidate [SKIP]
			MProduct product = MProduct.get(getCtx(), subcontract.getM_Product_ID());
			if(!product.isPurchased() || !MProduct.PRODUCTTYPE_Service.equals(product.getProductType()))
				throw new AdempiereException("The Product: " + product.getName() + " Do not is Purchase or Service Type");

			//
			// Find Vendor and Product PO data
			int C_BPartner_ID = activity.getC_BPartner_ID();
			MProductPO product_po = null;
			for (MProductPO ppo : MProductPO.getOfProduct(getCtx(), product.get_ID(), null))
			{
				if(C_BPartner_ID == ppo.getC_BPartner_ID())
				{
					C_BPartner_ID = ppo.getC_BPartner_ID();
					product_po = ppo;
					break;
				}
				if (ppo.isCurrentVendor() && ppo.getC_BPartner_ID() != 0)
				{
					C_BPartner_ID = ppo.getC_BPartner_ID();
					product_po = ppo;
					break;
				}
			}
			if(C_BPartner_ID <= 0 || product_po == null)
			{
				throw new NoVendorForProductException(product.getName());
			}
			//
			// Calculate Lead Time
			Timestamp today = new Timestamp(System.currentTimeMillis());
			Timestamp datePromised = TimeUtil.addDays(today, product_po.getDeliveryTime_Promised()); 
			//
			// Get/Create Purchase Order Header
			MOrder order = orders.get(C_BPartner_ID);
			if(order == null)
			{
				order = new MOrder(getCtx(), 0, get_TrxName());
				MBPartner vendor = MBPartner.get(getCtx(), C_BPartner_ID);
				order.setAD_Org_ID(getAD_Org_ID());
				order.setBPartner(vendor);
				order.setIsSOTrx(false);
				order.setC_DocTypeTarget_ID();
				order.setDatePromised(datePromised);
				order.setDescription(Msg.translate(getCtx(), MPPOrder.COLUMNNAME_PP_Order_ID) +":"+getPP_Order().getDocumentNo());
				order.setDocStatus(MOrder.DOCSTATUS_Drafted);
				order.setDocAction(MOrder.DOCACTION_Complete);
				order.setAD_User_ID(getAD_User_ID());
				order.setM_Warehouse_ID(getM_Warehouse_ID());
				//order.setSalesRep_ID(getAD_User_ID());
				order.saveEx(get_TrxName());
				addDescription(Msg.translate(getCtx(), "C_Order_ID")+": "+order.getDocumentNo());
				orders.put(C_BPartner_ID, order);
				msg = msg +  Msg.translate(getCtx(), "C_Order_ID")
				+" : "+ order.getDocumentNo() 
				+" - "
				+Msg.translate(getCtx(),"C_BPartner_ID")
				+" : "+vendor.getName()+" , ";
			}
			//
			// Create Order Line: 
			BigDecimal QtyOrdered = getMovementQty().multiply(subcontract.getQty());
			// Check Order Min 
			if(product_po.getOrder_Min().signum() > 0)
			{    
				QtyOrdered = QtyOrdered.max(product_po.getOrder_Min());
			}				
			// Check Order Pack
			if (product_po.getOrder_Pack().signum() > 0 && QtyOrdered.signum() > 0)
			{
				QtyOrdered = product_po.getOrder_Pack().multiply(QtyOrdered.divide(product_po.getOrder_Pack(), 0 , BigDecimal.ROUND_UP));
			}
			MOrderLine oline = new MOrderLine(order);
			oline.setM_Product_ID(product.getM_Product_ID());
			oline.setDescription(activity.getDescription());
			oline.setM_Warehouse_ID(getM_Warehouse_ID());
			oline.setQty(QtyOrdered);
			//line.setPrice(m_product_po.getPricePO());
			//oline.setPriceList(m_product_po.getPriceList());
			oline.setPP_Cost_Collector_ID(get_ID());			
			oline.setDatePromised(datePromised);
			oline.saveEx(get_TrxName());
			//
			// TODO: Mark this as processed? 
			setProcessed(true);
		} // each subcontracting line
		return msg;
	}
	
	@Override
	public MProduct getM_Product()
	{
		return MProduct.get(getCtx(), getM_Product_ID());
	}
	
	@Override
	public I_C_UOM getC_UOM()
	{
		return MUOM.get(getCtx(), getC_UOM_ID());
	}

	public boolean isIssue()
	{
		return
		isCostCollectorType(COSTCOLLECTORTYPE_ComponentIssue)
		|| (isCostCollectorType(COSTCOLLECTORTYPE_MethodChangeVariance) && getPP_Order_BOMLine_ID() > 0) // need inventory adjustment
		|| (isCostCollectorType(COSTCOLLECTORTYPE_MixVariance) && getPP_Order_BOMLine_ID() > 0)  // need inventory adjustment
		;
	}
	
	public boolean isReceipt()
	{
		return isCostCollectorType(COSTCOLLECTORTYPE_MaterialReceipt);
	}
	
	public boolean isActivityControl()
	{
		return isCostCollectorType(COSTCOLLECTORTYPE_ActivityControl);
	}
	
	public boolean isVariance()
	{
		return isCostCollectorType(COSTCOLLECTORTYPE_MethodChangeVariance
				, COSTCOLLECTORTYPE_UsegeVariance
				, COSTCOLLECTORTYPE_RateVariance
				, COSTCOLLECTORTYPE_MixVariance);
	}
	
	public String getMovementType()
	{
		if (isReceipt())
			return MTransaction.MOVEMENTTYPE_WorkOrderPlus;
		else if(isIssue())
			return MTransaction.MOVEMENTTYPE_WorkOrder_;
		else
			return null;
	}
	
	/**
	 * Check if CostCollectorType is equal with any of provided types
	 * @param types
	 * @return 
	 */
	public boolean isCostCollectorType(String ... types)
	{
		String type = getCostCollectorType();
		for (String t : types)
		{
			if (type.equals(t))
				return true;
		}
		return false;
	}
	
	
	public boolean isFloorStock()
	{
		final String whereClause = MPPOrderBOMLine.COLUMNNAME_PP_Order_BOMLine_ID+"=?"
									+" AND "+MPPOrderBOMLine.COLUMNNAME_IssueMethod+"=?";
		boolean isFloorStock = new Query(getCtx(), MPPOrderBOMLine.Table_Name, whereClause, get_TrxName())
						.setOnlyActiveRecords(true)
						.setParameters(new Object[]{getPP_Order_BOMLine_ID(), MPPOrderBOMLine.ISSUEMETHOD_FloorStock})
						.match();
		return isFloorStock;
	}
	
	/**
	 * set Is SubContracting
	 * @param PP_Order_Node_ID
	 **/
	public void setIsSubcontracting(int PP_Order_Node_ID)
	{
		
		setIsSubcontracting(MPPOrderNode.get(getCtx(), PP_Order_Node_ID, get_TrxName()).isSubcontracting());
	}
	
	// copy from costengine.java
		protected BigDecimal roundCost(BigDecimal price, int C_AcctSchema_ID)
		{
			// Fix Cost Precision 
			int precision = MAcctSchema.get(Env.getCtx(), C_AcctSchema_ID).getCostingPrecision();
			BigDecimal priceRounded = price;
			if (priceRounded.scale() > precision)
			{
				priceRounded = priceRounded.setScale(precision, RoundingMode.HALF_UP);
			}
			return priceRounded;
		}
		
		public static int getDocType(String CostCollectorType){
			int docTypeID = -1;
			String type = "";
			if(CostCollectorType.equals(COSTCOLLECTORTYPE_MaterialReceipt)){
				docTypeID = AWNSysConfig.DOCTYPE_CC_MaterialReceipt;
				type = "Material Receipt";
			} else if(CostCollectorType.equals(COSTCOLLECTORTYPE_ComponentIssue)){
				docTypeID = AWNSysConfig.DOCTYPE_CC_ComponentIssue;
				type = "Component Issue";
			} else if(CostCollectorType.equals(COSTCOLLECTORTYPE_UsegeVariance)){
				docTypeID = AWNSysConfig.DOCTYPE_CC_UsegeVariance;
				type = "Usege Variance";
			} else if(CostCollectorType.equals(COSTCOLLECTORTYPE_MethodChangeVariance)){
				docTypeID = AWNSysConfig.DOCTYPE_CC_MethodChangeVariance;
				type = "Method Change Variance";
			} else if(CostCollectorType.equals(COSTCOLLECTORTYPE_RateVariance)){
				docTypeID = AWNSysConfig.DOCTYPE_CC_RateVariance;
				type = "Rate Variance";
			} else if(CostCollectorType.equals(COSTCOLLECTORTYPE_MixVariance)){
				docTypeID = AWNSysConfig.DOCTYPE_CC_MixVariance;
				type = "Mix Variance";
			} else if(CostCollectorType.equals(COSTCOLLECTORTYPE_ActivityControl)){
				docTypeID = AWNSysConfig.DOCTYPE_CC_ActivityControl;
				type = "Activity Control";
			}
			MDocType docType = new Query(Env.getCtx(), MDocType.Table_Name, "C_DocType_ID = ?", null)
										.setParameters(new Object[]{docTypeID})
										.setOnlyActiveRecords(true)
										.first();
			if(docType==null){
				throw new AdempiereException("DocType for " + type + " is not found");
			}
			
			return docTypeID;
		}
	
}	//	MPPCostCollector
