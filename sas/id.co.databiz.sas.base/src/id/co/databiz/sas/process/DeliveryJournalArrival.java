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

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;

/**
 *	Delivery Journal Arrival
 *	
 *  @author Anozi Mada
 */
public class DeliveryJournalArrival extends SvrProcess
{	
	private int		p_Remote_Org_ID = 0;
	private int		p_R_Status_ID = 0;
	private String	p_DocumentNo = "";
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
			else if (name.equals("Remote_Org_ID"))
				p_Remote_Org_ID = para[i].getParameterAsInt();
			else if (name.equals("R_Status_ID"))
				p_R_Status_ID = para[i].getParameterAsInt();
			else if (name.equals("DocumentNo"))
				p_DocumentNo = para[i].getParameterAsString();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}		
		
		if (p_DocumentNo == null) {
			p_DocumentNo = "";
		}
	}	//	prepare

	/**
	 *  Perform process.
	 *  @return Message (clear text)
	 *  @throws Exception if not successful
	 */
	protected String doIt() throws Exception
	{
		if (log.isLoggable(Level.INFO)) 
			log.info("Remote_Org_ID=" + p_Remote_Org_ID + ", R_Status_ID=" + p_R_Status_ID +", DocumentNo=" + p_DocumentNo);
		Timestamp currentTime = new Timestamp(System.currentTimeMillis());
		MRequestBundle bundle = new Query(getCtx(), MRequestBundle.Table_Name, "UPPER(DocumentNo)=?", get_TrxName())
			.setClient_ID()
			.setParameters(p_DocumentNo.toUpperCase())
			.firstOnly();
		if (bundle == null) {
			throw new AdempiereException("Document " + p_DocumentNo + " not found");
		}
		if (bundle.getRemote_Org_ID() == 0 || bundle.getRemote_Org_ID() != p_Remote_Org_ID) {
			throw new AdempiereException("Invalid Remote Organization");
		}
		if (bundle.getDepartureTime() == null) {
			throw new AdempiereException("Delivery Journal is not departed yet");
		}
		if (bundle.getArrivalTime() != null) {
			throw new AdempiereException("Delivery Journal is already arrived");
		}
		
		bundle.setArrivalTime(currentTime);
		bundle.setArrive("Y");
		bundle.setR_Status_ID(p_R_Status_ID);
		bundle.setProcessed(true);
		bundle.saveEx();
		
		return bundle.getDocumentNo() + " updated";
	}	//	doIt

}	//	Delivery Journal Arrival