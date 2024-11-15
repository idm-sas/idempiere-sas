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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.FillMandatoryException;
import org.compiere.model.MRequest;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

/**
 *	Generate Invoice Request
 *	
 *  @author Anozi Mada
 */
public class GenerateInvoiceRequest extends SvrProcess
{	
	
	private int			p_SAS_RequestBundle_ID = 0;
	private int			p_C_DocType_ID = 0;
	private int			p_R_RequestType_ID = 0;
	private int			p_AD_User_ID = 0;
	
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
			else if (name.equals("SAS_RequestBundle_ID"))
				p_SAS_RequestBundle_ID = para[i].getParameterAsInt();
			else if (name.equals("C_DocType_ID"))
				p_C_DocType_ID = para[i].getParameterAsInt();
			else if (name.equals("R_RequestType_ID"))
				p_R_RequestType_ID = para[i].getParameterAsInt();
			else if (name.equals("AD_User_ID"))
				p_AD_User_ID = para[i].getParameterAsInt();
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
		if (p_SAS_RequestBundle_ID <= 0 && p_AD_User_ID <= 0) {
			throw new FillMandatoryException("AD_User_ID");
		}
		MRequestBundle rb = new MRequestBundle(getCtx(), p_SAS_RequestBundle_ID, get_TrxName());
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT i.C_Invoice_ID ");
		sql.append("FROM C_Invoice i ");
		sql.append("INNER JOIN T_Selection s ON (s.T_Selection_ID = i.C_Invoice_ID) ");
		sql.append("WHERE s.AD_PInstance_ID = ? ");
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql.toString(), get_TrxName());
			DB.setParameters(pstmt, new Object[]{getAD_PInstance_ID()});
		    rs = pstmt.executeQuery();
		    while(rs.next()) {
		          int invoiceID = rs.getInt("C_Invoice_ID");
		          MRequest request = new MRequest(getCtx(), 0, get_TrxName());
		          request.setAD_Org_ID(rb.getAD_Org_ID());
		          request.set_ValueOfColumn("SAS_RequestBundle_ID", rb.get_ID());
		          request.setR_RequestType_ID(rb.getR_RequestType_ID());
		          request.setR_Status_ID(rb.getR_Status_ID());
		          request.setC_Invoice_ID(invoiceID);
		          request.saveEx();
		    }
		}
		catch (Exception e)
		{
			throw new AdempiereException(e);
		}
		finally
		{
		      DB.close(rs, pstmt);
		      rs = null; pstmt = null;
		}
		return "";
	}	//	doIt

}	//	Generate Invoice Request