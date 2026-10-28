package id.co.databiz.sas.process;

import java.util.logging.Level;

import id.co.databiz.sas.model.MOrder;
import org.compiere.model.MProcessPara;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereSystemError;

/**
 *	Re-Open Order Process (from Closed to Completed)
 *	
 *  @author Jorg Janke
 *  @version $Id: OrderOpen.java,v 1.2 2006/07/30 00:51:02 jjanke Exp $
 */
@org.adempiere.base.annotation.Process
public class OrderOpen extends SvrProcess
{
	/**	The Order				*/
	private int		p_C_Order_ID = 0;

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
			else if (name.equals("C_Order_ID"))
				p_C_Order_ID = para[i].getParameterAsInt();
			else
				MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para[i]);
		}
	}	//	prepare

	/**
	 *  Perform process.
	 *  @return Message
	 *  @throws Exception if not successful
	 */
	protected String doIt() throws AdempiereSystemError
	{
		if (log.isLoggable(Level.INFO)) log.info("doIt - Open C_Order_ID=" + p_C_Order_ID);
		if (p_C_Order_ID == 0)
			return "";
		//
		MOrder order = new MOrder (getCtx(), p_C_Order_ID, get_TrxName());
		String msg = order.reopenIt();
		if ( msg.length() != 0 )
		{
			throw new AdempiereSystemError(msg);
		}
		
		return order.save() ? "@OK@" : "@Error@";
	}	//	doIt
	
}	//	OrderOpen