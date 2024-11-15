
package id.co.databiz.awn.process;

import id.co.databiz.awn.model.MKwitansi;

import java.util.logging.Level;

import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
 
/**
 *	Remove reference Z_Kwitansi_ID from corresponded invoice
 *	
 *  @author Anozi Mada
 */
public class UnlinkKwitansi extends SvrProcess
{
	/**	Kwitansi				*/
	private int 	kwitansiID = 0;	
	
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
			else if (name.equals("Z_Kwitansi_ID")){
				kwitansiID = para[i].getParameterAsInt();
			}							
			else
				log.log(Level.SEVERE, "prepare - Unknown Parameter: " + name);
		}		
	}	//	prepare

	/**
	 * 	Remove Z_Kwitansi_ID reference from invoice
	 *	@return status message
	 *	@throws Exception
	 */
	protected String doIt () throws Exception
	{
		log.info("Z_Kwitansi_ID=" + kwitansiID);
		String message = "";
		MKwitansi kwitansi = new MKwitansi(getCtx(), kwitansiID, null);
		if(kwitansi.get_ID() > 0){
			String sql = "UPDATE C_Invoice SET Z_Kwitansi_ID = NULL WHERE Z_Kwitansi_ID = ?";
			int rowUpdated = DB.executeUpdate(sql, kwitansiID, null);
			if(rowUpdated == -1){
				message =  "No Invoice processed.";
			} else {
				kwitansi.setAmount(Env.ZERO);
				kwitansi.saveEx();
				message =  "Kwitansi [" + kwitansi.getDocumentNo() + "] reference to invoice(s) has been removed.";
			}			
		} else {
			message =  "Kwitansi is not valid.";
		}	
		return message;
	}	//	doIt
	
}	