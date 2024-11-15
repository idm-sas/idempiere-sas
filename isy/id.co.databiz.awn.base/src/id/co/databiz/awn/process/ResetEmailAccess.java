
package id.co.databiz.awn.process;

import id.co.databiz.awn.model.AWNSysConfig;
import id.co.databiz.awn.util.EmailValidator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

import org.compiere.model.MSysConfig;
import org.compiere.model.MUser;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;

/**
 *	
 *	
 *  @author Anozi Mada
 */
public class ResetEmailAccess extends SvrProcess
{
	private String	scriptPath = null;
	private String	emailsPath = null;
	private StringBuffer	emails = null;
	
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
//			else if (name.equals("Value"))
//				scriptPath = para[i].getParameter().toString();
//			else if (name.equals("Name"))
//				emailsPath = para[i].getParameter().toString();
//			else if (name.equals("Description"))
//				emails = para[i].getParameter().toString();		
			else
				log.log(Level.SEVERE, "prepare - Unknown Parameter: " + name);
		}	
		
		scriptPath = "/opt/restart_firewall.sh";
		emailsPath = MSysConfig.getValue(AWNSysConfig.ISY_ACCESS_FILE_PATH, "/opt/access.txt");
		emails = new StringBuffer();
	}	//	prepare

	protected String doIt () throws Exception
	{
		log.info("Process Path = " + scriptPath);
		log.info("Emails Path = " + emailsPath);
		log.info("Emails = " + emails);
		String message = "";
		
		if(scriptPath!=null && !scriptPath.isEmpty() && emailsPath!=null && !emailsPath.isEmpty()){
			scriptPath = scriptPath.trim();
			emailsPath = emailsPath.trim();
//			emails = emails.trim();
			
			File scriptFile = new File(scriptPath);
			File emailsFile = new File(emailsPath);
			
			message = validateFile(scriptFile,false,false,true);
			if(message.isEmpty()){ // no error on scriptPath
				message = validateFile(emailsFile,true,true,false);
				if(message.isEmpty()){ // no error emailsPath
					List<MUser> userList = new Query(getCtx(), MUser.Table_Name, "OAuth IS NOT NULL", get_TrxName())
						.setClient_ID()
						.list();
					for (MUser user : userList) {
						message = validateEmail(user.get_ValueAsString("OAuth"));
						if (message.isEmpty()) {
							emails.append(user.get_ValueAsString("OAuth"));
							emails.append("\n");
						} else {
							break;
						}
					}
					
					if(message.isEmpty() && emails.length() > 0){
						FileOutputStream fop = null;
						try {
							fop = new FileOutputStream(emailsFile);
							byte[] contentInBytes = emails.toString().getBytes();
							fop.write(contentInBytes);
							fop.flush();
							fop.close();
						} catch (IOException  e) {
							message = e.getMessage();
						} finally {
							try {
								if (fop != null) {
									fop.close();
								}
							} catch (IOException e) {
								message = message + "\n" + e.getMessage();
							}
						}
						Runtime.getRuntime().exec(scriptFile.getAbsolutePath());
					}
				}
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
	
	private String validateEmail(String email){
		String message = "";
		if(!EmailValidator.validate(email.trim())){
			message += email + " is not valid email\n";
		}
		return message;
	}
	
}	
