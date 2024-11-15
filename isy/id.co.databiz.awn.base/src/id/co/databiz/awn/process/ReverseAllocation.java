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
package id.co.databiz.awn.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MAllocationHdr;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereSystemError;
import org.compiere.util.DB;
 
/**
 *	Reverse Allocation
 *	
 *  @author Anozi Mada
 *  
 */
public class ReverseAllocation extends SvrProcess
{
	private int		p_C_Invoice_ID = 0;
	private int		p_C_Payment_ID = 0;
	
	private int		counter = 0;

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
			else if (name.equals("C_Invoice_ID"))
				p_C_Invoice_ID = para[i].getParameterAsInt();
			else if (name.equals("C_Payment_ID"))
				p_C_Payment_ID = para[i].getParameterAsInt();
			else
				log.log(Level.SEVERE, "prepare - Unknown Parameter: " + name);
		}
	}	//	prepare

	/**
	 *  Perform process.
	 *  @return Message
	 *  @throws Exception if not successful
	 */
	protected String doIt() throws AdempiereSystemError
	{
		log.info("C_Invoice_ID=" + p_C_Invoice_ID + ", C_Payment_ID=" + p_C_Payment_ID);
		
		if (p_C_Invoice_ID == 0 && p_C_Payment_ID == 0)
			return "";
		
		List<Object> paramList = new ArrayList<Object>();
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT DISTINCT ah.C_AllocationHdr_ID ");
		sql.append("FROM C_AllocationHdr ah ");
		sql.append("INNER JOIN C_AllocationLine al ON (al.C_AllocationHdr_ID = ah.C_AllocationHdr_ID) ");
		sql.append("WHERE ah.DocStatus = 'CO' ");
		if (p_C_Invoice_ID > 0) {
			sql.append("AND al.C_Invoice_ID = ? ");
			paramList.add(p_C_Invoice_ID);
		}
		if (p_C_Payment_ID > 0) {
			sql.append("AND al.C_Payment_ID = ? ");
			paramList.add(p_C_Payment_ID);
		}
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql.toString(), get_TrxName());
			DB.setParameters(pstmt, paramList);
		    rs = pstmt.executeQuery();
		    while(rs.next()) {
		          int allocationID = rs.getInt("C_AllocationHdr_ID");
		          MAllocationHdr allocation = new MAllocationHdr(getCtx(), allocationID, get_TrxName());
		          allocation.setDocAction(MAllocationHdr.DOCACTION_Reverse_Accrual);
		          if (!allocation.processIt(MAllocationHdr.DOCACTION_Reverse_Accrual)) {
		        	  return "@Error@ " + allocation.getProcessMsg();
		          }
		          allocation.saveEx();
		          counter++;
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
		
		return counter + " @Updated@";
	}	//	doIt
	
}	//	Reverse allocation
