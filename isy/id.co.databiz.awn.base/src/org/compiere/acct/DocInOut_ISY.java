/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
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
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package org.compiere.acct;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.logging.Level;

import org.compiere.model.MAccount;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MCostDetail;
import org.compiere.model.MCurrency;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MInOutLineMA;
import org.compiere.model.MOrderLandedCostAllocation;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MSysConfig;
import org.compiere.model.MTax;
import org.compiere.model.ProductCost;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;

import id.co.databiz.awn.model.AWNSysConfig;

/**
 *  Post Shipment/Receipt Documents.
 *  <pre>
 *  Table:              M_InOut (319)
 *  Document Types:     MMS, MMR
 *  </pre>
 *  @author Jorg Janke
 *  @author Armen Rizal, Goodwill Consulting
 * 			<li>BF [ 1745154 ] Cost in Reversing Material Related Docs
 * 			<li>BF [ 2858043 ] Correct Included Tax in Average Costing
 *  @version  $Id: Doc_InOut.java,v 1.3 2006/07/30 00:53:33 jjanke Exp $
 */
public class DocInOut_ISY extends Doc_InOut
{
	/**
	 *  Constructor
	 * 	@param as accounting schema
	 * 	@param rs record
	 * 	@param trxName trx
	 */
	public DocInOut_ISY (MAcctSchema as, ResultSet rs, String trxName)
	{
		super (as, rs, trxName);
		//super (as, MInOut.class, rs, null, trxName);
	}   //  DocInOut

	private int				m_Reversal_ID = 0;
	@SuppressWarnings("unused")
	private String			m_DocStatus = "";

	/**
	 *  Load Document Details
	 *  @return error message or null
	 */
	protected String loadDocumentDetails()
	{
		setC_Currency_ID(NO_CURRENCY);
		MInOut inout = (MInOut)getPO();
		setDateDoc (inout.getMovementDate());
		m_Reversal_ID = inout.getReversal_ID();//store original (voided/reversed) document
		m_DocStatus = inout.getDocStatus();
		//	Contained Objects
		p_lines = loadLines(inout);
		if (log.isLoggable(Level.FINE)) log.fine("Lines=" + p_lines.length);
		return null;
	}   //  loadDocumentDetails

	/**
	 *	Load InOut Line
	 *	@param inout shipment/receipt
	 *  @return DocLine Array
	 */
	private DocLine[] loadLines(MInOut inout)
	{
		ArrayList<DocLine> list = new ArrayList<DocLine>();
		MInOutLine[] lines = inout.getLines(false);
		for (int i = 0; i < lines.length; i++)
		{
			MInOutLine line = lines[i];
			if (line.isDescription()
				|| line.getM_Product_ID() == 0
				|| line.getMovementQty().signum() == 0)
			{
				if (log.isLoggable(Level.FINER)) log.finer("Ignored: " + line);
				continue;
			}

			DocLine docLine = new DocLine (line, this);
			BigDecimal Qty = line.getMovementQty();
			docLine.setReversalLine_ID(line.getReversalLine_ID());
			docLine.setQty (Qty, getDocumentType().equals(DOCTYPE_MatShipment));    //  sets Trx and Storage Qty

			//Define if Outside Processing
			String sql = "SELECT PP_Cost_Collector_ID  FROM C_OrderLine WHERE C_OrderLine_ID=? AND PP_Cost_Collector_ID IS NOT NULL";
			int PP_Cost_Collector_ID = DB.getSQLValueEx(getTrxName(), sql, new Object[]{line.getC_OrderLine_ID()});
			docLine.setPP_Cost_Collector_ID(PP_Cost_Collector_ID);
			//
			if (log.isLoggable(Level.FINE)) log.fine(docLine.toString());
			list.add (docLine);
		}

		//	Return Array
		DocLine[] dls = new DocLine[list.size()];
		list.toArray(dls);
		return dls;
	}	//	loadLines

	/**
	 *  Get Balance
	 *  @return Zero (always balanced)
	 */
	public BigDecimal getBalance()
	{
		BigDecimal retValue = Env.ZERO;
		return retValue;
	}   //  getBalance

	/**
	 *  Create Facts (the accounting logic) for
	 *  MMS, MMR.
	 *  <pre>
	 *  Shipment
	 *      CoGS (RevOrg)   DR
	 *      Inventory               CR
	 *  Shipment of Project Issue
	 *      CoGS            DR
	 *      Project                 CR
	 *  Receipt
	 *      Inventory       DR
	 *      NotInvoicedReceipt      CR
	 *  </pre>
	 *  @param as accounting schema
	 *  @return Fact
	 */
	public ArrayList<Fact> createFacts (MAcctSchema as)
	{
		//
		ArrayList<Fact> facts = new ArrayList<Fact>();
		//  create Fact Header
		Fact fact = new Fact(this, as, Fact.POST_Actual);
		setC_Currency_ID (as.getC_Currency_ID());

		//  Line pointers
		FactLine dr = null;
		FactLine cr = null;

		//  *** Sales - Shipment
		if (getDocumentType().equals(DOCTYPE_MatShipment) && isSOTrx())
		{
			for (int i = 0; i < p_lines.length; i++)
			{
				DocLine line = p_lines[i];				
				MProduct product = line.getProduct();
				BigDecimal costs = null;
				if (!isReversal(line))
				{
					if (MAcctSchema.COSTINGLEVEL_BatchLot.equals(product.getCostingLevel(as)) ) 
					{	
						if (line.getM_AttributeSetInstance_ID() == 0 ) 
						{
							MInOutLine ioLine = (MInOutLine) line.getPO();
							MInOutLineMA mas[] = MInOutLineMA.get(getCtx(), ioLine.get_ID(), getTrxName());
							if (mas != null && mas.length > 0 )
							{
								costs  = BigDecimal.ZERO;
								for (int j = 0; j < mas.length; j++)
								{
									MInOutLineMA ma = mas[j];
									BigDecimal QtyMA = ma.getMovementQty();
									ProductCost pc = line.getProductCost();
									pc.setQty(QtyMA);
									pc.setM_M_AttributeSetInstance_ID(ma.getM_AttributeSetInstance_ID());
									BigDecimal maCosts = line.getProductCosts(as, line.getAD_Org_ID(), true, "M_InOutLine_ID=?");
								
									costs = costs.add(maCosts);
								}						
							}
						} 
						else 
						{							
							costs = line.getProductCosts(as, line.getAD_Org_ID(), true, "M_InOutLine_ID=?");				
						}
					} 
					else
					{
						// MZ Goodwill
						// if Shipment CostDetail exist then get Cost from Cost Detail
						costs = line.getProductCosts(as, line.getAD_Org_ID(), true, "M_InOutLine_ID=?");			
					}
			
					// end MZ
					if (costs == null || costs.signum() == 0)	//	zero costs OK
					{
						if (product.isStocked())
						{
							//ok if we have purchased zero cost item from vendor before
							int count = DB.getSQLValue(null, "SELECT Count(*) FROM M_CostDetail WHERE M_Product_ID=? AND Processed='Y' AND Amt=0.00 AND Qty > 0 AND (C_OrderLine_ID > 0 OR C_InvoiceLine_ID > 0)", 
									product.getM_Product_ID());
							if (count > 0 || MSysConfig.getValue(AWNSysConfig.ISY_STANDARD_COSTING_ALLOW_ZERO_WITHOUT_PURCHASE, "N").equals("Y")) //ISY-389
								//https://databiz.atlassian.net/browse/ISY-389?focusedCommentId=105871
							{
								costs = BigDecimal.ZERO;
							}
							else
							{
								p_Error = "No Costs for " + line.getProduct().getName();
								log.log(Level.WARNING, p_Error);
								return null;
							}
						}
						else	//	ignore service
							continue;
					}
				}
				else
				{
					//temp to avoid NPE
					costs = BigDecimal.ZERO;
				}
				
				//  CoGS            DR
				dr = fact.createLine(line,
					line.getAccount(ProductCost.ACCTTYPE_P_Cogs, as),
					as.getC_Currency_ID(), costs, null);
				if (dr == null)
				{
					p_Error = "FactLine DR not created: " + line;
					log.log(Level.WARNING, p_Error);
					return null;
				}
				dr.setM_Locator_ID(line.getM_Locator_ID());
				dr.setLocationFromLocator(line.getM_Locator_ID(), true);    //  from Loc
				dr.setLocationFromBPartner(getC_BPartner_Location_ID(), false);  //  to Loc
				dr.setAD_Org_ID(line.getOrder_Org_ID());		//	Revenue X-Org
				dr.setQty(line.getQty().negate());
				
				if (isReversal(line))
				{
					//	Set AmtAcctDr from Original Shipment/Receipt
					if (!dr.updateReverseLine (MInOut.Table_ID,
							m_Reversal_ID, line.getReversalLine_ID(),Env.ONE))
					{
						if (! product.isStocked())	{ //	ignore service
							fact.remove(dr);
							continue;
						}
						p_Error = "Original Shipment/Receipt not posted yet";
						return null;
					}
				}

				//  Inventory               CR
				cr = fact.createLine(line,
					line.getAccount(ProductCost.ACCTTYPE_P_Asset, as),
					as.getC_Currency_ID(), null, costs);
				if (cr == null)
				{
					p_Error = "FactLine CR not created: " + line;
					log.log(Level.WARNING, p_Error);
					return null;
				}
				cr.setM_Locator_ID(line.getM_Locator_ID());
				cr.setLocationFromLocator(line.getM_Locator_ID(), true);    // from Loc
				cr.setLocationFromBPartner(getC_BPartner_Location_ID(), false);  // to Loc
				
				if (isReversal(line))
				{
					//	Set AmtAcctCr from Original Shipment/Receipt
					if (!cr.updateReverseLine (MInOut.Table_ID,
							m_Reversal_ID, line.getReversalLine_ID(),Env.ONE))
					{
						p_Error = "Original Shipment/Receipt not posted yet";
						return null;
					}
					costs = cr.getAcctBalance(); //get original cost
				}
				if (MAcctSchema.COSTINGLEVEL_BatchLot.equals(product.getCostingLevel(as)) ) 
				{	
					if (line.getM_AttributeSetInstance_ID() == 0 ) 
					{
						MInOutLine ioLine = (MInOutLine) line.getPO();
						MInOutLineMA mas[] = MInOutLineMA.get(getCtx(), ioLine.get_ID(), getTrxName());
						if (mas != null && mas.length > 0 )
						{
							for (int j = 0; j < mas.length; j++)
							{
								MInOutLineMA ma = mas[j];
								if (!MCostDetail.createShipment(as, line.getAD_Org_ID(),
										line.getM_Product_ID(), ma.getM_AttributeSetInstance_ID(),
										line.get_ID(), 0,
										costs, ma.getMovementQty().negate(),
										line.getDescription(), true, getTrxName()))
								{
									p_Error = "Failed to create cost detail record";
									return null;
								}							
							}						
						}
					} 
					else
					{
						//
						if (line.getM_Product_ID() != 0)
						{
							if (!MCostDetail.createShipment(as, line.getAD_Org_ID(),
								line.getM_Product_ID(), line.getM_AttributeSetInstance_ID(),
								line.get_ID(), 0,
								costs, line.getQty(),
								line.getDescription(), true, getTrxName()))
							{
								p_Error = "Failed to create cost detail record";
								return null;
							}
						}
					}
				} 
				else
				{
					//
					if (line.getM_Product_ID() != 0)
					{
						if (!MCostDetail.createShipment(as, line.getAD_Org_ID(),
							line.getM_Product_ID(), line.getM_AttributeSetInstance_ID(),
							line.get_ID(), 0,
							costs, line.getQty(),
							line.getDescription(), true, getTrxName()))
						{
							p_Error = "Failed to create cost detail record";
							return null;
						}
					}
				}
			}	//	for all lines

			/** Commitment release										****/
			if (as.isAccrual() && as.isCreateSOCommitment())
			{
				for (int i = 0; i < p_lines.length; i++)
				{
					DocLine line = p_lines[i];
					Fact factcomm = getCommitmentSalesRelease(as, this,
						line.getQty(), line.get_ID(), Env.ONE);
					if (factcomm != null)
						facts.add(factcomm);
				}
			}	//	Commitment

		}	//	Shipment
        //	  *** Sales - Return
		else if ( getDocumentType().equals(DOCTYPE_MatReceipt) && isSOTrx() )
		{
			for (int i = 0; i < p_lines.length; i++)
			{
				DocLine line = p_lines[i];
				MProduct product = line.getProduct();
				BigDecimal costs = null;
				if (!isReversal(line)) 
				{
					if (MAcctSchema.COSTINGLEVEL_BatchLot.equals(product.getCostingLevel(as)) ) 
					{	
						if (line.getM_AttributeSetInstance_ID() == 0 ) 
						{
							MInOutLine ioLine = (MInOutLine) line.getPO();
							MInOutLineMA mas[] = MInOutLineMA.get(getCtx(), ioLine.get_ID(), getTrxName());
							costs = BigDecimal.ZERO;
							if (mas != null && mas.length > 0 )
							{
								for (int j = 0; j < mas.length; j++)
								{
									MInOutLineMA ma = mas[j];
									BigDecimal QtyMA = ma.getMovementQty();
									ProductCost pc = line.getProductCost();
									pc.setQty(QtyMA);
									pc.setM_M_AttributeSetInstance_ID(ma.getM_AttributeSetInstance_ID());
									BigDecimal maCosts = line.getProductCosts(as, line.getAD_Org_ID(), true, "M_InOutLine_ID=?");
								
									costs = costs.add(maCosts);
								}
							}
						} 
						else
						{
							costs = line.getProductCosts(as, line.getAD_Org_ID(), true, "M_InOutLine_ID=?");
						}
					}
					else
					{
						// MZ Goodwill
						// if Shipment CostDetail exist then get Cost from Cost Detail
						costs = line.getProductCosts(as, line.getAD_Org_ID(), true, "M_InOutLine_ID=?");
						// end MZ
					}
					if (costs == null || costs.signum() == 0)	//	zero costs OK
					{
						if (product.isStocked())
						{
							p_Error = "No Costs for " + line.getProduct().getName();
							log.log(Level.WARNING, p_Error);
							return null;
						}
						else	//	ignore service
							continue;
					}
				} 
				else
				{
					costs = BigDecimal.ZERO;
				}
				//  Inventory               DR
				dr = fact.createLine(line,
					line.getAccount(ProductCost.ACCTTYPE_P_Asset, as),
					as.getC_Currency_ID(), costs, null);
				if (dr == null)
				{
					p_Error = "FactLine DR not created: " + line;
					log.log(Level.WARNING, p_Error);
					return null;
				}
				dr.setM_Locator_ID(line.getM_Locator_ID());
				dr.setLocationFromLocator(line.getM_Locator_ID(), true);    // from Loc
				dr.setLocationFromBPartner(getC_BPartner_Location_ID(), false);  // to Loc
				if (isReversal(line))
				{
					//	Set AmtAcctDr from Original Shipment/Receipt
					if (!dr.updateReverseLine (MInOut.Table_ID,
							m_Reversal_ID, line.getReversalLine_ID(),Env.ONE))
					{
						if (! product.isStocked())	{ //	ignore service
							fact.remove(dr);
							continue;
						}
						p_Error = "Original Shipment/Receipt not posted yet";
						return null;
					}
					costs = dr.getAcctBalance(); //get original cost
				}
				//
				if (MAcctSchema.COSTINGLEVEL_BatchLot.equals(product.getCostingLevel(as)) ) 
				{	
					if (line.getM_AttributeSetInstance_ID() == 0 ) 
					{
						MInOutLine ioLine = (MInOutLine) line.getPO();
						MInOutLineMA mas[] = MInOutLineMA.get(getCtx(), ioLine.get_ID(), getTrxName());
						if (mas != null && mas.length > 0 )
						{
							for (int j = 0; j < mas.length; j++)
							{
								MInOutLineMA ma = mas[j];
								if (!MCostDetail.createShipment(as, line.getAD_Org_ID(),
										line.getM_Product_ID(), ma.getM_AttributeSetInstance_ID(),
										line.get_ID(), 0,
										costs, ma.getMovementQty(),
										line.getDescription(), true, getTrxName()))
								{
									p_Error = "Failed to create cost detail record";
									return null;
								}
							}
						}
					} else
					{
						if (line.getM_Product_ID() != 0)
						{
							if (!MCostDetail.createShipment(as, line.getAD_Org_ID(),
								line.getM_Product_ID(), line.getM_AttributeSetInstance_ID(),
								line.get_ID(), 0,
								costs, line.getQty(),
								line.getDescription(), true, getTrxName()))
							{
								p_Error = "Failed to create cost detail record";
								return null;
							}
						}
					}
				} else
				{
					//
					if (line.getM_Product_ID() != 0)
					{
						if (!MCostDetail.createShipment(as, line.getAD_Org_ID(),
							line.getM_Product_ID(), line.getM_AttributeSetInstance_ID(),
							line.get_ID(), 0,
							costs, line.getQty(),
							line.getDescription(), true, getTrxName()))
						{
							p_Error = "Failed to create cost detail record";
							return null;
						}
					}
				}

				//  CoGS            CR
				cr = fact.createLine(line,
					line.getAccount(ProductCost.ACCTTYPE_P_Cogs, as),
					as.getC_Currency_ID(), null, costs);
				if (cr == null)
				{
					p_Error = "FactLine CR not created: " + line;
					log.log(Level.WARNING, p_Error);
					return null;
				}
				cr.setM_Locator_ID(line.getM_Locator_ID());
				cr.setLocationFromLocator(line.getM_Locator_ID(), true);    //  from Loc
				cr.setLocationFromBPartner(getC_BPartner_Location_ID(), false);  //  to Loc
				cr.setAD_Org_ID(line.getOrder_Org_ID());		//	Revenue X-Org
				cr.setQty(line.getQty().negate());
				if (isReversal(line))
				{
					//	Set AmtAcctCr from Original Shipment/Receipt
					if (!cr.updateReverseLine (MInOut.Table_ID,
							m_Reversal_ID, line.getReversalLine_ID(),Env.ONE))
					{
						p_Error = "Original Shipment/Receipt not posted yet";
						return null;
					}
				}
			}	//	for all lines
		}	//	Sales Return

		//  *** Purchasing - Receipt
		else if (getDocumentType().equals(DOCTYPE_MatReceipt) && !isSOTrx())
		{
			for (int i = 0; i < p_lines.length; i++)
			{
				// Elaine 2008/06/26
				int C_Currency_ID = as.getC_Currency_ID();
				//
				DocLine line = p_lines[i];
				BigDecimal costs = null;
				MProduct product = line.getProduct();
				MOrderLine orderLine = null;
				BigDecimal landedCost = BigDecimal.ZERO;
				String costingMethod = product.getCostingMethod(as);
				if (!isReversal(line))
				{					
					int C_OrderLine_ID = line.getC_OrderLine_ID();
					if (C_OrderLine_ID > 0)
					{
						orderLine = new MOrderLine (getCtx(), C_OrderLine_ID, getTrxName());
						MOrderLandedCostAllocation[] allocations = MOrderLandedCostAllocation.getOfOrderLine(C_OrderLine_ID, getTrxName());
						for(MOrderLandedCostAllocation allocation : allocations) 
						{														
							BigDecimal totalAmt = allocation.getAmt();
							BigDecimal totalQty = allocation.getQty();
							BigDecimal amt = totalAmt.multiply(line.getQty()).divide(totalQty, RoundingMode.HALF_UP);
							landedCost = landedCost.add(amt);							
						}
					}
															
					//get costing method for product					
					if (MAcctSchema.COSTINGMETHOD_AveragePO.equals(costingMethod) ||
						MAcctSchema.COSTINGMETHOD_AverageInvoice.equals(costingMethod) ||
						MAcctSchema.COSTINGMETHOD_LastPOPrice.equals(costingMethod)  ||
						( MAcctSchema.COSTINGMETHOD_StandardCosting.equals(costingMethod) &&  MAcctSchema.COSTINGLEVEL_BatchLot.equals(product.getCostingLevel(as))))
					{
						// Low - check if c_orderline_id is valid
						if (orderLine != null)
						{
						    // Elaine 2008/06/26
						    C_Currency_ID = orderLine.getC_Currency_ID();
						    //
						    costs = orderLine.getPriceCost();
						    if (costs == null || costs.signum() == 0)
						    {
						    	costs = orderLine.getPriceActual();
								//	Goodwill: Correct included Tax
						    	int C_Tax_ID = orderLine.getC_Tax_ID();
								if (orderLine.isTaxIncluded() && C_Tax_ID != 0)
								{
									MTax tax = MTax.get(getCtx(), C_Tax_ID);
									if (!tax.isZeroTax())
									{
										int stdPrecision = MCurrency.getStdPrecision(getCtx(), C_Currency_ID);
										BigDecimal costTax = tax.calculateTax(costs, true, stdPrecision);
										if (log.isLoggable(Level.FINE)) log.fine("Costs=" + costs + " - Tax=" + costTax);
										costs = costs.subtract(costTax);
									}
								}	//	correct included Tax
						    }
						    costs = costs.multiply(line.getQty());
	                    }
	                    else
	                    {	                    	
	                    	costs = line.getProductCosts(as, line.getAD_Org_ID(), false);	//	current costs
	                    }
	                    //
					}
					else
					{
						if (MSysConfig.getValue("ISY_StandardCostingUseCurrentCost").equals("N") || product.getProductType().equals(MProduct.PRODUCTTYPE_Item) || product.getProductType().equals(MProduct.PRODUCTTYPE_ExpenseType)) {
							costs = line.getProductCosts(as, line.getAD_Org_ID(), true, "M_InOutLine_ID=?");
						} else {
							costs = line.getProductCosts(as, line.getAD_Org_ID(), false);	//	current costs
						}
					}					
					if (costs == null || costs.signum() == 0)
					{
						//ok if purchase price is actually zero 
						if (orderLine != null && orderLine.getPriceActual().signum() == 0)
                    	{
							costs = BigDecimal.ZERO;
                    	}
						else
						{
							p_Error = "Resubmit - No Costs for " + product.getName();
							log.log(Level.WARNING, p_Error);
							return null;
						}
					}										
				} 
				else
				{
					costs = BigDecimal.ZERO;
				}
				
				//  Inventory/Asset			DR
				MAccount assets = line.getAccount(ProductCost.ACCTTYPE_P_Asset, as);
				if (product.isService())
				{
					//if the line is a Outside Processing then DR WIP
					if(line.getPP_Cost_Collector_ID() > 0)
						assets = line.getAccount(ProductCost.ACCTTYPE_P_WorkInProcess, as);
					else
						assets = line.getAccount(ProductCost.ACCTTYPE_P_Expense, as);

				}

				BigDecimal drAsset = costs;
				if (landedCost.signum() != 0 && (MAcctSchema.COSTINGMETHOD_AverageInvoice.equals(costingMethod)
					|| MAcctSchema.COSTINGMETHOD_AveragePO.equals(costingMethod)))
				{
					drAsset = drAsset.add(landedCost);
				}
				dr = fact.createLine(line, assets,
					C_Currency_ID, drAsset, null);
				//
				if (dr == null)
				{
					p_Error = "DR not created: " + line;
					log.log(Level.WARNING, p_Error);
					return null;
				}
				dr.setM_Locator_ID(line.getM_Locator_ID());
				dr.setLocationFromBPartner(getC_BPartner_Location_ID(), true);   // from Loc
				dr.setLocationFromLocator(line.getM_Locator_ID(), false);   // to Loc
				if (isReversal(line))
				{
					//	Set AmtAcctDr from Original Shipment/Receipt
					if (!dr.updateReverseLine (MInOut.Table_ID,
							m_Reversal_ID, line.getReversalLine_ID(),Env.ONE))
					{
						if (! product.isStocked())	{ //	ignore service
							fact.remove(dr);
							continue;
						}
						p_Error = "Original Receipt not posted yet";
						return null;
					}
				}

				//  NotInvoicedReceipt				CR
				cr = fact.createLine(line,
					getAccount(Doc.ACCTTYPE_NotInvoicedReceipts, as),
					C_Currency_ID, null, costs);
				//
				if (cr == null)
				{
					p_Error = "CR not created: " + line;
					log.log(Level.WARNING, p_Error);
					return null;
				}
				cr.setM_Locator_ID(line.getM_Locator_ID());
				cr.setLocationFromBPartner(getC_BPartner_Location_ID(), true);   //  from Loc
				cr.setLocationFromLocator(line.getM_Locator_ID(), false);   //  to Loc
				cr.setQty(line.getQty().negate());
				if (isReversal(line))
				{
					//	Set AmtAcctCr from Original Shipment/Receipt
					if (!cr.updateReverseLine (MInOut.Table_ID,
							m_Reversal_ID, line.getReversalLine_ID(),Env.ONE))
					{
						p_Error = "Original Receipt not posted yet";
						return null;
					}
				}
				if (!fact.isAcctBalanced())
				{
					if (isReversal(line))
					{
						dr = fact.createLine(line,
								line.getAccount(ProductCost.ACCTTYPE_P_LandedCostClearing, as),
								C_Currency_ID, Env.ONE, (BigDecimal)null);
						if (!dr.updateReverseLine (MInOut.Table_ID,
								m_Reversal_ID, line.getReversalLine_ID(),Env.ONE))
						{
							p_Error = "Original Receipt not posted yet";
							return null;
						}
					}
					else if (landedCost.signum() != 0)
					{
						cr = fact.createLine(line,
								line.getAccount(ProductCost.ACCTTYPE_P_LandedCostClearing, as),
								C_Currency_ID, null, landedCost);
						//
						if (cr == null)
						{
							p_Error = "CR not created: " + line;
							log.log(Level.WARNING, p_Error);
							return null;
						}
						cr.setM_Locator_ID(line.getM_Locator_ID());
						cr.setLocationFromBPartner(getC_BPartner_Location_ID(), true);   //  from Loc
						cr.setLocationFromLocator(line.getM_Locator_ID(), false);   //  to Loc
						cr.setQty(line.getQty().negate());
					}
				}
			}
		}	//	Receipt
         //	  *** Purchasing - return
		else if (getDocumentType().equals(DOCTYPE_MatShipment) && !isSOTrx())
		{
			for (int i = 0; i < p_lines.length; i++)
			{
				// Elaine 2008/06/26
				int C_Currency_ID = as.getC_Currency_ID();
				//
				DocLine line = p_lines[i];
				BigDecimal costs = null;
				MProduct product = line.getProduct();
				if (!isReversal(line))
				{
//					MInOutLine ioLine = (MInOutLine) line.getPO();
//					I_M_RMALine rmaLine = ioLine.getM_RMALine();
//					costs = rmaLine != null ? rmaLine.getAmt() : BigDecimal.ZERO;
//					I_M_InOutLine originalInOutLine = rmaLine != null ? rmaLine.getM_InOutLine() : null;
//					if (originalInOutLine != null && originalInOutLine.getC_OrderLine_ID() > 0)
//					{
//						MOrderLine originalOrderLine = (MOrderLine) originalInOutLine.getC_OrderLine();
//						//	Goodwill: Correct included Tax
//				    	int C_Tax_ID = originalOrderLine.getC_Tax_ID();
//				    	if (originalOrderLine.isTaxIncluded() && C_Tax_ID != 0)
//						{
//							MTax tax = MTax.get(getCtx(), C_Tax_ID);
//							if (!tax.isZeroTax())
//							{
//								int stdPrecision = MCurrency.getStdPrecision(getCtx(), originalOrderLine.getC_Currency_ID());
//								BigDecimal costTax = tax.calculateTax(costs, true, stdPrecision);
//								if (log.isLoggable(Level.FINE)) log.fine("Costs=" + costs + " - Tax=" + costTax);
//								costs = costs.subtract(costTax);
//							}
//						}	//	correct included Tax
//				    	costs = costs.multiply(line.getQty());
//				    	costs = costs.negate();
//					}
//					else
//					{
					// ISY-297 always use product's current cost
//					costs = line.getProductCosts(as, line.getAD_Org_ID(), false);

					if (MSysConfig.getValue("ISY_StandardCostingUseCurrentCost").equals("N") || product.getProductType().equals(MProduct.PRODUCTTYPE_Item) || product.getProductType().equals(MProduct.PRODUCTTYPE_ExpenseType)) {
						costs = line.getProductCosts(as, line.getAD_Org_ID(), true, "M_InOutLine_ID=?");
						if (costs.compareTo(Env.ZERO) < 0)
							costs = costs.negate();
					} else {
						costs = line.getProductCosts(as, line.getAD_Org_ID(), false);	//	current costs
					}
				
//					}
					if (costs == null || costs.signum() == 0)
					{
						p_Error = "Resubmit - No Costs for " + product.getName();
						log.log(Level.WARNING, p_Error);
						return null;
					}
				}
				else
				{
					//update below
					costs = Env.ONE;
				}
				//  NotInvoicedReceipt				DR
				// Elaine 2008/06/26
				/*dr = fact.createLine(line,
					getAccount(Doc.ACCTTYPE_NotInvoicedReceipts, as),
					as.getC_Currency_ID(), costs , null);*/
				dr = fact.createLine(line,
					getAccount(Doc.ACCTTYPE_NotInvoicedReceipts, as),
					C_Currency_ID, costs , null);
				//
				if (dr == null)
				{
					p_Error = "CR not created: " + line;
					log.log(Level.WARNING, p_Error);
					return null;
				}
				dr.setM_Locator_ID(line.getM_Locator_ID());
				dr.setLocationFromBPartner(getC_BPartner_Location_ID(), true);   //  from Loc
				dr.setLocationFromLocator(line.getM_Locator_ID(), false);   //  to Loc
				dr.setQty(line.getQty().negate());
				if (isReversal(line))
				{
					//	Set AmtAcctDr from Original Shipment/Receipt
					if (!dr.updateReverseLine (MInOut.Table_ID,
							m_Reversal_ID, line.getReversalLine_ID(),Env.ONE))
					{
						if (! product.isStocked())	{ //	ignore service
							fact.remove(dr);
							continue;
						}
						p_Error = "Original Receipt not posted yet";
						return null;
					}
				}

				//  Inventory/Asset			CR
				MAccount assets = line.getAccount(ProductCost.ACCTTYPE_P_Asset, as);
				if (product.isService())
					assets = line.getAccount(ProductCost.ACCTTYPE_P_Expense, as);
				// Elaine 2008/06/26
				/*cr = fact.createLine(line, assets,
					as.getC_Currency_ID(), null, costs);*/
				cr = fact.createLine(line, assets,
					C_Currency_ID, null, costs);
				//
				if (cr == null)
				{
					p_Error = "DR not created: " + line;
					log.log(Level.WARNING, p_Error);
					return null;
				}
				cr.setM_Locator_ID(line.getM_Locator_ID());
				cr.setLocationFromBPartner(getC_BPartner_Location_ID(), true);   // from Loc
				cr.setLocationFromLocator(line.getM_Locator_ID(), false);   // to Loc
				if (isReversal(line))
				{
					//	Set AmtAcctCr from Original Shipment/Receipt
					if (!cr.updateReverseLine (MInOut.Table_ID,
							m_Reversal_ID, line.getReversalLine_ID(),Env.ONE))
					{
						p_Error = "Original Receipt not posted yet";
						return null;
					}
				}
				
				String costingError = createVendorRMACostDetail(as, line, costs);
				if (!Util.isEmpty(costingError))
				{
					p_Error = costingError;
					return null;
				}
			}
						
		}	//	Purchasing Return
		else
		{
			p_Error = "DocumentType unknown: " + getDocumentType();
			log.log(Level.SEVERE, p_Error);
			return null;
		}
		//
		facts.add(fact);
		return facts;
	}   //  createFact

	private boolean isReversal(DocLine line) {
		return m_Reversal_ID !=0 && line.getReversalLine_ID() != 0;
	}

	private String createVendorRMACostDetail(MAcctSchema as, DocLine line, BigDecimal costs)
	{		
		BigDecimal tQty = line.getQty();
		BigDecimal tAmt = costs;
		if (tAmt.signum() != tQty.signum())
		{
			tAmt = tAmt.negate();
		}
		MProduct product = line.getProduct();
		if (MAcctSchema.COSTINGLEVEL_BatchLot.equals(product.getCostingLevel(as)) ) 
		{	
			if (line.getM_AttributeSetInstance_ID() == 0 ) 
			{								
				MInOutLineMA mas[] = MInOutLineMA.get(getCtx(), line.get_ID(), getTrxName());
				if (mas != null && mas.length > 0 )
				{
					for (int j = 0; j < mas.length; j++)
					{
						MInOutLineMA ma = mas[j];
						if (!MCostDetail.createShipment(as, line.getAD_Org_ID(), line.getM_Product_ID(), 
								ma.getM_AttributeSetInstance_ID(), line.get_ID(), 0, tAmt, ma.getMovementQty().negate(), 
								line.getDescription(), false, getTrxName()))
							return "SaveError";
					}
				}
			} 
			else
			{
				if (!MCostDetail.createShipment(as, line.getAD_Org_ID(), line.getM_Product_ID(), 
						line.getM_AttributeSetInstance_ID(), line.get_ID(), 0, tAmt, tQty, 
						line.getDescription(), false, getTrxName()))
					return "SaveError";
			}
		} 
		else
		{
			if (!MCostDetail.createShipment(as, line.getAD_Org_ID(), line.getM_Product_ID(), 
					line.getM_AttributeSetInstance_ID(), line.get_ID(), 0, tAmt, tQty, 
					line.getDescription(), false, getTrxName()))
				return "SaveError";
		}
		return "";
	}
	
	/**
	 * 	Get Commitment Sales Release.
	 * 	Called from InOut
	 *	@param as accounting schema
	 *	@param doc doc
	 *	@param Qty qty invoiced/matched
	 *	@param C_OrderLine_ID line
	 *	@param multiplier 1 for accrual
	 *	@return Fact
	 */
	private Fact getCommitmentSalesRelease(MAcctSchema as, DocInOut_ISY doc,
		BigDecimal Qty, int M_InOutLine_ID, BigDecimal multiplier)
	{
		Fact fact = new Fact(doc, as, Fact.POST_Commitment);
		DocLine[] commitments = getCommitmentsSales(doc, Qty,
				M_InOutLine_ID);

		BigDecimal total = Env.ZERO;
		@SuppressWarnings("unused")
		FactLine fl = null;
		int C_Currency_ID = -1;
		MInOutLine iol = new MInOutLine(Env.getCtx(), M_InOutLine_ID, null);
		
		for (int i = 0; i < commitments.length; i++)
		{
			DocLine line = commitments[i];
			if (C_Currency_ID == -1)
				C_Currency_ID = line.getC_Currency_ID();
			else if (C_Currency_ID != line.getC_Currency_ID())
			{
				doc.p_Error = "Different Currencies of Order Lines";
				s_log.log(Level.SEVERE, doc.p_Error);
				return null;
			}
			BigDecimal cost = line.getAmtSource().multiply(multiplier);
			total = total.add(cost);

			//	Account
			MAccount revenue = line.getAccount(ProductCost.ACCTTYPE_P_Revenue, as);
			//	ISY-281
			if(iol.getReversalLine_ID()>0)
				fl = fact.createLine (line, revenue,
					C_Currency_ID, null, cost);
			else 
				fl = fact.createLine (line, revenue,
						C_Currency_ID, cost, null);
		}
		//	Offset
		MAccount offset = doc.getAccount(ACCTTYPE_CommitmentOffsetSales, as);
		if (offset == null)
		{
			doc.p_Error = "@NotFound@ @CommitmentOffsetSales_Acct@";
			s_log.log(Level.SEVERE, doc.p_Error);
			return null;
		}
		if(iol.getReversalLine_ID()>0)
			fact.createLine (null, offset,
				C_Currency_ID, total, null);
		else
			fact.createLine (null, offset,
					C_Currency_ID, null, total);
		return fact;
	}	//	getCommitmentSalesRelease
	
	/**
	 * 	Get Commitments Sales
	 * 	@param doc document
	 * 	@param maxQty Qty invoiced/matched
	 * 	@param C_OrderLine_ID invoice line
	 *	@return commitments (order lines)
	 */
	private DocLine[] getCommitmentsSales(Doc doc, BigDecimal maxQty, int M_InOutLine_ID)
	{
		int precision = -1;
		//
		ArrayList<DocLine> list = new ArrayList<DocLine>();
		StringBuilder sql = new StringBuilder("SELECT * FROM C_OrderLine ol ")
			.append("WHERE EXISTS ")
				.append("(SELECT * FROM M_InOutLine il ")
				.append("WHERE il.C_OrderLine_ID=ol.C_OrderLine_ID")
				.append(" AND il.M_InOutLine_ID=?)");
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql.toString(), null);
			pstmt.setInt (1, M_InOutLine_ID);
			rs = pstmt.executeQuery ();
			while (rs.next ())
			{
				if (maxQty.signum() == 0)
					continue;
				MOrderLine line = new MOrderLine (doc.getCtx(), rs, null);
				DocLine docLine = new DocLine (line, doc);
				//	Currency
				if (precision == -1)
				{
					doc.setC_Currency_ID(docLine.getC_Currency_ID());
					precision = MCurrency.getStdPrecision(doc.getCtx(), docLine.getC_Currency_ID());
				}
				//	Qty
				BigDecimal Qty = line.getQtyOrdered().max(maxQty);
				docLine.setQty(Qty, false);
				//
				BigDecimal PriceActual = line.getPriceActual();
				BigDecimal PriceCost = line.getPriceCost();
				BigDecimal LineNetAmt = null;
				if (PriceCost != null && PriceCost.signum() != 0)
					LineNetAmt = Qty.multiply(PriceCost);
				else if (Qty.equals(maxQty))
					LineNetAmt = line.getLineNetAmt();
				else
					LineNetAmt = Qty.multiply(PriceActual);
				maxQty = maxQty.subtract(Qty);

				docLine.setAmount (LineNetAmt);	//	DR
				BigDecimal PriceList = line.getPriceList();
				int C_Tax_ID = docLine.getC_Tax_ID();
				//	Correct included Tax
				if (C_Tax_ID != 0 && line.getParent().isTaxIncluded())
				{
					MTax tax = MTax.get(doc.getCtx(), C_Tax_ID);
					if (!tax.isZeroTax())
					{
						BigDecimal LineNetAmtTax = tax.calculateTax(LineNetAmt, true, precision);
						if (s_log.isLoggable(Level.FINE)) s_log.fine("LineNetAmt=" + LineNetAmt + " - Tax=" + LineNetAmtTax);
						LineNetAmt = LineNetAmt.subtract(LineNetAmtTax);
						BigDecimal PriceListTax = tax.calculateTax(PriceList, true, precision);
						PriceList = PriceList.subtract(PriceListTax);
					}
				}	//	correct included Tax

				docLine.setAmount (LineNetAmt, PriceList, Qty);
				list.add(docLine);
			}
		}
		catch (Exception e)
		{
			s_log.log (Level.SEVERE, sql.toString(), e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}

		//	Return Array
		DocLine[] dl = new DocLine[list.size()];
		list.toArray(dl);
		return dl;
	}	//	getCommitmentsSales
}   //  Doc_InOut
