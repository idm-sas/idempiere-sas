
package id.co.databiz.awn.process;

import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MMovement;
import org.compiere.model.MMovementLine;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 *  Copy Expense Report Lines
 *
 *	@author Wydiyanto
 */
public class CopyFromMovement extends SvrProcess
{
	private String 		pDocumentNo = null;
	private int			pMovementID = 0;
	private int			pLocatorID = 0;
	private int			pLocatorToID = 0;
	private String 		pIsOpenQtyOnly = null;
	
	private int			counter = 0;

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
			else if (name.equals("M_Movement_ID"))
				pMovementID = ((BigDecimal)para[i].getParameter()).intValue();
			else if (name.equals("M_Locator_ID"))
				pLocatorID = ((BigDecimal)para[i].getParameter()).intValue();
			else if (name.equals("M_LocatorTo_ID"))
				pLocatorToID = ((BigDecimal)para[i].getParameter()).intValue();
			else if (name.equals("IsOpenQtyOnly"))
				pIsOpenQtyOnly = para[i].getParameter().toString();
			else if (name.equals("DocumentNo"))
				pDocumentNo = para[i].getParameter().toString();
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
		int toMovement_ID = getRecord_ID();
		
		if (pDocumentNo!= null && pDocumentNo.isEmpty()) {
			pDocumentNo = null;
		}
		
		if (pMovementID <= 0 && pDocumentNo == null) {
			throw new AdempiereException("You must fill Document No or Inventory Move");
		}
		
		if (log.isLoggable(Level.INFO)) log.info("From M_Movement_ID=" + pMovementID + " to " + toMovement_ID);
		if (toMovement_ID == 0)
			throw new IllegalArgumentException("Target M_Movement_ID == 0");
		
		MMovement from = null;
		if (pDocumentNo != null) {
			from = new Query(getCtx(), MMovement.Table_Name, "UPPER(DocumentNo) = ?", get_TrxName())
				.setClient_ID()
				.setParameters(pDocumentNo.toUpperCase().trim())
				.first();
			if (from == null) {
				throw new AdempiereException("Invalid Document no " + pDocumentNo);
			}
		} else if (pMovementID > 0) {
			from = new MMovement (getCtx(), pMovementID, get_TrxName());
		}
		if (from == null)
			throw new IllegalArgumentException("Source Inventory Move not found");
		
		MMovement to = new MMovement (getCtx(), toMovement_ID, get_TrxName());
		//
		
		int no = copyLinesFrom (from, to);		//	no Attributes
		if (counter > 0) {
			to.set_ValueNoCheck("Ref_Movement_ID", from.getM_Movement_ID());
			to.saveEx();
		}
		//
		return "@Copied@=" + counter;
	}	//	doIt
	
	private int copyLinesFrom(MMovement from, MMovement to){
		List<MMovementLine> lines = new Query(getCtx(), MMovementLine.Table_Name, "M_Movement_ID=?", get_TrxName())
			.setOnlyActiveRecords(true)
			.setParameters(from.get_ID())
			.setOrderBy("Line")
			.list();
		
		for (MMovementLine lineFrom : lines) {
			MMovementLine lineTo = new MMovementLine(getCtx(), 0, get_TrxName());
			PO.copyValues(lineFrom, lineTo);
			lineTo.setM_Movement_ID(to.get_ID());
			lineTo.set_ValueNoCheck("M_MovementLine_ID", 0);
			lineTo.setAD_Org_ID(lineFrom.getAD_Org_ID());
			if(pLocatorID>0)
				lineTo.setM_Locator_ID(pLocatorID);
			else
				lineTo.setM_Locator_ID(lineFrom.getM_LocatorTo_ID());
			lineTo.setM_LocatorTo_ID(pLocatorToID);
			lineTo.set_ValueOfColumn("Ref_MovementLine_ID", lineFrom.getM_MovementLine_ID());
			lineTo.setProcessed(false);
			
			if(pIsOpenQtyOnly.equalsIgnoreCase("Y")) {
				String sql = "SELECT COALESCE(SUM(ml.MovementQty),0) " +
						 "FROM M_MovementLine ml " +
						 "INNER JOIN M_Movement m ON (m.M_Movement_ID=ml.M_Movement_ID) " +
						 "WHERE m.DocStatus NOT IN ('VO','RE') " +
						 "AND ml.Ref_MovementLine_ID=?";
				BigDecimal moveQty = DB.getSQLValueBD(get_TrxName(), sql, lineFrom.get_ID());
				if (moveQty.compareTo(lineFrom.getMovementQty()) > 0) {
					lineTo.setMovementQty(Env.ZERO);
					lineTo.set_ValueOfColumn("QtyEntered", Env.ZERO);
				} else {
					lineTo.setMovementQty(lineFrom.getMovementQty().subtract(moveQty));
					BigDecimal qtyEntered = ((BigDecimal)lineFrom.get_Value("QtyEntered")).divide(lineFrom.getMovementQty())
							.multiply(lineTo.getMovementQty());
					lineTo.set_ValueOfColumn("QtyEntered", qtyEntered);
				}
			}
			if (lineTo.getMovementQty().compareTo(Env.ZERO) != 0) {
				lineTo.saveEx();
				counter++;
			}
		}
		return lines.size();
	}
}
