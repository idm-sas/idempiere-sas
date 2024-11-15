
package id.co.databiz.awn.process;

import id.co.databiz.awn.model.MTaxInvoice;
import id.co.databiz.awn.model.SystemID;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
 
/**
 *	Recalculate Tax Invoice
 *	
 *  @author Anozi Mada
 */
public class RecalculateTaxInvoice extends SvrProcess
{
	private int pTaxInvoiceID = 0;
	private Timestamp pDateStart = null;
	private Timestamp pDateEnd = null;

	private int	mUpdated = 0;
	
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
			else if (name.equals("Z_TaxInvoice_ID"))
				pTaxInvoiceID = para[i].getParameterAsInt();
			else if (name.equals("DateInvoiced")) {
				pDateStart = (Timestamp)para[i].getParameter();
				pDateEnd = (Timestamp)para[i].getParameter_To();
			}
			else
				log.log(Level.SEVERE, "prepare - Unknown Parameter: " + name);
		}		
		
		if (pTaxInvoiceID <= 0) {
			pTaxInvoiceID = getRecord_ID();
		}
	}	//	prepare

	/**
	 * 	
	 *	@return status message
	 *	@throws Exception
	 */
	protected String doIt () throws Exception
	{
		log.info("Z_TaxInvoice_ID=" + pTaxInvoiceID + " DateStart=" + pDateStart + " DateEnd=" + pDateEnd);
		
		List<Object> parameters = new ArrayList<Object>();
		StringBuffer whereClause = new StringBuffer();
		whereClause.append("1=1 ");
		if (pTaxInvoiceID > 0) {
			whereClause.append("AND Z_TaxInvoice_ID = ? ");
			parameters.add(pTaxInvoiceID);
		}
		if (pDateStart != null) {
			whereClause.append("AND DateInvoiced >= ? ");
			parameters.add(pDateStart);
		}
		if (pDateEnd != null) {
			whereClause.append("AND DateInvoiced <= ? ");
			parameters.add(pDateEnd);
		}
		
		List<MTaxInvoice> taxInvoiceList = new Query(getCtx(), MTaxInvoice.Table_Name, whereClause.toString(), get_TrxName())
			.setClient_ID()
			.setOnlyActiveRecords(true)
			.setParameters(parameters)
			.list();
		
		BigDecimal taxBaseAmt = Env.ZERO;
		BigDecimal taxAmt = Env.ZERO;
		
		for (MTaxInvoice taxInvoice : taxInvoiceList) {
			
			StringBuffer taxBaseAmtSql = new StringBuffer();
			taxBaseAmtSql.append("SELECT SUM(CURRENCYBASE(it.TaxBaseAmt,i.C_Currency_ID,?,i.AD_Client_ID,i.AD_Org_ID)) ");
			taxBaseAmtSql.append("FROM C_InvoiceTax it ");
			taxBaseAmtSql.append("INNER JOIN C_Tax t ON (t.C_Tax_ID = it.C_Tax_ID) ");
			taxBaseAmtSql.append("INNER JOIN C_Invoice i ON (i.C_Invoice_ID = it.C_Invoice_ID) ");
			taxBaseAmtSql.append("WHERE i.Z_TaxInvoice_ID = ? AND t.C_TaxCategory_ID IN (?,?) ");
			taxBaseAmtSql.append("AND SIGN(it.TaxAmt)=SIGN(it.TaxBaseAmt)");
			taxBaseAmt = DB.getSQLValueBD(get_TrxName(), taxBaseAmtSql.toString(), 
					taxInvoice.getDateInvoiced(),taxInvoice.get_ID(),SystemID.TAX_CATEGORY_PPN,SystemID.TAX_CATEGORY_TAX_EXEMPT);
			if (taxBaseAmt == null) {
				throw new AdempiereException("@NoCurrencyConversion@ " + taxInvoice.getDateInvoiced());
			}
			taxInvoice.setTaxBaseAmt(taxBaseAmt);
			
			StringBuffer taxAmtSql = new StringBuffer();
			taxAmtSql.append("SELECT SUM(CURRENCYBASE(it.TaxAmt,i.C_Currency_ID,?,i.AD_Client_ID,i.AD_Org_ID)) ");
			taxAmtSql.append("FROM C_InvoiceTax it ");
			taxAmtSql.append("INNER JOIN C_Tax t ON (t.C_Tax_ID = it.C_Tax_ID)");
			taxAmtSql.append("INNER JOIN C_Invoice i ON (i.C_Invoice_ID = it.C_Invoice_ID) ");
			taxAmtSql.append("WHERE i.Z_TaxInvoice_ID = ? AND t.C_TaxCategory_ID IN (?,?)");
			taxAmt = DB.getSQLValueBD(get_TrxName(), taxAmtSql.toString(), 
					taxInvoice.getDateInvoiced(),taxInvoice.get_ID(),SystemID.TAX_CATEGORY_PPN,SystemID.TAX_CATEGORY_TAX_EXEMPT);
			taxInvoice.setTaxAmt(taxAmt);
			
			taxInvoice.saveEx();
			mUpdated++;
		}
		
		return "@Updated@ = " + mUpdated;
	}	//	doIt
	
	
}	
