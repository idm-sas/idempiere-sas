package id.co.databiz.awn.model;

import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.Query;
import org.compiere.util.DB;

public class MTaxInvoice extends X_Z_TaxInvoice {

	public MTaxInvoice(Properties ctx, int Z_TaxInvoice_ID, String trxName) {
		super(ctx, Z_TaxInvoice_ID, trxName);
	}

	public MTaxInvoice (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }
	
	private static final long serialVersionUID = -4558645575984000366L;
	
	protected boolean beforeSave (boolean newRecord){
		if(is_ValueChanged(MTaxInvoice.COLUMNNAME_DocumentNo)){
			// check duplicate DocumentNo
			String whereClause = 
					"Z_TaxInvoice_ID <> ? AND TRIM(DocumentNo) = ? AND IsSOTrx = ? AND C_DocType_ID = ?"; // 1..4
			
			List<Object> params = new ArrayList<Object>();
			params.add(get_ID());
			params.add(getDocumentNo().trim());
			params.add(isSOTrx());
			params.add(getC_DocType_ID());
			
			// Tax Invoice Vendor may have the same DocumentNo for different BP
			if(!isSOTrx()){
				whereClause += " AND C_BPartner_ID = ?"; // 5
				params.add(getC_BPartner_ID());
			}
			int count = new Query(getCtx(), MTaxInvoice.Table_Name, whereClause, get_TrxName())
							.setParameters(params)
							.count();
			if(count > 0){
				throw new AdempiereException("Duplicate Document No " + getDocumentNo() + " of " + getC_BPartner().getName());
			}
			

			// check DocumentNo not exceed the max value
			String sql = "SELECT y.MaxValue "
				+ "FROM AD_Sequence_No y, AD_Sequence s "
				+ "WHERE y.AD_Sequence_ID = s.AD_Sequence_ID "
				+ "AND y.AD_Sequence_ID = ? "
				+ "AND s.AD_Client_ID = ? "
				+ "AND y.CalendarYearMonth = ? "
				+ "AND s.IsActive='Y' AND s.IsTableID='N' AND s.IsAutoSequence='Y' "
				+ "ORDER BY s.AD_Client_ID DESC";
			int maxValue = DB.getSQLValue(null, sql, getC_DocType().getDocNoSequence_ID(),getAD_Client_ID(),new SimpleDateFormat("yyyy").format(getDateInvoiced()));
			int documentNumber = 0;
			
			if(maxValue > 0){
				
				try{
					documentNumber = Integer.parseInt(getDocumentNo().trim());
				} catch(NumberFormatException e){
					throw new AdempiereException("Invalid DocumentNo Format", e);
				}
			
				if(documentNumber > maxValue){
					throw new AdempiereException("DocumentNo exceeds maximum value");
				}
			}
		}
		
		
		return true;
	}
}
