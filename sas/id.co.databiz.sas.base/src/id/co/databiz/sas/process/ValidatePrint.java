/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package id.co.databiz.sas.process;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.apps.WProcessCtl;
import org.compiere.model.MColumn;
import org.compiere.model.MPInstance;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.model.MUser;
import org.compiere.model.PO;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.Env;
import org.compiere.util.Ini;

/**
 *	Validate Print
 *	
 *  @author Anozi Mada
 */
public class ValidatePrint extends SvrProcess
{	
	private int 	p_AD_Table_ID = 0;
	private int 	p_Record_ID = 0;
	private int 	p_AD_Process_ID = 0;	// report process
	private int 	p_AD_Column_ID = 0;		// printed by column
	private boolean p_IsAllowMultiplePrint = false;
	
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
			else if (name.equals("AD_Table_ID"))
				p_AD_Table_ID = para[i].getParameterAsInt();
			else if (name.equals("Record_ID"))
				p_Record_ID = para[i].getParameterAsInt();
			else if (name.equals("AD_Process_ID"))
				p_AD_Process_ID = para[i].getParameterAsInt();
			else if (name.equals("AD_Column_ID"))
				p_AD_Column_ID = para[i].getParameterAsInt();
			else if (name.equals("IsAllowMultiplePrint"))
				p_IsAllowMultiplePrint = para[i].getParameterAsBoolean();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}		
		
		if (p_AD_Table_ID == 0) {
			p_AD_Table_ID = getTable_ID();
		}
		if (p_Record_ID == 0) {
			p_Record_ID = getRecord_ID();
		}
	}	//	prepare

	/**
	 *  Perform process.
	 *  @return Message (clear text)
	 *  @throws Exception if not successful
	 */
	protected String doIt() throws Exception
	{
		if (log.isLoggable(Level.INFO)) log.info("AD_Table_ID=" + p_AD_Table_ID + ", Record_ID=" + p_Record_ID + 
				", AD_process_ID=" + p_AD_Process_ID + ", AD_Column_ID=" + p_AD_Column_ID + 
				", IsAllowMultiplePrint=" + p_IsAllowMultiplePrint);
		
		MUser user = MUser.get(getCtx(), Env.getAD_User_ID(getCtx()));
		MRole role = MRole.get(getCtx(), Env.getAD_Role_ID(getCtx()));
		MTable table = MTable.get(getCtx(), p_AD_Table_ID);
		MColumn column = MColumn.get(getCtx(), p_AD_Column_ID);
		PO po = table.getPO(p_Record_ID, get_TrxName());
		
		String printed = po.get_ValueAsString(column.getColumnName());
		boolean isPrinted = false;
		if (printed!= null && !printed.isEmpty()) {
			isPrinted = true;
		}
		
		if (isPrinted && !p_IsAllowMultiplePrint && !role.get_ValueAsBoolean("IsAllowMultiplePrint")) {
			throw new AdempiereUserError("SAS-158 Document is already printed");
		}
		
		ProcessInfo pi = new ProcessInfo("Print", p_AD_Process_ID, p_AD_Table_ID, p_Record_ID);
		MPInstance instance = new MPInstance(getCtx(), p_AD_Process_ID, p_Record_ID);
		instance.saveEx();
		pi.setAD_Client_ID(Env.getAD_Client_ID(getCtx()));
		pi.setAD_User_ID(Env.getAD_User_ID(getCtx()));
		pi.setAD_PInstance_ID(instance.get_ID());
		if (Ini.isClient()) {
			throw new AdempiereException("Not Supported");
		} else {
			final WProcessCtl worker = new WProcessCtl(null, 0, pi, null);
			worker.run();
		}
//		ReportCtl.start(pi, false);
		
		DateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd HH:mm");
		if (!isPrinted) {
			printed = user.getName() + " " + dateFormat.format(new Date());
		} else {
			printed = printed + ", " + user.getName() + " " + dateFormat.format(new Date());
		}
		int maxIndex = printed.length();
		if (printed.length() > column.getFieldLength()) {
			maxIndex = column.getFieldLength();
		}
		po.set_ValueOfColumn(column.getColumnName(), printed.substring(0, maxIndex));
		po.saveEx();
		
		return "";
	}	//	doIt

}	//	Validate Print