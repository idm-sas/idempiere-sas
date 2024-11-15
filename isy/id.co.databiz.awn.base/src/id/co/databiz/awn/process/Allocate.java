
package id.co.databiz.awn.process;

import java.sql.Timestamp;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.model.POWrapper;
import org.compiere.model.MBankStatementLine;
import org.compiere.model.MDocType;
import org.compiere.model.MPeriod;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;

import id.co.databiz.awn.model.I_C_PaymentCustom;
import id.co.databiz.awn.model.MPayment;
 
/**
 *	Allocate payment with IsAllocateLater = Y
 *	
 *  @author Anozi Mada
 */
public class Allocate extends SvrProcess
{
	
	private int 	paymentID = 0;
	private int		bankStatementID = 0;
	private Timestamp dateAllocate = null;
	
	private int count = 0;
	
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
			else if (name.equals("C_Payment_ID")){
				paymentID = para[i].getParameterAsInt();
			} else if (name.equals("C_BankStatement_ID")){
				bankStatementID = para[i].getParameterAsInt();
			} else if (name.equals("DateAllocate")){
				dateAllocate = (Timestamp) para[i].getParameter();
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
		log.info("C_Payment_ID = " + paymentID);
		log.info("C_BankStatement_ID = " + bankStatementID);
		log.info("DateAllocate = " + dateAllocate);
		String message = "";
		
		if(paymentID > 0){
			allocatePayment(paymentID);
		}
		
		if(bankStatementID>0){
			List<MBankStatementLine> lines = new Query(getCtx(), MBankStatementLine.Table_Name, "C_BankStatement_ID = ?", get_TrxName())
												.setParameters(new Object[]{bankStatementID})
												.list();
			for(MBankStatementLine line : lines){
				if(line.getC_Payment_ID()>0){
					allocatePayment(line.getC_Payment_ID());
				}
			}
		}
		
		if(count>1){
			message = count + " payments allocated.";
		} else {
			message = count + " payment allocated.";
		}
		return message;
	}	//	doIt
	
	private void allocatePayment(int paymentID){
		Timestamp date = dateAllocate;
		MPayment payment = new MPayment(getCtx(), paymentID, get_TrxName());
		I_C_PaymentCustom paymentCustom = POWrapper.create(payment, I_C_PaymentCustom.class);
		if(!payment.isAllocated() && paymentCustom.isAllocateLater()){
			if(date==null){
				date = paymentCustom.getEftValutaDate();
			}
			if(date==null){
				date = payment.getDateTrx();
			}
			MPeriod.testPeriodOpen(getCtx(), date, MDocType.DOCBASETYPE_PaymentAllocation, payment.getAD_Org_ID());
			if(payment.allocateIt(date)){				
				paymentCustom.setIsAllocateLater(false);
				if(payment.save(get_TrxName())){
					count++;
				}
			}
		}
	}
	
}	
