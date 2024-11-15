package id.co.databiz.awn.controller;

import org.compiere.model.MTimeExpense;

public class TimeExpenseController {
	
	public static void setProcessed(MTimeExpense expense){
		expense.setProcessed(true);
		expense.saveEx();
	}
}
