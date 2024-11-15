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

import id.co.databiz.sas.model.MRequestBundle;

import java.sql.Timestamp;
import java.util.logging.Level;

import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;

/**
 *	Process Delivery Journal
 *	
 *  @author Anozi Mada
 */
public class ProcessDeliveryJournal extends SvrProcess
{	
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
		if (log.isLoggable(Level.INFO)) log.info("SAS_RequestBundle_ID=" + getRecord_ID());
		Timestamp currentTime = new Timestamp(System.currentTimeMillis());
		if (getRecord_ID() > 0) {
			MRequestBundle rb = new MRequestBundle(getCtx(), getRecord_ID(), get_TrxName());
			if (rb.getDepartureTime() == null) {
				rb.setDepartureTime(currentTime);
				rb.set_ValueOfColumn("Depart", "Y");
				rb.saveEx();
			} else if (rb.getArrivalTime() == null) {
				rb.setArrivalTime(currentTime);
				rb.set_ValueOfColumn("Arrive", "Y");
				rb.saveEx();
			}
		}
		return "";
	}	//	doIt

}	//	Process Delivery Journal