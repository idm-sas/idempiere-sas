package id.co.databiz.awn.factory;

import id.co.databiz.awn.model.MInOut;
import id.co.databiz.awn.model.MPayment;
import id.co.databiz.awn.model.MProduction;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;

import org.adempiere.base.IDocFactory;
import org.compiere.acct.Doc;
import org.compiere.acct.DocAllocationHdr;
import org.compiere.acct.DocBankStatement;
import org.compiere.acct.DocInOut_ISY;
import org.compiere.acct.DocInventory;
import org.compiere.acct.DocInvoice;
import org.compiere.acct.DocMatchInv;
import org.compiere.acct.DocMatchPO;
import org.compiere.acct.DocMovement;
import org.compiere.acct.DocPayment;
import org.compiere.acct.DocProduction;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MAllocationHdr;
import org.compiere.model.MBankStatement;
import org.compiere.model.MInventory;
import org.compiere.model.MInvoice;
import org.compiere.model.MMatchInv;
import org.compiere.model.MMatchPO;
import org.compiere.model.MMovement;
import org.compiere.model.MTable;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;

public class DocFactory implements IDocFactory {

	private final static CLogger s_log = CLogger.getCLogger(DocFactory.class);

	@Override
	public Doc getDocument(MAcctSchema as, int AD_Table_ID, int Record_ID,
			String trxName) {
		String tableName = MTable.getTableName(Env.getCtx(), AD_Table_ID);
		//
		Doc doc = null;
		StringBuffer sql = new StringBuffer("SELECT * FROM ")
			.append(tableName)
			.append(" WHERE ").append(tableName).append("_ID=? AND Processed='Y'");
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql.toString(), trxName);
			pstmt.setInt (1, Record_ID);
			rs = pstmt.executeQuery ();
			if (rs.next ())
			{
				if(tableName.equals(MAllocationHdr.Table_Name)){
					doc = new DocAllocationHdr(as, rs, trxName);
				} else if(tableName.equals(MInvoice.Table_Name)){
					doc = new DocInvoice(as, rs, trxName);
				} else if(tableName.equals(MPayment.Table_Name)){
					doc = new DocPayment(as, rs, trxName);
				} else if(tableName.equals(MMatchInv.Table_Name)){
					doc = new DocMatchInv(as, rs, trxName);
				} else if(tableName.equals(MMatchPO.Table_Name)){
					doc = new DocMatchPO(as, rs, trxName);
				} else if(tableName.equals(MProduction.Table_Name)){
					doc = new DocProduction(as, rs, trxName);
				} else if(tableName.equals(MBankStatement.Table_Name)){
					doc = new DocBankStatement(as, rs, trxName);
				} else if(tableName.equals(MInventory.Table_Name)){
					doc = new DocInventory(as, rs, trxName);
				} else if(tableName.equals(MInOut.Table_Name)){
					doc = new DocInOut_ISY(as, rs, trxName);
				} else if(tableName.equals(MMovement.Table_Name)){
					doc = new DocMovement(as, rs, trxName);
				} else {
					doc = getDocument(as, AD_Table_ID, rs, trxName);
				}
			}
			else
				s_log.severe("Not Found: " + tableName + "_ID=" + Record_ID);
		}
		catch (Exception e)
		{
			s_log.log (Level.SEVERE, sql.toString(), e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		return doc;
	}

	@Override
	public Doc getDocument(MAcctSchema as, int AD_Table_ID, ResultSet rs,
			String trxName) {
		
		if(AD_Table_ID==MAllocationHdr.Table_ID){
			return new DocAllocationHdr(as, rs, trxName);
		} else if(AD_Table_ID==MInvoice.Table_ID){
			return new DocInvoice(as, rs, trxName);
		} else if(AD_Table_ID==MPayment.Table_ID){
			return new DocPayment(as, rs, trxName);
		} else if(AD_Table_ID==MMatchInv.Table_ID){
			return new DocMatchInv(as, rs, trxName);
		} else if(AD_Table_ID==MMatchPO.Table_ID){
			return new DocMatchPO(as, rs, trxName);
		} else if(AD_Table_ID==MProduction.Table_ID){
			return new DocProduction(as, rs, trxName);
		} else if(AD_Table_ID==MBankStatement.Table_ID){
			return new DocBankStatement(as, rs, trxName);
		} else if(AD_Table_ID==MInventory.Table_ID){
			return new DocInventory(as, rs, trxName);
		} else if(AD_Table_ID==MInOut.Table_ID){
			return new DocInOut_ISY(as, rs, trxName);
		} else if(AD_Table_ID==MMovement.Table_ID){
			return new DocMovement(as, rs, trxName);
		}
		
		return null;
	}

}
