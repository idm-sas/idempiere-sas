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
import id.co.databiz.sas.model.MInvoice;
import id.co.databiz.sas.model.MOrder;
import id.co.databiz.sas.model.MRequestBundle;
import id.co.databiz.sas.utils.ApiUtil;

import java.sql.Timestamp;
import java.util.List;
import java.util.logging.Level;

import org.compiere.model.MRequest;
import org.compiere.model.MStatus;
import org.compiere.model.MSysConfig;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;

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
		String salesOrderApiUrl = MSysConfig.getValue("API_SALES_ORDER_URL", "", Env.getAD_Client_ID(Env.getCtx()));
		
		if (log.isLoggable(Level.INFO)) log.info("SAS_RequestBundle_ID=" + getRecord_ID());
		Timestamp currentTime = new Timestamp(System.currentTimeMillis());
		if (getRecord_ID() > 0) {
			MRequestBundle rb = new MRequestBundle(getCtx(), getRecord_ID(), get_TrxName());
			if (rb.getDepartureTime() == null) {
				rb.setDepartureTime(currentTime);
				rb.set_ValueOfColumn("Depart", "Y");
				rb.setR_Status_ID(SASSystemID.DJ_STATUS_ON_DELIVERY);
				rb.saveEx();
				
				List<MRequest> requestList = new Query(getCtx(), MRequest.Table_Name, "SAS_RequestBundle_ID = ?", get_TrxName())
						.setOnlyActiveRecords(true)
						.setParameters(getRecord_ID())
						.list();
				
				for (MRequest request : requestList) {
					request.setR_Status_ID(SASSystemID.DJ_STATUS_ON_DELIVERY);
					request.saveEx();
					
					String docType = (String) request.get_Value("DocumentType");
					if (request.getR_Status_ID() == SASSystemID.DJ_STATUS_ON_DELIVERY) {
						MStatus status = MStatus.get(request.getCtx(), request.getR_Status_ID());
						
						if (SASSystemID.DJ_DOCTYPE_INVOICE.equals(docType)) {
							int invoiceId = request.getC_Invoice_ID();
							
						    if (invoiceId > 0) {
						        MInvoice invoice = new MInvoice(request.getCtx(), invoiceId, request.get_TrxName());

						        if (invoice.getC_DocType_ID() == SASSystemID.DOCTYPE_AR_INVOICE_SF_TAX 
						        		|| invoice.getC_DocType_ID() == SASSystemID.DOCTYPE_AR_INVOICE_SF_NON_TAX) { // FSN/FST
						            int orderId = invoice.getC_Order_ID();

						            if (orderId > 0) {
						                MOrder order = new MOrder(request.getCtx(), orderId, request.get_TrxName());

						                boolean success = ApiUtil.updateSalesOrderStatus(
						                		salesOrderApiUrl, 
						                		order.getDocumentNo(), 
						                		status.getName()
						                );
						                
						                if (!success) {
						                	log.log(Level.WARNING, "Failed to update sales order status for " + order.getDocumentNo());
						                }
						            }
						        }
						    }
						} else if (SASSystemID.DJ_DOCTYPE_SURAT_JALAN.equals(docType)
						        || SASSystemID.DJ_DOCTYPE_SURAT_JALAN_PARTIAL.equals(docType)) {
							int inoutId = request.getM_InOut_ID();
							
							if (inoutId > 0) {
								MInOut inout = new MInOut(request.getCtx(), inoutId, request.get_TrxName());

						        if (inout.getC_DocType_ID() == SASSystemID.DOCTYPE_SHIPMENT_SSN 
						        		|| inout.getC_DocType_ID() == SASSystemID.DOCTYPE_SHIPMENT_SST) { // SSN/SST
						            int orderId = inout.getC_Order_ID();
						            
						            if (orderId > 0) {
						                MOrder order = new MOrder(request.getCtx(), orderId, request.get_TrxName());
						                
						                boolean success = ApiUtil.updateSalesOrderStatus(
						                		salesOrderApiUrl,
						                		order.getDocumentNo(), 
						                		status.getName()
						                );
						                
						                if (!success) {
						                    log.log(Level.WARNING, "Failed to update sales order status for " + order.getDocumentNo());
						                }
						            }
						        }
							}
						}
					}
				}
			} else if (rb.getArrivalTime() == null) {
				rb.setArrivalTime(currentTime);
				rb.set_ValueOfColumn("Arrive", "Y");
				rb.saveEx();
			}
		}
		return "";
	}	//	doIt

}	//	Process Delivery Journal