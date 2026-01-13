package id.co.databiz.sas.model;

import java.sql.ResultSet;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.model.MRequest;
import org.compiere.model.MStatus;
import org.compiere.model.MSysConfig;
import org.compiere.model.Query;
import org.compiere.model.X_R_Status;
import org.compiere.util.Env;

import id.co.databiz.sas.SASSystemID;
import id.co.databiz.sas.utils.ApiUtil;

public class MRequestBundle extends X_SAS_RequestBundle {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2830371435575441630L;


	public MRequestBundle(Properties ctx, int SAS_RequestBundle_ID,
			String trxName) {
		super(ctx, SAS_RequestBundle_ID, trxName);
	}

	
	public MRequestBundle(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
	
	protected boolean beforeSave (boolean newRecord) {
		String salesOrderApiUrl = MSysConfig.getValue("API_SALES_ORDER_URL", "", Env.getAD_Client_ID(Env.getCtx()));
		
		if (is_ValueChanged(COLUMNNAME_R_Status_ID) && getR_Status_ID() > 0) {
			X_R_Status status = new X_R_Status(getCtx(), getR_Status_ID(), get_TrxName());
			if (!status.isOpen()) {
				List<MRequest> requestList = new Query(getCtx(), MRequest.Table_Name, "SAS_RequestBundle_ID = ?", get_TrxName())
					.setOnlyActiveRecords(true)
					.setParameters(get_ID())
					.list();
				for (MRequest request : requestList) {
					if (request.getR_Status_ID() == 0 || !request.getR_Status().isClosed()) {
						request.setR_Status_ID(status.get_ID());
						request.saveEx();
					}
				}
			}
		}
		
		if (is_ValueChanged(COLUMNNAME_Processed) && isProcessed()) {
			List<MRequest> requestList = new Query(getCtx(), MRequest.Table_Name, "SAS_RequestBundle_ID = ?", get_TrxName())
				.setOnlyActiveRecords(true)
				.setParameters(get_ID())
				.list();
			for (MRequest request : requestList) {
				if (request.getR_Status().isOpen()) {
					log.saveError("SaveError", "Line with open status exists"); 
					return false;
				}
				request.setProcessed(true);
				request.saveEx();
				
				String docType = (String) request.get_Value("DocumentType");
				MStatus status = MStatus.get(request.getCtx(), request.getR_Status_ID());
				
				if (salesOrderApiUrl == null || salesOrderApiUrl.isEmpty()) {
				    log.log(Level.SEVERE, "API URL not found in SysConfig (API_SALES_ORDER_URL)");
				    return false;
				}
				
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
		return true;
	}
}
