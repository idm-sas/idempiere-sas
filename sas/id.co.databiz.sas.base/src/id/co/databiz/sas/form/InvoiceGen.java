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
package id.co.databiz.sas.form;

import static org.compiere.model.SystemIDs.PROCESS_C_INVOICE_GENERATERMA_MANUAL;
import static org.compiere.model.SystemIDs.PROCESS_C_INVOICE_GENERATE_MANUAL;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.logging.Level;

import org.compiere.apps.IStatusBar;
import org.compiere.apps.form.GenForm;
import org.compiere.minigrid.IDColumn;
import org.compiere.minigrid.IMiniTable;
import org.compiere.model.MOrder;
import org.compiere.model.MPInstance;
import org.compiere.model.MPInstancePara;
import org.compiere.model.MRMA;
import org.compiere.model.MRole;
import org.compiere.print.ReportEngine;
import org.compiere.process.ProcessInfo;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Msg;
import org.compiere.util.Trx;

/**
 * Generate Invoice (manual) controller class
 * 
 */
public class InvoiceGen extends GenForm
{
	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(InvoiceGen.class);
	//
	
	public Object 			m_AD_Org_ID = null;
	public Object 			m_C_BPartner_ID = null;
	
	public void dynInit() throws Exception
	{
		setTitle("InvGenerateInfo");
		setReportEngineType(ReportEngine.INVOICE);
		setAskPrintMsg("PrintInvoices");
	}
	
	public void configureMiniTable(IMiniTable miniTable)
	{
		//  create Columns
		miniTable.addColumn("C_Order_ID");
		miniTable.addColumn("AD_Org_ID");
		miniTable.addColumn("C_DocType_ID");
		miniTable.addColumn("DocumentNo");
		miniTable.addColumn("C_BPartner_ID");
		miniTable.addColumn("Bill_Location_ID");
		miniTable.addColumn("DateOrdered");
		miniTable.addColumn("TotalLines");
		//
		miniTable.setMultiSelection(true);
		//  set details
		int i = 0;
		miniTable.setColumnClass(i++, IDColumn.class, false, " ");
		miniTable.setColumnClass(i++, String.class, true, Msg.translate(Env.getCtx(), "AD_Org_ID"));
		miniTable.setColumnClass(i++, String.class, true, Msg.translate(Env.getCtx(), "C_DocType_ID"));
		miniTable.setColumnClass(i++, String.class, true, Msg.translate(Env.getCtx(), "DocumentNo"));
		miniTable.setColumnClass(i++, String.class, true, Msg.translate(Env.getCtx(), "C_BPartner_ID"));
		miniTable.setColumnClass(i++, String.class, true, Msg.translate(Env.getCtx(), "Bill_Location_ID"));
		miniTable.setColumnClass(i++, Timestamp.class, true, Msg.translate(Env.getCtx(), "DateOrdered"));
		miniTable.setColumnClass(i++, BigDecimal.class, true, Msg.translate(Env.getCtx(), "TotalLines"));
		//
		miniTable.autoSize();
	}
	
	/**
	 * Get SQL for Orders that needs to be shipped
	 * @return sql
	 */
	private String getOrderSQL()
	{
	    StringBuilder sql = new StringBuilder(
	            "SELECT ic.C_Order_ID, o.Name, dt.Name, ic.DocumentNo, bp.Name, ic.DateOrdered, ic.TotalLines, bpl.Name "
	    		// use C_Order instead of C_Invoice_Candidate_v for access purposes, will be replaced later
	            + "FROM C_Order ic, AD_Org o, C_BPartner bp, C_BPartner_Location bpl, C_DocType dt, C_Order ox "
	            + "WHERE ic.AD_Org_ID=o.AD_Org_ID"
	            + " AND ic.C_Order_ID=ox.C_Order_ID"
	            + " AND ic.C_BPartner_ID=bp.C_BPartner_ID"
	            + " AND ox.Bill_Location_ID=bpl.C_BPartner_Location_ID"
	            + " AND ic.C_DocType_ID=dt.C_DocType_ID"
	            + " AND ic.AD_Client_ID=?");

        if (m_AD_Org_ID != null)
            sql.append(" AND ic.AD_Org_ID=").append(m_AD_Org_ID);
        if (m_C_BPartner_ID != null)
            sql.append(" AND ic.C_BPartner_ID=").append(m_C_BPartner_ID);
        sql.append(" ORDER BY o.Name,bp.Name,ic.DateOrdered");
        sql = new StringBuilder(MRole.getDefault().addAccessSQL(sql.toString(), "ic", true, false));
        // Replace C_Order by C_Invoice_Candidate_v
        int idxtable = sql.indexOf(" C_Order ");
        sql.replace(idxtable, idxtable+" C_Order ".length(), " Z_C_Invoice_Candidate_v "); ///C_Invoice_Candidate yang baru querynya agak berat
        
        return sql.toString();
	}
	
	/**
	 * Get SQL for Customer RMA that need to be invoiced
	 * @return sql
	 */
	private String getRMASql()
	{
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT rma.M_RMA_ID, org.Name, dt.Name, rma.DocumentNo, bp.Name, rma.Created, rma.Amt, bpl.Name ");
		sql.append("FROM M_RMA rma INNER JOIN AD_Org org ON rma.AD_Org_ID=org.AD_Org_ID ");
		sql.append("INNER JOIN C_DocType dt ON rma.C_DocType_ID=dt.C_DocType_ID ");
		sql.append("INNER JOIN C_BPartner bp ON rma.C_BPartner_ID=bp.C_BPartner_ID ");
		sql.append("LEFT JOIN C_BPartner_Location bpl ON rma.C_BPartner_Location_ID=bpl.C_BPartner_Location_ID ");
		sql.append("INNER JOIN M_InOut io ON rma.InOut_ID=io.M_InOut_ID ");
		sql.append("WHERE rma.DocStatus='CO' ");
		sql.append("AND dt.DocBaseType = 'SOO' ");
		sql.append("AND NOT EXISTS (SELECT * FROM C_Invoice i ");
		sql.append("WHERE i.M_RMA_ID=rma.M_RMA_ID AND i.DocStatus IN ('IP', 'CO', 'CL')) ");
//		sql.append("AND EXISTS (SELECT * FROM C_InvoiceLine il INNER JOIN M_InOutLine iol ");
//		sql.append("ON il.M_InOutLine_ID=iol.M_InOutLine_ID INNER JOIN C_Invoice i ");
//		sql.append("ON i.C_Invoice_ID=il.C_Invoice_ID WHERE i.DocStatus IN ('CO', 'CL') ");
//		sql.append("AND iol.M_InOutLine_ID IN ");
//		sql.append("(SELECT M_InOutLine_ID FROM M_RMALine rl WHERE rl.M_RMA_ID=rma.M_RMA_ID ");
//		sql.append("AND rl.M_InOutLine_ID IS NOT NULL)) ");
		sql.append("AND rma.AD_Client_ID=?");

        if (m_AD_Org_ID != null)
            sql.append(" AND rma.AD_Org_ID=").append(m_AD_Org_ID);
        if (m_C_BPartner_ID != null)
            sql.append(" AND bp.C_BPartner_ID=").append(m_C_BPartner_ID);
        
        sql.append(" ORDER BY org.Name, bp.Name, rma.Created ");
        sql = new StringBuilder(MRole.getDefault().addAccessSQL(sql.toString(), "rma", true, false));
        
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
        
        if (docTypeKNPair.getKey() == MOrder.Table_ID)
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
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql.toString(), null);
			pstmt.setInt(1, AD_Client_ID);
			rs = pstmt.executeQuery();
			//
			while (rs.next())
			{
				//  extend table
				miniTable.setRowCount(row+1);
				//  set values
				int column = 0;
				miniTable.setValueAt(new IDColumn(rs.getInt(1)), row, column++);   //  C_Order_ID
				miniTable.setValueAt(rs.getString(2), row, column++);              //  Org
				miniTable.setValueAt(rs.getString(3), row, column++);              //  DocType
				miniTable.setValueAt(rs.getString(4), row, column++);              //  Doc No
				miniTable.setValueAt(rs.getString(5), row, column++);              //  BPartner
				miniTable.setValueAt(rs.getString(8), row, column++);              //  BPLocation
				miniTable.setValueAt(rs.getTimestamp(6), row, column++);           //  DateOrdered
				miniTable.setValueAt(rs.getBigDecimal(7), row, column++);          //  TotalLines
				//  prepare next
				row++;
			}
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, sql.toString(), e);
		}
		finally
		{
			DB.close(rs,pstmt);
			rs = null;
			pstmt = null;
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
		for (int i = 0; i < rows; i++)
		{
			IDColumn id = (IDColumn)miniTable.getValueAt(i, 0);     //  ID in column 0
		//	log.fine( "Row=" + i + " - " + id);
			if (id != null && id.isSelected())
				results.add(id.getRecord_ID());
		}

		if (results.size() == 0)
			return;
		if (log.isLoggable(Level.CONFIG)) log.config("Selected #" + results.size());
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
        
        if (docTypeKNPair.getKey() == MRMA.Table_ID)
        {
            AD_Process_ID = PROCESS_C_INVOICE_GENERATERMA_MANUAL; // C_Invoice_GenerateRMA - org.adempiere.process.InvoiceGenerateRMA
        }
        else
        {
            AD_Process_ID = PROCESS_C_INVOICE_GENERATE_MANUAL;  // HARDCODED    C_InvoiceCreate
        }
		MPInstance instance = new MPInstance(Env.getCtx(), AD_Process_ID, 0);
		if (!instance.save())
		{
			info = Msg.getMsg(Env.getCtx(), "ProcessNoInstance");
			return info;
		}
		
		//insert selection
		StringBuilder insert = new StringBuilder();
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
				insert = new StringBuilder();
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
		
		setTrx(trx);
		setProcessInfo(pi);
		
		return info;
	}	//	generateInvoices
}