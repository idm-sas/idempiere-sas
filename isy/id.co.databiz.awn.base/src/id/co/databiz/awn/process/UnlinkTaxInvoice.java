
package id.co.databiz.awn.process;

import id.co.databiz.awn.model.MTaxInvoice;

import java.util.logging.Level;

import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
 
/**
 *	Remove reference Z_TaxInvoice_ID from corresponded invoice
 *	
 *  @author Anozi Mada
 */
public class UnlinkTaxInvoice extends SvrProcess
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
			else if (name.equals("Z_TaxInvoice_ID")){
				taxInvoiceID = para[i].getParameterAsInt();
			}							
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
		MTaxInvoice taxInvoice = new MTaxInvoice(getCtx(), taxInvoiceID, null);
		if(taxInvoice.get_ID() > 0){
			String sql = "UPDATE C_Invoice SET Z_TaxInvoice_ID = NULL WHERE Z_TaxInvoice_ID = ?";
			int rowUpdated = DB.executeUpdate(sql, taxInvoiceID, null);
			if(rowUpdated == -1){
				message =  "No Invoice processed.";
			} else {
				message =  "Tax Invoice [" + taxInvoice.getDocumentNo() + "] reference to invoice(s) has been removed.";
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
		return message;
	}	//	doIt
	
}	
