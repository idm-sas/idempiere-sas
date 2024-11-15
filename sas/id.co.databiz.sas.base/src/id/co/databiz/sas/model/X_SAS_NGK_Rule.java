package id.co.databiz.sas.model;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;

import org.compiere.model.I_C_Campaign;
import org.compiere.model.I_Persistent;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.POInfo;
import org.compiere.util.Env;

public class X_SAS_NGK_Rule extends PO implements I_SAS_NGK_Rule, I_Persistent {
	
	private static final long serialVersionUID = -8869357732020766593L;

	/** Standard Constructor */
	   public X_SAS_NGK_Rule (Properties ctx, int SAS_NGK_Rule_ID, String trxName)
	   {
	     super (ctx, SAS_NGK_Rule_ID, trxName);
	   }

	   /** Load Constructor */
	   public X_SAS_NGK_Rule (Properties ctx, ResultSet rs, String trxName)
	   {
	     super (ctx, rs, trxName);
	   }

	   /** AccessLevel
	    * @return 7 - System - Client - Org 
	    */
	   protected int get_AccessLevel()
	   {
	     return accessLevel.intValue();
	   }

	   /** Load Meta Data */
	   protected POInfo initPO (Properties ctx)
	   {
	     POInfo poi = POInfo.getPOInfo (ctx, Table_ID, get_TrxName());
	     return poi;
	   }

	   public String toString()
	   {
	     StringBuilder sb = new StringBuilder ("SAS_NGK_Rule[")
	       .append(get_ID()).append("]");
	     return sb.toString();
	   }

	
	public void setAD_OrgTrx_ID(int AD_OrgTrx_ID) {
		if (AD_OrgTrx_ID < 1) 
			set_Value (COLUMNNAME_AD_OrgTrx_ID, null);
		else 
			set_Value (COLUMNNAME_AD_OrgTrx_ID, Integer.valueOf(AD_OrgTrx_ID));		
	}

	
	public int getAD_OrgTrx_ID() {
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_OrgTrx_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	
	public void setSAS_C_BPartnerNGK_ID(int SAS_C_BPartnerNGK_ID) {
		if (SAS_C_BPartnerNGK_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_SAS_C_BPARTNERNGK_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_SAS_C_BPARTNERNGK_ID, Integer.valueOf(SAS_C_BPartnerNGK_ID));
		
	}

	
	public int getSAS_C_BPartnerNGK_ID() {
		Integer ii = (Integer)get_Value(COLUMNNAME_SAS_C_BPARTNERNGK_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	
	public I_SAS_C_BPartnerNGK getSAS_C_BPartnerNGK() throws RuntimeException {
		return (id.co.databiz.sas.model.I_SAS_C_BPartnerNGK)MTable.get(getCtx(), id.co.databiz.sas.model.I_SAS_C_BPartnerNGK.Table_Name)
				.getPO(getSAS_C_BPartnerNGK_ID(), get_TrxName());
	}

	
	public void setC_Campaign_ID(int C_Campaign_ID) {
		if (C_Campaign_ID < 1) 
			set_Value (COLUMNNAME_C_Campaign_ID, null);
		else 
			set_Value (COLUMNNAME_C_Campaign_ID, Integer.valueOf(C_Campaign_ID));		
	}

	
	public int getC_Campaign_ID() {
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Campaign_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	
	public I_C_Campaign getC_Campaign() throws RuntimeException {
		return (org.compiere.model.I_C_Campaign)MTable.get(getCtx(), org.compiere.model.I_C_Campaign.Table_Name)
				.getPO(getC_Campaign_ID(), get_TrxName());
	}

	
	public void setName(String Name) {
		set_Value (COLUMNNAME_Name, Name);		
	}

	
	public String getName() {
		return (String)get_Value(COLUMNNAME_Name);
	}

	
	public void setValue(String Value) {
		set_Value (COLUMNNAME_Value, Value);
	}

	
	public String getValue() {
		return (String)get_Value(COLUMNNAME_Value);
	}

	
	public void setPercentage(BigDecimal Percentage) {
		set_Value (COLUMNNAME_Percentage, Percentage);
	}

	
	public BigDecimal getPercentage() {
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_Percentage);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	
	public void setValueMax(BigDecimal ValueMax) {
		set_Value (COLUMNNAME_ValueMax, ValueMax);		
	}

	
	public BigDecimal getValueMax() {
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_ValueMax);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	
	public void setValueMin(BigDecimal ValueMin) {
		set_Value (COLUMNNAME_ValueMin, ValueMin);		
	}

	
	public BigDecimal getValueMin() {
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_ValueMin);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	
	public void setLockInValue(BigDecimal LockInValue) {
		set_Value (COLUMNNAME_LockInValue, LockInValue);				
	}

	
	public BigDecimal getLockInValue() {
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_LockInValue);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	
	public void setVoucherValue(BigDecimal VoucherValue) {
		set_Value (COLUMNNAME_VoucherValue, VoucherValue);						
	}

	
	public BigDecimal getVoucherValue() {
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_VoucherValue);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	
	public void setStartDate(Timestamp StartDate) {
		set_Value (COLUMNNAME_StartDate, StartDate);		
	}

	
	public Timestamp getStartDate() {
		return (Timestamp)get_Value(COLUMNNAME_StartDate);
	}

	
	public void setEndDate(Timestamp EndDate) {
		set_Value (COLUMNNAME_EndDate, EndDate);				
	}

	
	public Timestamp getEndDate() {
		return (Timestamp)get_Value(COLUMNNAME_EndDate);
	}

	
	public void setSAS_NGK_Rule_ID(int SAS_NGK_Rule_ID) {
		if (SAS_NGK_Rule_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_SAS_NGK_Rule_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_SAS_NGK_Rule_ID, Integer.valueOf(SAS_NGK_Rule_ID));		
	}

	
	public int getSAS_NGK_Rule_ID() {
		Integer ii = (Integer)get_Value(COLUMNNAME_SAS_NGK_Rule_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

}