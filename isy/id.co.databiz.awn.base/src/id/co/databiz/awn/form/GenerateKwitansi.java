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
 *                 Teo Sarca, http://www.arhipac.ro                           *
 ******************************************************************************/

package id.co.databiz.awn.form;

import id.co.databiz.awn.model.I_C_DocTypeCustom;
import id.co.databiz.awn.model.I_C_InvoiceCustom;
import id.co.databiz.awn.model.MKwitansi;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.model.POWrapper;
import org.compiere.apps.form.GenForm;
import org.compiere.minigrid.IDColumn;
import org.compiere.minigrid.IMiniTable;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Msg;

public class GenerateKwitansi extends GenForm
{
	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(GenerateKwitansi.class);
	//
	
	private Object 			orgID = null;
	private Object 			bpartnerID = null;
	private Object			docTypeID = null;
	private Object			dateInvoiced = null;
	private Object			dateInvoicedTo = null;
	private Object			isConsolidate = null;
	private Object			currencyID = null;
	private Object			kwitansiID = null;
	private Object			dateDoc = null;
	private Object			dueDate = null;
	private Object			dueDateTo = null;
	
	/** Format                  */
	public DecimalFormat   m_format = DisplayType.getNumberFormat(DisplayType.Amount);
	
	public void dynInit() throws Exception
	{
		setTitle("Generate Kwitansi (Manual)");
	}
	
	public void configureMiniTable(IMiniTable miniTable)
	{
		//  create Columns
		miniTable.addColumn("C_Invoice_ID");
		miniTable.addColumn("AD_Org_ID");
		miniTable.addColumn("C_DocType_ID");
		miniTable.addColumn("DocumentNo");
		miniTable.addColumn("C_BPartner_ID");
		miniTable.addColumn("DateInvoiced");
		miniTable.addColumn("TotalLines");
		miniTable.addColumn("Z_TaxInvoice_ID");
		miniTable.addColumn("DueDate");
		//
		miniTable.setMultiSelection(true);
		//  set details
		miniTable.setColumnClass(0, IDColumn.class, false, " ");
		miniTable.setColumnClass(1, String.class, true, Msg.translate(Env.getCtx(), "AD_Org_ID"));
		miniTable.setColumnClass(2, KeyNamePair.class, true, Msg.translate(Env.getCtx(), "C_DocType_ID"));
		miniTable.setColumnClass(3, String.class, true, Msg.translate(Env.getCtx(), "DocumentNo"));
		miniTable.setColumnClass(4, String.class, true, Msg.translate(Env.getCtx(), "C_BPartner_ID"));
		miniTable.setColumnClass(5, Timestamp.class, true, Msg.translate(Env.getCtx(), "DateInvoiced"));
		miniTable.setColumnClass(6, BigDecimal.class, true, Msg.translate(Env.getCtx(), "TotalLines"));
		miniTable.setColumnClass(7, String.class, true, "Tax Invoice");
		miniTable.setColumnClass(8, Timestamp.class, true, Msg.translate(Env.getCtx(), "DueDate"));
		//
		miniTable.autoSize();
	}
	
	/**
	 * Get SQL for Invoice
	 * @return sql
	 */
	private String getSQL()
	{
	    StringBuffer sql = new StringBuffer();
	    sql.append("SELECT i.C_Invoice_ID, ");
	    sql.append("PARSE('AD_Org',i.AD_Org_ID,2), ");
//	    sql.append("PARSE('C_DocType',i.C_DocType_ID,1), ");
	    sql.append("i.C_DocType_ID, ");
	    sql.append("i.DocumentNo, ");
	    sql.append("PARSE('C_BPartner',i.C_BPartner_ID,1), ");
	    sql.append("i.DateInvoiced, ");
	    sql.append("i.TotalLines, ");
	    sql.append("PARSE('Z_TaxInvoice',i.Z_TaxInvoice_ID,0), ");
	    sql.append("ADDDAYS(i.DateInvoiced,pt.NetDays) ");
	    sql.append("FROM C_Invoice i ");
	    sql.append("INNER JOIN C_DocType dt ON (dt.C_DocType_ID = i.C_DocTypeTarget_ID) ");
	    sql.append("LEFT JOIN C_PaymentTerm pt ON (pt.C_PaymentTerm_ID = i.C_PaymentTerm_ID) ");
	    sql.append("WHERE dt.DocBaseType IN ('ARI','ARC','APC') ");
	    sql.append("AND i.IsActive = 'Y' ");
	    sql.append("AND i.Z_Kwitansi_ID IS NULL ");
	    sql.append("AND i.DocStatus IN ('CO','CL') ");
	    sql.append("AND dt.C_DocTypeKwitansi_ID > 0 ");

        if (orgID != null)
            sql.append(" AND i.AD_Org_ID=").append(orgID);
        if (bpartnerID != null)
            sql.append(" AND i.C_BPartner_ID=").append(bpartnerID);
        if( getDateInvoiced() != null )
        	sql.append(" AND i.DateInvoiced >=").append(DB.TO_DATE((Timestamp)getDateInvoiced()));
        if( getDateInvoicedTo() != null )
        	sql.append(" AND i.DateInvoiced <=").append(DB.TO_DATE((Timestamp)getDateInvoicedTo()));
        if( getDueDate() != null )
        	sql.append(" AND ADDDAYS(i.DateInvoiced,pt.NetDays) >=").append(DB.TO_DATE((Timestamp)getDueDate()));
        if( getDueDateTo() != null )
        	sql.append(" AND ADDDAYS(i.DateInvoiced,pt.NetDays) <=").append(DB.TO_DATE((Timestamp)getDueDateTo()));
        if (currencyID != null)
            sql.append(" AND i.C_Currency_ID=").append(currencyID);
        if (docTypeID != null)
            sql.append(" AND i.C_DocType_ID=").append(docTypeID);
            
        sql.append(" ORDER BY i.DocumentNo");
        
        return sql.toString();
	}
	
	/**
	 *  Query Info
	 */
	public void executeQuery(IMiniTable miniTable)
	{
		log.info("");		
		//  Create SQL
		
		String sql = getSQL();               

		//  reset table
		int row = 0;
		miniTable.setRowCount(row);
		//  Execute
		try
		{
			PreparedStatement pstmt = DB.prepareStatement(sql.toString(), null);			
			ResultSet rs = pstmt.executeQuery();
			//
			while (rs.next())
			{
				//  extend table
				miniTable.setRowCount(row+1);
				//  set values
				miniTable.setValueAt(new IDColumn(rs.getInt(1)), row, 0);   //  C_Invoice_ID
				miniTable.setValueAt(rs.getString(2), row, 1);              //  Org
//				miniTable.setValueAt(rs.getString(3), row, 2);              //  DocType
				int docTypeID = rs.getInt(3);
				MDocType docType = MDocType.get(Env.getCtx(), docTypeID);
				KeyNamePair docTypeKNP = new KeyNamePair(docTypeID, docType.getName());
				miniTable.setValueAt(docTypeKNP, row, 2);              //  DocType
				miniTable.setValueAt(rs.getString(4), row, 3);              //  Doc No
				miniTable.setValueAt(rs.getString(5), row, 4);              //  BPartner
				miniTable.setValueAt(rs.getTimestamp(6), row, 5);           //  DateInvoiced
				miniTable.setValueAt(rs.getBigDecimal(7), row, 6);          //  TotalLines
				miniTable.setValueAt(rs.getString(8), row, 7);              //  TaxInvoice
				miniTable.setValueAt(rs.getTimestamp(9), row, 8);           //  DueDate
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
	 *	Generate Kwitansi
	 */
	public String generate(String trxName)
	{	
		StringBuffer info = new StringBuffer();		
		
//		setSelectionActive(false);  //  prevents from being called twice
		
		ArrayList<Integer> selection = getSelection();
		if (selection != null && selection.size() > 0){
			MKwitansi kwitansi = null;
			int m_bpartnerID = 0;
			int m_docTypeID = 0;
			String documentNo = "";
			Integer oldKwitansiID = 0;
			if(getKwitansiID()!=null){
				oldKwitansiID = (Integer) getKwitansiID();
			}
			
			StringBuilder sb = new StringBuilder();
			for(Integer s: selection) {
			   sb.append(s).append(',');
			}
			sb.deleteCharAt(sb.length()-1); //delete last comma
			String ids = sb.toString();
			StringBuffer sql = new StringBuffer();
		    sql.append("SELECT i.C_Invoice_ID, ");		//1  
		    sql.append("i.C_BPartner_ID, ");				//2
		    sql.append("dt.C_DocTypeKwitansi_ID ");	//3
		    sql.append("FROM C_Invoice i ");
		    sql.append("INNER JOIN C_DocType dt ON (dt.C_DocType_ID = i.C_DocTypeTarget_ID) ");
		    sql.append("WHERE C_Invoice_ID IN("+ids+") ");
		    sql.append("ORDER BY dt.C_DocTypeTaxInvoice_ID, i.C_BPartner_ID");
		    
		    PreparedStatement pstmt = null;			
			ResultSet rs = null;
		    try
			{
		    	pstmt = DB.prepareStatement(sql.toString(), null);			
				rs = pstmt.executeQuery();
				//
				while (rs.next())
				{
					int invoiceID = rs.getInt(1);
					int bpartnerID = rs.getInt(2);
					int docTypeID = rs.getInt(3);
					
					MInvoice invoice = new MInvoice(Env.getCtx(), invoiceID, trxName);
					I_C_InvoiceCustom invoiceCustom = POWrapper.create(invoice, I_C_InvoiceCustom.class);
					MDocType doctypeInvoice = new MDocType(Env.getCtx(), invoice.getC_DocType_ID(), null);
					I_C_DocTypeCustom doctypeCustom = POWrapper.create(doctypeInvoice, I_C_DocTypeCustom.class);
					
					if(kwitansiID != null && ((m_bpartnerID!=0 && m_bpartnerID!=bpartnerID) || (m_docTypeID!=0 && m_docTypeID!=docTypeID))){
						throw new AdempiereException("Cannot consolidate using existing Kwitansi");
					}
					
					if(kwitansi==null || !(Boolean)isConsolidate || m_bpartnerID!=bpartnerID  || m_docTypeID!=docTypeID){
						m_bpartnerID = bpartnerID;
						m_docTypeID = docTypeID;
						kwitansi = new Query(Env.getCtx(), "Z_Kwitansi", "Z_Kwitansi_ID = ?", trxName)
										.setParameters(new Object[]{oldKwitansiID})
										.first();
						if(kwitansi==null){
							kwitansi = new MKwitansi(Env.getCtx(), 0, trxName);
						}
						kwitansi.setAD_Org_ID(invoice.getAD_Org_ID());			
						if(kwitansiID==null)
							kwitansi.setC_DocType_ID(doctypeCustom.getC_DocTypeKwitansi_ID());
						kwitansi.setDateDoc((Timestamp)getDateDoc());
						kwitansi.setAD_User_ID(Env.getAD_User_ID(Env.getCtx()));
						kwitansi.setC_BPartner_ID(invoice.getC_BPartner_ID());
					}
					
					kwitansi.saveEx();
					if(!documentNo.equals(kwitansi.getDocumentNo())){
						documentNo = kwitansi.getDocumentNo();
						info.append(kwitansi.getDocumentNo());
						info.append("\n");
					}
					invoiceCustom.setZ_Kwitansi_ID(kwitansi.getZ_Kwitansi_ID());
					BigDecimal amount = invoice.getTotalLines();
					if (doctypeInvoice.getDocBaseType().equals(MDocType.DOCBASETYPE_ARCreditMemo)) {
						amount = amount.negate();
					}
					kwitansi.setAmount(kwitansi.getAmount().add(amount));
					invoice.saveEx();
					kwitansi.saveEx();
				
				}
			} catch (SQLException e) {
				log.log(Level.SEVERE, sql.toString(), e);
			} finally {
				 DB.close(rs, pstmt);
			     rs = null; pstmt = null;
			}
		}	
					
		return info.toString();
	}	//	generateTaxInvoices

	/**
	 *  Calculate selected rows.
	 *  - add up selected rows
	 */
	public String calculateSelection(IMiniTable miniTable)
	{
		BigDecimal totalAmount = Env.ZERO;

		int rows = miniTable.getRowCount();
		for (int i = 0; i < rows; i++)
		{
			IDColumn id = (IDColumn)miniTable.getValueAt(i, 0);
			if (id.isSelected())
			{
				BigDecimal amt = (BigDecimal)miniTable.getValueAt(i, 6);
				if (amt != null) {
					KeyNamePair docTypeKNP = (KeyNamePair)miniTable.getValueAt(i, 2);
					MDocType docType = MDocType.get(Env.getCtx(), docTypeKNP.getKey());
					if (docType.getDocBaseType().equals(MDocType.DOCBASETYPE_ARCreditMemo)) {
						amt = amt.negate();
					}
					totalAmount = totalAmount.add(amt);
				}
					
			}
		}

		//  Information
		String m_SumGrandTotal = m_format.format(totalAmount);
		return m_SumGrandTotal;
	}   //  calculateSelection

	public Object getOrgID() {
		return orgID;
	}

	public void setOrgID(Object orgID) {
		this.orgID = orgID;
	}

	public Object getBpartnerID() {
		return bpartnerID;
	}

	public void setBpartnerID(Object bpartnerID) {
		this.bpartnerID = bpartnerID;
	}

	public Object getDocTypeID() {
		return docTypeID;
	}

	public void setDocTypeID(Object docTypeID) {
		this.docTypeID = docTypeID;
	}

	public Object getDateInvoiced() {
		return dateInvoiced;
	}

	public void setDateInvoiced(Object dateInvoiced) {
		this.dateInvoiced = dateInvoiced;
	}

	public Object getDateInvoicedTo() {
		return dateInvoicedTo;
	}

	public void setDateInvoicedTo(Object dateInvoicedTo) {
		this.dateInvoicedTo = dateInvoicedTo;
	}

	public Object getIsConsolidate() {
		return isConsolidate;
	}

	public void setIsConsolidate(Object isConsolidate) {
		this.isConsolidate = isConsolidate;
	}

	public Object getCurrencyID() {
		return currencyID;
	}

	public void setCurrencyID(Object currencyID) {
		this.currencyID = currencyID;
	}

	public Object getKwitansiID() {
		return kwitansiID;
	}

	public void setKwitansiID(Object kwitansiID) {
		this.kwitansiID = kwitansiID;
	}		
	
	public Object getDateDoc() {
		return dateDoc;
	}

	public void setDateDoc(Object dateDoc) {
		this.dateDoc = dateDoc;
	}	
	
	public Object getDueDate() {
		return dueDate;
	}

	public void setDueDate(Object dueDate) {
		this.dueDate = dueDate;
	}

	public Object getDueDateTo() {
		return dueDateTo;
	}

	public void setDueDateTo(Object dueDateTo) {
		this.dueDateTo = dueDateTo;
	}
}
