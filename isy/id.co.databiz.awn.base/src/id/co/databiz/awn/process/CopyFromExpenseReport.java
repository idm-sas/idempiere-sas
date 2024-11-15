
package id.co.databiz.awn.process;

import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Level;

import org.compiere.model.MTimeExpense;
import org.compiere.model.MTimeExpenseLine;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;

/**
 *  Copy Expense Report Lines
 *
 *	@author Wydiyanto
 */
public class CopyFromExpenseReport extends SvrProcess
{
	private int		pExpenseID = 0;

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
			else if (name.equals("S_TimeExpense_ID"))
				pExpenseID = ((BigDecimal)para[i].getParameter()).intValue();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
	}	//	prepare

	/**
	 *  Perform process.
	 *  @return Message (clear text)
	 *  @throws Exception if not successful
	 */
	protected String doIt() throws Exception
	{
		int toExpenseID = getRecord_ID();
		if (log.isLoggable(Level.INFO)) log.info("From S_TimeExpense_ID=" + pExpenseID + " to " + toExpenseID);
		if (toExpenseID == 0)
			throw new IllegalArgumentException("Target S_TimeExpense_ID == 0");
		if (toExpenseID == 0)
			throw new IllegalArgumentException("Source S_TimeExpense_ID == 0");
		MTimeExpense from = new MTimeExpense (getCtx(), pExpenseID, get_TrxName());
		MTimeExpense to = new MTimeExpense (getCtx(), toExpenseID, get_TrxName());
		//
		int no = copyLinesFrom (from, to);		//	no Attributes
		//
		return "@Copied@=" + no;
	}	//	doIt
	
	private int copyLinesFrom(MTimeExpense from, MTimeExpense to){
		List<MTimeExpenseLine> lines = new Query(getCtx(), MTimeExpenseLine.Table_Name, "S_TimeExpense_ID=?", get_TrxName())
			.setOnlyActiveRecords(true)
			.setParameters(from.get_ID())
			.setOrderBy("Line")
			.list();
		
		for (MTimeExpenseLine lineFrom : lines) {
			MTimeExpenseLine lineTo = new MTimeExpenseLine(getCtx(), 0, get_TrxName());
			PO.copyValues(lineFrom, lineTo);
			lineTo.setS_TimeExpense_ID(to.get_ID());
			lineTo.set_ValueNoCheck("S_TimeExpenseLine_ID", 0);
			lineTo.setAD_Org_ID(lineFrom.getAD_Org_ID());
			lineTo.saveEx();
		}
		return lines.size();
	}
}
