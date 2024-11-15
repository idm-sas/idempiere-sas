
package id.co.databiz.awn.process;

import java.io.File;
import java.util.logging.Level;

import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;

/**
 *	
 *	
 *  @author Anozi Mada
 */
public class BackupDB extends SvrProcess
{
	private String	scriptPath = null;
	
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
			else if (name.equals("Value"))
				scriptPath = para[i].getParameter().toString();	
			else
				log.log(Level.SEVERE, "prepare - Unknown Parameter: " + name);
		}		
	}	//	prepare

	protected String doIt () throws Exception
	{
		log.info("Process Path = " + scriptPath);
		String message = "";
		
		if(scriptPath!=null && !scriptPath.isEmpty()){
			scriptPath = scriptPath.trim();
			
			File scriptFile = new File(scriptPath);
			
			message = validateFile(scriptFile,false,false,true);
			if(message.isEmpty()){ // no error on scriptPath
				Runtime.getRuntime().exec(scriptFile.getAbsolutePath());
			}
		}
		return message;
	}	//	doIt
	
	private String validateFile(File file, boolean canRead, boolean canWrite, boolean canExecute){
		String message = "";
		if(file.exists()){
			if(canRead && !file.canRead()){
				message = file.getName() + " cannot be read";
			}
			if(canWrite && !file.canWrite()){
				message = file.getName() + " cannot be written";
			}
			if(canExecute && !file.canExecute()){
				message = file.getName() + " cannot be executed";
			}
		} else {
			message = file.getName() + " is not exists";
		}
		return message;
	}
}	
