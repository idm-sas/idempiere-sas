package id.co.databiz.awn.controller;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.DBException;
import org.adempiere.exceptions.PeriodClosedException;
import org.compiere.model.MInOut;
import org.compiere.model.MLocator;
import org.compiere.model.MPeriod;
import org.compiere.model.MProduct;
import org.compiere.model.MWarehouse;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;

public class InOutController {

	public static void beforeReverseCorrect(MInOut io) {
		// ISY-154
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT COUNT(il.M_InOutLine_ID) FROM C_InvoiceLine il ");
		sb.append("INNER JOIN C_Invoice i ON (i.C_Invoice_ID = il.C_Invoice_ID) ");
		sb.append("INNER JOIN M_InOutLine iol ON (iol.M_InOutLine_ID = il.M_InOutLine_ID) ");
		sb.append("INNER JOIN M_InOut io ON (io.M_InOut_ID = iol.M_InOut_ID) ");
		sb.append("WHERE i.DocStatus NOT IN ('VO','RE') AND io.M_InOut_ID = ? ");
		int count = DB.getSQLValue(null, sb.toString(), io.get_ID());
		if (count > 0){
			throw new AdempiereException ("ISY-154\n You have to void the invoice first");
		}
		
		//ISY-367
		boolean stockReduction = io.getMovementType().contains("-");		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT iol.M_Locator_ID, iol.M_Product_ID, SUM(iol.MovementQty) AS MovementQty ");
		sql.append("FROM M_InOutLine iol ");
		sql.append("INNER JOIN M_Product p ON (p.M_Product_ID = iol.M_Product_ID) ");
		sql.append("WHERE iol.M_InOut_ID = ? AND p.IsStocked = 'Y' ");
		sql.append("GROUP BY iol.M_Locator_ID, iol.M_Product_ID ");
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try
		{
			pstmt = DB.prepareStatement(sql.toString(), io.get_TrxName());
			DB.setParameters(pstmt, new Object[]{io.get_ID()});
			rs = pstmt.executeQuery();
			while (rs.next()) {
				int productID = rs.getInt("M_Product_ID");
				int locatorID = rs.getInt("M_Locator_ID");
				BigDecimal movementQty = rs.getBigDecimal("MovementQty");
				if (movementQty.compareTo(Env.ZERO) > 0) {
					StringBuilder sqlAvailable = new StringBuilder();
					sqlAvailable.append("SELECT COALESCE(SUM(s.QtyOnHand - s.QtyReserved),0) ");
					sqlAvailable.append("FROM M_Storage s ");
					sqlAvailable.append("INNER JOIN M_Locator l ON (l.M_Locator_ID = s.M_Locator_ID) ");
					sqlAvailable.append("WHERE s.M_Product_ID = ? AND l.M_Locator_ID = ? ");
					BigDecimal availableQty = DB.getSQLValueBD(io.get_TrxName(), sqlAvailable.toString(), 
							productID, locatorID); 
					MLocator mLocator = new MLocator(Env.getCtx(), locatorID, io.get_TrxName());
					MWarehouse mWarehouse = new MWarehouse(Env.getCtx(), mLocator.getM_Warehouse_ID(), io.get_TrxName());
					if (movementQty.compareTo(availableQty) > 0 && !stockReduction && mWarehouse.isDisallowNegativeInv()) {
						throw new AdempiereException("ISY-367 \nStock Not Available " +
								MProduct.get(Env.getCtx(), productID).getValue());
					}
				}
			}
		}
		catch (SQLException e)
		{
			throw new DBException(e, sql.toString());
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
	}
	
	public static void beforeReverseAccrual(MInOut io) {
		//ISY-136 Check Period
		try{
			MPeriod.testPeriodOpen(Env.getCtx(), io.getDateAcct(), io.getC_DocType_ID(), io.getAD_Org_ID());
		}catch (PeriodClosedException e){
			throw new AdempiereException("ISY-136 Cannot Reverse Acruall with Void DocAction , Is Period Closed?");
		}
				
		// ISY-154
		boolean stockReduction = io.getMovementType().contains("-");
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT COUNT(il.M_InOutLine_ID) FROM C_InvoiceLine il ");
		sb.append("INNER JOIN C_Invoice i ON (i.C_Invoice_ID = il.C_Invoice_ID) ");
		sb.append("INNER JOIN M_InOutLine iol ON (iol.M_InOutLine_ID = il.M_InOutLine_ID) ");
		sb.append("INNER JOIN M_InOut io ON (io.M_InOut_ID = iol.M_InOut_ID) ");
		sb.append("WHERE i.DocStatus NOT IN ('VO','RE') AND io.M_InOut_ID = ? ");
		int count = DB.getSQLValue(null, sb.toString(), io.get_ID());
		if (count > 0){
			throw new AdempiereException ("ISY-154\n You have to void the invoice first");
		}
		
		//ISY-367
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT iol.M_Locator_ID, iol.M_Product_ID, SUM(iol.MovementQty) AS MovementQty ");
		sql.append("FROM M_InOutLine iol ");
		sql.append("INNER JOIN M_Product p ON (p.M_Product_ID = iol.M_Product_ID) ");
		sql.append("WHERE iol.M_InOut_ID = ? AND p.IsStocked = 'Y' ");
		sql.append("GROUP BY iol.M_Locator_ID, iol.M_Product_ID ");
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try
		{
			pstmt = DB.prepareStatement(sql.toString(), io.get_TrxName());
			DB.setParameters(pstmt, new Object[]{io.get_ID()});
			rs = pstmt.executeQuery();
			while (rs.next()) {
				int productID = rs.getInt("M_Product_ID");
				int locatorID = rs.getInt("M_Locator_ID");
				BigDecimal movementQty = rs.getBigDecimal("MovementQty");
				if (movementQty.compareTo(Env.ZERO) > 0) {
					StringBuilder sqlAvailable = new StringBuilder();
					sqlAvailable.append("SELECT COALESCE(SUM(s.QtyOnHand - s.QtyReserved),0) ");
					sqlAvailable.append("FROM M_Storage s ");
					sqlAvailable.append("INNER JOIN M_Locator l ON (l.M_Locator_ID = s.M_Locator_ID) ");
					sqlAvailable.append("WHERE s.M_Product_ID = ? AND l.M_Locator_ID = ? ");
					BigDecimal availableQty = DB.getSQLValueBD(io.get_TrxName(), sqlAvailable.toString(), 
							productID, locatorID); 
					MLocator mLocator = new MLocator(Env.getCtx(), locatorID, io.get_TrxName());
					MWarehouse mWarehouse = new MWarehouse(Env.getCtx(), mLocator.getM_Warehouse_ID(), io.get_TrxName());
					if (movementQty.compareTo(availableQty) > 0 && !stockReduction && mWarehouse.isDisallowNegativeInv()) {
						throw new AdempiereException("ISY-367 \nStock Not Available " +
								MProduct.get(Env.getCtx(), productID).getValue());
					}
				}
			}
		}
		catch (SQLException e)
		{
			throw new DBException(e, sql.toString());
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
	}
	
	public static void afterReverseAccrual(MInOut io) {
		//ISY-420 Delete MatchPO Journal if MR Reverse
				
		String sqldelete = "delete from fact_acct where ad_table_id = 473 and record_id in "
				+ "(select mm.m_matchpo_id from m_matchpo mm "
				+ "join m_inoutline il on il.m_inoutline_id = mm.m_inoutline_id and il.m_inout_id = ?) ";
		
		DB.executeUpdate(sqldelete, io.getM_InOut_ID(), io.get_TrxName());
		try {
			DB.commit(false, io.get_TrxName());
		} catch (IllegalStateException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void afterReverseCorrect(MInOut io) {
		//ISY-420 Delete MatchPO Journal if MR Reverse
				
		String sqldelete = "delete from fact_acct where ad_table_id = 473 and record_id in "
				+ "(select mm.m_matchpo_id from m_matchpo mm "
				+ "join m_inoutline il on il.m_inoutline_id = mm.m_inoutline_id and il.m_inout_id = ?) ";
		
		DB.executeUpdate(sqldelete, io.getM_InOut_ID(), io.get_TrxName());
		
		try {
			DB.commit(false, io.get_TrxName());
		} catch (IllegalStateException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
	}
}

