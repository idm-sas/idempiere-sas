package id.co.databiz.awn.model;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;

import org.compiere.util.DB;

public class MKwitansi extends X_Z_Kwitansi {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1598008703544829329L;

	public MKwitansi(Properties ctx, int Z_Kwitansi_ID, String trxName) {
		super(ctx, Z_Kwitansi_ID, trxName);
	}

	public MKwitansi (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }
	
}