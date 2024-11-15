package id.co.databiz.sas.model;

import java.sql.ResultSet;
import java.util.List;
import java.util.Properties;

import org.compiere.model.MRequest;
import org.compiere.model.Query;
import org.compiere.model.X_R_Status;

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
			}
		}
		return true;
	}
}
