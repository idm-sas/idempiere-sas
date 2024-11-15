package id.co.databiz.awn.process;

import java.util.logging.Level;

import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

public class RefreshMaterializedViews extends SvrProcess
{
	
	private String TableName="mv_internal";

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
			else if (name.equals("TableName"))
				TableName  = para[i].getParameterAsString();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
	}	//	prepare

	/**
	 *  Perform process.
	 *  @return Message 
	 *  @throws Exception if not successful
	 */
	protected String doIt() throws Exception
	{
		if (log.isLoggable(Level.INFO)) log.info("Refresh Materialized Views "+TableName);
		
		String sql = "";
		int no;

			sql = "REFRESH MATERIALIZED VIEW "+TableName;
			statusUpdate(sql);
			no = DB.executeUpdateEx(sql, get_TrxName());
			DB.commit(false, get_TrxName());			
			addLog(no+" updated - "+sql);

		return "@Updated@";
	}	//	doIt
}	//	Refresh Materialized Views

