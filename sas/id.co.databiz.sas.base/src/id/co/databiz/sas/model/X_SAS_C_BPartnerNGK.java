package id.co.databiz.sas.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.I_Persistent;
import org.compiere.model.PO;
import org.compiere.model.POInfo;

public class X_SAS_C_BPartnerNGK extends PO implements I_SAS_C_BPartnerNGK, I_Persistent{
	
	private static final long serialVersionUID = 4753962192906951029L;

   /** Standard Constructor */
   public X_SAS_C_BPartnerNGK (Properties ctx, int SAS_C_BPartnerNGK_ID, String trxName)
   {
     super (ctx, SAS_C_BPartnerNGK_ID, trxName);
   }

   /** Load Constructor */
   public X_SAS_C_BPartnerNGK (Properties ctx, ResultSet rs, String trxName)
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
     StringBuilder sb = new StringBuilder ("SAS_C_BPartnerNGK[")
       .append(get_ID()).append("]");
     return sb.toString();
   }
   
   /** Set SAS_C_BPartnerNGK_ID.
	@param SAS_C_BPartnerNGK_ID SAS_C_BPartnerNGK_ID	  */
   public void setSAS_C_BPartnerNGK_ID (int SAS_C_BPartnerNGK_ID)
   {
		if (SAS_C_BPartnerNGK_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_SAS_C_BPARTNERNGK_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_SAS_C_BPARTNERNGK_ID, Integer.valueOf(SAS_C_BPartnerNGK_ID));
   }
	
   /** Get SAS_C_BPartnerNGK_ID.
	@return SAS_C_BPartnerNGK_ID	  */
	public int getSAS_C_BPartnerNGK_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_SAS_C_BPARTNERNGK_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Name.
		@param Name 
		Alphanumeric identifier of the entity
	 */
	public void setName (String Name)
	{
		set_Value (COLUMNNAME_Name, Name);
	}
	
	/** Get Name.
		@return Alphanumeric identifier of the entity
	  */
	public String getName () 
	{
		return (String)get_Value(COLUMNNAME_Name);
	}
	
	/** Set Search Key.
		@param Value 
		Search key for the record in the format required - must be unique
	  */
	public void setValue (String Value)
	{
		set_Value (COLUMNNAME_Value, Value);
	}
	
	/** Get Search Key.
		@return Search key for the record in the format required - must be unique
	  */
	public String getValue () 
	{
		return (String)get_Value(COLUMNNAME_Value);
	}

}