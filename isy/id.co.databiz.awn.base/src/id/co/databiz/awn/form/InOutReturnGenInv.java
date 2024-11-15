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
package id.co.databiz.awn.form;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPartner;
import org.compiere.model.MClient;
import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MLocation;
import org.compiere.model.MOrder;
import org.compiere.model.MRMA;
import org.compiere.process.DocAction;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Language;

/**
 *	Generate Invoices from RMA InOut
 *	
 *  @author Jorg Janke
 *  @version $Id: InvoiceGenerate.java,v 1.2 2006/07/30 00:51:01 jjanke Exp $
 *  
 *  @author Darwanto ~dar~
 */
public class InOutReturnGenInv extends SvrProcess
{
	/**	Manual Selection		*/
	private boolean 	p_Selection = false;
	/**	Date Invoiced			*/
	private Timestamp	p_DateInvoiced = null;
	/**	Date Invoiced			*/
	private Timestamp	p_MovementDate = null;
	/**	Date Invoiced			*/
	private Timestamp	p_MovementDate_To = null;
	/**	Org						*/
	private int			p_AD_Org_ID = 0;
	/** BPartner				*/
	private int			p_C_BPartner_ID = 0;
	/** Order					*/
	private int			p_M_InOut_ID = 0;
	/** Consolidate				*/
	private boolean		p_ConsolidateDocument = true;
	/** Invoice Document Action	*/
	private String		p_docAction = DocAction.ACTION_Complete;
	/** Document Type	*/
	private int			p_C_DocType_ID = MRMA.Table_ID;
	
	/**	The current Invoice	*/
	private MInvoice 	m_invoice = null;
	/**	The current Shipment	*/
	private MInOut	 	m_ship = null;
	/** Numner of Invoices		*/
	private int			m_created = 0;
	/**	Line Number				*/
	private int			m_line = 0;
	/**	Business Partner		*/
	private MBPartner	m_bp = null;
	
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
			else if (name.equals("Selection"))
				p_Selection = "Y".equals(para[i].getParameter());
			else if (name.equals("DateInvoiced"))
				p_DateInvoiced = (Timestamp)para[i].getParameter();
			else if (name.equals("MovementDate")) {
				p_MovementDate = (Timestamp)para[i].getParameter();
				p_MovementDate_To = (Timestamp)para[i].getParameter_To();
			}
			else if (name.equals("AD_Org_ID"))
				p_AD_Org_ID = para[i].getParameterAsInt();
			else if (name.equals("C_BPartner_ID"))
				p_C_BPartner_ID = para[i].getParameterAsInt();
			else if (name.equals("M_InOut_ID"))
				p_M_InOut_ID = para[i].getParameterAsInt();
			else if (name.equals("ConsolidateDocument"))
				p_ConsolidateDocument = "Y".equals(para[i].getParameter());
			else if (name.equals("DocAction"))
				p_docAction = (String)para[i].getParameter();
			else if (name.equals("C_DocType_ID"))
				p_C_DocType_ID = para[i].getParameterAsInt();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}

		//	Login Date
		if (p_DateInvoiced == null)
			p_DateInvoiced = Env.getContextAsDate(getCtx(), "#Date");
		if (p_DateInvoiced == null)
			p_DateInvoiced = new Timestamp(System.currentTimeMillis());

		//	DocAction check
		if (!DocAction.ACTION_Complete.equals(p_docAction))
			p_docAction = DocAction.ACTION_Prepare;
		
	}	//	prepare

	/**
	 * 	Generate Invoices
	 *	@return info
	 *	@throws Exception
	 */
	protected String doIt () throws Exception
	{
		log.info("Selection=" + p_Selection + ", DateInvoiced=" + p_DateInvoiced
			+ ", AD_Org_ID=" + p_AD_Org_ID + ", C_BPartner_ID=" + p_C_BPartner_ID
			+ ", C_Order_ID=" + p_M_InOut_ID + ", DocAction=" + p_docAction 
			+ ", Consolidate=" + p_ConsolidateDocument);
		//
		String sql = null;
		if (p_Selection)	//	VInOutGenInvoice
		{
			sql = "SELECT M_InOut.* FROM M_InOut, T_Selection "
				+ "WHERE M_InOut.DocStatus='CO' AND M_InOut.IsSOTrx='Y' "
				+ "AND M_InOut.M_InOut_ID = T_Selection.T_Selection_ID "
				+ "AND T_Selection.AD_PInstance_ID=? "
				+ "ORDER BY M_InOut.M_Warehouse_ID, M_InOut.C_BPartner_ID, M_InOut.M_InOut_ID";
		}
		else
		{
			sql = "SELECT * FROM M_InOut o "
				+ "WHERE DocStatus IN('CO') AND IsSOTrx='Y'";
			if (p_AD_Org_ID != 0)
				sql += " AND AD_Org_ID=?";
			if (p_C_BPartner_ID != 0)
				sql += " AND C_BPartner_ID=?";
			if (p_M_InOut_ID != 0)
				sql += " AND M_InOut_ID=?";
			if( p_MovementDate != null )
				sql += " AND o.MovementDate >= ?";
			if( p_MovementDate_To != null )
				sql += " AND o.MovementDate <= ?";
			//
				sql += " AND EXISTS (SELECT * FROM M_RMALine ol "
					+ "WHERE o.M_RMA_ID=ol.M_RMA_ID AND ol.QtyOrdered<>ol.QtyInvoiced) "
				+ "AND o.C_DocType_ID IN (SELECT C_DocType_ID FROM C_DocType "
					+ "WHERE DocBaseType='MMR') ";
			sql	+= "ORDER BY M_Warehouse_ID, C_BPartner_ID, M_InOut_ID";
		}
	//	sql += " FOR UPDATE";
		
		PreparedStatement pstmt = null;
		try
		{
			pstmt = DB.prepareStatement (sql, get_TrxName());
			int index = 1;
			if (p_Selection) 
			{
				pstmt.setInt(index, getAD_PInstance_ID());
			}
			else
			{
				if (p_AD_Org_ID != 0)
					pstmt.setInt(index++, p_AD_Org_ID);
				if (p_C_BPartner_ID != 0)
					pstmt.setInt(index++, p_C_BPartner_ID);
				if (p_M_InOut_ID != 0)
					pstmt.setInt(index++, p_M_InOut_ID);
				if( p_MovementDate != null )
					pstmt.setString(index++, DB.TO_DATE(p_MovementDate));
				if( p_MovementDate_To != null )
					pstmt.setString(index++, DB.TO_DATE(p_MovementDate_To));
			}
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, sql, e);
		}
		
		if( p_C_DocType_ID != MRMA.Table_ID )
			return generateFromRMA(pstmt);
		
		return generateFromRMA(pstmt);
	}	//	doIt
	
	/**
	 * 	Complete Invoice
	 */
	private void completeInvoice()
	{
		if (m_invoice != null)
		{
			if (!m_invoice.processIt(p_docAction))
			{
				log.warning("completeInvoice - failed: " + m_invoice);
				addLog("completeInvoice - failed: " + m_invoice); // Elaine 2008/11/25
			}
			m_invoice.save();
			//
			addLog(m_invoice.getC_Invoice_ID(), m_invoice.getDateInvoiced(), null, m_invoice.getDocumentNo());
			m_created++;
		}
		m_invoice = null;
		m_ship = null;
		m_line = 0;
	}	//	completeInvoice

	/**
	 * 	Generate Invoice From Return
	 * 	@param pstmt inout query 
	 *	@return info
	 */
	private String generateFromRMA (PreparedStatement pstmt)
	{
		try
		{
			ResultSet rs = pstmt.executeQuery ();
			while (rs.next ())
			{
				MRMA rma = new MRMA (getCtx(), rs.getInt("M_RMA_ID"), get_TrxName());
				
				//	New Invoice Location
				if (!p_ConsolidateDocument 
					|| (m_invoice != null) )
					completeInvoice();
				//boolean completeOrder = MOrder.INVOICERULE_AfterOrderDelivered.equals(order.getInvoiceRule());
				//boolean doInvoice = false;
				
				//	After Delivery
				//if (doInvoice)
				//{
					//MInOut[] shipments = order.getShipments();
					//for (int i = 0; i < shipments.length; i++)
					//{
						//MInOut ship = shipments[i];
						MInOut ship = new MInOut(getCtx(), rs.getInt("M_InOut_ID"), get_TrxName());
						if (!ship.isComplete()		//	ignore incomplete or reversals 
							|| ship.getDocStatus().equals(MInOut.DOCSTATUS_Reversed))
							continue;
						MInOutLine[] shipLines = ship.getLines(false);
						for (int j = 0; j < shipLines.length; j++)
						{
							MInOutLine shipLine = shipLines[j];
							//if (!rma.is.isOrderLine(shipLine.getC_OrderLine_ID()))
							//	continue;
							if (!shipLine.isInvoiced())
								createLineFromRMA(rma, ship, shipLine);
						}
						m_line += 1000;
					//}
				//}
			}	//	for all orders
			rs.close ();
			pstmt.close ();
			pstmt = null;
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "", e);
		}
		try
		{
			if (pstmt != null)
				pstmt.close ();
			pstmt = null;
		}
		catch (Exception e)
		{
			pstmt = null;
		}
		completeInvoice();
		return "@Created@ = " + m_created;
	}	//	generate

	/**
	 * 	Create Invoice Line from Shipment
	 *	@param order order
	 *	@param ship shipment header
	 *	@param sLine shipment line
	 */
	private void createLineFromRMA (MRMA rma, MInOut ship, MInOutLine sLine)
	{
		MOrder rmaOrder = rma.getOriginalOrder();
		if (m_invoice == null)
		{
			m_invoice = new MInvoice (getCtx(), 0, get_TrxName());
			
			m_invoice.setClientOrg(rma.getAD_Client_ID(), rma.getAD_Org_ID());
			m_invoice.setIsSOTrx(rma.isSOTrx());
			m_invoice.setIsDiscountPrinted(rmaOrder.isDiscountPrinted());
			m_invoice.setIsSelfService(rmaOrder.isSelfService());
			m_invoice.setSendEMail(rmaOrder.isSendEMail());
			//
			m_invoice.setM_PriceList_ID(rmaOrder.getM_PriceList_ID());
			m_invoice.setIsTaxIncluded(rmaOrder.isTaxIncluded());
			m_invoice.setC_Currency_ID(rmaOrder.getC_Currency_ID());
			m_invoice.setC_ConversionType_ID(rmaOrder.getC_ConversionType_ID());
			//
			m_invoice.setPaymentRule(rmaOrder.getPaymentRule());
			m_invoice.setC_PaymentTerm_ID(rmaOrder.getC_PaymentTerm_ID());
			m_invoice.setPOReference(rmaOrder.getPOReference());
			m_invoice.setDescription(rma.getDescription());
			m_invoice.setDateOrdered(rmaOrder.getDateOrdered());
			//
			m_invoice.setAD_OrgTrx_ID(rmaOrder.getAD_OrgTrx_ID());
			m_invoice.setC_Project_ID(rmaOrder.getC_Project_ID());
			m_invoice.setC_Campaign_ID(rmaOrder.getC_Campaign_ID());
			m_invoice.setC_Activity_ID(rmaOrder.getC_Activity_ID());
			m_invoice.setUser1_ID(rmaOrder.getUser1_ID());
			m_invoice.setUser2_ID(rmaOrder.getUser2_ID());
			//
			int C_DocTypeTarget_ID = 0;
			if (C_DocTypeTarget_ID <= 0)
			{
				MDocType odt = MDocType.get(rma.getCtx(), rma.getC_DocType_ID());
				if (odt != null)
				{
					C_DocTypeTarget_ID = odt.getC_DocTypeInvoice_ID();
					if (C_DocTypeTarget_ID <= 0)
						throw new AdempiereException("@NotFound@ @C_DocTypeInvoice_ID@ - @C_DocType_ID@:"+odt.get_Translation(MDocType.COLUMNNAME_Name));
				}
			}
			m_invoice.setC_DocTypeTarget_ID(C_DocTypeTarget_ID);
			if (p_DateInvoiced != null)
				m_invoice.setDateInvoiced(p_DateInvoiced);
			m_invoice.setDateAcct(m_invoice.getDateInvoiced());
			//
			m_invoice.setSalesRep_ID(rma.getSalesRep_ID());
			//
			m_invoice.setC_BPartner_ID(rmaOrder.getBill_BPartner_ID());
			m_invoice.setC_BPartner_Location_ID(rmaOrder.getBill_Location_ID());
			m_invoice.setAD_User_ID(rmaOrder.getBill_User_ID());
			
			if (!m_invoice.save())
				throw new IllegalStateException("Could not create RMA Invoice (s)");
		}
		//	Create Shipment Comment Line
		if (m_ship == null 
			|| m_ship.getM_InOut_ID() != ship.getM_InOut_ID())
		{
			MDocType dt = MDocType.get(getCtx(), ship.getC_DocType_ID());
			if (m_bp == null || m_bp.getC_BPartner_ID() != ship.getC_BPartner_ID())
				m_bp = new MBPartner (getCtx(), ship.getC_BPartner_ID(), get_TrxName());
			
			//	Reference: Delivery: 12345 - 12.12.12
			MClient client = MClient.get(getCtx(), rma.getAD_Client_ID ());
			String AD_Language = client.getAD_Language();
			if (client.isMultiLingualDocument() && m_bp.getAD_Language() != null)
				AD_Language = m_bp.getAD_Language();
			if (AD_Language == null)
				AD_Language = Language.getBaseAD_Language();
			java.text.SimpleDateFormat format = DisplayType.getDateFormat 
				(DisplayType.Date, Language.getLanguage(AD_Language));
			String reference = dt.getPrintName(m_bp.getAD_Language())
				+ ": " + ship.getDocumentNo() 
				+ " - " + format.format(ship.getMovementDate());
			m_ship = ship;
			//
			MInvoiceLine line = new MInvoiceLine (m_invoice);
			line.setIsDescription(true);
			line.setDescription(reference);
			line.setLine(m_line + sLine.getLine() - 2);
			if (!line.save())
				throw new IllegalStateException("Could not create RMA Invoice Comment Line (sh)");
			//	Optional Ship Address if not Bill Address
			if (rmaOrder.getBill_Location_ID() != ship.getC_BPartner_Location_ID())
			{
				MLocation addr = MLocation.getBPLocation(getCtx(), ship.getC_BPartner_Location_ID(), null);
				line = new MInvoiceLine (m_invoice);
				line.setIsDescription(true);
				line.setDescription(addr.toString());
				line.setLine(m_line + sLine.getLine() - 1);
				if (!line.save())
					throw new IllegalStateException("Could not create RMA Invoice Comment Line 2 (sh)");
			}
		}
		//	
		MInvoiceLine line = new MInvoiceLine (m_invoice);
		line.setShipLine(sLine);
		if( sLine.getConfirmedQty() != null && sLine.getConfirmedQty().compareTo(Env.ZERO) > 0 ) {
			line.setQtyEntered(sLine.getConfirmedQty());
		} else
			if (sLine.sameOrderLineUOM())
			line.setQtyEntered(sLine.getQtyEntered());
		else
			line.setQtyEntered(sLine.getMovementQty());
		line.setQtyInvoiced(sLine.getMovementQty());
		line.setLine(m_line + sLine.getLine());
		if (!line.save())
			throw new IllegalStateException("Could not create RMA Invoice Line (s)");
		//	Link
		sLine.setIsInvoiced(true);
		if (!sLine.save())
			throw new IllegalStateException("Could not update Shipment Line");
		
		log.fine(line.toString());
	}	//	createLine

}	//	InvoiceGenerate
