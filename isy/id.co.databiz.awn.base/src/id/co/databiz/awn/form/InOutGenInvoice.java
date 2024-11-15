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

import id.co.databiz.awn.model.SystemID;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.logging.Level;

import org.compiere.apps.IStatusBar;
import org.compiere.apps.form.GenForm;
import org.compiere.minigrid.IDColumn;
import org.compiere.minigrid.IMiniTable;
import org.compiere.model.MInOut;
import org.compiere.model.MOrder;
import org.compiere.model.MPInstance;
import org.compiere.model.MPInstancePara;
import org.compiere.model.MPrivateAccess;
import org.compiere.model.MRMA;
import org.compiere.print.ReportEngine;
import org.compiere.process.ProcessInfo;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Msg;
import org.compiere.util.Trx;

/**
 * Generate Invoice (manual) controller class
 * 
 * @author Darwanto ~dar~
 */
public class InOutGenInvoice extends GenForm
{
	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(InOutGenInvoice.class);
	//
	
	public Object 			m_AD_Org_ID = null;
	public Object 			m_AD_OrgTrx_ID = null;
	public Object 			m_C_BPartner_ID = null;
	public Object			m_MovementDate = null;
	public Object			m_MovementDate_To = null;
	public Object			m_IsConsolidate = null;
	public Object			m_C_Currency_ID = null;
	//public Object			m_C_DocType_ID = null;
	//public Object			m_SumGrandTotal = null;
	/** Format                  */
	public DecimalFormat   m_format = DisplayType.getNumberFormat(DisplayType.Amount);
	
	public void dynInit() throws Exception
	{
		setTitle("InvGenerateInfo");
		//setReportEngineType(ReportEngine.INVOICE);
		//setAskPrintMsg("PrintInvoices");
	}
	
	public void configureMiniTable(IMiniTable miniTable)
	{
		//  create Columns
		miniTable.addColumn("M_InOut_ID");
		miniTable.addColumn("AD_Org_ID");
		miniTable.addColumn("AD_OrgTrx_ID");
		miniTable.addColumn("C_DocType_ID");
		miniTable.addColumn("DocumentNo");
		miniTable.addColumn("C_BPartner_ID");
		miniTable.addColumn("MovementDate");
		miniTable.addColumn("TotalLines");
		miniTable.addColumn("Order");
		//
		miniTable.setMultiSelection(true);
		//  set details
		miniTable.setColumnClass(0, IDColumn.class, false, " ");
		miniTable.setColumnClass(1, String.class, true, Msg.translate(Env.getCtx(), "AD_Org_ID"));
		miniTable.setColumnClass(2, String.class, true, Msg.translate(Env.getCtx(), "AD_OrgTrx_ID"));
		miniTable.setColumnClass(3, String.class, true, Msg.translate(Env.getCtx(), "C_DocType_ID"));
		miniTable.setColumnClass(4, String.class, true, Msg.translate(Env.getCtx(), "DocumentNo"));
		miniTable.setColumnClass(5, String.class, true, Msg.translate(Env.getCtx(), "C_BPartner_ID"));
		miniTable.setColumnClass(6, Timestamp.class, true, Msg.translate(Env.getCtx(), "MovementDate"));
		miniTable.setColumnClass(7, BigDecimal.class, true, Msg.translate(Env.getCtx(), "TotalLines"));
		miniTable.setColumnClass(8, String.class, true, "Order");
		//
		miniTable.autoSize();
	}
	
	/**
	 * Get SQL for Orders that needs to be shipped
	 * @return sql
	 */
	private String getOrderSQL()
	{
	    StringBuffer sql = new StringBuffer(
	            "SELECT ic.M_InOut_ID, o.Name, trx.Name, dt.Name, ic.DocumentNo, bp.Name, ic.MovementDate, ic.TotalLines "
	            + ", ic.C_Currency_ID,z.DocumentNo As OrderNo  "
	            + "FROM Z_Invoice_InOutCandidate_v ic "
	            + "join M_InOut io on io.m_inout_id = ic.m_inout_id  "
	            + "join AD_Org o on o.ad_org_id = ic.ad_org_id  "
	            + "join C_BPartner bp on bp.c_bpartner_id = ic.c_bpartner_id  "
	            + "join C_DocType dt on dt.c_doctype_id = ic.c_doctype_id  "
	            + "join C_Order z on z.c_order_id = ic.c_order_id  "
	            + "left join ad_org trx on trx.ad_org_id = ic.ad_orgtrx_id  "
	            + " where ic.AD_Client_ID=? ");
//	            + " AND isDocTypeInternal(dt.C_DocType_ID)='N'");	//	~dar~
	            //+ " AND SUBSTR(dt.NAME, 3, 2) = '10'");	//	20110727 ~dar~
	            //+ " AND NOT EXISTS (SELECT * FROM C_Invoice i"
	            //+ " WHERE io.C_Order_ID=i.C_Order_ID AND i.DocStatus IN ('IP', 'CO', 'CL')) ");

        if (m_AD_Org_ID != null)
            sql.append(" AND ic.AD_Org_ID=").append(m_AD_Org_ID);
        if (m_AD_OrgTrx_ID != null)
            sql.append(" AND ic.AD_OrgTrx_ID=").append(m_AD_OrgTrx_ID);
        if (m_C_BPartner_ID != null)
            sql.append(" AND ic.C_BPartner_ID=").append(m_C_BPartner_ID);
        if( m_MovementDate != null )
        	sql.append(" AND ic.MovementDate >=").append(DB.TO_DATE((Timestamp)m_MovementDate));
        if( m_MovementDate_To != null )
        	sql.append(" AND ic.MovementDate <=").append(DB.TO_DATE((Timestamp)m_MovementDate_To));
        if (m_C_Currency_ID != null)
            sql.append(" AND ic.C_Currency_ID=").append(m_C_Currency_ID);
        
        // bug - [ 1713337 ] "Generate Invoices (manual)" show locked records.
        /* begin - Exclude locked records; @Trifon */
        int AD_User_ID = Env.getContextAsInt(Env.getCtx(), "#AD_User_ID");
        String lockedIDs = MPrivateAccess.getLockedRecordWhere(MOrder.Table_ID, AD_User_ID);
        if (lockedIDs != null)
        {
            if (sql.length() > 0)
                sql.append(" AND ");
            sql.append("ic.C_Order_ID").append(lockedIDs);
        }
        /* eng - Exclude locked records; @Trifon */

        //
        sql.append(" ORDER BY o.Name,bp.Name,MovementDate");
        
        return sql.toString();
	}
	
	/**
	 * Get SQL for Customer RMA that need to be invoiced
	 * @return sql
	 */
	private String getRMASql()
	{
		StringBuffer sql = new StringBuffer(
	            "SELECT ic.M_InOut_ID, o.Name, trx.Name, dt.Name, ic.DocumentNo, bp.Name, ic.MovementDate, ic.TotalLines,z.DocumentNo As OrderNo  "
	            + "FROM z_invoice_inoutreturncandidate_v ic, M_InOut io, AD_Org o, C_BPartner bp, C_DocType dt, M_RMA z, ad_org trx "
	            + "WHERE ic.AD_Org_ID=o.AD_Org_ID "
	            + " and ic.ad_orgtrx_id=trx.ad_org_id  "
	            + " AND ic.M_InOut_ID=io.M_InOut_ID "
	            + " AND ic.C_BPartner_ID=bp.C_BPartner_ID "
	            + " AND ic.C_DocType_ID=dt.C_DocType_ID "
	            + " AND ic.M_RMA_ID=z.M_RMA_ID"
	            + " AND ic.AD_Client_ID=? ");
	            //+ " AND NOT EXISTS (SELECT * FROM C_Invoice i"
	            //+ " WHERE io.C_Order_ID=i.C_Order_ID AND i.DocStatus IN ('IP', 'CO', 'CL')) ");

        if (m_AD_Org_ID != null)
            sql.append(" AND ic.AD_Org_ID=").append(m_AD_Org_ID);
        if (m_AD_OrgTrx_ID != null)
            sql.append(" AND ic.AD_OrgTrx_ID=").append(m_AD_OrgTrx_ID);
        if (m_C_BPartner_ID != null)
            sql.append(" AND ic.C_BPartner_ID=").append(m_C_BPartner_ID);
        if( m_MovementDate != null )
        	sql.append(" AND ic.MovementDate >=").append(DB.TO_DATE((Timestamp)m_MovementDate));
        if( m_MovementDate_To != null )
        	sql.append(" AND ic.MovementDate <=").append(DB.TO_DATE((Timestamp)m_MovementDate_To));
        
		//StringBuffer sql = new StringBuffer();
	    //sql.append("SELECT rma.M_RMA_ID, org.Name, dt.Name, rma.DocumentNo, bp.Name, rma.Created, rma.Amt ");
        //sql.append("FROM M_RMA rma INNER JOIN AD_Org org ON rma.AD_Org_ID=org.AD_Org_ID ");
        //sql.append("INNER JOIN C_DocType dt ON rma.C_DocType_ID=dt.C_DocType_ID ");
        //sql.append("INNER JOIN C_BPartner bp ON rma.C_BPartner_ID=bp.C_BPartner_ID ");
        //sql.append("INNER JOIN M_InOut io ON rma.InOut_ID=io.M_InOut_ID ");
        //sql.append("WHERE rma.DocStatus='CO' ");
        //sql.append("AND dt.DocBaseType = 'SOO' ");
        //sql.append("AND rma.AD_Client_ID=?");
        
        //if (m_AD_Org_ID != null)
        //    sql.append(" AND rma.AD_Org_ID=").append(m_AD_Org_ID);
        //if (m_C_BPartner_ID != null)
        //    sql.append(" AND bp.C_BPartner_ID=").append(m_C_BPartner_ID);
        
        int AD_User_ID = Env.getContextAsInt(Env.getCtx(), "#AD_User_ID");
        String lockedIDs = MPrivateAccess.getLockedRecordWhere(MInOut.Table_ID, AD_User_ID);
        if (lockedIDs != null)
        {
            sql.append(" AND io.M_InOut_ID").append(lockedIDs);
        }
        
        sql.append(" ORDER BY o.Name, bp.Name, io.Created ");
        
        return sql.toString();
	}
	
	/**
	 *  Query Info
	 */
	public void executeQuery(KeyNamePair docTypeKNPair, IMiniTable miniTable)
	{
		log.info("");
		int AD_Client_ID = Env.getAD_Client_ID(Env.getCtx());
		//  Create SQL
		
		String sql = "";
        
        if (docTypeKNPair.getKey() == MInOut.Table_ID)
        {
            sql = getOrderSQL();
        }
        else
        {
            sql = getRMASql();
        }

		//  reset table
		int row = 0;
		miniTable.setRowCount(row);
		//  Execute
		try
		{
			PreparedStatement pstmt = DB.prepareStatement(sql.toString(), null);
			pstmt.setInt(1, AD_Client_ID);
			ResultSet rs = pstmt.executeQuery();
			//
			while (rs.next())
			{
				//  extend table
				miniTable.setRowCount(row+1);
				//  set values
				miniTable.setValueAt(new IDColumn(rs.getInt(1)), row, 0);   //  M_InOut_ID
				miniTable.setValueAt(rs.getString(2), row, 1);              //  Org
				miniTable.setValueAt(rs.getString(3), row, 2);              //  OrgTrx
				miniTable.setValueAt(rs.getString(4), row, 3);              //  DocType
				miniTable.setValueAt(rs.getString(5), row, 4);              //  Doc No
				miniTable.setValueAt(rs.getString(6), row, 5);              //  BPartner
				miniTable.setValueAt(rs.getTimestamp(7), row, 6);           //  MovementDate
				miniTable.setValueAt(rs.getBigDecimal(8), row, 7);          //  TotalLines
				miniTable.setValueAt(rs.getString("OrderNo"), row, 8);          //  Order
				//  prepare next
				row++;
			}
			rs.close();
			pstmt.close();
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, sql.toString(), e);
		}
		//
		miniTable.autoSize();
	//	statusBar.setStatusDB(String.valueOf(miniTable.getRowCount()));
	}   //  executeQuery
	
	/**
	 *	Save Selection & return selecion Query or ""
	 *  @return where clause like C_Order_ID IN (...)
	 */
	public void saveSelection(IMiniTable miniTable)
	{
		log.info("");
		//  Array of Integers
		ArrayList<Integer> results = new ArrayList<Integer>();
		setSelection(null);

		//	Get selected entries
		int rows = miniTable.getRowCount();
		//BigDecimal ttlLines = Env.ZERO;
		for (int i = 0; i < rows; i++)
		{
			IDColumn id = (IDColumn)miniTable.getValueAt(i, 0);     //  ID in column 0
		//	log.fine( "Row=" + i + " - " + id);
			if (id != null && id.isSelected()) {
				//BigDecimal ttlLine = (BigDecimal)miniTable.getValueAt(i, 6);
				//if( ttlLine != null )
				//	ttlLines = ttlLines.add(ttlLine);	// Total Lines in Column 6
				results.add(id.getRecord_ID());
			}
		}
		//m_SumGrandTotal = "Total Selected = " + m_format.format(ttlLines);
		if (results.size() == 0)
			return;
		log.config("Selected #" + results.size());
		setSelection(results);
	}	//	saveSelection

	
	/**************************************************************************
	 *	Generate Invoices
	 */
	public String generate(IStatusBar statusBar, KeyNamePair docTypeKNPair, String docActionSelected)
	{
		String info = "";
		String trxName = Trx.createTrxName("IVG");
		Trx trx = Trx.get(trxName, true);	//trx needs to be committed too
		
		setSelectionActive(false);  //  prevents from being called twice
		statusBar.setStatusLine(Msg.getMsg(Env.getCtx(), "InvGenerateGen"));
		statusBar.setStatusDB(String.valueOf(getSelection().size()));

		//	Prepare Process
		int AD_Process_ID = 0;
		//m_C_DocType_ID = docTypeKNPair.getKey();
        if (docTypeKNPair.getKey() == MRMA.Table_ID)
        {
            AD_Process_ID = SystemID.PROCESS_SHIPMENT_RETURN_GENERATE_INVOICES; //~dar~
        }
        else
        {
            AD_Process_ID = SystemID.PROCESS_SHIPMENT_GENERATE_INVOICES;  // HARDCODED    Z InOut Generate Invoice (manual) ~dar~
        }
		MPInstance instance = new MPInstance(Env.getCtx(), AD_Process_ID, 0);
		if (!instance.save())
		{
			info = Msg.getMsg(Env.getCtx(), "ProcessNoInstance");
			return info;
		}
		
		//insert selection
		StringBuffer insert = new StringBuffer();
		insert.append("INSERT INTO T_SELECTION(AD_PINSTANCE_ID, T_SELECTION_ID) ");
		int counter = 0;
		for(Integer selectedId : getSelection())
		{
			counter++;
			if (counter > 1)
				insert.append(" UNION ");
			insert.append("SELECT ");
			insert.append(instance.getAD_PInstance_ID());
			insert.append(", ");
			insert.append(selectedId);
			insert.append(" FROM DUAL ");
			
			if (counter == 1000) 
			{
				if ( DB.executeUpdate(insert.toString(), trxName) < 0 )
				{
					String msg = "No Invoices";     //  not translated!
					info = msg;
					log.config(msg);
					trx.rollback();
					return info;
				}
				insert = new StringBuffer();
				insert.append("INSERT INTO T_SELECTION(AD_PINSTANCE_ID, T_SELECTION_ID) ");
				counter = 0;
			}
		}
		if (counter > 0)
		{
			if ( DB.executeUpdate(insert.toString(), trxName) < 0 )
			{
				String msg = "No Invoices";     //  not translated!
				info = msg;
				log.config(msg);
				trx.rollback();
				return info;
			}
		}
		
		ProcessInfo pi = new ProcessInfo ("", AD_Process_ID);
		pi.setAD_PInstance_ID (instance.getAD_PInstance_ID());

		//	Add Parameters
		MPInstancePara para = new MPInstancePara(instance, 10);
		para.setParameter("Selection", "Y");
		if (!para.save())
		{
			String msg = "No Selection Parameter added";  //  not translated
			info = msg;
			log.log(Level.SEVERE, msg);
			return info;
		}
		
		para = new MPInstancePara(instance, 20);
		para.setParameter("DocAction", docActionSelected);
		
		if (!para.save())
		{
			String msg = "No DocAction Parameter added";  //  not translated
			info = msg;
			log.log(Level.SEVERE, msg);
			return info;
		}

		para = new MPInstancePara(instance, 30);
		if( m_IsConsolidate == null ) m_IsConsolidate = "false";
		para.setParameter("ConsolidateDocument", (Boolean.parseBoolean(m_IsConsolidate.toString()) ? "Y" : "N"));
		
		if (!para.save())
		{
			String msg = "No Consolidate Document Parameter added";  //  not translated
			info = msg;
			log.log(Level.SEVERE, msg);
			return info;
		}
		setTrx(trx);
		setProcessInfo(pi);
		
		return info;
	}	//	generateInvoices

	/**
	 *  Calculate selected rows.
	 *  - add up selected rows
	 */
	public String calculateSelection(IMiniTable miniTable)
	{
		BigDecimal shipAmt = new BigDecimal(0.0);

		int rows = miniTable.getRowCount();
		for (int i = 0; i < rows; i++)
		{
			IDColumn id = (IDColumn)miniTable.getValueAt(i, 0);
			if (id.isSelected())
			{
				BigDecimal amt = (BigDecimal)miniTable.getValueAt(i, 7);
				if (amt != null)
					shipAmt = shipAmt.add(amt);
			}
		}

		//  Information
		String m_SumGrandTotal = m_format.format(shipAmt);
		return m_SumGrandTotal;
	}   //  calculateSelection
	
	public int getReportEngineType() {
		return ReportEngine.INVOICE;
	}

}