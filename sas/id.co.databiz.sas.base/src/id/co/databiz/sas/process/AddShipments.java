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

import id.co.databiz.sas.SASSystemID;
import id.co.databiz.sas.model.MInOut;
import id.co.databiz.sas.model.MRequestBundle;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MOrder;
import org.compiere.model.MRequest;
import org.compiere.model.MUser;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

/**
 *	Add Shipments
 *	
 *  @author Anozi Mada
 */
public class AddShipments extends SvrProcess
{	
	
	private int			p_AD_Org_ID = 0;
	private int			p_C_BPartner_ID = 0;
	private Timestamp	p_DateStart = null;
	private Timestamp	p_DateEnd = null;
	
	private MRequestBundle requestBundle = null;
	private MUser user = null;
	StringBuilder sql = null;
	List<Object> paramList = null;
	
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
			else if (name.equals("AD_Org_ID"))
				p_AD_Org_ID = para[i].getParameterAsInt();
			else if (name.equals("C_BPartner_ID"))
				p_C_BPartner_ID = para[i].getParameterAsInt();
			else if (name.equals("DateProcessed")) {
				p_DateStart = para[i].getParameterAsTimestamp();
				p_DateEnd = para[i].getParameter_ToAsTimestamp();
			}
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}	
		
		requestBundle = new MRequestBundle(getCtx(), getRecord_ID(), get_TrxName());
		user = new MUser(getCtx(), requestBundle.getAD_User_ID(), get_TrxName());
	}	//	prepare
	
	private void setDeliveryJournalSQL() {
		sql = new StringBuilder();
		sql.append("SELECT i.M_InOut_ID, 0 AS R_Request_ID ");
		sql.append("FROM M_InOut i ");
		sql.append("WHERE i.DocStatus IN ('CO','CL') ");
		sql.append("AND i.IsSOTrx = 'Y' ");
		sql.append("AND i.AD_Org_ID = ? ");						// 1	org
		sql.append("AND i.MovementDate BETWEEN ? AND ? ");		// 2,3	startdate,enddate
		sql.append("AND NOT EXISTS(SELECT 1 FROM R_Request r ");
		sql.append("WHERE r.M_InOut_ID = i.M_InOut_ID ");
		sql.append("AND r.R_Status_ID NOT IN (?) "); 			// 4 	status not delivered
		sql.append("AND r.R_RequestType_ID = ?) ");				// 5	request type delivery journal
		if (p_C_BPartner_ID > 0) {
			sql.append("AND i.C_BPartner_ID = ? ");				// 6	bp
		}
		sql.append("ORDER BY i.DocumentNo");
		
		paramList = new ArrayList<Object>();
		paramList.add(p_AD_Org_ID);
		paramList.add(p_DateStart);
		paramList.add(p_DateEnd);
		paramList.add(SASSystemID.DJ_STATUS_NOT_DELIVERED);
		paramList.add(requestBundle.getR_RequestType_ID());
		if (p_C_BPartner_ID > 0) {
			paramList.add(p_C_BPartner_ID);
		}
	}
	
	private void setSerahTerimaDeliverySQL() {
		sql = new StringBuilder();
		sql.append("SELECT i.M_InOut_ID, rl.R_Request_ID ");
		sql.append("FROM M_InOut i ");
		sql.append("INNER JOIN R_Request rl ON (rl.M_InOut_ID = i.M_InOut_ID) ");
		sql.append("INNER JOIN SAS_RequestBundle rb ON (rb.SAS_RequestBundle_ID = rl.SAS_RequestBundle_ID) ");
		sql.append("WHERE i.DocStatus IN ('CO','CL') ");
		sql.append("AND i.IsSOTrx = 'Y' ");
		sql.append("AND i.AD_Org_ID = ? ");						// 1	org
		sql.append("AND rb.DateDoc BETWEEN ? AND ? ");			// 2,3	startdate,enddate
		sql.append("AND rl.R_Request_ID = (SELECT MAX(r.R_Request_ID) FROM R_Request r ");
		sql.append("WHERE r.Processed = 'Y' ");
		sql.append("AND r.M_InOut_ID = i.M_InOut_ID ");
		sql.append("AND r.R_Status_ID IN (?,?) "); 				// 4,5 	status delivered,delivered with note
		sql.append("AND r.R_RequestType_ID = ?) "); 			// 6 	type delivery journal
		sql.append("AND NOT EXISTS(SELECT 1 FROM R_Request r ");
		sql.append("WHERE r.M_InOut_ID = i.M_InOut_ID ");
		sql.append("AND r.R_Status_ID IN (?,?) "); 				// 7,8	status drafted,delivery to billing
		sql.append("AND r.R_RequestType_ID = ?) ");				// 9	type serah terima delivery
		if (p_C_BPartner_ID > 0) {
			sql.append("AND i.C_BPartner_ID = ? ");				// 10	bp
		}
		sql.append("ORDER BY rb.DocumentNo, rl.DocumentNo ");
		
		paramList = new ArrayList<Object>();
		paramList.add(p_AD_Org_ID);
		paramList.add(p_DateStart);
		paramList.add(p_DateEnd);
		paramList.add(SASSystemID.DJ_STATUS_DELIVERED);
		paramList.add(SASSystemID.DJ_STATUS_DELIVERED_WITH_NOTE);
		paramList.add(SASSystemID.REQUEST_TYPE_DELIVERY_JOURNAL);
		paramList.add(SASSystemID.STD_STATUS_DRAFTED);
		paramList.add(SASSystemID.STD_STATUS_DELIVERY_TO_BILLING);
		paramList.add(requestBundle.getR_RequestType_ID());
		if (p_C_BPartner_ID > 0) {
			paramList.add(p_C_BPartner_ID);
		}
	}
	
	private void setSerahTerimaBillingSQL() {
		sql = new StringBuilder();
		sql.append("SELECT i.M_InOut_ID, COALESCE(rl.R_Request_ID,0) AS R_Request_ID ");
		sql.append("FROM M_InOut i ");
		sql.append("INNER JOIN C_BPartner bp ON (bp.C_BPartner_ID = i.C_BPartner_ID) "); 
		sql.append("LEFT JOIN R_Request rl ON (rl.M_InOut_ID = i.M_InOut_ID) ");
		sql.append("LEFT JOIN SAS_RequestBundle rb ON (rb.SAS_RequestBundle_ID = rl.SAS_RequestBundle_ID) ");
		sql.append("WHERE i.DocStatus IN ('CO','CL') ");
		sql.append("AND i.IsSOTrx = 'Y' ");
		sql.append("AND i.AD_Org_ID = ? ");								// 1 org
		sql.append("AND bp.Billing_ID = ? ");							// 2 billing bp
		if (p_AD_Org_ID != SASSystemID.ORG_KENARI) {
			sql.append("AND rb.DateDoc BETWEEN ? AND ? ");				// 3,4 startdate,enddate
			sql.append("AND rl.R_Request_ID = (SELECT MAX(r.R_Request_ID) FROM R_Request r ");
			sql.append("WHERE r.M_InOut_ID = i.M_InOut_ID ");
			sql.append("AND r.Processed = 'Y' ");
			sql.append("AND r.R_Status_ID IN (?)"); 					// 5 status delivery to billing
			sql.append("AND r.R_RequestType_ID = ?) "); 				// 6 type serah terima delivery
		}
		sql.append("AND NOT EXISTS(SELECT 1 FROM R_Request r ");
		sql.append("WHERE r.M_InOut_ID = i.M_InOut_ID ");
		sql.append("AND r.R_Status_ID IN (?,?) "); 						// 7,8	status drafted,billing
		sql.append("AND r.R_RequestType_ID = ?) ");						// 9 type serah terima billing
		if (p_C_BPartner_ID > 0) {
			sql.append("AND i.C_BPartner_ID = ? ");						// 10 bp
		}
		sql.append("ORDER BY rb.DocumentNo, rl.DocumentNo ");
		
		paramList = new ArrayList<Object>();
		paramList.add(p_AD_Org_ID);
		paramList.add(user.getC_BPartner_ID());
		if (p_AD_Org_ID != SASSystemID.ORG_KENARI) {
			paramList.add(p_DateStart);
			paramList.add(p_DateEnd);
			paramList.add(SASSystemID.STD_STATUS_DELIVERY_TO_BILLING);
			paramList.add(SASSystemID.REQUEST_TYPE_SERAH_TERIMA_DELIVERY);
		}
		paramList.add(SASSystemID.STB_STATUS_DRAFTED);
		paramList.add(SASSystemID.STB_STATUS_BILLING);
		paramList.add(requestBundle.getR_RequestType_ID());
		if (p_C_BPartner_ID > 0) {
			paramList.add(p_C_BPartner_ID);
		}
	}

	/**
	 *  Perform process.
	 *  @return Message (clear text)
	 *  @throws Exception if not successful
	 */
	protected String doIt() throws Exception
	{
		int count = 0;

//		StringBuilder sql = new StringBuilder();
//		sql.append("SELECT io.M_InOut_ID ");
//		sql.append("FROM M_InOut io ");
//		sql.append("WHERE io.DocStatus IN ('CO','CL') ");
//		sql.append("AND io.IsSOTrx = 'Y' ");
//		sql.append("AND io.AD_Org_ID = ? ");
//		sql.append("AND io.MovementDate BETWEEN ? AND ? ");
//		
//		List<Object> paramList = new ArrayList<Object>();
//		paramList.add(p_AD_Org_ID);
//		paramList.add(p_DateStart);
//		paramList.add(p_DateEnd);
//		
//		if (p_C_BPartner_ID > 0) {
//			sql.append("AND io.C_BPartner_ID = ? ");
//			paramList.add(p_C_BPartner_ID);
//		}
//		
//		if (requestBundle.getR_RequestType_ID() == SASSystemID.REQUEST_TYPE_DELIVERY_JOURNAL) {
//			sql.append("AND NOT EXISTS(SELECT 1 FROM R_Request r ");
//			sql.append("WHERE r.M_InOut_ID = io.M_InOut_ID ");
//			sql.append("AND r.R_Status_ID NOT IN (?)"); // not delivered
//			sql.append("AND r.R_RequestType_ID = ?) ");
//			
//			paramList.add(SASSystemID.DJ_STATUS_NOT_DELIVERED);
//			paramList.add(requestBundle.getR_RequestType_ID());
//		} else if (requestBundle.getR_RequestType_ID() == SASSystemID.REQUEST_TYPE_SERAH_TERIMA_DELIVERY) {
//			sql.append("AND EXISTS(SELECT 1 FROM R_Request r ");
//			sql.append("WHERE r.M_InOut_ID = io.M_InOut_ID ");
//			sql.append("AND r.Processed = 'Y' ");
//			sql.append("AND r.R_Status_ID IN (?,?) "); // delivered,delivered with note
//			sql.append("AND r.R_RequestType_ID = ?) "); // delivery journal
//			sql.append("AND NOT EXISTS(SELECT 1 FROM R_Request r ");
//			sql.append("WHERE r.M_InOut_ID = io.M_InOut_ID ");
//			sql.append("AND r.R_Status_ID IN (?,?) "); // drafted,delivery to billing
//			sql.append("AND r.R_RequestType_ID = ?) ");
//			
//			paramList.add(SASSystemID.DJ_STATUS_DELIVERED);
//			paramList.add(SASSystemID.DJ_STATUS_DELIVERED_WITH_NOTE);
//			paramList.add(SASSystemID.REQUEST_TYPE_DELIVERY_JOURNAL);
//			paramList.add(SASSystemID.STD_STATUS_DRAFTED);
//			paramList.add(SASSystemID.STD_STATUS_DELIVERY_TO_BILLING);
//			paramList.add(requestBundle.getR_RequestType_ID());
//		} else if (requestBundle.getR_RequestType_ID() == SASSystemID.REQUEST_TYPE_SERAH_TERIMA_BILLING) {
//			sql.append("AND EXISTS(SELECT 1 FROM R_Request r ");
//			sql.append("WHERE r.M_InOut_ID = io.M_InOut_ID ");
//			sql.append("AND r.Processed = 'Y' ");
//			sql.append("AND r.R_Status_ID IN (?)"); // delivery to billing
//			sql.append("AND r.R_RequestType_ID = ?) "); // serah terima delivery
//			sql.append("AND NOT EXISTS(SELECT 1 FROM R_Request r ");
//			sql.append("WHERE r.M_InOut_ID = io.M_InOut_ID ");
//			sql.append("AND r.R_RequestType_ID = ?) ");
//			
//			paramList.add(SASSystemID.STD_STATUS_DELIVERY_TO_BILLING);
//			paramList.add(SASSystemID.REQUEST_TYPE_SERAH_TERIMA_DELIVERY);
//			paramList.add(requestBundle.getR_RequestType_ID());
//		} else if (requestBundle.getR_RequestType_ID() == SASSystemID.REQUEST_TYPE_DAFTAR_TAGIH_OFFICE) {
//			sql.append("AND EXISTS(SELECT 1 FROM R_Request r ");
//			sql.append("WHERE r.M_InOut_ID = io.M_InOut_ID ");
//			sql.append("AND r.Processed = 'Y' ");
//			sql.append("AND r.R_Status_ID NOT IN (?)"); // drafted
//			sql.append("AND r.R_RequestType_ID = ?) "); // serah terima billing
//			sql.append("AND NOT EXISTS(SELECT 1 FROM R_Request r ");
//			sql.append("AND r.R_Status_ID IN (?,?,?)"); // paid,giro,antar dokumen
//			sql.append("WHERE r.M_InOut_ID = io.M_InOut_ID ");
//			sql.append("AND r.R_RequestType_ID = ?) ");
//			
//			paramList.add(SASSystemID.STB_STATUS_DRAFTED);
//			paramList.add(SASSystemID.REQUEST_TYPE_SERAH_TERIMA_BILLING);
//			paramList.add(SASSystemID.DTO_STATUS_PAID);
//			paramList.add(SASSystemID.DTO_STATUS_GIRO);
//			paramList.add(SASSystemID.DTO_STATUS_ANTAR_DOKUMEN);
//			paramList.add(requestBundle.getR_RequestType_ID());
//		}
		
		if (requestBundle.getR_RequestType_ID() == SASSystemID.REQUEST_TYPE_DELIVERY_JOURNAL) {
			setDeliveryJournalSQL();
		} else if (requestBundle.getR_RequestType_ID() == SASSystemID.REQUEST_TYPE_SERAH_TERIMA_DELIVERY) {
			setSerahTerimaDeliverySQL();
		} else if (requestBundle.getR_RequestType_ID() == SASSystemID.REQUEST_TYPE_SERAH_TERIMA_BILLING) {
			setSerahTerimaBillingSQL();
		}
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql.toString(), get_TrxName());
			DB.setParameters(pstmt, paramList);
		    rs = pstmt.executeQuery();
		    while(rs.next()) {
		          int inoutID = rs.getInt("M_InOut_ID");
		          int prevRequestID = rs.getInt("R_Request_ID");
		          MInOut io = new MInOut(getCtx(), inoutID, get_TrxName());
		          MRequest request = new MRequest(getCtx(), 0, get_TrxName());
		          MRequest prevRequest = null;
		          if (prevRequestID > 0) {
		        	  prevRequest = new MRequest(getCtx(), prevRequestID, get_TrxName());
		          }
		          request.setAD_Org_ID(requestBundle.getAD_Org_ID());
		          request.set_ValueOfColumn("SAS_RequestBundle_ID", requestBundle.get_ID());
		          request.setR_RequestType_ID(requestBundle.getR_RequestType_ID());
		          request.setR_Status_ID(requestBundle.getR_Status_ID());
		          request.set_ValueOfColumn("DocumentType", "Surat Jalan");
		          request.setM_InOut_ID(inoutID);
		          request.setC_BPartner_ID(io.getC_BPartner_ID());
		          if (prevRequest != null && request.getR_RequestType_ID() == SASSystemID.REQUEST_TYPE_SERAH_TERIMA_DELIVERY) {
		        	  request.setSummary(prevRequest.getSummary());
		          } else {
		        	  request.setSummary("-");
		          }
		          if (io.getC_Order_ID() > 0) {
		        	  MOrder order = new MOrder(getCtx(), io.getC_Order_ID(), get_TrxName());
		        	  request.set_ValueOfColumn("PickListNote", order.get_Value("PickListNote"));
		          }
		          request.setSalesRep_ID(requestBundle.getAD_User_ID());
		          request.setConfidentialType(MRequest.CONFIDENTIALTYPE_PartnerConfidential);
		          request.setConfidentialTypeEntry(MRequest.CONFIDENTIALTYPEENTRY_PartnerConfidential);
		          request.setPriority(MRequest.PRIORITY_Medium);
		          request.saveEx();
		          count++;
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
		return "@Created@ " + count;
	}	//	doIt

}	//	Add Shipments