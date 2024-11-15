/**
 * 
 */
package id.co.databiz.awn.controller;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MMovement;
import org.compiere.model.MMovementLine;
import org.compiere.util.Env;

/**
 * @author fm
 *
 */
public class MovementController {
	public static void beforePrepare(MMovement movement){
		List<MMovementLine> lines = Arrays.asList(movement.getLines(true));
		//ISY-416 issue validasi di tambahkan karena conversi terhadap pembulatan presisi dari uom nya
		for (MMovementLine line : lines) {
			BigDecimal qtyEntered = (BigDecimal) line.get_Value("QtyEntered");
			BigDecimal movementQty  = line.getMovementQty();
			if (qtyEntered.compareTo(Env.ZERO) != 0) {
				if(movementQty.compareTo(Env.ZERO) == 0) {
					throw new AdempiereException("ISY-416 Movement Quantity cannot be zero on Line No : "+line.getLine()+" - Product : "+line.getProduct().getValue());
				}					
			}
		}
	}
}
