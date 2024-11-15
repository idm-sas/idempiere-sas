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
package id.co.databiz.awn.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

import org.adempiere.exceptions.DBException;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereSystemError;
import org.compiere.util.DB;

/**
 * Storage Correction
 * 
 * @author Anozi Mada
 * 
 */
public class StorageCorrection extends SvrProcess {

	private int count = 0;
	private int productID = 0;
	/**
	 * Prepare - e.g., get Parameters.
	 */
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++) {
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals("M_Product_ID")){
				productID = para[i].getParameterAsInt();
			}	
			else
				log.log(Level.SEVERE, "prepare - Unknown Parameter: " + name);
		}
	} // prepare

	/**
	 * Perform process.
	 * 
	 * @return Message
	 * @throws Exception
	 *             if not successful
	 */
	protected String doIt() throws AdempiereSystemError
	{
		StringBuffer sql = new StringBuffer();

		sql.append("select t.m_locator_id, t.m_product_id, t.m_attributesetinstance_id, sum(t.movementqty) MovementQty, s.qtyonhand ");
		sql.append("from m_transaction t ");
		sql.append("inner join m_locator l on (l.m_locator_id = t.m_locator_id) ");
		sql.append("inner join (select sum(sx.qtyonhand) qtyonhand, sx.m_product_id, sx.m_locator_id, sx.m_attributesetinstance_id ");
		sql.append("			from m_storage sx ");
		sql.append("			group by sx.m_product_id,sx.m_locator_id,sx.m_attributesetinstance_id) s on (s.m_product_id=t.m_product_id and s.m_locator_id=t.m_locator_id and t.m_attributesetinstance_id=s.m_attributesetinstance_id) ");
		sql.append("where 1=1 ");
		if (productID > 0) {
			sql.append(" AND t.m_product_id = ").append(productID).append("");
		}
		sql.append("group by t.m_locator_id, t.m_product_id, t.m_attributesetinstance_id, s.qtyonhand ");
		sql.append("HAVING SUM(t.MovementQty) <> s.QtyOnHand  ");
		
		StringBuffer sqlUpdate = new StringBuffer();
		sqlUpdate.append("UPDATE M_StorageOnHand s SET QtyOnHand = QtyOnHand + (?) ");
		sqlUpdate.append("WHERE M_Locator_ID = ? AND M_Product_ID = ? AND M_AttributeSetInstance_ID = ? ");
		sqlUpdate.append("AND DateMaterialPolicy=(SELECT MAX(sx.DateMaterialPolicy) FROM M_StorageOnHand sx WHERE sx.M_Locator_ID = s.M_Locator_ID AND sx.M_Product_ID = s.M_Product_ID AND sx.M_AttributeSetInstance_ID = s.M_AttributeSetInstance_ID) ");
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
		  pstmt = DB.prepareStatement(sql.toString(), get_TrxName());
//		  DB.setParameters(pstmt, new Object[]{...''parameters''...});
		  rs = pstmt.executeQuery();
		  while(rs.next())
		  {
			  int locatorID = rs.getInt("M_Locator_ID");
			  int productID = rs.getInt("M_Product_ID");
			  int asiID = rs.getInt("M_AttributeSetInstance_ID");
			  BigDecimal movementQty = rs.getBigDecimal("MovementQty");
			  BigDecimal qtyOnHand = rs.getBigDecimal("QtyOnHand");
			  BigDecimal deltaQty = movementQty.subtract(qtyOnHand);
			  
			  DB.executeUpdateEx(sqlUpdate.toString(), new Object[]{deltaQty,locatorID,productID,asiID}, get_TrxName());
			  String message = "M_Locator_ID=" + locatorID + ", M_Product_ID=" + productID + 
					  ", M_AttributeSetInstance_ID=" + asiID + ", DeltaQty=" + deltaQty;
			  addLog(message);
			  count++;
		  }
		}
		// If your method is not throwing Exception or SQLException you need this block to catch SQLException
		// and convert them to unchecked DBException
		catch (SQLException e)
		{
		  throw new DBException(e, sql.toString());
		}
		// '''ALWAYS''' close your ResultSet in a finally statement
		finally
		{
		  DB.close(rs, pstmt);
		  rs = null; pstmt = null;
		}

		StringBuffer sqlReserve = new StringBuffer();
		
		sqlReserve.append("select s.m_warehouse_id, s.m_product_id, s.issotrx, ");
		sqlReserve.append("coalesce(po.qtyreserved,0) qtyreserved, sum(s.qty) qty ");
		sqlReserve.append("from m_storagereservation s ");
		sqlReserve.append("left join ( ");
		sqlReserve.append("		select o.issotrx,o.m_warehouse_id,o.m_product_id,sum(o.qtyreserved) qtyreserved ");
		sqlReserve.append("		from rv_orderdetail o ");
		sqlReserve.append("		group by o.issotrx,o.m_warehouse_id,o.m_product_id ");
		sqlReserve.append(") po ON (po.issotrx=s.issotrx and po.m_product_id = s.m_product_id and po.m_warehouse_id = s.m_warehouse_id) ");
		sqlReserve.append("where 1=1 ");
		if (productID > 0) {
			sqlReserve.append(" AND s.m_product_id = ").append(productID).append("");
		}
		sqlReserve.append("group by s.m_product_id,s.issotrx,s.m_warehouse_id,po.qtyreserved ");
		sqlReserve.append("having po.qtyreserved <> sum(s.qty) ");
		sqlReserve.append("or (sum(s.qty) <> 0 and coalesce(po.qtyreserved,0) = 0) ");
		
		StringBuffer sqlUpdateReserve = new StringBuffer();
		sqlUpdateReserve.append("UPDATE M_StorageReservation SET Qty = Qty + (?) ");
		sqlUpdateReserve.append("WHERE M_Warehouse_ID = ? AND M_Product_ID = ? AND IsSOTrx = ? AND M_AttributeSetInstance_ID = 0 ");
		
		try
		{
		  pstmt = DB.prepareStatement(sqlReserve.toString(), get_TrxName());
//		  DB.setParameters(pstmt, new Object[]{...''parameters''...});
		  rs = pstmt.executeQuery();
		  while(rs.next())
		  {
			  int warehouseID = rs.getInt("M_Warehouse_ID");
			  int productID = rs.getInt("M_Product_ID");
			  String isSOTrx = rs.getString("IsSOTrx");
			  BigDecimal qtyReserved = rs.getBigDecimal("QtyReserved");
			  BigDecimal qty = rs.getBigDecimal("Qty");
			  BigDecimal deltaQty = qtyReserved.subtract(qty);
			  
			  DB.executeUpdateEx(sqlUpdateReserve.toString(), new Object[]{deltaQty,warehouseID,productID,isSOTrx}, get_TrxName());
			  String message = "M_Warehouse_ID=" + warehouseID + ", M_Product_ID=" + productID + 
					  ", IsSOTrx=" + isSOTrx + ", DeltaQty=" + deltaQty;
			  addLog(message);
			  count++;
		  }
		}
		// If your method is not throwing Exception or SQLException you need this block to catch SQLException
		// and convert them to unchecked DBException
		catch (SQLException e)
		{
		  throw new DBException(e, sql.toString());
		}
		// '''ALWAYS''' close your ResultSet in a finally statement
		finally
		{
		  DB.close(rs, pstmt);
		  rs = null; pstmt = null;
		}
		
		return "@Updated@: " + count;
	}	//	doIt
} // OrderOpen
