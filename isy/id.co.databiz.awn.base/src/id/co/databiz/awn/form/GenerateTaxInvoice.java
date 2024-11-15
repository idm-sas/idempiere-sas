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

import id.co.databiz.awn.model.MTaxInvoice;
import id.co.databiz.awn.model.SystemID;
import id.co.databiz.awn.model.wrapper.ICBPartner;
import id.co.databiz.awn.model.wrapper.ICInvoice;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.model.POWrapper;
import org.compiere.apps.form.GenForm;
import org.compiere.minigrid.IDColumn;
import org.compiere.minigrid.IMiniTable;
import org.compiere.model.MBPartner;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MPeriod;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.compiere.util.TrxRunnable;

/**
 * Generate Tax Invoice (manual) controller class
 * 
 * @author Anozi Mada
 */
public class GenerateTaxInvoice extends GenForm
{
	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(GenerateTaxInvoice.class);
	//
	
	private Object 		orgID = null;
	private Object 		bpartnerID = null;
	private Object		docTypeID = null;
	private Object		dateInvoiced = null;
	private Object		dateInvoicedTo = null;
	private Object		isConsolidate = null;
	private Object		isSOTrx = null;
	private Object		currencyID = null;
	private Object		taxInvoiceID = null;
	private Object		documentNo = null;
	
	/** Format                  */
	public DecimalFormat   m_format = DisplayType.getNumberFormat(DisplayType.Amount);
	
	public void dynInit() throws Exception
	{
		super.dynInit();
		setTitle("Generate Tax Invoice (Manual)");
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
		//
		miniTable.setMultiSelection(true);
		//  set details
		miniTable.setColumnClass(0, IDColumn.class, false, " ");
		miniTable.setColumnClass(1, String.class, true, Msg.translate(Env.getCtx(), "AD_Org_ID"));
		miniTable.setColumnClass(2, String.class, true, Msg.translate(Env.getCtx(), "C_DocType_ID"));
		miniTable.setColumnClass(3, String.class, true, Msg.translate(Env.getCtx(), "DocumentNo"));
		miniTable.setColumnClass(4, String.class, true, Msg.translate(Env.getCtx(), "C_BPartner_ID"));
		miniTable.setColumnClass(5, Timestamp.class, true, Msg.translate(Env.getCtx(), "DateInvoiced"));
		miniTable.setColumnClass(6, BigDecimal.class, true, Msg.translate(Env.getCtx(), "TotalLines"));
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
	    sql.append("PARSE('C_DocType',i.C_DocType_ID,1), ");
	    sql.append("i.DocumentNo, ");
	    sql.append("PARSE('C_BPartner',i.C_BPartner_ID,1), ");
	    sql.append("i.DateInvoiced, ");
	    sql.append("i.TotalLines ");
	    sql.append("FROM C_Invoice i ");
	    sql.append("INNER JOIN C_DocType dt ON (dt.C_DocType_ID = i.C_DocTypeTarget_ID) ");
	    sql.append("WHERE i.IsSOTrx = ").append(getIsSOTrx().equals(Boolean.TRUE) ? "'Y' " : "'N' ");
	    sql.append("AND i.IsActive = 'Y' ");
	    sql.append("AND i.Z_TaxInvoice_ID IS NULL ");
	    sql.append("AND i.DocStatus IN ('CO','CL') ");
	    sql.append("AND dt.C_DocTypeTaxInvoice_ID > 0 ");

        if (orgID != null)
            sql.append(" AND i.AD_Org_ID=").append(orgID);
        if (bpartnerID != null)
            sql.append(" AND i.C_BPartner_ID=").append(bpartnerID);
        if( dateInvoiced != null )
        	sql.append(" AND i.DateInvoiced >=").append(DB.TO_DATE((Timestamp)dateInvoiced));
        if( dateInvoicedTo != null )
        	sql.append(" AND i.DateInvoiced <=").append(DB.TO_DATE((Timestamp)dateInvoicedTo));
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
				miniTable.setValueAt(rs.getString(3), row, 2);              //  DocType
				miniTable.setValueAt(rs.getString(4), row, 3);              //  Doc No
				miniTable.setValueAt(rs.getString(5), row, 4);              //  BPartner
				miniTable.setValueAt(rs.getTimestamp(6), row, 5);           //  DateInvoiced
				miniTable.setValueAt(rs.getBigDecimal(7), row, 6);          //  TotalLines
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
	 *	Generate Tax Invoice
	 */
	public String generate()
	{
		final StringBuffer info = new StringBuffer();		
		Trx.run(new TrxRunnable() {
			
			@Override
			public void run(String trxName) {
				ArrayList<Integer> selection = getSelection();
				if (selection != null && selection.size() > 0){
					MTaxInvoice taxInvoice = null;
					int m_bpartnerID = 0;
					int m_docTypeID = 0;
					String documentNo = "";
					Integer oldTaxInvoiceID = 0;
					BigDecimal taxAmt = Env.ZERO;
					BigDecimal taxBaseAmt = Env.ZERO;
					if(taxInvoiceID!= null){
						oldTaxInvoiceID = (Integer)taxInvoiceID;
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
				    sql.append("dt.C_DocTypeTaxInvoice_ID ");	//3
				    sql.append("FROM C_Invoice i ");
				    sql.append("INNER JOIN C_DocType dt ON (dt.C_DocType_ID = i.C_DocTypeTarget_ID) ");
				    sql.append("WHERE C_Invoice_ID IN("+ids+") ");
				    sql.append("ORDER BY dt.C_DocTypeTaxInvoice_ID, i.DateInvoiced , i.C_Invoice_ID");
					
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
							ICInvoice invoiceCustom = POWrapper.create(invoice, ICInvoice.class);
							
							StringBuffer sqlTax = new StringBuffer();
							sqlTax.append("SELECT COUNT(*) ");
							sqlTax.append("FROM C_InvoiceTax it ");
							sqlTax.append("INNER JOIN C_Tax t ON (t.C_Tax_ID = it.C_Tax_ID) ");
							sqlTax.append("WHERE it.C_Invoice_ID = ? AND t.C_TaxCategory_ID IN (?,?,?)");
							int count = DB.getSQLValue(null, sqlTax.toString(), invoiceID,SystemID.TAX_CATEGORY_PPN,SystemID.TAX_CATEGORY_TAX_EXEMPT,SystemID.TAX_CATEGORY_PPN_PLUS_PPH);
							if(count <= 0) {
								throw new AdempiereException("Invalid Tax Category");
							}
							
							if(taxInvoiceID != null && ((m_bpartnerID!=0 && m_bpartnerID!=bpartnerID) || (m_docTypeID!=0 && m_docTypeID!=docTypeID))){
								throw new AdempiereException("Cannot consolidate using existing Tax Invoice");
							}
							
							if(taxInvoice==null || !(Boolean)isConsolidate || m_bpartnerID!=bpartnerID  || m_docTypeID!=docTypeID){
								m_bpartnerID = bpartnerID;
								m_docTypeID = docTypeID;
								taxInvoice = new Query(Env.getCtx(), MTaxInvoice.Table_Name, "Z_TaxInvoice_ID = ?", trxName)
												.setParameters(new Object[]{oldTaxInvoiceID})
												.first();
								if(taxInvoice==null){
									taxInvoice = new MTaxInvoice(Env.getCtx(), 0, trxName);
									taxInvoice.setTaxAmt(Env.ZERO);
									taxInvoice.setTaxBaseAmt(Env.ZERO);
								}
								taxInvoice.setAD_Org_ID(invoice.getAD_Org_ID());
								taxInvoice.setIsSOTrx(invoice.isSOTrx());
								taxInvoice.setC_DocType_ID(docTypeID);
								taxInvoice.setDateInvoiced(invoice.getDateInvoiced());
								taxInvoice.setC_Period_ID(MPeriod.getC_Period_ID(Env.getCtx(), invoice.getDateInvoiced(), invoice.getAD_Org_ID()));
								taxInvoice.setC_BPartner_ID(bpartnerID);
								taxInvoice.setTaxID(invoice.getC_BPartner().getTaxID());
								taxInvoice.setC_Currency_ID(invoice.getC_Currency_ID());
								if(getDocumentNo()!=null){
									taxInvoice.setDocumentNo(getDocumentNo().toString());
								}
								
								MBPartner bpartner = new MBPartner(Env.getCtx(), bpartnerID, null);
								ICBPartner bpCustom = POWrapper.create(bpartner, ICBPartner.class);
								taxInvoice.setCustomerTaxStatus(bpCustom.getCustomerTaxStatus());
								
								MDocType doctype = new MDocType(Env.getCtx(), m_docTypeID, null);
								String prefixSql = "SELECT y.CustomPrefix "
										+ "FROM AD_Sequence_No y, AD_Sequence s "
										+ "WHERE y.AD_Sequence_ID = s.AD_Sequence_ID "
										+ "AND y.AD_Sequence_ID = ? "
										+ "AND s.AD_Client_ID = ? "
										+ "AND y.CalendarYearMonth = ? "
										+ "AND s.IsActive='Y' AND s.IsTableID='N' AND s.IsAutoSequence='Y' "
										+ "ORDER BY s.AD_Client_ID DESC";
								String prefix = DB.getSQLValueString(null, prefixSql, doctype.getDocNoSequence_ID(),invoice.getAD_Client_ID(),new SimpleDateFormat("yyyy").format(invoice.getDateInvoiced()));
								taxInvoice.setCustomPrefix(prefix);
							}
							
							// different year period is prohibited
							if((Boolean)isConsolidate && !taxInvoice.getDateInvoiced().equals(invoice.getDateInvoiced())){
								Calendar calendar = Calendar.getInstance();
								calendar.setTimeInMillis(taxInvoice.getDateInvoiced().getTime());
								Integer firstYear = calendar.get(Calendar.YEAR);
								calendar.setTimeInMillis(invoice.getDateInvoiced().getTime());
								Integer secondYear = calendar.get(Calendar.YEAR);
								if(!firstYear.equals(secondYear)){
									throw new AdempiereException("Tax Invoice has different period of year");
								}
							}
							
							// get most recent date
							if(taxInvoice.getDateInvoiced().before(invoice.getDateInvoiced())){
								taxInvoice.setDateInvoiced(invoice.getDateInvoiced());
							}
							
							// update amount
							StringBuffer taxBaseAmtSql = new StringBuffer();
							taxBaseAmtSql.append("SELECT SUM(CURRENCYBASE(it.TaxBaseAmt,i.C_Currency_ID,i.DateInvoiced,i.AD_Client_ID,i.AD_Org_ID)) ");
							taxBaseAmtSql.append("FROM C_InvoiceTax it ");
							taxBaseAmtSql.append("INNER JOIN C_Tax t ON (t.C_Tax_ID = it.C_Tax_ID) ");
							taxBaseAmtSql.append("INNER JOIN C_Invoice i ON (i.C_Invoice_ID = it.C_Invoice_ID) ");
							taxBaseAmtSql.append("WHERE it.C_Invoice_ID = ? AND t.C_TaxCategory_ID IN (?,?,?) ");
							taxBaseAmtSql.append("AND SIGN(it.TaxAmt)=SIGN(it.TaxBaseAmt)");
							taxBaseAmt = DB.getSQLValueBD(null, taxBaseAmtSql.toString(), invoiceID,SystemID.TAX_CATEGORY_PPN,SystemID.TAX_CATEGORY_TAX_EXEMPT,SystemID.TAX_CATEGORY_PPN_PLUS_PPH);
							if (taxBaseAmt == null) {
								throw new AdempiereException("@NoCurrencyConversion@");
							}
							taxInvoice.setTaxBaseAmt(taxInvoice.getTaxBaseAmt().add(taxBaseAmt));
							
							StringBuffer taxAmtSql = new StringBuffer();
							taxAmtSql.append("SELECT SUM(CURRENCYBASE(it.TaxAmt,i.C_Currency_ID,i.DateInvoiced,i.AD_Client_ID,i.AD_Org_ID)) ");
							taxAmtSql.append("FROM C_InvoiceTax it ");
							taxAmtSql.append("INNER JOIN C_Tax t ON (t.C_Tax_ID = it.C_Tax_ID)");
							taxAmtSql.append("INNER JOIN C_Invoice i ON (i.C_Invoice_ID = it.C_Invoice_ID) ");
							taxAmtSql.append("WHERE it.C_Invoice_ID = ? AND t.C_TaxCategory_ID IN (?,?,?)");
							taxAmt = DB.getSQLValueBD(null, taxAmtSql.toString(), invoiceID,SystemID.TAX_CATEGORY_PPN,SystemID.TAX_CATEGORY_TAX_EXEMPT,SystemID.TAX_CATEGORY_PPN_PLUS_PPH);
							taxInvoice.setTaxAmt(taxInvoice.getTaxAmt().add(taxAmt));
							
							taxInvoice.saveEx();
							if(!documentNo.equals(taxInvoice.getDocumentNo())){
								documentNo = taxInvoice.getDocumentNo();
								info.append(taxInvoice.getDocumentNo());
								info.append("\n");
							}
							invoiceCustom.setZ_TaxInvoice_ID(taxInvoice.getZ_TaxInvoice_ID());
							invoice.saveEx();
						}
						
					} catch (SQLException e) {
						log.log(Level.SEVERE, sql.toString(), e);
					} finally {
						 DB.close(rs, pstmt);
					     rs = null; pstmt = null;
					}
				}	
				info.insert(0, "Generated Tax Invoice:\n");
			}
		});
		return info.toString();
	}	//	generateTaxInvoices

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
				BigDecimal amt = (BigDecimal)miniTable.getValueAt(i, 6);
				if (amt != null)
					shipAmt = shipAmt.add(amt);
			}
		}

		//  Information
		String m_SumGrandTotal = m_format.format(shipAmt);
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
	
	public Object getIsSOTrx() {
		return isSOTrx;
	}

	public void setIsSOTrx(Object isSOTrx) {
		this.isSOTrx = isSOTrx;
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

	public Object getTaxInvoiceID() {
		return taxInvoiceID;
	}

	public void setTaxInvoiceID(Object taxInvoiceID) {
		this.taxInvoiceID = taxInvoiceID;
	}	
	
	public Object getDocumentNo() {
		return documentNo;
	}

	public void setDocumentNo(Object documentNo) {
		this.documentNo = documentNo;
	}
}