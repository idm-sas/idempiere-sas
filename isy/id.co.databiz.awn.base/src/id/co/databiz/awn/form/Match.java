/******************************************************************************
 * Copyright (C) 2009 Low Heng Sin                                            *
 * Copyright (C) 2009 Idalica Corporation                                     *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/
package id.co.databiz.awn.form;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Vector;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.minigrid.IDColumn;
import org.compiere.minigrid.IMiniTable;
import org.compiere.model.MClient;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MMatchInv;
import org.compiere.model.MMatchPO;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MRole;
import org.compiere.model.MStorageReservation;
import org.compiere.process.DocumentEngine;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.compiere.util.ValueNamePair;

public class Match
{

	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(Match.class);

	/** Match Options           */
	private String[] m_matchOptions = new String[] {
		Msg.getElement(Env.getCtx(), "C_Invoice_ID", false),
		Msg.getElement(Env.getCtx(), "M_InOut_ID", false),
		Msg.getElement(Env.getCtx(), "C_Order_ID", false) };
	private static final int		MATCH_INVOICE = 0;
	private static final int		MATCH_SHIPMENT = 1;
	private static final int		MATCH_ORDER = 2;

	private static final int		MODE_NOTMATCHED = 0;
	//private static final int		MODE_MATCHED = 1;

	/**	Indexes in Table			*/
	private static final int		I_BPartner = 3;
	private static final int		I_Line = 4;
	private static final int		I_Product = 5;
	private static final int		I_QTY = 6;
	private static final int		I_MATCHED = 7;
	//private static final int        I_Org = 8; //JAVIER 
	


	private StringBuffer    m_sql = null;
	private String          m_dateColumn = "";
	private String          m_qtyColumn = "";
	private String          m_groupBy = "";
	private StringBuffer	m_linetype = null;
	//private BigDecimal      m_xMatched = Env.ZERO;
	//private BigDecimal      m_xMatchedTo = Env.ZERO;

	
	/**
	 *  Match From Changed - Fill Match To
	 */
	protected Vector<String> cmd_matchFrom(String selection)
	{
	//	if (log.isLoggable(Level.FINE)) log.fine( "VMatch.cmd_matchFrom");
		//String selection = (String)matchFrom.getSelectedItem();
		Vector<String> vector = new Vector<String>(2);
		if (selection.equals(m_matchOptions[MATCH_INVOICE]))
			vector.add(m_matchOptions[MATCH_SHIPMENT]);
		else if (selection.equals(m_matchOptions[MATCH_ORDER]))
			vector.add(m_matchOptions[MATCH_SHIPMENT]);
		else    //  shipment
		{
			vector.add(m_matchOptions[MATCH_INVOICE]);
			vector.add(m_matchOptions[MATCH_ORDER]);
		}
		return vector;
	}   //  cmd_matchFrom

	
	/**
	 *  Search Button Pressed - Fill xMatched
	 */
	protected IMiniTable cmd_search(IMiniTable xMatchedTable, int display, String matchToString, Integer Product, Integer Vendor, Timestamp from, Timestamp to, boolean matched)
	{
		//  ** Create SQL **
		//int display = matchFrom.getSelectedIndex();
		//String matchToString = (String)matchTo.getSelectedItem();
		int matchToType = MATCH_INVOICE;
		if (matchToString.equals(m_matchOptions[MATCH_SHIPMENT]))
			matchToType = MATCH_SHIPMENT;
		else if (matchToString.equals(m_matchOptions[MATCH_ORDER]))
			matchToType = MATCH_ORDER;
		//
		tableInit(display, matchToType, matched, 0, "");	//	sets m_sql

		//  ** Add Where Clause **
		//  Product
		if (Product != null)
		{
			//Integer Product = (Integer)onlyProduct.getValue();
			m_sql.append(" AND lin.M_Product_ID=").append(Product);
		}
		//  BPartner
		if (Vendor != null)
		{
			//Integer Vendor = (Integer)onlyVendor.getValue();
			m_sql.append(" AND hdr.C_BPartner_ID=").append(Vendor);
		}
		//  Date
		//Timestamp from = (Timestamp)dateFrom.getValue();
		//Timestamp to = (Timestamp)dateTo.getValue();
		if (from != null && to != null)
			m_sql.append(" AND ").append(m_dateColumn).append(" BETWEEN ")
				.append(DB.TO_DATE(from)).append(" AND ").append(DB.TO_DATE(to));
		else if (from != null)
			m_sql.append(" AND ").append(m_dateColumn).append(" >= ").append(DB.TO_DATE(from));
		else if (to != null)
			m_sql.append(" AND ").append(m_dateColumn).append(" <= ").append(DB.TO_DATE(to));
		
		//  ** Load Table **
		tableLoad (xMatchedTable);
		return xMatchedTable;

	}   //  cmd_search


	/**
	 *  Process Button Pressed - Process Matching
	 */
	protected void cmd_process(IMiniTable xMatchedTable, IMiniTable xMatchedToTable, int matchMode, int matchFrom, Object matchTo, BigDecimal m_xMatched)
	{
		log.config("");
		//  Matched From
		int matchedRow = xMatchedTable.getSelectedRow();
		if (matchedRow < 0)
			return;
	//	KeyNamePair BPartner = (KeyNamePair)xMatchedTable.getValueAt(matchedRow, I_BPartner);
		KeyNamePair lineMatched = (KeyNamePair)xMatchedTable.getValueAt(matchedRow, I_Line);
		KeyNamePair Product = (KeyNamePair)xMatchedTable.getValueAt(matchedRow, I_Product);

		double totalQty = m_xMatched.doubleValue();

		//  Matched To
		for (int row = 0; row < xMatchedToTable.getRowCount(); row++)
		{
			IDColumn id = (IDColumn)xMatchedToTable.getValueAt(row, 0);
			if (id != null && id.isSelected())
			{
				//  need to be the same product
				KeyNamePair ProductCompare = (KeyNamePair)xMatchedToTable.getValueAt(row, I_Product);
				if (Product.getKey() != ProductCompare.getKey())
					continue;

				KeyNamePair lineMatchedTo = (KeyNamePair)xMatchedToTable.getValueAt(row, I_Line);

				//	Qty
				double qty = 0.0;
				if (matchMode == MODE_NOTMATCHED)
					qty = ((Double)xMatchedToTable.getValueAt(row, I_QTY)).doubleValue();	//  doc
				qty -= ((Double)xMatchedToTable.getValueAt(row, I_MATCHED)).doubleValue();  //  matched
				if (qty > totalQty)
					qty = totalQty;
				totalQty -= qty;

				//  Invoice or PO
				boolean invoice = true;
				if (matchFrom == MATCH_ORDER ||
						matchTo.equals(m_matchOptions[MATCH_ORDER]))
					invoice = false;
				//  Get Shipment_ID
				int M_InOutLine_ID = 0;
				int Line_ID = 0;
				if (matchFrom == MATCH_SHIPMENT)
				{
					M_InOutLine_ID = lineMatched.getKey();      //  upper table
					Line_ID = lineMatchedTo.getKey();
				}
				else
				{
					M_InOutLine_ID = lineMatchedTo.getKey();    //  lower table
					Line_ID = lineMatched.getKey();
				}

				//  Create it
				String innerTrxName = Trx.createTrxName("Match");
				Trx innerTrx = Trx.get(innerTrxName, true);
				innerTrx.setDisplayName(getClass().getName()+"_cmd_process");
				
				try{
					if (createMatchRecord(invoice, M_InOutLine_ID, Line_ID, BigDecimal.valueOf(qty), innerTrxName))
						innerTrx.commit();
					else
						innerTrx.rollback();
				}catch(Exception ex){
					innerTrx.rollback();
					throw new AdempiereException(ex);
				}finally{
					innerTrx.close();
					innerTrx = null;
				}
			}
		}
		//  requery
		//cmd_search();
	}   //  cmd_process
	

	/**
	 *  Fill xMatchedTo
	 */
	protected IMiniTable cmd_searchTo(IMiniTable xMatchedTable, IMiniTable xMatchedToTable, String displayString, int matchToType, boolean sameBPartner, boolean sameProduct, boolean sameQty, boolean matched)
	{
		int row = xMatchedTable.getSelectedRow();
		if (log.isLoggable(Level.CONFIG)) log.config("Row=" + row);

		//  ** Create SQL **
		//String displayString = (String)matchTo.getSelectedItem();
		int display = MATCH_INVOICE;
		if (displayString.equals(m_matchOptions[MATCH_SHIPMENT]))
			display = MATCH_SHIPMENT;
		else if (displayString.equals(m_matchOptions[MATCH_ORDER]))
			display = MATCH_ORDER;
		
		String tableName = MInvoice.Table_Name;
		if (matchToType == MATCH_SHIPMENT){
			tableName = MInOut.Table_Name;
		}
		else if (matchToType == MATCH_ORDER){
			tableName = MOrder.Table_Name;
		}
		
		IDColumn idColumn = (IDColumn) xMatchedTable.getValueAt(row, 0);
		KeyNamePair line = (KeyNamePair) xMatchedTable.getValueAt(row, I_Line);
		String sql = "SELECT " + tableName + "Line_ID FROM " + tableName + "Line WHERE Line = ? AND " + tableName + "_ID = ?";
		int lineID = DB.getSQLValue(null, sql, Integer.valueOf(line.getName()),idColumn.getRecord_ID());
		
		//int matchToType = matchFrom.getSelectedIndex();
		tableInit (display, matchToType, matched, lineID, tableName);	//	sets m_sql
		//  ** Add Where Clause **
		KeyNamePair BPartner = (KeyNamePair)xMatchedTable.getValueAt(row, I_BPartner);
		//KeyNamePair Org = (KeyNamePair)xMatchedTable.getValueAt(row, I_Org); //JAVIER
		KeyNamePair Product = (KeyNamePair)xMatchedTable.getValueAt(row, I_Product);
		if (log.isLoggable(Level.FINE)) log.fine("BPartner=" + BPartner + " - Product=" + Product);
		//
		if (sameBPartner)
			m_sql.append(" AND hdr.C_BPartner_ID=").append(BPartner.getKey());
		if (sameProduct)
			m_sql.append(" AND lin.M_Product_ID=").append(Product.getKey());

		//  calculate qty
		double docQty = ((Double)xMatchedTable.getValueAt(row, I_QTY)).doubleValue();
		if (sameQty)
			m_sql.append(" AND ").append(m_qtyColumn).append("=").append(docQty);
		//  ** Load Table **
		tableLoad (xMatchedToTable);

		return xMatchedToTable;
	}   //  cmd_seachTo
	
	/**************************************************************************
	 *  Initialize Table access - create SQL, dateColumn.
	 *  <br>
	 *  The driving table is "hdr", e.g. for hdr.C_BPartner_ID=..
	 *  The line table is "lin", e.g. for lin.M_Product_ID=..
	 *  You use the dateColumn/qtyColumn variable directly as it is table specific.
	 *  <br>
	 *  The sql is dependent on MatchMode:
	 *  - If Matched - all (fully or partially) matched records are listed
	 *  - If Not Matched - all not fully matched records are listed
	 *  @param display (Invoice, Shipment, Order) see MATCH_*
	 *  @param matchToType (Invoice, Shipment, Order) see MATCH_*
	 */
	protected void tableInit (int display, int matchToType, boolean matched, int lineID, String tableName)
	{
		//boolean matched = matchMode.getSelectedIndex() == MODE_MATCHED;
		log.config("Display=" + m_matchOptions[display]
			+ ", MatchTo=" + m_matchOptions[matchToType]
			+ ", Matched=" + matched);

		m_sql = new StringBuffer ();
		if (display == MATCH_INVOICE)
		{
			m_dateColumn = "hdr.DateInvoiced";
			m_qtyColumn = "lin.QtyInvoiced";
			m_sql.append("SELECT hdr.C_Invoice_ID,hdr.DocumentNo, hdr.DateInvoiced, bp.Name,hdr.C_BPartner_ID,"
				+ " lin.Line,lin.C_InvoiceLine_ID, p.Name,lin.M_Product_ID,"
				+ " lin.QtyInvoiced,SUM(NVL(mi.Qty,0)), org.Name, hdr.AD_Org_ID "  //JAVIER
				+ "FROM C_Invoice hdr"
				+ " INNER JOIN AD_Org org ON (hdr.AD_Org_ID=org.AD_Org_ID)" //JAVIER
				+ " INNER JOIN C_BPartner bp ON (hdr.C_BPartner_ID=bp.C_BPartner_ID)"
				+ " INNER JOIN C_InvoiceLine lin ON (hdr.C_Invoice_ID=lin.C_Invoice_ID)"
				+ " INNER JOIN M_Product p ON (lin.M_Product_ID=p.M_Product_ID)"
				+ " INNER JOIN C_DocType dt ON (hdr.C_DocType_ID=dt.C_DocType_ID AND dt.DocBaseType IN ('API','APC'))"
				+ " FULL JOIN M_MatchInv mi ON (lin.C_InvoiceLine_ID=mi.C_InvoiceLine_ID) "
				+ "WHERE hdr.DocStatus IN ('CO','CL')");
			if(matched && lineID > 0 && !tableName.isEmpty()){
				m_sql.append(" AND mi.");
				m_sql.append(tableName);
				m_sql.append("Line_ID = ");
				m_sql.append(lineID);
			}
			m_groupBy = " GROUP BY hdr.C_Invoice_ID,hdr.DocumentNo,hdr.DateInvoiced,bp.Name,hdr.C_BPartner_ID,"
				+ " lin.Line,lin.C_InvoiceLine_ID,p.Name,lin.M_Product_ID,lin.QtyInvoiced, org.Name, hdr.AD_Org_ID " //JAVIER
				+ "HAVING "
				+ (matched ? "0" : "lin.QtyInvoiced")
				+ "<>SUM(NVL(mi.Qty,0)) ORDER BY SUM(NVL(mi.Qty,0))";
		}
		else if (display == MATCH_ORDER)
		{
			m_dateColumn = "hdr.DateOrdered";
			m_qtyColumn = "lin.QtyOrdered";
			m_sql.append("SELECT hdr.C_Order_ID,hdr.DocumentNo, hdr.DateOrdered, bp.Name,hdr.C_BPartner_ID,"
				+ " lin.Line,lin.C_OrderLine_ID, p.Name,lin.M_Product_ID,"
				+ " lin.QtyOrdered,SUM(COALESCE(mo.Qty,0)) Qty, org.Name, hdr.AD_Org_ID " //JAVIER
				+ "FROM C_Order hdr"
				+ " INNER JOIN AD_Org org ON (hdr.AD_Org_ID=org.AD_Org_ID)" //JAVIER
				+ " INNER JOIN C_BPartner bp ON (hdr.C_BPartner_ID=bp.C_BPartner_ID)"
				+ " INNER JOIN C_OrderLine lin ON (hdr.C_Order_ID=lin.C_Order_ID)"
				+ " INNER JOIN M_Product p ON (lin.M_Product_ID=p.M_Product_ID)"
				+ " INNER JOIN C_DocType dt ON (hdr.C_DocType_ID=dt.C_DocType_ID AND dt.DocBaseType='POO')"
				+ " FULL JOIN M_MatchPO mo ON (lin.C_OrderLine_ID=mo.C_OrderLine_ID AND mo.M_InOutLine_ID > 0) "
				+ " WHERE " ) ; //[ 1876972 ] Can't match partially matched PO with an unmatched receipt SOLVED BY BOJANA, AGENDA_GM
			m_linetype = new StringBuffer();
			m_linetype.append( matchToType == MATCH_SHIPMENT ? "M_InOutLine_ID" : "C_InvoiceLine_ID") ;
			if ( matched ) {
				m_sql.append( " mo." + m_linetype + " IS NOT NULL AND "
				+ " (lin.QtyOrdered >  (SELECT sum(mo1.Qty) AS Qty" 
				+ " FROM m_matchpo mo1 WHERE "
				+ " mo1.C_ORDERLINE_ID=lin.C_ORDERLINE_ID AND "
				+ " hdr.C_ORDER_ID=lin.C_ORDER_ID AND "
				+ " mo1." + m_linetype
				+ " IS NOT NULL group by mo1.C_ORDERLINE_ID))) " );	 
			} else {
 				m_sql.append( " ( mo." + m_linetype + " IS NULL OR "
				+ " (lin.QtyOrdered >  (SELECT sum(mo1.Qty) AS Qty" 
				+ " FROM m_matchpo mo1 WHERE "
				+ " mo1.C_ORDERLINE_ID=lin.C_ORDERLINE_ID AND "
				+ " hdr.C_ORDER_ID=lin.C_ORDER_ID AND "
				+ " mo1." + m_linetype
				+ " IS NOT NULL group by mo1.C_ORDERLINE_ID))) " );	
			}
			m_sql.append( " AND hdr.DocStatus IN ('CO','CL')" );
			if(matched && lineID > 0 && !tableName.isEmpty()){
				m_sql.append(" AND mo.");
				m_sql.append(tableName);
				m_sql.append("Line_ID = ");
				m_sql.append(lineID);
			}
			m_groupBy = " GROUP BY hdr.C_Order_ID,hdr.DocumentNo,hdr.DateOrdered,bp.Name,hdr.C_BPartner_ID,"
				+ " lin.Line,lin.C_OrderLine_ID,p.Name,lin.M_Product_ID,lin.QtyOrdered, org.Name, hdr.AD_Org_ID " //JAVIER
				+ "HAVING "
				+ (matched ? "0" : "lin.QtyOrdered")
				+ "<>SUM(COALESCE(mo.Qty,0)) ORDER BY SUM(COALESCE(mo.Qty,0))";
		}
		else    //  Shipment
		{
			m_dateColumn = "hdr.MovementDate";
			m_qtyColumn = "lin.MovementQty";
			m_sql.append("SELECT hdr.M_InOut_ID,hdr.DocumentNo, hdr.MovementDate, bp.Name,hdr.C_BPartner_ID,"
				+ " lin.Line,lin.M_InOutLine_ID, p.Name,lin.M_Product_ID,"
				+ " lin.MovementQty,SUM(NVL(m.Qty,0)),org.Name, hdr.AD_Org_ID " //JAVIER
				+ "FROM M_InOut hdr"
				+ " INNER JOIN AD_Org org ON (hdr.AD_Org_ID=org.AD_Org_ID)" //JAVIER
				+ " INNER JOIN C_BPartner bp ON (hdr.C_BPartner_ID=bp.C_BPartner_ID)"
				+ " INNER JOIN M_InOutLine lin ON (hdr.M_InOut_ID=lin.M_InOut_ID)"
				+ " INNER JOIN M_Product p ON (lin.M_Product_ID=p.M_Product_ID)"
//				+ " INNER JOIN C_DocType dt ON (hdr.C_DocType_ID = dt.C_DocType_ID AND dt.DocBaseType='MMR')"
				+ " INNER JOIN C_DocType dt ON (hdr.C_DocType_ID = dt.C_DocType_ID)" // ISY-417
				+ " FULL JOIN ")
				.append(matchToType == MATCH_ORDER ? "M_MatchPO" : "M_MatchInv")
				.append(" m ON (lin.M_InOutLine_ID=m.M_InOutLine_ID) "
				+ "WHERE hdr.DocStatus IN ('CO','CL') AND hdr.IsSOTrx = 'N'");
			if(matched && lineID > 0 && !tableName.isEmpty()){
				m_sql.append(" AND m.");
				m_sql.append(tableName);
				m_sql.append("Line_ID = ");
				m_sql.append(lineID);
			}
			m_groupBy = " GROUP BY hdr.M_InOut_ID,hdr.DocumentNo,hdr.MovementDate,bp.Name,hdr.C_BPartner_ID,"
				+ " lin.Line,lin.M_InOutLine_ID,p.Name,lin.M_Product_ID,lin.MovementQty, org.Name, hdr.AD_Org_ID " //JAVIER
				+ "HAVING "
				+ (matched ? "0" : "lin.MovementQty")
				+ "<>SUM(NVL(m.Qty,0)) ORDER BY SUM(NVL(m.Qty,0))";
		}
	//	Log.trace(7, "VMatch.tableInit", m_sql + "\n" + m_groupBy);
	}   //  tableInit


	/**
	 *  Fill the table using m_sql
	 *  @param table table
	 */
	protected void tableLoad (IMiniTable table)
	{
		//	log.finest(m_sql + " - " +  m_groupBy);
		String sql = MRole.getDefault().addAccessSQL(
			m_sql.toString(), "hdr", MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
			+ m_groupBy;
		log.finest(sql);
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			stmt = DB.createStatement();
			rs = stmt.executeQuery(sql);
			table.loadTable(rs);
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, sql, e);
		}
		finally
		{
			DB.close(rs,stmt);
			rs = null;stmt = null;
		}
	}   //  tableLoad

	/**
	 *  Create Matching Record
	 *  @param invoice true if matching invoice false if matching PO
	 *  @param M_InOutLine_ID shipment line
	 *  @param Line_ID C_InvoiceLine_ID or C_OrderLine_ID
	 *  @param qty quantity
	 *  @param trxName 
	 *  @return true if created
	 */
	protected boolean createMatchRecord (boolean invoice, int M_InOutLine_ID, int Line_ID,
		BigDecimal qty, String trxName)
	{
		if (qty.compareTo(Env.ZERO) == 0)
			return true;
		if (log.isLoggable(Level.FINE)) log.fine("IsInvoice=" + invoice
			+ ", M_InOutLine_ID=" + M_InOutLine_ID + ", Line_ID=" + Line_ID
			+ ", Qty=" + qty);
		//
		boolean success = false;
		MInOutLine sLine = new MInOutLine (Env.getCtx(), M_InOutLine_ID, trxName);
		if (invoice)	//	Shipment - Invoice
		{
			//	Update Invoice Line
			MInvoiceLine iLine = new MInvoiceLine (Env.getCtx(), Line_ID, trxName);
			iLine.setM_InOutLine_ID(M_InOutLine_ID);
			if (sLine.getC_OrderLine_ID() != 0)
				iLine.setC_OrderLine_ID(sLine.getC_OrderLine_ID());
			iLine.saveEx();
			//	Create Shipment - Invoice Link
			if (iLine.getM_Product_ID() != 0)
			{
				MMatchInv match = new MMatchInv (iLine, null, qty);
				match.setM_InOutLine_ID(M_InOutLine_ID);
				match.saveEx();
				success = true;
				if (MClient.isClientAccountingImmediate()) {
					String ignoreError = DocumentEngine.postImmediate(match.getCtx(), match.getAD_Client_ID(), match.get_Table_ID(), match.get_ID(), true, match.get_TrxName());						
					if (ignoreError != null) {
						log.info(ignoreError);
					}
				}
			}
			else
				success = true;
			//	Create PO - Invoice Link = corrects PO
			if (iLine.getM_Product_ID() != 0)
			{
				BigDecimal matchedQty = DB.getSQLValueBD(iLine.get_TrxName(), "SELECT Coalesce(SUM(Qty),0) FROM M_MatchPO WHERE C_InvoiceLine_ID=?" , iLine.getC_InvoiceLine_ID());
				if (matchedQty.add(qty).compareTo(iLine.getQtyInvoiced()) <= 0) 
				{
					MMatchPO matchPO = MMatchPO.create(iLine, sLine, null, qty);
					if (matchPO != null)
					{
						matchPO.saveEx();
						if (MClient.isClientAccountingImmediate()) {
							String ignoreError = DocumentEngine.postImmediate(matchPO.getCtx(), matchPO.getAD_Client_ID(), matchPO.get_Table_ID(), matchPO.get_ID(), true, matchPO.get_TrxName());						
							if (ignoreError != null)
								log.info(ignoreError);
						}
					}
				}
			}
		}
		else	//	Shipment - Order
		{
			//	Update Order Line
			MOrderLine oLine = new MOrderLine(Env.getCtx(), Line_ID, trxName);
			if (oLine.get_ID() != 0)	//	other in MInOut.completeIt
			{
				oLine.setQtyReserved(oLine.getQtyReserved().subtract(qty));
				oLine.saveEx();
			}

			// Update Shipment Line
			BigDecimal toDeliver = oLine.getQtyOrdered().subtract(oLine.getQtyDelivered());
			if (sLine.getMovementQty().compareTo(toDeliver) <= 0)
			{
				sLine.setC_OrderLine_ID(Line_ID);
				sLine.saveEx();
			}
			else if (sLine.getC_OrderLine_ID() != 0)
			{ 
				sLine.setC_OrderLine_ID(0);
				sLine.saveEx();
			}

			//	Create PO - Shipment Link
			if (sLine.getM_Product_ID() != 0)
			{
				MMatchPO match = new MMatchPO (sLine, null, qty);
				match.setC_OrderLine_ID(Line_ID);
				if (!match.save())
				{
					String msg = "PO Match not created: " + match;
					ValueNamePair error = CLogger.retrieveError();
					if (error != null)
					{
						msg = msg + ". " + error.getName();
					}
					throw new AdempiereException(msg);
				}	
				else
				{
					success = true;
					//	Correct Ordered Qty for Stocked Products (see MOrder.reserveStock / MInOut.processIt)
					if (sLine.getProduct() != null && sLine.getProduct().isStocked())
						success = MStorageReservation.add (Env.getCtx(), sLine.getM_Warehouse_ID(),
							sLine.getM_Product_ID(),
							sLine.getM_AttributeSetInstance_ID(),
							qty.negate(), false, trxName);
				}
			}
			else
				success = true;
		}
		return success;
	}   //  createMatchRecord
}
