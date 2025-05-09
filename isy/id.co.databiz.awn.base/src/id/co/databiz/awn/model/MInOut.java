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
package id.co.databiz.awn.model;

import java.io.File;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.NegativeInventoryDisallowedException;
import org.adempiere.exceptions.PeriodClosedException;
import org.compiere.model.I_C_Order;
import org.compiere.model.I_M_InOutConfirm;
import org.compiere.model.I_M_InOutLine;
import org.compiere.model.MAsset;
import org.compiere.model.MBPartner;
import org.compiere.model.MBPartnerLocation;
import org.compiere.model.MClient;
import org.compiere.model.MDocType;
import org.compiere.model.MDocTypeCounter;
import org.compiere.model.MInOutConfirm;
import org.compiere.model.MInOutLine;
import org.compiere.model.MInOutLineMA;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MMatchInv;
import org.compiere.model.MMatchPO;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MOrg;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MPeriod;
import org.compiere.model.MProduct;
import org.compiere.model.MRMA;
import org.compiere.model.MRMALine;
import org.compiere.model.MRefList;
import org.compiere.model.MStorageOnHand;
import org.compiere.model.MStorageReservation;
import org.compiere.model.MSysConfig;
import org.compiere.model.MTransaction;
import org.compiere.model.MUser;
import org.compiere.model.MWarehouse;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.print.MPrintFormat;
import org.compiere.print.ReportEngine;
import org.compiere.process.DocAction;
import org.compiere.process.DocumentEngine;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ServerProcessCtl;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;

/**
 *  Shipment Model
 *
 *  @author Jorg Janke
 *  @version $Id: MInOut.java,v 1.4 2006/07/30 00:51:03 jjanke Exp $
 *
 *  Modifications: Added the RMA functionality (Ashley Ramdass)
 *  @author Karsten Thiemann, Schaeffer AG
 * 			<li>Bug [ 1759431 ] Problems with VCreateFrom
 *  @author victor.perez@e-evolution.com, e-Evolution http://www.e-evolution.com
 * 			<li>FR [ 1948157  ]  Is necessary the reference for document reverse
 * 			<li> FR [ 2520591 ] Support multiples calendar for Org
 *			@see http://sourceforge.net/tracker2/?func=detail&atid=879335&aid=2520591&group_id=176962
 *  @author Armen Rizal, Goodwill Consulting
 * 			<li>BF [ 1745154 ] Cost in Reversing Material Related Docs
 *  @see http://sourceforge.net/tracker/?func=detail&atid=879335&aid=1948157&group_id=176962
 *  @author Teo Sarca, teo.sarca@gmail.com
 * 			<li>BF [ 2993853 ] Voiding/Reversing Receipt should void confirmations
 * 				https://sourceforge.net/tracker/?func=detail&atid=879332&aid=2993853&group_id=176962
 */
public class MInOut extends org.compiere.model.MInOut implements DocAction
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1226522383231204912L;

	/**
	 * 	Create Shipment From Order
	 *	@param order order
	 *	@param movementDate optional movement date
	 *	@param forceDelivery ignore order delivery rule
	 *	@param allAttributeInstances if true, all attribute set instances
	 *	@param minGuaranteeDate optional minimum guarantee date if all attribute instances
	 *	@param complete complete document (Process if false, Complete if true)
	 *	@param trxName transaction
	 *	@return Shipment or null
	 */
	public static MInOut createFrom (MOrder order, Timestamp movementDate,
			boolean forceDelivery, boolean allAttributeInstances, Timestamp minGuaranteeDate,
			boolean complete, String trxName)
	{		
		if (order == null)
			throw new IllegalArgumentException("No Order");
		//
		if (!forceDelivery && DELIVERYRULE_CompleteLine.equals(order.getDeliveryRule()))
		{
			return null;
		}

		//	Create Header
		MInOut retValue = new MInOut (order, 0, movementDate);
		retValue.setDocAction(complete ? DOCACTION_Complete : DOCACTION_Prepare);

		//	Check if we can create the lines
		MOrderLine[] oLines = order.getLines(true, "M_Product_ID");
		for (int i = 0; i < oLines.length; i++)
		{
			// Calculate how much is left to deliver (ordered - delivered)
			BigDecimal qty = oLines[i].getQtyOrdered().subtract(oLines[i].getQtyDelivered());
			//	Nothing to deliver
			if (qty.signum() == 0)
				continue;
			//	Stock Info
			MStorageOnHand[] storages = null;
			MProduct product = oLines[i].getProduct();
			if (product != null && product.get_ID() != 0 && product.isStocked())
			{
				String MMPolicy = product.getMMPolicy();
				storages = MStorageOnHand.getWarehouse (order.getCtx(), order.getM_Warehouse_ID(),
					oLines[i].getM_Product_ID(), oLines[i].getM_AttributeSetInstance_ID(),
					minGuaranteeDate, MClient.MMPOLICY_FiFo.equals(MMPolicy), true, 0, trxName);
			} else {
				continue;
			}

			if (!forceDelivery)
			{
				BigDecimal maxQty = Env.ZERO;
				for (int ll = 0; ll < storages.length; ll++)
					maxQty = maxQty.add(storages[ll].getQtyOnHand());
				if (DELIVERYRULE_Availability.equals(order.getDeliveryRule()))
				{
					if (maxQty.compareTo(qty) < 0)
						qty = maxQty;
				}
				else if (DELIVERYRULE_CompleteLine.equals(order.getDeliveryRule()))
				{
					if (maxQty.compareTo(qty) < 0)
						continue;
				}
			}
			//	Create Line
			if (retValue.get_ID() == 0)	//	not saved yet
				retValue.saveEx(trxName);
			//	Create a line until qty is reached
			for (int ll = 0; ll < storages.length; ll++)
			{
				BigDecimal lineQty = storages[ll].getQtyOnHand();
				if (lineQty.compareTo(qty) > 0)
					lineQty = qty;
				MInOutLine line = new MInOutLine (retValue);
				line.setOrderLine(oLines[i], storages[ll].getM_Locator_ID(),
					order.isSOTrx() ? lineQty : Env.ZERO);
				line.setQty(lineQty);	//	Correct UOM for QtyEntered
				if (oLines[i].getQtyEntered().compareTo(oLines[i].getQtyOrdered()) != 0)
					line.setQtyEntered(lineQty
						.multiply(oLines[i].getQtyEntered())
						.divide(oLines[i].getQtyOrdered(), 12, BigDecimal.ROUND_HALF_UP));
				line.setC_Project_ID(oLines[i].getC_Project_ID());
				line.saveEx(trxName);
				//	Delivered everything ?
				qty = qty.subtract(lineQty);
			//	storage[ll].changeQtyOnHand(lineQty, !order.isSOTrx());	// Credit Memo not considered
			//	storage[ll].saveEx(get_TrxName());
				if (qty.signum() == 0)
					break;
			}
		}	//	for all order lines

		//	No Lines saved
		if (retValue.get_ID() == 0)
			return null;

		return retValue;
		
	}

	/**
	 * 	Create new Shipment by copying
	 * 	@param from shipment
	 * 	@param dateDoc date of the document date
	 * 	@param C_DocType_ID doc type
	 * 	@param isSOTrx sales order
	 * 	@param counter create counter links
	 * 	@param trxName trx
	 * 	@param setOrder set the order link
	 *	@return Shipment
	 */
	public static MInOut copyFrom (MInOut from, Timestamp dateDoc, Timestamp dateAcct,
		int C_DocType_ID, boolean isSOTrx, boolean counter, String trxName, boolean setOrder)
	{
		MInOut to = new MInOut (from.getCtx(), 0, null);
		to.set_TrxName(trxName);
		copyValues(from, to, from.getAD_Client_ID(), from.getAD_Org_ID());
		to.set_ValueNoCheck ("M_InOut_ID", I_ZERO);
		to.set_ValueNoCheck ("DocumentNo", null);
		//
		to.setDocStatus (DOCSTATUS_Drafted);		//	Draft
		to.setDocAction(DOCACTION_Complete);
		//
		to.setC_DocType_ID (C_DocType_ID);
		to.setIsSOTrx(isSOTrx);
		if (counter)
		{
			MDocType docType = MDocType.get(from.getCtx(), C_DocType_ID);
			if (MDocType.DOCBASETYPE_MaterialDelivery.equals(docType.getDocBaseType()))
			{
				to.setMovementType (isSOTrx ? MOVEMENTTYPE_CustomerShipment : MOVEMENTTYPE_VendorReturns);
			}
			else if (MDocType.DOCBASETYPE_MaterialReceipt.equals(docType.getDocBaseType()))
			{
				to.setMovementType (isSOTrx ? MOVEMENTTYPE_CustomerReturns : MOVEMENTTYPE_VendorReceipts);
			}
		}

		//
		to.setDateOrdered (dateDoc);
		to.setDateAcct (dateAcct);
		to.setMovementDate(dateDoc);
		to.setDatePrinted(null);
		to.setIsPrinted (false);
		to.setDateReceived(null);
		to.setNoPackages(0);
		to.setShipDate(null);
		to.setPickDate(null);
		to.setIsInTransit(false);
		//
		to.setIsApproved (false);
		to.setC_Invoice_ID(0);
		to.setTrackingNo(null);
		to.setIsInDispute(false);
		//
		to.setPosted (false);
		to.setProcessed (false);
		//[ 1633721 ] Reverse Documents- Processing=Y
		to.setProcessing(false);
		to.setC_Order_ID(0);	//	Overwritten by setOrder
		to.setM_RMA_ID(0);      //  Overwritten by setOrder
		if (counter)
		{
			to.setC_Order_ID(0);
			to.setRef_InOut_ID(from.getM_InOut_ID());
			//	Try to find Order/Invoice link
			if (from.getC_Order_ID() != 0)
			{
				MOrder peer = new MOrder (from.getCtx(), from.getC_Order_ID(), from.get_TrxName());
				if (peer.getRef_Order_ID() != 0)
					to.setC_Order_ID(peer.getRef_Order_ID());
			}
			if (from.getC_Invoice_ID() != 0)
			{
				MInvoice peer = new MInvoice (from.getCtx(), from.getC_Invoice_ID(), from.get_TrxName());
				if (peer.getRef_Invoice_ID() != 0)
					to.setC_Invoice_ID(peer.getRef_Invoice_ID());
			}
			//find RMA link
			if (from.getM_RMA_ID() != 0)
			{
				MRMA peer = new MRMA (from.getCtx(), from.getM_RMA_ID(), from.get_TrxName());
				if (peer.getRef_RMA_ID() > 0)
					to.setM_RMA_ID(peer.getRef_RMA_ID());
			}
		}
		else
		{
			to.setRef_InOut_ID(0);
			if (setOrder)
			{
				to.setC_Order_ID(from.getC_Order_ID());
				to.setM_RMA_ID(from.getM_RMA_ID()); // Copy also RMA
			}
		}
		//
		if (!to.save(trxName))
			throw new IllegalStateException("Could not create Shipment");
		if (counter)
			from.setRef_InOut_ID(to.getM_InOut_ID());

		if (to.copyLinesFrom(from, counter, setOrder) <= 0)
			throw new IllegalStateException("Could not create Shipment Lines");

		return to;
	}	//	copyFrom

	/**
	 *  @deprecated
	 * 	Create new Shipment by copying
	 * 	@param from shipment
	 * 	@param dateDoc date of the document date
	 * 	@param C_DocType_ID doc type
	 * 	@param isSOTrx sales order
	 * 	@param counter create counter links
	 * 	@param trxName trx
	 * 	@param setOrder set the order link
	 *	@return Shipment
	 */
	public static MInOut copyFrom (MInOut from, Timestamp dateDoc,
		int C_DocType_ID, boolean isSOTrx, boolean counter, String trxName, boolean setOrder)
	{
		MInOut to = copyFrom ( from, dateDoc, dateDoc,
				C_DocType_ID, isSOTrx, counter,
				trxName, setOrder);
		return to;

	}

	/**************************************************************************
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param M_InOut_ID
	 *	@param trxName rx name
	 */
	public MInOut (Properties ctx, int M_InOut_ID, String trxName)
	{
		super (ctx, M_InOut_ID, trxName);
		if (M_InOut_ID == 0)
		{
		//	setDocumentNo (null);
		//	setC_BPartner_ID (0);
		//	setC_BPartner_Location_ID (0);
		//	setM_Warehouse_ID (0);
		//	setC_DocType_ID (0);
			setIsSOTrx (false);
			setMovementDate (new Timestamp (System.currentTimeMillis ()));
			setDateAcct (getMovementDate());
		//	setMovementType (MOVEMENTTYPE_CustomerShipment);
			setDeliveryRule (DELIVERYRULE_Availability);
			setDeliveryViaRule (DELIVERYVIARULE_Pickup);
			setFreightCostRule (FREIGHTCOSTRULE_FreightIncluded);
			setDocStatus (DOCSTATUS_Drafted);
			setDocAction (DOCACTION_Complete);
			setPriorityRule (PRIORITYRULE_Medium);
			setNoPackages(0);
			setIsInTransit(false);
			setIsPrinted (false);
			setSendEMail (false);
			setIsInDispute(false);
			//
			setIsApproved(false);
			super.setProcessed (false);
			setProcessing(false);
			setPosted(false);
		}
	}	//	MInOut

	/**
	 *  Load Constructor
	 *  @param ctx context
	 *  @param rs result set record
	 *	@param trxName transaction
	 */
	public MInOut (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MInOut

	/**
	 * 	Order Constructor - create header only
	 *	@param order order
	 *	@param movementDate optional movement date (default today)
	 *	@param C_DocTypeShipment_ID document type or 0
	 */
	public MInOut (MOrder order, int C_DocTypeShipment_ID, Timestamp movementDate)
	{
		this (order.getCtx(), 0, order.get_TrxName());
		setClientOrg(order);
		setC_BPartner_ID (order.getC_BPartner_ID());
		setC_BPartner_Location_ID (order.getC_BPartner_Location_ID());	//	shipment address
		setAD_User_ID(order.getAD_User_ID());
		//
		setM_Warehouse_ID (order.getM_Warehouse_ID());
		setIsSOTrx (order.isSOTrx());
		if (C_DocTypeShipment_ID == 0)
			C_DocTypeShipment_ID = DB.getSQLValue(null,
				"SELECT C_DocTypeShipment_ID FROM C_DocType WHERE C_DocType_ID=?",
				order.getC_DocType_ID());
		setC_DocType_ID (C_DocTypeShipment_ID);

		// patch suggested by Armen
		// setMovementType (order.isSOTrx() ? MOVEMENTTYPE_CustomerShipment : MOVEMENTTYPE_VendorReceipts);
		String movementTypeShipment = null;
		MDocType dtShipment = new MDocType(order.getCtx(), C_DocTypeShipment_ID, order.get_TrxName()); 
		if (dtShipment.getDocBaseType().equals(MDocType.DOCBASETYPE_MaterialDelivery)) 
			movementTypeShipment = dtShipment.isSOTrx() ? MOVEMENTTYPE_CustomerShipment : MOVEMENTTYPE_VendorReturns; 
		else if (dtShipment.getDocBaseType().equals(MDocType.DOCBASETYPE_MaterialReceipt)) 
			movementTypeShipment = dtShipment.isSOTrx() ? MOVEMENTTYPE_CustomerReturns : MOVEMENTTYPE_VendorReceipts;  
		setMovementType (movementTypeShipment); 
		
		//	Default - Today
		if (movementDate != null)
			setMovementDate (movementDate);
		setDateAcct (getMovementDate());

		//	Copy from Order
		setC_Order_ID(order.getC_Order_ID());
		setDeliveryRule (order.getDeliveryRule());
		setDeliveryViaRule (order.getDeliveryViaRule());
		setM_Shipper_ID(order.getM_Shipper_ID());
		setFreightCostRule (order.getFreightCostRule());
		setFreightAmt(order.getFreightAmt());
		setSalesRep_ID(order.getSalesRep_ID());
		//
		setC_Activity_ID(order.getC_Activity_ID());
		setC_Campaign_ID(order.getC_Campaign_ID());
		setC_Charge_ID(order.getC_Charge_ID());
		setChargeAmt(order.getChargeAmt());
		//
		setC_Project_ID(order.getC_Project_ID());
		setDateOrdered(order.getDateOrdered());
		setDescription(order.getDescription());
		setPOReference(order.getPOReference());
		setSalesRep_ID(order.getSalesRep_ID());
		setAD_OrgTrx_ID(order.getAD_OrgTrx_ID());
		setUser1_ID(order.getUser1_ID());
		setUser2_ID(order.getUser2_ID());
		setPriorityRule(order.getPriorityRule());
		// Drop shipment
		setIsDropShip(order.isDropShip());
		setDropShip_BPartner_ID(order.getDropShip_BPartner_ID());
		setDropShip_Location_ID(order.getDropShip_Location_ID());
		setDropShip_User_ID(order.getDropShip_User_ID());
	}	//	MInOut

	/**
	 * 	Invoice Constructor - create header only
	 *	@param invoice invoice
	 *	@param C_DocTypeShipment_ID document type or 0
	 *	@param movementDate optional movement date (default today)
	 *	@param M_Warehouse_ID warehouse
	 */
	public MInOut (MInvoice invoice, int C_DocTypeShipment_ID, Timestamp movementDate, int M_Warehouse_ID)
	{
		this (invoice.getCtx(), 0, invoice.get_TrxName());
		setClientOrg(invoice);
		setC_BPartner_ID (invoice.getC_BPartner_ID());
		setC_BPartner_Location_ID (invoice.getC_BPartner_Location_ID());	//	shipment address
		setAD_User_ID(invoice.getAD_User_ID());
		//
		setM_Warehouse_ID (M_Warehouse_ID);
		setIsSOTrx (invoice.isSOTrx());
		setMovementType (invoice.isSOTrx() ? MOVEMENTTYPE_CustomerShipment : MOVEMENTTYPE_VendorReceipts);
		MOrder order = null;
		if (invoice.getC_Order_ID() != 0)
			order = new MOrder (invoice.getCtx(), invoice.getC_Order_ID(), invoice.get_TrxName());
		if (C_DocTypeShipment_ID == 0 && order != null)
			C_DocTypeShipment_ID = DB.getSQLValue(null,
				"SELECT C_DocTypeShipment_ID FROM C_DocType WHERE C_DocType_ID=?",
				order.getC_DocType_ID());
		if (C_DocTypeShipment_ID != 0)
			setC_DocType_ID (C_DocTypeShipment_ID);
		else
			setC_DocType_ID();

		//	Default - Today
		if (movementDate != null)
			setMovementDate (movementDate);
		setDateAcct (getMovementDate());

		//	Copy from Invoice
		setC_Order_ID(invoice.getC_Order_ID());
		setSalesRep_ID(invoice.getSalesRep_ID());
		//
		setC_Activity_ID(invoice.getC_Activity_ID());
		setC_Campaign_ID(invoice.getC_Campaign_ID());
		setC_Charge_ID(invoice.getC_Charge_ID());
		setChargeAmt(invoice.getChargeAmt());
		//
		setC_Project_ID(invoice.getC_Project_ID());
		setDateOrdered(invoice.getDateOrdered());
		setDescription(invoice.getDescription());
		setPOReference(invoice.getPOReference());
		setAD_OrgTrx_ID(invoice.getAD_OrgTrx_ID());
		setUser1_ID(invoice.getUser1_ID());
		setUser2_ID(invoice.getUser2_ID());

		if (order != null)
		{
			setDeliveryRule (order.getDeliveryRule());
			setDeliveryViaRule (order.getDeliveryViaRule());
			setM_Shipper_ID(order.getM_Shipper_ID());
			setFreightCostRule (order.getFreightCostRule());
			setFreightAmt(order.getFreightAmt());

			// Drop Shipment
			setIsDropShip(order.isDropShip());
			setDropShip_BPartner_ID(order.getDropShip_BPartner_ID());
			setDropShip_Location_ID(order.getDropShip_Location_ID());
			setDropShip_User_ID(order.getDropShip_User_ID());
		}
	}	//	MInOut

	/**
	 * 	Copy Constructor - create header only
	 *	@param original original
	 *	@param movementDate optional movement date (default today)
	 *	@param C_DocTypeShipment_ID document type or 0
	 */
	public MInOut (MInOut original, int C_DocTypeShipment_ID, Timestamp movementDate)
	{
		this (original.getCtx(), 0, original.get_TrxName());
		setClientOrg(original);
		setC_BPartner_ID (original.getC_BPartner_ID());
		setC_BPartner_Location_ID (original.getC_BPartner_Location_ID());	//	shipment address
		setAD_User_ID(original.getAD_User_ID());
		//
		setM_Warehouse_ID (original.getM_Warehouse_ID());
		setIsSOTrx (original.isSOTrx());
		setMovementType (original.getMovementType());
		if (C_DocTypeShipment_ID == 0)
			setC_DocType_ID(original.getC_DocType_ID());
		else
			setC_DocType_ID (C_DocTypeShipment_ID);

		//	Default - Today
		if (movementDate != null)
			setMovementDate (movementDate);
		setDateAcct (getMovementDate());

		//	Copy from Order
		setC_Order_ID(original.getC_Order_ID());
		setDeliveryRule (original.getDeliveryRule());
		setDeliveryViaRule (original.getDeliveryViaRule());
		setM_Shipper_ID(original.getM_Shipper_ID());
		setFreightCostRule (original.getFreightCostRule());
		setFreightAmt(original.getFreightAmt());
		setSalesRep_ID(original.getSalesRep_ID());
		//
		setC_Activity_ID(original.getC_Activity_ID());
		setC_Campaign_ID(original.getC_Campaign_ID());
		setC_Charge_ID(original.getC_Charge_ID());
		setChargeAmt(original.getChargeAmt());
		//
		setC_Project_ID(original.getC_Project_ID());
		setDateOrdered(original.getDateOrdered());
		setDescription(original.getDescription());
		setPOReference(original.getPOReference());
		setSalesRep_ID(original.getSalesRep_ID());
		setAD_OrgTrx_ID(original.getAD_OrgTrx_ID());
		setUser1_ID(original.getUser1_ID());
		setUser2_ID(original.getUser2_ID());

		// DropShipment
		setIsDropShip(original.isDropShip());
		setDropShip_BPartner_ID(original.getDropShip_BPartner_ID());
		setDropShip_Location_ID(original.getDropShip_Location_ID());
		setDropShip_User_ID(original.getDropShip_User_ID());

	}	//	MInOut


	/**	Lines					*/
	protected MInOutLine[]	m_lines = null;
	/** Confirmations			*/
	protected MInOutConfirm[]	m_confirms = null;
	/** BPartner				*/
	protected MBPartner		m_partner = null;


	/**
	 * 	Get Document Status
	 *	@return Document Status Clear Text
	 */
	public String getDocStatusName()
	{
		return MRefList.getListName(getCtx(), 131, getDocStatus());
	}	//	getDocStatusName

	/**
	 * 	Add to Description
	 *	@param description text
	 */
	public void addDescription (String description)
	{
		String desc = getDescription();
		if (desc == null)
			setDescription(description);
		else{
			StringBuilder msgd = new StringBuilder(desc).append(" | ").append(description);
			setDescription(msgd.toString());
		}	
	}	//	addDescription

	/**
	 *	String representation
	 *	@return info
	 */
	public String toString ()
	{
		StringBuilder sb = new StringBuilder ("MInOut[")
			.append (get_ID()).append("-").append(getDocumentNo())
			.append(",DocStatus=").append(getDocStatus())
			.append ("]");
		return sb.toString ();
	}	//	toString

	/**
	 * 	Get Document Info
	 *	@return document info (untranslated)
	 */
	public String getDocumentInfo()
	{
		MDocType dt = MDocType.get(getCtx(), getC_DocType_ID());
		StringBuilder msgreturn = new StringBuilder().append(dt.getNameTrl()).append(" ").append(getDocumentNo());
		return msgreturn.toString();
	}	//	getDocumentInfo

	/**
	 * 	Create PDF
	 *	@return File or null
	 */
	public File createPDF ()
	{
		try
		{
			StringBuilder msgfile = new StringBuilder().append(get_TableName()).append(get_ID()).append("_");
			File temp = File.createTempFile(msgfile.toString(), ".pdf");
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
		ReportEngine re = ReportEngine.get (getCtx(), ReportEngine.SHIPMENT, getM_InOut_ID(), get_TrxName());
		if (re == null)
			return null;
		MPrintFormat format = re.getPrintFormat();
		// We have a Jasper Print Format
		// ==============================
		if(format.getJasperProcess_ID() > 0)	
		{
			ProcessInfo pi = new ProcessInfo ("", format.getJasperProcess_ID());
			pi.setRecord_ID ( getM_InOut_ID() );
			pi.setIsBatch(true);
			
			ServerProcessCtl.process(pi, null);
			
			return pi.getPDFReport();
		}
		// Standard Print Format (Non-Jasper)
		// ==================================
		return re.getPDF(file);
	}	//	createPDF

	/**
	 * 	Get Lines of Shipment
	 * 	@param requery refresh from db
	 * 	@return lines
	 */
	public MInOutLine[] getLines (boolean requery)
	{
		if (m_lines != null && !requery) {
			set_TrxName(m_lines, get_TrxName());
			return m_lines;
		}
		List<MInOutLine> list = new Query(getCtx(), I_M_InOutLine.Table_Name, "M_InOut_ID=?", get_TrxName())
		.setParameters(getM_InOut_ID())
		.setOrderBy(MInOutLine.COLUMNNAME_Line)
		.list();
		//
		m_lines = new MInOutLine[list.size()];
		list.toArray(m_lines);
		return m_lines;
	}	//	getMInOutLines

	/**
	 * 	Get Lines of Shipment
	 * 	@return lines
	 */
	public MInOutLine[] getLines()
	{
		return getLines(false);
	}	//	getLines


	/**
	 * 	Get Confirmations
	 * 	@param requery requery
	 *	@return array of Confirmations
	 */
	public MInOutConfirm[] getConfirmations(boolean requery)
	{
		if (m_confirms != null && !requery)
		{
			set_TrxName(m_confirms, get_TrxName());
			return m_confirms;
		}
		List<MInOutConfirm> list = new Query(getCtx(), I_M_InOutConfirm.Table_Name, "M_InOut_ID=?", get_TrxName())
		.setParameters(getM_InOut_ID())
		.list();
		m_confirms = new MInOutConfirm[list.size ()];
		list.toArray (m_confirms);
		return m_confirms;
	}	//	getConfirmations


	/**
	 * 	Copy Lines From other Shipment
	 *	@param otherShipment shipment
	 *	@param counter set counter info
	 *	@param setOrder set order link
	 *	@return number of lines copied
	 */
	public int copyLinesFrom (MInOut otherShipment, boolean counter, boolean setOrder)
	{
		if (isProcessed() || isPosted() || otherShipment == null)
			return 0;
		MInOutLine[] fromLines = otherShipment.getLines(false);
		int count = 0;
		for (int i = 0; i < fromLines.length; i++)
		{
			MInOutLine line = new MInOutLine (this);
			MInOutLine fromLine = fromLines[i];
			line.set_TrxName(get_TrxName());
			if (counter)	//	header
				PO.copyValues(fromLine, line, getAD_Client_ID(), getAD_Org_ID());
			else
				PO.copyValues(fromLine, line, fromLine.getAD_Client_ID(), fromLine.getAD_Org_ID());
			line.setM_InOut_ID(getM_InOut_ID());
			line.set_ValueNoCheck ("M_InOutLine_ID", I_ZERO);	//	new
			//	Reset
			if (!setOrder)
			{
				line.setC_OrderLine_ID(0);
				line.setM_RMALine_ID(0);  // Reset RMA Line
			}
			if (!counter)
				line.setM_AttributeSetInstance_ID(0);
		//	line.setS_ResourceAssignment_ID(0);
			line.setRef_InOutLine_ID(0);
			line.setIsInvoiced(false);
			//
			line.setConfirmedQty(Env.ZERO);
			line.setPickedQty(Env.ZERO);
			line.setScrappedQty(Env.ZERO);
			line.setTargetQty(Env.ZERO);
			//	Set Locator based on header Warehouse
			if (getM_Warehouse_ID() != otherShipment.getM_Warehouse_ID())
			{
				line.setM_Locator_ID(0);
				line.setM_Locator_ID(Env.ZERO);
			}
			//
			if (counter)
			{
				line.setRef_InOutLine_ID(fromLine.getM_InOutLine_ID());
				if (fromLine.getC_OrderLine_ID() != 0)
				{
					MOrderLine peer = new MOrderLine (getCtx(), fromLine.getC_OrderLine_ID(), get_TrxName());
					if (peer.getRef_OrderLine_ID() != 0)
						line.setC_OrderLine_ID(peer.getRef_OrderLine_ID());
				}
				//RMALine link
				if (fromLine.getM_RMALine_ID() != 0)
				{
					MRMALine peer = new MRMALine (getCtx(), fromLine.getM_RMALine_ID(), get_TrxName());
					if (peer.getRef_RMALine_ID() > 0)
						line.setM_RMALine_ID(peer.getRef_RMALine_ID());
				}
			}
			
//			line.setQtyOverReceipt(fromLine.getQtyOverReceipt());
			
			//
			line.setProcessed(false);
			if (line.save(get_TrxName()))
				count++;
			//	Cross Link
			if (counter)
			{
				fromLine.setRef_InOutLine_ID(line.getM_InOutLine_ID());
				fromLine.saveEx(get_TrxName());
			}
		}
		if (fromLines.length != count) {
			log.log(Level.SEVERE, "Line difference - From=" + fromLines.length + " <> Saved=" + count);
			count = -1; // caller must validate error in count and rollback accordingly - BF [3160928]
		}
		return count;
	}	//	copyLinesFrom

	/** Reversal Flag		*/
	protected boolean m_reversal = false;

	/**
	 * 	Set Reversal
	 *	@param reversal reversal
	 */
	protected void setReversal(boolean reversal)
	{
		m_reversal = reversal;
	}	//	setReversal
	/**
	 * 	Is Reversal
	 *	@return reversal
	 */
	public boolean isReversal()
	{
		return m_reversal;
	}	//	isReversal

	/**
	 * 	Set Processed.
	 * 	Propagate to Lines/Taxes
	 *	@param processed processed
	 */
	public void setProcessed (boolean processed)
	{
		super.setProcessed (processed);
		if (get_ID() == 0)
			return;
		StringBuilder sql = new StringBuilder("UPDATE M_InOutLine SET Processed='")
			.append((processed ? "Y" : "N"))
			.append("' WHERE M_InOut_ID=").append(getM_InOut_ID());
		int noLine = DB.executeUpdate(sql.toString(), get_TrxName());
		m_lines = null;
		if (log.isLoggable(Level.FINE)) log.fine(processed + " - Lines=" + noLine);
	}	//	setProcessed

	/**
	 * 	Get BPartner
	 *	@return partner
	 */
	public MBPartner getBPartner()
	{
		if (m_partner == null)
			m_partner = new MBPartner (getCtx(), getC_BPartner_ID(), get_TrxName());
		return m_partner;
	}	//	getPartner

	/**
	 * 	Set Document Type
	 * 	@param DocBaseType doc type MDocType.DOCBASETYPE_
	 */
	public void setC_DocType_ID (String DocBaseType)
	{
		String sql = "SELECT C_DocType_ID FROM C_DocType "
			+ "WHERE AD_Client_ID=? AND DocBaseType=?"
			+ " AND IsActive='Y'"
			+ " AND IsSOTrx='" + (isSOTrx() ? "Y" : "N") + "' "
			+ "ORDER BY IsDefault DESC";
		int C_DocType_ID = DB.getSQLValue(null, sql, getAD_Client_ID(), DocBaseType);
		if (C_DocType_ID <= 0)
			log.log(Level.SEVERE, "Not found for AC_Client_ID="
				+ getAD_Client_ID() + " - " + DocBaseType);
		else
		{
			if (log.isLoggable(Level.FINE)) log.fine("DocBaseType=" + DocBaseType + " - C_DocType_ID=" + C_DocType_ID);
			setC_DocType_ID (C_DocType_ID);
			boolean isSOTrx = MDocType.DOCBASETYPE_MaterialDelivery.equals(DocBaseType);
			setIsSOTrx (isSOTrx);
		}
	}	//	setC_DocType_ID

	/**
	 * 	Set Default C_DocType_ID.
	 * 	Based on SO flag
	 */
	public void setC_DocType_ID()
	{
		if (isSOTrx())
			setC_DocType_ID(MDocType.DOCBASETYPE_MaterialDelivery);
		else
			setC_DocType_ID(MDocType.DOCBASETYPE_MaterialReceipt);
	}	//	setC_DocType_ID

	/**
	 * 	Set Business Partner Defaults & Details
	 * 	@param bp business partner
	 */
	public void setBPartner (MBPartner bp)
	{
		if (bp == null)
			return;

		setC_BPartner_ID(bp.getC_BPartner_ID());

		//	Set Locations
		MBPartnerLocation[] locs = bp.getLocations(false);
		if (locs != null)
		{
			for (int i = 0; i < locs.length; i++)
			{
				if (locs[i].isShipTo())
					setC_BPartner_Location_ID(locs[i].getC_BPartner_Location_ID());
			}
			//	set to first if not set
			if (getC_BPartner_Location_ID() == 0 && locs.length > 0)
				setC_BPartner_Location_ID(locs[0].getC_BPartner_Location_ID());
		}
		if (getC_BPartner_Location_ID() == 0)
			log.log(Level.SEVERE, "Has no To Address: " + bp);

		//	Set Contact
		MUser[] contacts = bp.getContacts(false);
		if (contacts != null && contacts.length > 0)	//	get first User
			setAD_User_ID(contacts[0].getAD_User_ID());
	}	//	setBPartner

	/**
	 * 	Create the missing next Confirmation
	 */
	public void createConfirmation()
	{
		MDocType dt = MDocType.get(getCtx(), getC_DocType_ID());
		boolean pick = dt.isPickQAConfirm();
		boolean ship = dt.isShipConfirm();
		//	Nothing to do
		if (!pick && !ship)
		{
			log.fine("No need");
			return;
		}

		//	Create Both .. after each other
		if (pick && ship)
		{
			boolean havePick = false;
			boolean haveShip = false;
			MInOutConfirm[] confirmations = getConfirmations(false);
			for (int i = 0; i < confirmations.length; i++)
			{
				MInOutConfirm confirm = confirmations[i];
				if (MInOutConfirm.CONFIRMTYPE_PickQAConfirm.equals(confirm.getConfirmType()))
				{
					if (!confirm.isProcessed())		//	wait intil done
					{
						if (log.isLoggable(Level.FINE)) log.fine("Unprocessed: " + confirm);
						return;
					}
					havePick = true;
				}
				else if (MInOutConfirm.CONFIRMTYPE_ShipReceiptConfirm.equals(confirm.getConfirmType()))
					haveShip = true;
			}
			//	Create Pick
			if (!havePick)
			{
				MInOutConfirm.create (this, MInOutConfirm.CONFIRMTYPE_PickQAConfirm, false);
				return;
			}
			//	Create Ship
			if (!haveShip)
			{
				MInOutConfirm.create (this, MInOutConfirm.CONFIRMTYPE_ShipReceiptConfirm, false);
				return;
			}
			return;
		}
		//	Create just one
		if (pick)
			MInOutConfirm.create (this, MInOutConfirm.CONFIRMTYPE_PickQAConfirm, true);
		else if (ship)
			MInOutConfirm.create (this, MInOutConfirm.CONFIRMTYPE_ShipReceiptConfirm, true);
	}	//	createConfirmation
	
	protected void voidConfirmations()
	{
		for(MInOutConfirm confirm : getConfirmations(true))
		{
			if (!confirm.isProcessed())
			{
				if (!confirm.processIt(MInOutConfirm.DOCACTION_Void))
					throw new AdempiereException(confirm.getProcessMsg());
				confirm.saveEx();
			}
		}
	}


	/**
	 * 	Set Warehouse and check/set Organization
	 *	@param M_Warehouse_ID id
	 */
	public void setM_Warehouse_ID (int M_Warehouse_ID)
	{
		if (M_Warehouse_ID == 0)
		{
			log.severe("Ignored - Cannot set AD_Warehouse_ID to 0");
			return;
		}
		super.setM_Warehouse_ID (M_Warehouse_ID);
		//
		MWarehouse wh = MWarehouse.get(getCtx(), getM_Warehouse_ID());
		if (wh.getAD_Org_ID() != getAD_Org_ID())
		{
			log.warning("M_Warehouse_ID=" + M_Warehouse_ID
				+ ", Overwritten AD_Org_ID=" + getAD_Org_ID() + "->" + wh.getAD_Org_ID());
			setAD_Org_ID(wh.getAD_Org_ID());
		}
	}	//	setM_Warehouse_ID


	/**
	 * 	Before Save
	 *	@param newRecord new
	 *	@return true or false
	 */
	protected boolean beforeSave (boolean newRecord)
	{
		MWarehouse wh = MWarehouse.get(getCtx(), getM_Warehouse_ID());
		//	Warehouse Org
		if (newRecord)
		{
			if (wh.getAD_Org_ID() != getAD_Org_ID())
			{
				log.saveError("WarehouseOrgConflict", "");
				return false;
			}
		}

		boolean disallowNegInv = wh.isDisallowNegativeInv();
		String DeliveryRule = getDeliveryRule();
		if((disallowNegInv && DELIVERYRULE_Force.equals(DeliveryRule)) ||
				(DeliveryRule == null || DeliveryRule.length()==0))
			setDeliveryRule(DELIVERYRULE_Availability);

        // Shipment/Receipt can have either Order/RMA (For Movement type)
        if (getC_Order_ID() != 0 && getM_RMA_ID() != 0)
        {
            log.saveError("OrderOrRMA", "");
            return false;
        }

		//	Shipment - Needs Order/RMA
		if (!getMovementType().contentEquals(MInOut.MOVEMENTTYPE_CustomerReturns) && isSOTrx() && getC_Order_ID() == 0 && getM_RMA_ID() == 0)
		{
			log.saveError("FillMandatory", Msg.translate(getCtx(), "C_Order_ID"));
			return false;
		}

        if (isSOTrx() && getM_RMA_ID() != 0)
        {
            // Set Document and Movement type for this Receipt
            MRMA rma = new MRMA(getCtx(), getM_RMA_ID(), get_TrxName());
            MDocType docType = MDocType.get(getCtx(), rma.getC_DocType_ID());
            setC_DocType_ID(docType.getC_DocTypeShipment_ID());
        }

		return true;
	}	//	beforeSave

	/**
	 * 	After Save
	 *	@param newRecord new
	 *	@param success success
	 *	@return success
	 */
	protected boolean afterSave (boolean newRecord, boolean success)
	{
		if (!success || newRecord)
			return success;

		if (is_ValueChanged("AD_Org_ID"))
		{
			final String sql = "UPDATE M_InOutLine ol"
					+ " SET AD_Org_ID ="
					+ "(SELECT AD_Org_ID"
					+ " FROM M_InOut o WHERE ol.M_InOut_ID=o.M_InOut_ID) "
					+ "WHERE M_InOut_ID=?";
			int no = DB.executeUpdateEx(sql, new Object[] {getM_InOut_ID()}, get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine("Lines -> #" + no);
		}
		return true;
	}	//	afterSave


	/**************************************************************************
	 * 	Process document
	 *	@param processAction document action
	 *	@return true if performed
	 */
	public boolean processIt (String processAction)
	{
		m_processMsg = null;
		DocumentEngine engine = new DocumentEngine (this, getDocStatus());
		return engine.processIt (processAction, getDocAction());
	}	//	process

	/**	Process Message 			*/
	protected String		m_processMsg = null;
	/**	Just Prepared Flag			*/
	protected boolean		m_justPrepared = false;

	/**
	 * 	Unlock Document.
	 * 	@return true if success
	 */
	public boolean unlockIt()
	{
		if (log.isLoggable(Level.INFO)) log.info(toString());
		setProcessing(false);
		return true;
	}	//	unlockIt

	/**
	 * 	Invalidate Document
	 * 	@return true if success
	 */
	public boolean invalidateIt()
	{
		if (log.isLoggable(Level.INFO)) log.info(toString());
		setDocAction(DOCACTION_Prepare);
		return true;
	}	//	invalidateIt

	/**
	 *	Prepare Document
	 * 	@return new status (In Progress or Invalid)
	 */
	public String prepareIt()
	{
		if (log.isLoggable(Level.INFO)) log.info(toString());
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_PREPARE);
		if (m_processMsg != null)
			return DocAction.STATUS_Invalid;

		MDocType dt = MDocType.get(getCtx(), getC_DocType_ID());

		//  Order OR RMA can be processed on a shipment/receipt
		if (getC_Order_ID() != 0 && getM_RMA_ID() != 0)
		{
		    m_processMsg = "@OrderOrRMA@";
		    return DocAction.STATUS_Invalid;
		}
		//	Std Period open?
		if (!MPeriod.isOpen(getCtx(), getDateAcct(), dt.getDocBaseType(), getAD_Org_ID()))
		{
			m_processMsg = "@PeriodClosed@";
			return DocAction.STATUS_Invalid;
		}

		//	Credit Check
		if (isSOTrx() && !isReversal())
		{
			I_C_Order order = getC_Order();
			if (order != null && MDocType.DOCSUBTYPESO_PrepayOrder.equals(order.getC_DocType().getDocSubTypeSO())
					&& !MSysConfig.getBooleanValue(MSysConfig.CHECK_CREDIT_ON_PREPAY_ORDER, true, getAD_Client_ID(), getAD_Org_ID())) {
				// ignore -- don't validate Prepay Orders depending on sysconfig parameter
			} else {
				MBPartner bp = new MBPartner (getCtx(), getC_BPartner_ID(), get_TrxName());
				if (MBPartner.SOCREDITSTATUS_CreditStop.equals(bp.getSOCreditStatus()))
				{
					m_processMsg = "@BPartnerCreditStop@ - @TotalOpenBalance@="
						+ bp.getTotalOpenBalance()
						+ ", @SO_CreditLimit@=" + bp.getSO_CreditLimit();
					return DocAction.STATUS_Invalid;
				}
				if (MBPartner.SOCREDITSTATUS_CreditHold.equals(bp.getSOCreditStatus()))
				{
					m_processMsg = "@BPartnerCreditHold@ - @TotalOpenBalance@="
						+ bp.getTotalOpenBalance()
						+ ", @SO_CreditLimit@=" + bp.getSO_CreditLimit();
					return DocAction.STATUS_Invalid;
				}
				BigDecimal notInvoicedAmt = MBPartner.getNotInvoicedAmt(getC_BPartner_ID());
				if (MBPartner.SOCREDITSTATUS_CreditHold.equals(bp.getSOCreditStatus(notInvoicedAmt)))
				{
					m_processMsg = "@BPartnerOverSCreditHold@ - @TotalOpenBalance@="
						+ bp.getTotalOpenBalance() + ", @NotInvoicedAmt@=" + notInvoicedAmt
						+ ", @SO_CreditLimit@=" + bp.getSO_CreditLimit();
					return DocAction.STATUS_Invalid;
				}
			}
		}

		//	Lines
		MInOutLine[] lines = getLines(true);
		if (lines == null || lines.length == 0)
		{
			m_processMsg = "@NoLines@";
			return DocAction.STATUS_Invalid;
		}
		BigDecimal Volume = Env.ZERO;
		BigDecimal Weight = Env.ZERO;

		//	Mandatory Attributes
		for (int i = 0; i < lines.length; i++)
		{
			MInOutLine line = lines[i];
			MProduct product = line.getProduct();
			if (product != null)
			{
				Volume = Volume.add(product.getVolume().multiply(line.getMovementQty()));
				Weight = Weight.add(product.getWeight().multiply(line.getMovementQty()));
			}
			//
			if (line.getM_AttributeSetInstance_ID() != 0)
				continue;
			if (product != null && product.isASIMandatory(isSOTrx()))
			{
				if(product.getAttributeSet()==null){
					m_processMsg = "@NoAttributeSet@=" + product.getValue();
					return DocAction.STATUS_Invalid;

				}
				if (! product.getAttributeSet().excludeTableEntry(MInOutLine.Table_ID, isSOTrx())) {
					m_processMsg = "@M_AttributeSet_ID@ @IsMandatory@ (@Line@ #" + lines[i].getLine() +
									", @M_Product_ID@=" + product.getValue() + ")";
					return DocAction.STATUS_Invalid;
				}
			}
		}
		setVolume(Volume);
		setWeight(Weight);

		if (!isReversal())	//	don't change reversal
		{
			createConfirmation();
		}

		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_PREPARE);
		if (m_processMsg != null)
			return DocAction.STATUS_Invalid;

		m_justPrepared = true;
		if (!DOCACTION_Complete.equals(getDocAction()))
			setDocAction(DOCACTION_Complete);
		return DocAction.STATUS_InProgress;
	}	//	prepareIt

	/**
	 * 	Approve Document
	 * 	@return true if success
	 */
	public boolean  approveIt()
	{
		if (log.isLoggable(Level.INFO)) log.info(toString());
		setIsApproved(true);
		return true;
	}	//	approveIt

	/**
	 * 	Reject Approval
	 * 	@return true if success
	 */
	public boolean rejectIt()
	{
		if (log.isLoggable(Level.INFO)) log.info(toString());
		setIsApproved(false);
		return true;
	}	//	rejectIt

	/**
	 * 	Complete Document
	 * 	@return new status (Complete, In Progress, Invalid, Waiting ..)
	 */
	public String completeIt()
	{
		//	Re-Check
		if (!m_justPrepared)
		{
			String status = prepareIt();
			m_justPrepared = false;
			if (!DocAction.STATUS_InProgress.equals(status))
				return status;
		}

		// Set the definite document number after completed (if needed)
		setDefiniteDocumentNo();

		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_COMPLETE);
		if (m_processMsg != null)
			return DocAction.STATUS_Invalid;

		//	Outstanding (not processed) Incoming Confirmations ?
		MInOutConfirm[] confirmations = getConfirmations(true);
		for (int i = 0; i < confirmations.length; i++)
		{
			MInOutConfirm confirm = confirmations[i];
			if (!confirm.isProcessed())
			{
				if (MInOutConfirm.CONFIRMTYPE_CustomerConfirmation.equals(confirm.getConfirmType()))
					continue;
				//
				m_processMsg = "Open @M_InOutConfirm_ID@: " +
					confirm.getConfirmTypeName() + " - " + confirm.getDocumentNo();
				return DocAction.STATUS_InProgress;
			}
		}


		//	Implicit Approval
		if (!isApproved())
			approveIt();
		if (log.isLoggable(Level.INFO)) log.info(toString());
		StringBuilder info = new StringBuilder();

		StringBuilder errors = new StringBuilder();
		//	For all lines
		MInOutLine[] lines = getLines(false);
		for (int lineIndex = 0; lineIndex < lines.length; lineIndex++)
		{
			MInOutLine sLine = lines[lineIndex];
			MProduct product = sLine.getProduct();

			try
			{
				//	Qty & Type
				String MovementType = getMovementType();
				BigDecimal Qty = sLine.getMovementQty();
				if (MovementType.charAt(1) == '-')	//	C- Customer Shipment - V- Vendor Return
					Qty = Qty.negate();
	
				//	Update Order Line
				MOrderLine oLine = null;
				if (sLine.getC_OrderLine_ID() != 0)
				{
					oLine = new MOrderLine (getCtx(), sLine.getC_OrderLine_ID(), get_TrxName());
					if (log.isLoggable(Level.FINE)) log.fine("OrderLine - Reserved=" + oLine.getQtyReserved()
						+ ", Delivered=" + oLine.getQtyDelivered());
				}
	
	
	            // Load RMA Line
	            MRMALine rmaLine = null;
	
	            if (sLine.getM_RMALine_ID() != 0)
	            {
	                rmaLine = new MRMALine(getCtx(), sLine.getM_RMALine_ID(), get_TrxName());
	            }
	
				if (log.isLoggable(Level.INFO)) log.info("Line=" + sLine.getLine() + " - Qty=" + sLine.getMovementQty());
	
				//	Stock Movement - Counterpart MOrder.reserveStock
				if (product != null
					&& product.isStocked() )
				{
					//Ignore the Material Policy when is Reverse Correction
					if(!isReversal())
					{
						BigDecimal movementQty = sLine.getMovementQty();
						BigDecimal qtyOnLineMA = MInOutLineMA.getManualQty(sLine.getM_InOutLine_ID(), get_TrxName());
	
						if (   (movementQty.signum() != 0 && qtyOnLineMA.signum() != 0 && movementQty.signum() != qtyOnLineMA.signum()) // must have same sign
							|| (qtyOnLineMA.abs().compareTo(movementQty.abs())>0)) { // compare absolute values
							// More then line qty on attribute tab for line 10
							m_processMsg = "@Over_Qty_On_Attribute_Tab@ " + sLine.getLine();
							return DOCSTATUS_Invalid;
						}
						
						checkMaterialPolicy(sLine,movementQty.subtract(qtyOnLineMA));
					}
	
					log.fine("Material Transaction");
					MTransaction mtrx = null;
					
					//
					BigDecimal overReceipt = BigDecimal.ZERO;
					if (!isReversal()) 
					{
						if (oLine != null) 
						{
							BigDecimal toDelivered = oLine.getQtyOrdered()
									.subtract(oLine.getQtyDelivered());
							if (toDelivered.signum() < 0) // IDEMPIERE-2889
								toDelivered = Env.ZERO;
							if (sLine.getMovementQty().compareTo(toDelivered) > 0)
								overReceipt = sLine.getMovementQty().subtract(
										toDelivered);
							if (overReceipt.signum() != 0) 
							{
//								sLine.setQtyOverReceipt(overReceipt);
								sLine.saveEx();
							}
						}
					} 
					else 
					{
//						overReceipt = sLine.getQtyOverReceipt();
					}
					BigDecimal orderedQtyToUpdate = sLine.getMovementQty().subtract(overReceipt);
					//
					if (sLine.getM_AttributeSetInstance_ID() == 0)
					{
						MInOutLineMA mas[] = MInOutLineMA.get(getCtx(),
							sLine.getM_InOutLine_ID(), get_TrxName());
						for (int j = 0; j < mas.length; j++)
						{
							MInOutLineMA ma = mas[j];
							BigDecimal QtyMA = ma.getMovementQty();
							if (MovementType.charAt(1) == '-')	//	C- Customer Shipment - V- Vendor Return
								QtyMA = QtyMA.negate();
	
							//	Update Storage - see also VMatch.createMatchRecord
							if (!MStorageOnHand.add(getCtx(), getM_Warehouse_ID(),
								sLine.getM_Locator_ID(),
								sLine.getM_Product_ID(),
								ma.getM_AttributeSetInstance_ID(),
								QtyMA,ma.getDateMaterialPolicy(),
								get_TrxName()))
							{
								String lastError = CLogger.retrieveErrorString("");
								m_processMsg = "Cannot correct Inventory OnHand (MA) [" + product.getValue() + "] - " + lastError;
								return DocAction.STATUS_Invalid;
							}					
							
							//	Create Transaction
							mtrx = new MTransaction (getCtx(), sLine.getAD_Org_ID(),
								MovementType, sLine.getM_Locator_ID(),
								sLine.getM_Product_ID(), ma.getM_AttributeSetInstance_ID(),
								QtyMA, getMovementDate(), get_TrxName());
							mtrx.setM_InOutLine_ID(sLine.getM_InOutLine_ID());
							if (!mtrx.save())
							{
								m_processMsg = "Could not create Material Transaction (MA) [" + product.getValue() + "]";
								return DocAction.STATUS_Invalid;
							}
						}
						
						if (oLine!=null && mtrx!=null && oLine.getQtyOrdered().signum() != 0)
						{					
							if (sLine.getC_OrderLine_ID() != 0)
							{
								if (!MStorageReservation.add(getCtx(), oLine.getM_Warehouse_ID(),
										sLine.getM_Product_ID(),
										oLine.getM_AttributeSetInstance_ID(),
										orderedQtyToUpdate.negate(),
										isSOTrx(),
										get_TrxName()))
								{
									String lastError = CLogger.retrieveErrorString("");
									m_processMsg = "Cannot correct Inventory " + (isSOTrx()? "Reserved" : "Ordered") + " (MA) - [" + product.getValue() + "] - " + lastError;
									return DocAction.STATUS_Invalid;
								}
							}
						}
						
					}
					//	sLine.getM_AttributeSetInstance_ID() != 0
					if (mtrx == null)
					{
						Timestamp dateMPolicy= null;
						MStorageOnHand[] storages = MStorageOnHand.getWarehouse(getCtx(), 0,
								sLine.getM_Product_ID(), sLine.getM_AttributeSetInstance_ID(), null,
								MClient.MMPOLICY_FiFo.equals(product.getMMPolicy()), false,
								sLine.getM_Locator_ID(), get_TrxName());
						for (MStorageOnHand storage : storages) {
							if (storage.getQtyOnHand().compareTo(sLine.getMovementQty()) >= 0) {
								dateMPolicy = storage.getDateMaterialPolicy();
								break;
							}
						}
	
						if (dateMPolicy == null && storages.length > 0)
							dateMPolicy = storages[0].getDateMaterialPolicy();
	
						if(dateMPolicy==null)
							dateMPolicy = getMovementDate();
						
						//	Fallback: Update Storage - see also VMatch.createMatchRecord
						if (!MStorageOnHand.add(getCtx(), getM_Warehouse_ID(),
							sLine.getM_Locator_ID(),
							sLine.getM_Product_ID(),
							sLine.getM_AttributeSetInstance_ID(),
							Qty,dateMPolicy,get_TrxName()))
						{
							String lastError = CLogger.retrieveErrorString("");
							m_processMsg = "Cannot correct Inventory OnHand [" + product.getValue() + "] - " + lastError;
							return DocAction.STATUS_Invalid;
						}
						if (oLine!=null && oLine.getQtyOrdered().signum() != 0)  
						{
							if (!MStorageReservation.add(getCtx(), oLine.getM_Warehouse_ID(),
									sLine.getM_Product_ID(),
									oLine.getM_AttributeSetInstance_ID(),
									orderedQtyToUpdate.negate(), isSOTrx(), get_TrxName()))
							{
								m_processMsg = "Cannot correct Inventory Reserved " + (isSOTrx()? "Reserved [" :"Ordered [") + product.getValue() + "]";
								return DocAction.STATUS_Invalid;
							}
						}
						
						//	FallBack: Create Transaction
						mtrx = new MTransaction (getCtx(), sLine.getAD_Org_ID(),
							MovementType, sLine.getM_Locator_ID(),
							sLine.getM_Product_ID(), sLine.getM_AttributeSetInstance_ID(),
							Qty, getMovementDate(), get_TrxName());
						mtrx.setM_InOutLine_ID(sLine.getM_InOutLine_ID());
						if (!mtrx.save())
						{
							m_processMsg = CLogger.retrieveErrorString("Could not create Material Transaction [" + product.getValue() + "]");
							return DocAction.STATUS_Invalid;
						}
					}
				}	//	stock movement
	
				//	Correct Order Line
				if (product != null && oLine != null)		//	other in VMatch.createMatchRecord
				{
//					oLine.setQtyReserved(oLine.getQtyReserved().subtract(sLine.getMovementQty().subtract(sLine.getQtyOverReceipt())));
					oLine.setQtyReserved(oLine.getQtyReserved().subtract(sLine.getMovementQty()));

				}
	
				//	Update Sales Order Line
				if (oLine != null)
				{
					if (isSOTrx()							//	PO is done by Matching
						|| sLine.getM_Product_ID() == 0)	//	PO Charges, empty lines
					{
						if (isSOTrx())
							oLine.setQtyDelivered(oLine.getQtyDelivered().subtract(Qty));
						else
							oLine.setQtyDelivered(oLine.getQtyDelivered().add(Qty));
						oLine.setDateDelivered(getMovementDate());	//	overwrite=last
					}
					if (!oLine.save())
					{
						m_processMsg = "Could not update Order Line";
						return DocAction.STATUS_Invalid;
					}
					else
						if (log.isLoggable(Level.FINE)) log.fine("OrderLine -> Reserved=" + oLine.getQtyReserved()
							+ ", Delivered=" + oLine.getQtyReserved());
				}
	            //  Update RMA Line Qty Delivered
	            else if (rmaLine != null)
	            {
	                if (isSOTrx())
	                {
	                    rmaLine.setQtyDelivered(rmaLine.getQtyDelivered().add(Qty));
	                }
	                else
	                {
	                    rmaLine.setQtyDelivered(rmaLine.getQtyDelivered().subtract(Qty));
	                }
	                if (!rmaLine.save())
	                {
	                    m_processMsg = "Could not update RMA Line";
	                    return DocAction.STATUS_Invalid;
	                }
	            }
	
				//	Create Asset for SO
				if (product != null
					&& isSOTrx()
					&& product.isCreateAsset()
					&& !product.getM_Product_Category().getA_Asset_Group().isFixedAsset()
					&& sLine.getMovementQty().signum() > 0
					&& !isReversal())
				{
					log.fine("Asset");
					info.append("@A_Asset_ID@: ");
					int noAssets = sLine.getMovementQty().intValue();
					if (!product.isOneAssetPerUOM())
						noAssets = 1;
					for (int i = 0; i < noAssets; i++)
					{
						if (i > 0)
							info.append(" - ");
						int deliveryCount = i+1;
						if (!product.isOneAssetPerUOM())
							deliveryCount = 0;
						MAsset asset = new MAsset (this, sLine, deliveryCount);
						if (!asset.save(get_TrxName()))
						{
							m_processMsg = "Could not create Asset";
							return DocAction.STATUS_Invalid;
						}
						info.append(asset.getValue());
					}
				}	//	Asset
	
	
				//	Matching
				if (!isSOTrx()
					&& sLine.getM_Product_ID() != 0
					&& !isReversal())
				{
					BigDecimal matchQty = sLine.getMovementQty();
					//	Invoice - Receipt Match (requires Product)
					MInvoiceLine iLine = MInvoiceLine.getOfInOutLine (sLine);
					if (iLine != null && iLine.getM_Product_ID() != 0)
					{
						if (matchQty.compareTo(iLine.getQtyInvoiced())>0)
							matchQty = iLine.getQtyInvoiced();
	
						MMatchInv[] matches = MMatchInv.get(getCtx(),
							sLine.getM_InOutLine_ID(), iLine.getC_InvoiceLine_ID(), get_TrxName());
						if (matches == null || matches.length == 0)
						{
							MMatchInv inv = new MMatchInv (iLine, getMovementDate(), matchQty);
							if (sLine.getM_AttributeSetInstance_ID() != iLine.getM_AttributeSetInstance_ID())
							{
								iLine.setM_AttributeSetInstance_ID(sLine.getM_AttributeSetInstance_ID());
								iLine.saveEx();	//	update matched invoice with ASI
								inv.setM_AttributeSetInstance_ID(sLine.getM_AttributeSetInstance_ID());
							}
							if (!inv.save(get_TrxName()))
							{
								m_processMsg = CLogger.retrieveErrorString("Could not create Inv Matching");
								return DocAction.STATUS_Invalid;
							}
							addDocsPostProcess(inv);
						}
					}
	
					//	Link to Order
					if (sLine.getC_OrderLine_ID() != 0)
					{
						log.fine("PO Matching");
						//	Ship - PO
						MMatchPO po = MMatchPO.create (null, sLine, getMovementDate(), matchQty);
						if (po != null) {
							if (!po.save(get_TrxName()))
							{
								m_processMsg = "Could not create PO Matching";
								return DocAction.STATUS_Invalid;
							}
							if (!po.isPosted())
								addDocsPostProcess(po);
							MMatchInv matchInvCreated = po.getMatchInvCreated();
							if (matchInvCreated != null) {
								addDocsPostProcess(matchInvCreated);
							}
						}
						//	Update PO with ASI
						if (   oLine != null && oLine.getM_AttributeSetInstance_ID() == 0
							&& sLine.getMovementQty().compareTo(oLine.getQtyOrdered()) == 0) //  just if full match [ 1876965 ]
						{
							oLine.setM_AttributeSetInstance_ID(sLine.getM_AttributeSetInstance_ID());
							oLine.saveEx(get_TrxName());
						}
					}
					else	//	No Order - Try finding links via Invoice
					{
						//	Invoice has an Order Link
						if (iLine != null && iLine.getC_OrderLine_ID() != 0)
						{
							//	Invoice is created before  Shipment
							log.fine("PO(Inv) Matching");
							//	Ship - Invoice
							MMatchPO po = MMatchPO.create (iLine, sLine,
								getMovementDate(), matchQty);
							if (po != null) {
								if (!po.save(get_TrxName()))
								{
									m_processMsg = "Could not create PO(Inv) Matching";
									return DocAction.STATUS_Invalid;
								}
								if (!po.isPosted())
									addDocsPostProcess(po);
							}
							
							//	Update PO with ASI
							oLine = new MOrderLine (getCtx(), iLine.getC_OrderLine_ID(), get_TrxName());
							if (   oLine != null && oLine.getM_AttributeSetInstance_ID() == 0
								&& sLine.getMovementQty().compareTo(oLine.getQtyOrdered()) == 0) //  just if full match [ 1876965 ]
							{
								oLine.setM_AttributeSetInstance_ID(sLine.getM_AttributeSetInstance_ID());
								oLine.saveEx(get_TrxName());
							}
						}
					}	//	No Order
				}	//	PO Matching
			}
			catch (NegativeInventoryDisallowedException e)
			{
				log.severe(e.getMessage());
				errors.append(Msg.getElement(getCtx(), "Line")).append(" ").append(sLine.getLine()).append(": ");
				errors.append(e.getMessage()).append("\n");
			}
		}	//	for all lines

		if (errors.toString().length() > 0)
		{
			m_processMsg = errors.toString();
			return DocAction.STATUS_Invalid;
		}
		
		//	Counter Documents
		MInOut counter = createCounterDoc();
		if (counter != null)
			info.append(" - @CounterDoc@: @M_InOut_ID@=").append(counter.getDocumentNo());

		//  Drop Shipments
		MInOut dropShipment = createDropShipment();
		if (dropShipment != null)
			info.append(" - @DropShipment@: @M_InOut_ID@=").append(dropShipment.getDocumentNo());
		//	User Validation
		String valid = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_COMPLETE);
		if (valid != null)
		{
			m_processMsg = valid;
			return DocAction.STATUS_Invalid;
		}

		m_processMsg = info.toString();
		setProcessed(true);
		setDocAction(DOCACTION_Close);
		return DocAction.STATUS_Completed;
	}	//	completeIt

	/* Save array of documents to process AFTER completing this one */
	ArrayList<PO> docsPostProcess = new ArrayList<PO>();

	protected void addDocsPostProcess(PO doc) {
		docsPostProcess.add(doc);
	}

	public ArrayList<PO> getDocsPostProcess() {
		return docsPostProcess;
	}

	/**
	 * Automatically creates a customer shipment for any
	 * drop shipment material receipt
	 * Based on createCounterDoc() by JJ
	 * @return shipment if created else null
	 */
	protected MInOut createDropShipment() {

		if ( isSOTrx() || !isDropShip() || getC_Order_ID() == 0 )
			return null;

		int linkedOrderID = new MOrder (getCtx(), getC_Order_ID(), get_TrxName()).getLink_Order_ID();
		if (linkedOrderID <= 0)
			return null;

		//	Document Type
		int C_DocTypeTarget_ID = 0;
		MDocType[] shipmentTypes = MDocType.getOfDocBaseType(getCtx(), MDocType.DOCBASETYPE_MaterialDelivery);

		for (int i = 0; i < shipmentTypes.length; i++ )
		{
			if (shipmentTypes[i].isSOTrx() && ( C_DocTypeTarget_ID == 0 || shipmentTypes[i].isDefault() ) )
				C_DocTypeTarget_ID = shipmentTypes[i].getC_DocType_ID();
		}

		//	Deep Copy
		MInOut dropShipment = copyFrom(this, getMovementDate(), getDateAcct(),
			C_DocTypeTarget_ID, !isSOTrx(), false, get_TrxName(), true);

		dropShipment.setC_Order_ID(linkedOrderID);

		// get invoice id from linked order
		int invID = new MOrder (getCtx(), linkedOrderID, get_TrxName()).getC_Invoice_ID();
		if ( invID != 0 )
			dropShipment.setC_Invoice_ID(invID);

		dropShipment.setC_BPartner_ID(getDropShip_BPartner_ID());
		dropShipment.setC_BPartner_Location_ID(getDropShip_Location_ID());
		dropShipment.setAD_User_ID(getDropShip_User_ID());
		dropShipment.setIsDropShip(false);
		dropShipment.setDropShip_BPartner_ID(0);
		dropShipment.setDropShip_Location_ID(0);
		dropShipment.setDropShip_User_ID(0);
		dropShipment.setMovementType(MOVEMENTTYPE_CustomerShipment);

		//	References (Should not be required
		dropShipment.setSalesRep_ID(getSalesRep_ID());
		dropShipment.saveEx(get_TrxName());

		//		Update line order references to linked sales order lines
		MInOutLine[] lines = dropShipment.getLines(true);
		for (int i = 0; i < lines.length; i++)
		{
			MInOutLine dropLine = lines[i];
			MOrderLine ol = new MOrderLine(getCtx(), dropLine.getC_OrderLine_ID(), null);
			if ( ol.getC_OrderLine_ID() != 0 ) {
				dropLine.setC_OrderLine_ID(ol.getLink_OrderLine_ID());
				dropLine.saveEx();
			}
		}

		if (log.isLoggable(Level.FINE)) log.fine(dropShipment.toString());

		dropShipment.setDocAction(DocAction.ACTION_Complete);
		// added AdempiereException by Zuhri
		if (!dropShipment.processIt(DocAction.ACTION_Complete))
			throw new AdempiereException("Failed when processing document - " + dropShipment.getProcessMsg());
		// end added
		dropShipment.saveEx();

		return dropShipment;
	}

	/**
	 * 	Set the definite document number after completed
	 */
	protected void setDefiniteDocumentNo() {
		MDocType dt = MDocType.get(getCtx(), getC_DocType_ID());
		if (dt.isOverwriteDateOnComplete()) {
			setMovementDate(new Timestamp (System.currentTimeMillis()));
			if (getDateAcct().before(getMovementDate())) {
				setDateAcct(getMovementDate());
				MPeriod.testPeriodOpen(getCtx(), getDateAcct(), getC_DocType_ID(), getAD_Org_ID());
			}
		}
		if (dt.isOverwriteSeqOnComplete()) {
			String value = DB.getDocumentNo(getC_DocType_ID(), get_TrxName(), true, this);
			if (value != null)
				setDocumentNo(value);
		}
	}

	/**
	 * 	Check Material Policy
	 * 	Sets line ASI
	 */
	protected void checkMaterialPolicy(MInOutLine line,BigDecimal qty)
	{
			
		int no = MInOutLineMA.deleteInOutLineMA(line.getM_InOutLine_ID(), get_TrxName());
		if (no > 0)
			if (log.isLoggable(Level.CONFIG)) log.config("Delete old #" + no);
		
		if(Env.ZERO.compareTo(qty)==0)
			return;
		
		//	Incoming Trx
		String MovementType = getMovementType();
		boolean inTrx = MovementType.charAt(1) == '+';	//	V+ Vendor Receipt

		boolean needSave = false;

		MProduct product = line.getProduct();

		//	Need to have Location
		if (product != null
				&& line.getM_Locator_ID() == 0)
		{
			//MWarehouse w = MWarehouse.get(getCtx(), getM_Warehouse_ID());
			line.setM_Warehouse_ID(getM_Warehouse_ID());
			line.setM_Locator_ID(inTrx ? Env.ZERO : line.getMovementQty());	//	default Locator
			needSave = true;
		}

		//	Attribute Set Instance
		//  Create an  Attribute Set Instance to any receipt FIFO/LIFO
		if (product != null && line.getM_AttributeSetInstance_ID() == 0)
		{
			//Validate Transaction
			if (getMovementType().compareTo(MInOut.MOVEMENTTYPE_VendorReceipts) == 0 )
			{
				//auto balance negative on hand
				BigDecimal qtyToReceive = autoBalanceNegative(line, product,qty);
				
				//Allocate remaining qty.
				if (qtyToReceive.compareTo(Env.ZERO)>0)
				{
					MInOutLineMA ma = MInOutLineMA.addOrCreate(line, 0, qtyToReceive, getMovementDate(),true); 
					ma.saveEx();
				}
				
			} else if (getMovementType().compareTo(MInOut.MOVEMENTTYPE_CustomerReturns) == 0){
				BigDecimal qtyToReturn = autoBalanceNegative(line, product,qty);
				
				if (line.getM_RMALine_ID()!=0 && qtyToReturn.compareTo(Env.ZERO)>0){
					//Linking to shipment line
					MRMALine rmaLine = new MRMALine(getCtx(), line.getM_RMALine_ID(), get_TrxName());
					if(rmaLine.getM_InOutLine_ID()>0){
						//retrieving ASI which is not already returned
						MInOutLineMA shipmentMAS[] = MInOutLineMA.getNonReturned(getCtx(), rmaLine.getM_InOutLine_ID(), get_TrxName());
						
						for(MInOutLineMA sMA : shipmentMAS){
							BigDecimal lineMAQty = sMA.getMovementQty();
							if(lineMAQty.compareTo(qtyToReturn)>0){
								lineMAQty = qtyToReturn;
							}
							
							MInOutLineMA ma = MInOutLineMA.addOrCreate(line, sMA.getM_AttributeSetInstance_ID(), lineMAQty, sMA.getDateMaterialPolicy(),true); 
							ma.saveEx();			
							
							qtyToReturn = qtyToReturn.subtract(lineMAQty);
							if(qtyToReturn.compareTo(Env.ZERO)==0)
								break;
						}
					}
				}
				if(qtyToReturn.compareTo(Env.ZERO)>0){
					//Use movement data for  Material policy if no linkage found to Shipment.
					MInOutLineMA ma = MInOutLineMA.addOrCreate(line, 0, qtyToReturn, getMovementDate(),true); 
					ma.saveEx();			
				}	
			}
			// Create consume the Attribute Set Instance using policy FIFO/LIFO
			else if(getMovementType().compareTo(MInOut.MOVEMENTTYPE_VendorReturns) == 0 || getMovementType().compareTo(MInOut.MOVEMENTTYPE_CustomerShipment) == 0)
			{
				String MMPolicy = product.getMMPolicy();
				Timestamp minGuaranteeDate = getMovementDate();
				MStorageOnHand[] storages = MStorageOnHand.getWarehouse(getCtx(), getM_Warehouse_ID(), line.getM_Product_ID(), line.getM_AttributeSetInstance_ID(),
						minGuaranteeDate, MClient.MMPOLICY_FiFo.equals(MMPolicy), true, line.getM_Locator_ID(), get_TrxName(), false);
				BigDecimal qtyToDeliver = qty;
				for (MStorageOnHand storage: storages)
				{
					if (storage.getQtyOnHand().compareTo(qtyToDeliver) >= 0)
					{
						MInOutLineMA ma = new MInOutLineMA (line,
								storage.getM_AttributeSetInstance_ID(),
								qtyToDeliver,storage.getDateMaterialPolicy(),true);
						ma.saveEx();
						qtyToDeliver = Env.ZERO;
					}
					else
					{
						MInOutLineMA ma = new MInOutLineMA (line,
								storage.getM_AttributeSetInstance_ID(),
								storage.getQtyOnHand(),storage.getDateMaterialPolicy(),true);
						ma.saveEx();
						qtyToDeliver = qtyToDeliver.subtract(storage.getQtyOnHand());
						if (log.isLoggable(Level.FINE)) log.fine( ma + ", QtyToDeliver=" + qtyToDeliver);
					}

					if (qtyToDeliver.signum() == 0)
						break;
				}

				if (qtyToDeliver.signum() != 0)
				{					
					//Over Delivery
					MInOutLineMA ma = MInOutLineMA.addOrCreate(line, line.getM_AttributeSetInstance_ID(), qtyToDeliver, getMovementDate(),true);
					ma.saveEx();
					if (log.isLoggable(Level.FINE)) log.fine("##: " + ma);
				}
			}	//	outgoing Trx
		}	//	attributeSetInstance

		if (needSave)
		{
			line.saveEx();
		}
	}	//	checkMaterialPolicy

	protected BigDecimal autoBalanceNegative(MInOutLine line, MProduct product,BigDecimal qtyToReceive) {
		MStorageOnHand[] storages = MStorageOnHand.getWarehouseNegative(getCtx(), getM_Warehouse_ID(), line.getM_Product_ID(), 0,
				null, MClient.MMPOLICY_FiFo.equals(product.getMMPolicy()), line.getM_Locator_ID(), get_TrxName(), false);
		
		Timestamp dateMPolicy = null;
			
		for (MStorageOnHand storage : storages)
		{
			if (storage.getQtyOnHand().signum() < 0 && qtyToReceive.compareTo(Env.ZERO)>0)
			{
				dateMPolicy = storage.getDateMaterialPolicy();
				BigDecimal lineMAQty = qtyToReceive;
				if(lineMAQty.compareTo(storage.getQtyOnHand().negate())>0)
					lineMAQty = storage.getQtyOnHand().negate();
				
				//Using ASI from storage record
				MInOutLineMA ma = new MInOutLineMA (line, storage.getM_AttributeSetInstance_ID(), lineMAQty,dateMPolicy,true);
				ma.saveEx();			
				qtyToReceive = qtyToReceive.subtract(lineMAQty);
			}
		}
		return qtyToReceive;
	}


	/**************************************************************************
	 * 	Create Counter Document
	 * 	@return InOut
	 */
	protected MInOut createCounterDoc()
	{
		//	Is this a counter doc ?
		if (getRef_InOut_ID() != 0)
			return null;

		//	Org Must be linked to BPartner
		MOrg org = MOrg.get(getCtx(), getAD_Org_ID());
		int counterC_BPartner_ID = org.getLinkedC_BPartner_ID(get_TrxName());
		if (counterC_BPartner_ID == 0)
			return null;
		//	Business Partner needs to be linked to Org
		MBPartner bp = new MBPartner (getCtx(), getC_BPartner_ID(), get_TrxName());
		int counterAD_Org_ID = bp.getAD_OrgBP_ID_Int();
		if (counterAD_Org_ID == 0)
			return null;

		MBPartner counterBP = new MBPartner (getCtx(), counterC_BPartner_ID, null);
		MOrgInfo counterOrgInfo = MOrgInfo.get(getCtx(), counterAD_Org_ID, get_TrxName());
		if (log.isLoggable(Level.INFO)) log.info("Counter BP=" + counterBP.getName());

		//	Document Type
		int C_DocTypeTarget_ID = 0;
		MDocTypeCounter counterDT = MDocTypeCounter.getCounterDocType(getCtx(), getC_DocType_ID());
		if (counterDT != null)
		{
			if (log.isLoggable(Level.FINE)) log.fine(counterDT.toString());
			if (!counterDT.isCreateCounter() || !counterDT.isValid())
				return null;
			C_DocTypeTarget_ID = counterDT.getCounter_C_DocType_ID();
		}
		else	//	indirect
		{
			C_DocTypeTarget_ID = MDocTypeCounter.getCounterDocType_ID(getCtx(), getC_DocType_ID());
			if (log.isLoggable(Level.FINE)) log.fine("Indirect C_DocTypeTarget_ID=" + C_DocTypeTarget_ID);
			if (C_DocTypeTarget_ID <= 0)
				return null;
		}

		//	Deep Copy
		MInOut counter = copyFrom(this, getMovementDate(), getDateAcct(),
			C_DocTypeTarget_ID, !isSOTrx(), true, get_TrxName(), true);

		//
		counter.setAD_Org_ID(counterAD_Org_ID);
		counter.setM_Warehouse_ID(counterOrgInfo.getM_Warehouse_ID());
		//
		counter.setBPartner(counterBP);

		if ( isDropShip() )
		{
			counter.setIsDropShip(true );
			counter.setDropShip_BPartner_ID(getDropShip_BPartner_ID());
			counter.setDropShip_Location_ID(getDropShip_Location_ID());
			counter.setDropShip_User_ID(getDropShip_User_ID());
		}

		//	Refernces (Should not be required
		counter.setSalesRep_ID(getSalesRep_ID());
		counter.saveEx(get_TrxName());

		String MovementType = counter.getMovementType();
		boolean inTrx = MovementType.charAt(1) == '+';	//	V+ Vendor Receipt

		//	Update copied lines
		MInOutLine[] counterLines = counter.getLines(true);
		for (int i = 0; i < counterLines.length; i++)
		{
			MInOutLine counterLine = counterLines[i];
			counterLine.setAD_Org_ID(counter.getAD_Org_ID());
//			counterLine.setClientOrg(counter);
			counterLine.setM_Warehouse_ID(counter.getM_Warehouse_ID());
			counterLine.setM_Locator_ID(0);
			counterLine.setM_Locator_ID(inTrx ? Env.ZERO : counterLine.getMovementQty());
			//
			counterLine.saveEx(get_TrxName());
		}

		if (log.isLoggable(Level.FINE)) log.fine(counter.toString());

		//	Document Action
		if (counterDT != null)
		{
			if (counterDT.getDocAction() != null)
			{
				counter.setDocAction(counterDT.getDocAction());
				// added AdempiereException by zuhri
				if (!counter.processIt(counterDT.getDocAction()))
					throw new AdempiereException("Failed when processing document - " + counter.getProcessMsg());
				// end added
				counter.saveEx(get_TrxName());
			}
		}
		return counter;
	}	//	createCounterDoc

	/**
	 * 	Void Document.
	 * 	@return true if success
	 */
	public boolean voidIt()
	{
		if (log.isLoggable(Level.INFO)) log.info(toString());		

		if (DOCSTATUS_Closed.equals(getDocStatus())
			|| DOCSTATUS_Reversed.equals(getDocStatus())
			|| DOCSTATUS_Voided.equals(getDocStatus()))
		{
			m_processMsg = "Document Closed: " + getDocStatus();
			return false;
		}

		//	Not Processed
		if (DOCSTATUS_Drafted.equals(getDocStatus())
			|| DOCSTATUS_Invalid.equals(getDocStatus())
			|| DOCSTATUS_InProgress.equals(getDocStatus())
			|| DOCSTATUS_Approved.equals(getDocStatus())
			|| DOCSTATUS_NotApproved.equals(getDocStatus()) )
		{
			// Before Void
			m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_VOID);
			if (m_processMsg != null)
				return false;
			
			//	Set lines to 0
			MInOutLine[] lines = getLines(false);
			for (int i = 0; i < lines.length; i++)
			{
				MInOutLine line = lines[i];
				BigDecimal old = line.getMovementQty();
				if (old.signum() != 0)
				{
					line.setQty(Env.ZERO);
					StringBuilder msgadd = new StringBuilder("Void (").append(old).append(")");
					line.addDescription(msgadd.toString());
					line.saveEx(get_TrxName());
				}
			}
			//
			// Void Confirmations
			setDocStatus(DOCSTATUS_Voided); // need to set & save docstatus to be able to check it in MInOutConfirm.voidIt()
			saveEx();
			voidConfirmations();
		}
		else
		{
			boolean accrual = false;
			try 
			{
				MPeriod.testPeriodOpen(getCtx(), getDateAcct(), getC_DocType_ID(), getAD_Org_ID());
			}
			catch (PeriodClosedException e) 
			{
				accrual = true;
			}
			
			if (accrual)
				return reverseAccrualIt();
			else
				return reverseCorrectIt();
		}

		// After Void
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_VOID);
		if (m_processMsg != null)
			return false;

		setProcessed(true);
		setDocAction(DOCACTION_None);
		return true;
	}	//	voidIt

	/**
	 * 	Close Document.
	 * 	@return true if success
	 */
	public boolean closeIt()
	{
		if (log.isLoggable(Level.INFO)) log.info(toString());
		// Before Close
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_CLOSE);
		if (m_processMsg != null)
			return false;

		setProcessed(true);
		setDocAction(DOCACTION_None);

		// After Close
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_CLOSE);
		if (m_processMsg != null)
			return false;
		return true;
	}	//	closeIt

	/**
	 * 	Reverse Correction - same date
	 * 	@return true if success
	 */
	public boolean reverseCorrectIt()
	{
		if (log.isLoggable(Level.INFO)) log.info(toString());
		// Before reverseCorrect
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_REVERSECORRECT);
		if (m_processMsg != null)
			return false;

		MInOut reversal = reverse(false);
		if (reversal == null)
			return false;

		// After reverseCorrect
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_REVERSECORRECT);
		if (m_processMsg != null)
			return false;

		m_processMsg = reversal.getDocumentNo();
		setProcessed(true);
		setDocStatus(DOCSTATUS_Reversed);		//	 may come from void
		setDocAction(DOCACTION_None);
		return true;
	}	//	reverseCorrectionIt

	protected MInOut reverse(boolean accrual) {
		MDocType dt = MDocType.get(getCtx(), getC_DocType_ID());
		
		//		ISY-256
		Timestamp dateReverseAccrue = null;
		dateReverseAccrue = (Timestamp) get_Value("DateReverseAccrue");
		if(accrual && dateReverseAccrue==null){
			throw new AdempiereException("ISY-256 Please fill field Reverse Accrue Date for process Reverse Accrual.");
		}
		Timestamp reversalDate = accrual ? dateReverseAccrue : getDateAcct();
		if (reversalDate == null) {
			reversalDate = new Timestamp(System.currentTimeMillis());
		}
		Timestamp reversalMovementDate = accrual ? reversalDate : getMovementDate();
		if (!MPeriod.isOpen(getCtx(), reversalDate, dt.getDocBaseType(), getAD_Org_ID()))
		{
			m_processMsg = "@PeriodClosed@";
			return null;
		}

		//	Reverse/Delete Matching
		if (!isSOTrx())
		{
			if (!reverseMatching(reversalDate))
				return null;			
		}

		//	Deep Copy
		MInOut reversal = copyFrom (this, reversalMovementDate, reversalDate,
			getC_DocType_ID(), isSOTrx(), false, get_TrxName(), true);
		if (reversal == null)
		{
			m_processMsg = "Could not create Ship Reversal";
			return null;
		}
		reversal.setReversal(true);

		//	Reverse Line Qty
		MInOutLine[] sLines = getLines(false);
		MInOutLine[] rLines = reversal.getLines(false);
		for (int i = 0; i < rLines.length; i++)
		{
			MInOutLine rLine = rLines[i];
			rLine.setQtyEntered(rLine.getQtyEntered().negate());
			rLine.setMovementQty(rLine.getMovementQty().negate());
//			rLine.setQtyOverReceipt(rLine.getQtyOverReceipt().negate());
			rLine.setM_AttributeSetInstance_ID(sLines[i].getM_AttributeSetInstance_ID());
			// Goodwill: store original (voided/reversed) document line
			rLine.setReversalLine_ID(sLines[i].getM_InOutLine_ID());
			if (!rLine.save(get_TrxName()))
			{
				m_processMsg = "Could not correct Ship Reversal Line";
				return null;
			}
			//	We need to copy MA
			if (rLine.getM_AttributeSetInstance_ID() == 0)
			{
				MInOutLineMA mas[] = MInOutLineMA.get(getCtx(),
					sLines[i].getM_InOutLine_ID(), get_TrxName());
				for (int j = 0; j < mas.length; j++)
				{
					MInOutLineMA ma = new MInOutLineMA (rLine,
						mas[j].getM_AttributeSetInstance_ID(),
						mas[j].getMovementQty().negate(),mas[j].getDateMaterialPolicy(),true);
					ma.saveEx();
				}
			}
			//	De-Activate Asset
			MAsset asset = MAsset.getFromShipment(getCtx(), sLines[i].getM_InOutLine_ID(), get_TrxName());
			if (asset != null)
			{
				asset.setIsActive(false);
				asset.setDescription(asset.getDescription() + " (" + reversal.getDocumentNo() + " #" + rLine.getLine() + "<-)");
				asset.saveEx();
			}
		}
		reversal.setC_Order_ID(getC_Order_ID());
		// Set M_RMA_ID
		reversal.setM_RMA_ID(getM_RMA_ID());
		StringBuilder msgadd = new StringBuilder("{->").append(getDocumentNo()).append(")");
		reversal.addDescription(msgadd.toString());
		//FR1948157
		reversal.setReversal_ID(getM_InOut_ID());
		reversal.saveEx(get_TrxName());
		//
		reversal.docsPostProcess = this.docsPostProcess;
		this.docsPostProcess = new ArrayList<PO>();
		//
		if (!reversal.processIt(DocAction.ACTION_Complete)
			|| !reversal.getDocStatus().equals(DocAction.STATUS_Completed))
		{
			m_processMsg = "Reversal ERROR: " + reversal.getProcessMsg();
			return null;
		}
		reversal.closeIt();
		reversal.setProcessing (false);
		reversal.setDocStatus(DOCSTATUS_Reversed);
		reversal.setDocAction(DOCACTION_None);
		reversal.saveEx(get_TrxName());
		//
		msgadd = new StringBuilder("(").append(reversal.getDocumentNo()).append("<-)");
		addDescription(msgadd.toString());
		
		//
		// Void Confirmations
		setDocStatus(DOCSTATUS_Reversed); // need to set & save docstatus to be able to check it in MInOutConfirm.voidIt()
		saveEx();
		//FR1948157
		this.setReversal_ID(reversal.getM_InOut_ID());
		voidConfirmations();
		return reversal;
	}

	protected boolean reverseMatching(Timestamp reversalDate) {
		MMatchInv[] mInv = MMatchInv.getInOut(getCtx(), getM_InOut_ID(), get_TrxName());
		for (MMatchInv mMatchInv : mInv)
		{		
			if (mMatchInv.getReversal_ID() > 0)
				continue;
			
			String description = mMatchInv.getDescription();
			if (description == null || !description.endsWith("<-)"))
			{
				if (!mMatchInv.reverse(reversalDate))
				{
					log.log(Level.SEVERE, "Failed to create reversal for match invoice " + mMatchInv.getDocumentNo());
					return false;
				}
				addDocsPostProcess(new MMatchInv(Env.getCtx(), mMatchInv.getReversal_ID(), get_TrxName()));
			}
		}
		MMatchPO[] mMatchPOList = MMatchPO.getInOut(getCtx(), getM_InOut_ID(), get_TrxName());
		for (MMatchPO mMatchPO : mMatchPOList) 
		{
			if (mMatchPO.getReversal_ID() > 0)
				continue;
			
			String description = mMatchPO.getDescription();
			if (description == null || !description.endsWith("<-)"))
			{
				if (!mMatchPO.reverse(reversalDate))
				{
					log.log(Level.SEVERE, "Failed to create reversal for match purchase order " + mMatchPO.getDocumentNo());
					return false;
				}
				addDocsPostProcess(new MMatchPO(Env.getCtx(), mMatchPO.getReversal_ID(), get_TrxName()));
			}
		}
		return true;
	}

	/**
	 * 	Reverse Accrual - none
	 * 	@return false
	 */
	public boolean reverseAccrualIt()
	{
		if (log.isLoggable(Level.INFO)) log.info(toString());
		// Before reverseAccrual
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_REVERSEACCRUAL);
		if (m_processMsg != null)
			return false;

		MInOut reversal = reverse(true);
		if (reversal == null)
			return false;
		
		// After reverseAccrual
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_REVERSEACCRUAL);
		if (m_processMsg != null)
			return false;

		m_processMsg = reversal.getDocumentNo();
		setProcessed(true);
		setDocStatus(DOCSTATUS_Reversed);		//	 may come from void
		setDocAction(DOCACTION_None);
		return true;
	}	//	reverseAccrualIt

	/**
	 * 	Re-activate
	 * 	@return false
	 */
	public boolean reActivateIt()
	{
		if (log.isLoggable(Level.INFO)) log.info(toString());
		// Before reActivate
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_REACTIVATE);
		if (m_processMsg != null)
			return false;

		// After reActivate
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_REACTIVATE);
		if (m_processMsg != null)
			return false;

		return false;
	}	//	reActivateIt


	/*************************************************************************
	 * 	Get Summary
	 *	@return Summary of Document
	 */
	public String getSummary()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(getDocumentNo());
		//	: Total Lines = 123.00 (#1)
		sb.append(":")
		//	.append(Msg.translate(getCtx(),"TotalLines")).append("=").append(getTotalLines())
			.append(" (#").append(getLines(false).length).append(")");
		//	 - Description
		if (getDescription() != null && getDescription().length() > 0)
			sb.append(" - ").append(getDescription());
		return sb.toString();
	}	//	getSummary

	/**
	 * 	Get Process Message
	 *	@return clear text error message
	 */
	public String getProcessMsg()
	{
		return m_processMsg;
	}	//	getProcessMsg

	/**
	 * 	Get Document Owner (Responsible)
	 *	@return AD_User_ID
	 */
	public int getDoc_User_ID()
	{
		return getSalesRep_ID();
	}	//	getDoc_User_ID

	/**
	 * 	Get Document Approval Amount
	 *	@return amount
	 */
	public BigDecimal getApprovalAmt()
	{
		return Env.ZERO;
	}	//	getApprovalAmt

	/**
	 * 	Get C_Currency_ID
	 *	@return Accounting Currency
	 */
	public int getC_Currency_ID ()
	{
		return Env.getContextAsInt(getCtx(),"$C_Currency_ID");
	}	//	getC_Currency_ID

	/**
	 * 	Document Status is Complete or Closed
	 *	@return true if CO, CL or RE
	 */
	public boolean isComplete()
	{
		String ds = getDocStatus();
		return DOCSTATUS_Completed.equals(ds)
			|| DOCSTATUS_Closed.equals(ds)
			|| DOCSTATUS_Reversed.equals(ds);
	}	//	isComplete

}	//	MInOut
