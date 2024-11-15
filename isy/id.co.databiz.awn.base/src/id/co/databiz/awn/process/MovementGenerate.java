
package id.co.databiz.awn.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.exceptions.DBException;
import org.adempiere.exceptions.FillMandatoryException;
import org.compiere.model.MMovement;
import org.compiere.model.MMovementLine;
import org.compiere.model.MProductionLine;
import org.compiere.process.DocAction;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
 
/**
 *	Generate Inventory Move from Production
 *	
 *  @author Anozi Mada
 */
public class MovementGenerate extends SvrProcess
{
	// filter
	private int			p_AD_Org_ID = 0;
	private int			p_M_Production_ID = 0;
	private int			p_M_PartType_ID = 0;
	private Timestamp	p_DateStart = null;
	private Timestamp	p_DateEnd = null;
	
	// target field
	private int			p_C_DocTypeTarget_ID = 0;
	private int			p_M_Locator_ID = 0;
	private String		p_DocAction = null;
	
	private MMovement	m_Movement = null;
	private int			m_created = 0;
	
	/**
	 *  Prepare - e.g., get Parameters.
	 */
	protected void prepare()
	{
		ProcessInfoParameter[] para = getParameter();
		StringBuffer params = new StringBuffer();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals("AD_Org_ID")){
				p_AD_Org_ID = para[i].getParameterAsInt();
				params.append(" AD_Org_ID=").append(p_AD_Org_ID);
			} else if (name.equals("M_Production_ID")){
				p_M_Production_ID = para[i].getParameterAsInt();
				params.append(" M_Production_ID=").append(p_M_Production_ID);
			} else if (name.equals("C_DocTypeTarget_ID")){
				p_C_DocTypeTarget_ID = para[i].getParameterAsInt();
				params.append(" C_TargetDocType_ID=").append(p_C_DocTypeTarget_ID);
			} else if (name.equals("M_PartType_ID")){
				p_M_PartType_ID = para[i].getParameterAsInt();
				params.append(" M_PartType_ID=").append(p_M_PartType_ID);
			} else if (name.equals("M_Locator_ID")){
				p_M_Locator_ID = para[i].getParameterAsInt();
				params.append(" M_Locator_ID=").append(p_M_Locator_ID);
			} else if (name.equals("DocAction")){
				p_DocAction = (String)para[i].getParameter();
				params.append(" DocAction=").append(p_DocAction);
			} else if (name.equals("MovementDate")){
				p_DateStart = (Timestamp)para[i].getParameter();
				p_DateEnd = (Timestamp)para[i].getParameter_To();
				params.append(" MovementDate=").append(p_DateStart);
				params.append(" - =").append(p_DateEnd);
			}
			else
				log.log(Level.SEVERE, "prepare - Unknown Parameter: " + name);
			log.info("Paramters: " + params.toString());
		}		
		
		if (p_C_DocTypeTarget_ID == 0) {
			throw new FillMandatoryException("C_DocTypeTarget_ID");
		}
		if (p_M_Locator_ID == 0) {
			throw new FillMandatoryException("M_Locator_ID");
		}
	}	//	prepare

	/**
	 * 	
	 *	@return status message
	 *	@throws Exception
	 */
	protected String doIt () throws Exception
	{
		List<Object> params = new ArrayList<Object>();
		StringBuffer sql = new StringBuffer();
		sql.append("SELECT pdl.M_ProductionLine_ID ");
		sql.append("FROM M_ProductionLine pdl ");
		sql.append("INNER JOIN M_Production pd ON (pd.M_Production_ID = pdl.M_Production_ID) ");
		sql.append("INNER JOIN M_Product p ON (p.M_Product_ID = pdl.M_Product_ID) ");
		sql.append("WHERE pdl.IsActive='Y' AND pdl.IsEndProduct='N' AND pd.Processed = 'N' AND pd.IsCreated='Y' AND p.IsStocked = 'Y' ");
		//	ISY-165
		sql.append("AND pd.M_Production_ID NOT IN (SELECT DISTINCT M_Production_ID FROM M_Movement WHERE DocStatus!='VO' AND M_Production_ID IS NOT NULL) ");
		if (p_AD_Org_ID > 0) {
			sql.append("AND pdl.AD_Org_ID = ? ");
			params.add(p_AD_Org_ID);
		}
		if (p_M_Production_ID > 0) {
			sql.append("AND pdl.M_Production_ID = ? ");
			params.add(p_M_Production_ID);
		}
		if (p_M_PartType_ID > 0) {
			sql.append("AND p.M_PartType_ID = ? ");
			params.add(p_M_PartType_ID);
		}
		if (p_DateStart != null) {
			sql.append("AND pd.MovementDate >= ? ");
			params.add(p_DateStart);
		}
		if (p_DateEnd != null) {
			sql.append("AND pd.MovementDate <= ? ");
			params.add(p_DateEnd);
		}
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
		  pstmt = DB.prepareStatement(sql.toString(), get_TrxName());
		  DB.setParameters(pstmt, params);
		  rs = pstmt.executeQuery();
		  while(rs.next())
		  {
			  int productionLineID = rs.getInt("M_ProductionLine_ID");
			  MProductionLine productionLine = new MProductionLine(getCtx(), productionLineID, get_TrxName());
			  
			  if (m_Movement == null) {
				  m_Movement = new MMovement(getCtx(), 0, get_TrxName());
				  m_Movement.setAD_Org_ID(p_AD_Org_ID);
				  m_Movement.setC_DocType_ID(p_C_DocTypeTarget_ID);
				  m_Movement.setDescription("Generated from production " + productionLine.getM_Production().getDocumentNo() + " - " +productionLine.getM_Production().getC_OrderLine().getC_Order().getC_BPartner().getName());
				  m_Movement.set_ValueOfColumn("M_Production_ID", p_M_Production_ID);
				  m_Movement.saveEx();
			  }
			  
			  MMovementLine line = new MMovementLine(m_Movement);
				if (productionLine.getQtyUsed().doubleValue() > 0) {
					line.setM_Product_ID(productionLine.getM_Product_ID());
					line.setM_Locator_ID(p_M_Locator_ID);
					line.setM_LocatorTo_ID(productionLine.getM_Locator_ID());
					line.setMovementQty(productionLine.getQtyUsed());
					line.set_ValueOfColumn("QtyEntered",
							productionLine.getQtyUsed());
					line.set_ValueOfColumn("C_UOM_ID", line.getM_Product()
							.getC_UOM_ID());
					line.setDescription("Production "
							+ productionLine.getM_Production().getDocumentNo());
					//line.set_ValueOfColumn("M_ProductionLine_ID",
					//	productionLineID);
					line.saveEx();
				}
		  }
		  
		  
		  if (p_DocAction != null && !p_DocAction.equals(DocAction.ACTION_None)) {
			  m_Movement.setDocAction(p_DocAction);
			  if (!m_Movement.processIt(p_DocAction)) {
				  log.warning("Failed: " + m_Movement);
				  throw new IllegalStateException("Movement Process Failed: " + m_Movement + " - " + m_Movement.getProcessMsg());
			  }
			  m_Movement.saveEx();
		  }
		  
		  if (m_Movement != null) {
			  addLog(m_Movement.get_ID(), m_Movement.getMovementDate(), null, m_Movement.getDocumentNo(), m_Movement.get_Table_ID(), m_Movement.get_ID());
			  m_created++;
		  }
		}
		// If your method is not throwing Exception or SQLException you need this block to catch SQLException
		// and convert them to unchecked DBException
		catch (SQLException e)
		{
		  throw new DBException(e);
		}
		// '''ALWAYS''' close your ResultSet in a finally statement
		finally
		{
		  DB.close(rs, pstmt);
		  rs = null; pstmt = null;
		}
			
		return "@Created@ = " + m_created;
	}	//	doIt
	
}	
