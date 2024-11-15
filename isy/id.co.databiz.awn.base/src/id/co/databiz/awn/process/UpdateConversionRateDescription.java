
package id.co.databiz.awn.process;

import java.sql.Timestamp;
import java.util.logging.Level;

import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
 
/**
 *	Update Conversion Rate
 *	
 *  @author Anozi Mada
 */
public class UpdateConversionRateDescription extends SvrProcess
{

	private Timestamp 	validFrom = null;
	private Timestamp 	validTo = null;
	private int 		conversionTypeID = 0;
	private String		description = null;
	
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
			else if (name.equals("ValidFrom"))
				validFrom = (Timestamp)para[i].getParameter();
			else if (name.equals("ValidTo"))
				validTo = (Timestamp)para[i].getParameter();
			else if (name.equals("C_ConversionType_ID"))
				conversionTypeID = para[i].getParameterAsInt();
			else if (name.equals("Description"))
				description = (String)para[i].getParameter();
			else
				log.log(Level.SEVERE, "prepare - Unknown Parameter: " + name);
		}		
	}	//	prepare

	/**
	 * 	
	 *	@return status message
	 *	@throws Exception
	 */
	protected String doIt () throws Exception
	{
		log.info("ValidFrom = " + validFrom + ", ValidTo = " + validTo);
		log.info("C_ConversionType_ID = " + conversionTypeID + ", Description = " + description);
		String message = "";
		
		StringBuffer sql = new StringBuffer();
		sql.append("UPDATE C_Conversion_Rate ");
		sql.append("SET Description = ? ");
		sql.append("WHERE C_ConversionType_ID = ? ");
		sql.append("AND (ValidFrom BETWEEN ? AND ?) ");
		sql.append("AND (ValidTo BETWEEN ? AND ?) ");
		
		int updatedCount = DB.executeUpdateEx(sql.toString(), new Object[]{description,conversionTypeID,validFrom,validTo,validFrom,validTo}, get_TrxName());
		message = updatedCount + " records updated";
		return message;
	}	//	doIt
	
}	
