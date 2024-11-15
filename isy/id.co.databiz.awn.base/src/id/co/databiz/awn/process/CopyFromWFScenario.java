
package id.co.databiz.awn.process;

import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Level;

import org.compiere.model.MWFScenarioLine;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.wf.MWFScenario;

/**
 *  Copy WF Scenario Lines
 *
 *	@author Anozi Mada
 */
public class CopyFromWFScenario extends SvrProcess
{
	private int		pScenarioID = 0;

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
			else if (name.equals("Z_WFScenario_ID"))
				pScenarioID = ((BigDecimal)para[i].getParameter()).intValue();
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
		int toScenarioID = getRecord_ID();
		if (log.isLoggable(Level.INFO)) log.info("From Z_WFScenario_ID=" + pScenarioID + " to " + toScenarioID);
		if (toScenarioID == 0)
			throw new IllegalArgumentException("Target Z_WFScenario_ID == 0");
		if (pScenarioID == 0)
			throw new IllegalArgumentException("Source Z_WFScenario_ID == 0");
		MWFScenario from = new MWFScenario (getCtx(), pScenarioID, get_TrxName());
		MWFScenario to = new MWFScenario (getCtx(), toScenarioID, get_TrxName());
		//
		int no = copyLinesFrom (from, to);		//	no Attributes
		//
		return "@Copied@=" + no;
	}	//	doIt
	
	private int copyLinesFrom(MWFScenario from, MWFScenario to){
		List<MWFScenarioLine> lines = new Query(getCtx(), MWFScenarioLine.Table_Name, "Z_WFScenario_ID=?", get_TrxName())
			.setOnlyActiveRecords(true)
			.setParameters(from.get_ID())
			.setOrderBy("Line")
			.list();
		
		for (MWFScenarioLine lineFrom : lines) {
			MWFScenarioLine lineTo = new MWFScenarioLine(getCtx(), 0, get_TrxName());
			PO.copyValues(lineFrom, lineTo);
			lineTo.setZ_WFScenario_ID(to.get_ID());
			lineTo.set_ValueNoCheck("Z_WFScenarioLine_ID", 0);
			lineTo.saveEx();
		}
		return lines.size();
	}
}
