
package id.co.databiz.awn.process;

import id.co.databiz.awn.model.MTaxInvoice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;

import org.compiere.model.I_C_Invoice;
import org.compiere.model.MInvoice;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
 
/**
 *	Remove reference Z_TaxInvoice_ID from corresponded invoice
 *	
 *  @author FM
 */
public class UnlinkTaxInvoiceInfo extends SvrProcess
{
	/**	Tax Invoice				*/
	private int 	taxInvoiceID = 0;	
	
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
			else
				log.log(Level.SEVERE, "prepare - Unknown Parameter: " + name);
		}		
	}	//	prepare

	/**
	 * 	Remove Z_TaxInvoice_ID reference from invoice
	 *	@return status message
	 *	@throws Exception
	 */
	protected String doIt () throws Exception
	{
		log.info("Z_TaxInvoice_ID=" + taxInvoiceID);
		String message = "";
		ArrayList<String> taxInvoices = new ArrayList<String>();
		String whereClause = "EXISTS (SELECT T_Selection_ID FROM T_Selection WHERE T_Selection.AD_PInstance_ID=? "
				+ "AND T_Selection.T_Selection_ID=Z_TaxInvoice.Z_TaxInvoice_ID)";
		Collection<MTaxInvoice> mTaxInvoices = new Query(getCtx(), MTaxInvoice.Table_Name, whereClause, get_TrxName())
											.setClient_ID()
											.setParameters(new Object[] {getAD_PInstance_ID()})
											.setOrderBy("C_BPartner_ID , DateInvoiced DESC") //urut BP dan tanggal termuda
											.list();
		for (MTaxInvoice mTaxInvoice : mTaxInvoices) {
				taxInvoiceID = mTaxInvoice.get_ID();
				
				MTaxInvoice taxInvoice = new MTaxInvoice(getCtx(), taxInvoiceID, null);
				if(taxInvoice.get_ID() > 0){
					String sql = "UPDATE C_Invoice SET Z_TaxInvoice_ID = NULL WHERE Z_TaxInvoice_ID = ?";
					int rowUpdated = DB.executeUpdate(sql, taxInvoiceID, null);
					if(rowUpdated == -1){
						message =  "No Invoice processed.";
					} else {
						taxInvoices.add(taxInvoice.getDocumentNo());
					}
					// clear values
					taxInvoice.setDateInvoiced(null);
					taxInvoice.set_ValueOfColumn("UserID", null);
					taxInvoice.setProcessed(false);
					taxInvoice.setAD_User_ID(0);
					taxInvoice.setTaxBaseAmt(Env.ZERO);
					taxInvoice.setTaxAmt(Env.ZERO);
					taxInvoice.setC_Currency_ID(0);
					taxInvoice.setC_BPartner_ID(0);
					taxInvoice.setTaxID(null);
					taxInvoice.setCustomPrefix(null);
					taxInvoice.setCustomerTaxStatus(null);
					taxInvoice.setIsRevision(false);
					taxInvoice.setC_Period_ID(0);
					taxInvoice.set_ValueOfColumn("TaxInvoiceType", null);
					taxInvoice.saveEx();
				} else {
					message =  "Tax Invoice is not valid.";
				}				 
			}
		message = "Tax Invoice " + taxInvoices + " reference to invoice(s) has been removed.";
		return message;
	}	//	doIt
	
}	
