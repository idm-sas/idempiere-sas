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

import id.co.databiz.awn.model.MKwitansi;
import id.co.databiz.awn.model.MMovement;
import id.co.databiz.sas.SASSystemID;
import id.co.databiz.sas.model.MRMA;
import id.co.databiz.sas.model.MRequestBundle;

import java.sql.Timestamp;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.PeriodClosedException;
import org.compiere.model.MPeriod;
import org.compiere.model.MRequest;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;

/**
 *	Unprocess Request Bundle
 *	
 *  @author Anozi Mada
 */
public class UnprocessRequestBundle extends SvrProcess
{	
	
	private int			p_SAS_RequestBundle_ID = 0;
	private int 		counter = 0;
	
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
		if (log.isLoggable(Level.INFO)) log.info("SAS_RequestBundle_ID=" + p_SAS_RequestBundle_ID);
		
		if (p_SAS_RequestBundle_ID > 0) {
			MRequestBundle bundle = new Query(getCtx(), MRequestBundle.Table_Name, 
					"SAS_RequestBundle_ID = ? AND Processed='Y'", get_TrxName())
				.setParameters(p_SAS_RequestBundle_ID)
				.first();
			if (bundle != null) {
				bundle.setProcessed(false);
				bundle.saveEx();
				List<MRequest> requestList = new Query(getCtx(), MRequest.Table_Name, 
						"SAS_RequestBundle_ID = ? AND Processed='Y'", get_TrxName())
					.setParameters(p_SAS_RequestBundle_ID)
					.list();
				if (bundle.getR_RequestType_ID() != SASSystemID.REQUEST_TYPE_DAFTAR_TAGIH_OFFICE) {
					for (MRequest request : requestList) {
						//	SAS-156
						if(request.getC_Order_ID()>0){
							try{
								MPeriod.testPeriodOpen(Env.getCtx(), request.getC_Order().getDateOrdered(), request.getC_Order().getC_DocType_ID(), request.getC_Order().getAD_Org_ID());
							}catch (PeriodClosedException e){
								throw new AdempiereException("SAS-156 Cannot Unprocess Document, Is Period Closed? " + request.getDocumentNo());
							}
						}
						if(request.getM_InOut_ID()>0){
							try{
								MPeriod.testPeriodOpen(Env.getCtx(), request.getM_InOut().getMovementDate(), request.getM_InOut().getC_DocType_ID(), request.getM_InOut().getAD_Org_ID());
							}catch (PeriodClosedException e){
								throw new AdempiereException("SAS-156 Cannot Unprocess Document, Is Period Closed? " + request.getDocumentNo());
							}
						}
						if(request.getC_Invoice_ID()>0){
							try{
								MPeriod.testPeriodOpen(Env.getCtx(), request.getC_Invoice().getDateInvoiced(), request.getC_Invoice().getC_DocType_ID(), request.getC_Invoice().getAD_Org_ID());
							}catch (PeriodClosedException e){
								throw new AdempiereException("SAS-156 Cannot Unprocess Document, Is Period Closed? " + request.getDocumentNo());
							}
						}
						if(request.getC_Payment_ID()>0){
							try{
								MPeriod.testPeriodOpen(Env.getCtx(), request.getC_Payment().getDateTrx(), request.getC_Payment().getC_DocType_ID(), request.getC_Payment().getAD_Org_ID());
							}catch (PeriodClosedException e){
								throw new AdempiereException("SAS-156 Cannot Unprocess Document, Is Period Closed? " + request.getDocumentNo());
							}
						}
						if(request.getM_RMA_ID()>0){
							MRMA rma = new MRMA(Env.getCtx(), request.getM_RMA_ID(), get_TrxName());
							try{
								MPeriod.testPeriodOpen(Env.getCtx(), (Timestamp)rma.get_Value("DateOrdered"), rma.getC_DocType_ID(), rma.getAD_Org_ID());
							}catch (PeriodClosedException e){
								throw new AdempiereException("SAS-156 Cannot Unprocess Document, Is Period Closed? " + request.getDocumentNo());
							}
						}
						if(request.get_ValueAsInt("M_Movement_ID")>0){
							MMovement movement = new MMovement(Env.getCtx(), request.get_ValueAsInt("M_Movement_ID"), get_TrxName());
							try{
								MPeriod.testPeriodOpen(Env.getCtx(), movement.getMovementDate(), movement.getC_DocType_ID(), movement.getAD_Org_ID());
							}catch (PeriodClosedException e){
								throw new AdempiereException("SAS-156 Cannot Unprocess Document, Is Period Closed? " + request.getDocumentNo());
							}
						}
						if(request.get_ValueAsInt("Z_Kwitansi_ID")>0){
							MKwitansi kwitansi = new MKwitansi(Env.getCtx(), request.get_ValueAsInt("Z_Kwitansi_ID"), get_TrxName());
							try{
								MPeriod.testPeriodOpen(Env.getCtx(), kwitansi.getDateDoc(), kwitansi.getC_DocType_ID(), kwitansi.getAD_Org_ID());
							}catch (PeriodClosedException e){
								throw new AdempiereException("SAS-156 Cannot Unprocess Document, Is Period Closed? " + request.getDocumentNo());
							}
						}
					}
				}
				for (MRequest request : requestList) {
					request.setProcessed(false);
					request.saveEx();
				}
				addLog(bundle.get_ID(), null, null, bundle.getDocumentNo(), MRequestBundle.Table_ID, bundle.get_ID());
				counter++;
			}
		}
		
		return "@Updated@: " + counter;
	}	//	doIt

}	//	Process Delivery Journal