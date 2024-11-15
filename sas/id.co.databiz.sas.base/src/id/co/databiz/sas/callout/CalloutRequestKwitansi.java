package id.co.databiz.sas.callout;

import id.co.databiz.awn.model.MKwitansi;

import java.math.BigDecimal;
import java.util.List;
import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MInvoice;
import org.compiere.model.Query;
import org.compiere.util.Env;

public class CalloutRequestKwitansi implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		if(value == null){
			return "";
		}
		Integer kwitansiID = (Integer) value;
		if(kwitansiID>0){
			// SAS-62
			MKwitansi kwitansi = new MKwitansi(ctx, kwitansiID, null);
			mTab.setValue("C_BPartner_ID", kwitansi.getC_BPartner_ID());
			BigDecimal openAmt = Env.ZERO;
			List<MInvoice> invoiceList = new Query(ctx, MInvoice.Table_Name, "Z_Kwitansi_ID = ?", null)
				.setParameters(kwitansi.get_ID())
				.list();
			for (MInvoice invoice : invoiceList) {
				openAmt = openAmt.add(invoice.getOpenAmt());
			}
			mTab.setValue("OpenAmt", openAmt);
			mTab.setValue("RemainingAmt", openAmt);
		}
		return "";
	}

}
