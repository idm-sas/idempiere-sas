/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2012 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
/** Generated Model - DO NOT CHANGE */
package id.co.databiz.awn.model;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;
import org.compiere.model.*;

/** Generated Model for Z_OrderRef
 *  @author iDempiere (generated) 
 *  @version Release 2.0 b494 - $Id$ */
public class X_Z_OrderRef extends PO implements I_Z_OrderRef, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20141211L;

    /** Standard Constructor */
    public X_Z_OrderRef (Properties ctx, int Z_OrderRef_ID, String trxName)
    {
      super (ctx, Z_OrderRef_ID, trxName);
      /** if (Z_OrderRef_ID == 0)
        {
			setA_Entry_Type (null);
			setC_Order_ID (0);
// @C_Order_ID@
			setZ_OrderRef_ID (0);
        } */
    }

    /** Load Constructor */
    public X_Z_OrderRef (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 3 - Client - Org 
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
      StringBuffer sb = new StringBuffer ("X_Z_OrderRef[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_A_Asset getA_Asset() throws RuntimeException
    {
		return (org.compiere.model.I_A_Asset)MTable.get(getCtx(), org.compiere.model.I_A_Asset.Table_Name)
			.getPO(getA_Asset_ID(), get_TrxName());	}

	/** Set Asset.
		@param A_Asset_ID 
		Asset used internally or by customers
	  */
	public void setA_Asset_ID (int A_Asset_ID)
	{
		if (A_Asset_ID < 1) 
			set_Value (COLUMNNAME_A_Asset_ID, null);
		else 
			set_Value (COLUMNNAME_A_Asset_ID, Integer.valueOf(A_Asset_ID));
	}

	/** Get Asset.
		@return Asset used internally or by customers
	  */
	public int getA_Asset_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_A_Asset_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** A_Entry_Type AD_Reference_ID=550016 */
	public static final int A_ENTRY_TYPE_AD_Reference_ID=550016;
	/** Building = B */
	public static final String A_ENTRY_TYPE_Building = "B";
	/** Electrical = E */
	public static final String A_ENTRY_TYPE_Electrical = "E";
	/** Mechanical = M */
	public static final String A_ENTRY_TYPE_Mechanical = "M";
	/** Utilities = U */
	public static final String A_ENTRY_TYPE_Utilities = "U";
	/** Pre Start Up = 1 */
	public static final String A_ENTRY_TYPE_PreStartUp = "1";
	/** Start Up = 2 */
	public static final String A_ENTRY_TYPE_StartUp = "2";
	/** Check = 3 */
	public static final String A_ENTRY_TYPE_Check = "3";
	/** Troubleshooting = 4 */
	public static final String A_ENTRY_TYPE_Troubleshooting = "4";
	/** Maintenance = 5 */
	public static final String A_ENTRY_TYPE_Maintenance = "5";
	/** Training = 6 */
	public static final String A_ENTRY_TYPE_Training = "6";
	/** Set Entry Type.
		@param A_Entry_Type Entry Type	  */
	public void setA_Entry_Type (String A_Entry_Type)
	{

		set_Value (COLUMNNAME_A_Entry_Type, A_Entry_Type);
	}

	/** Get Entry Type.
		@return Entry Type	  */
	public String getA_Entry_Type () 
	{
		return (String)get_Value(COLUMNNAME_A_Entry_Type);
	}

	public org.compiere.model.I_AD_User getA_User1() throws RuntimeException
    {
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_Name)
			.getPO(getA_User1_ID(), get_TrxName());	}

	/** Set User 1.
		@param A_User1_ID User 1	  */
	public void setA_User1_ID (int A_User1_ID)
	{
		if (A_User1_ID < 1) 
			set_Value (COLUMNNAME_A_User1_ID, null);
		else 
			set_Value (COLUMNNAME_A_User1_ID, Integer.valueOf(A_User1_ID));
	}

	/** Get User 1.
		@return User 1	  */
	public int getA_User1_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_A_User1_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_AD_User getA_User2() throws RuntimeException
    {
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_Name)
			.getPO(getA_User2_ID(), get_TrxName());	}

	/** Set User 2.
		@param A_User2_ID User 2	  */
	public void setA_User2_ID (int A_User2_ID)
	{
		if (A_User2_ID < 1) 
			set_Value (COLUMNNAME_A_User2_ID, null);
		else 
			set_Value (COLUMNNAME_A_User2_ID, Integer.valueOf(A_User2_ID));
	}

	/** Get User 2.
		@return User 2	  */
	public int getA_User2_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_A_User2_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_AD_User getA_User3() throws RuntimeException
    {
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_Name)
			.getPO(getA_User3_ID(), get_TrxName());	}

	/** Set User 3.
		@param A_User3_ID User 3	  */
	public void setA_User3_ID (int A_User3_ID)
	{
		if (A_User3_ID < 1) 
			set_Value (COLUMNNAME_A_User3_ID, null);
		else 
			set_Value (COLUMNNAME_A_User3_ID, Integer.valueOf(A_User3_ID));
	}

	/** Get User 3.
		@return User 3	  */
	public int getA_User3_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_A_User3_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_AD_User getA_User4() throws RuntimeException
    {
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_Name)
			.getPO(getA_User4_ID(), get_TrxName());	}

	/** Set User 4.
		@param A_User4_ID User 4	  */
	public void setA_User4_ID (int A_User4_ID)
	{
		if (A_User4_ID < 1) 
			set_Value (COLUMNNAME_A_User4_ID, null);
		else 
			set_Value (COLUMNNAME_A_User4_ID, Integer.valueOf(A_User4_ID));
	}

	/** Get User 4.
		@return User 4	  */
	public int getA_User4_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_A_User4_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_Order getC_Order() throws RuntimeException
    {
		return (org.compiere.model.I_C_Order)MTable.get(getCtx(), org.compiere.model.I_C_Order.Table_Name)
			.getPO(getC_Order_ID(), get_TrxName());	}

	/** Set Order.
		@param C_Order_ID 
		Order
	  */
	public void setC_Order_ID (int C_Order_ID)
	{
		if (C_Order_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_C_Order_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_C_Order_ID, Integer.valueOf(C_Order_ID));
	}

	/** Get Order.
		@return Order
	  */
	public int getC_Order_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Order_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Date received.
		@param DateReceived 
		Date a product was received
	  */
	public void setDateReceived (Timestamp DateReceived)
	{
		set_Value (COLUMNNAME_DateReceived, DateReceived);
	}

	/** Get Date received.
		@return Date a product was received
	  */
	public Timestamp getDateReceived () 
	{
		return (Timestamp)get_Value(COLUMNNAME_DateReceived);
	}

	/** Set End Time.
		@param EndTime 
		End of the time span
	  */
	public void setEndTime (Timestamp EndTime)
	{
		set_Value (COLUMNNAME_EndTime, EndTime);
	}

	/** Get End Time.
		@return End of the time span
	  */
	public Timestamp getEndTime () 
	{
		return (Timestamp)get_Value(COLUMNNAME_EndTime);
	}

	/** Set Start Time.
		@param StartTime 
		Time started
	  */
	public void setStartTime (Timestamp StartTime)
	{
		set_Value (COLUMNNAME_StartTime, StartTime);
	}

	/** Get Start Time.
		@return Time started
	  */
	public Timestamp getStartTime () 
	{
		return (Timestamp)get_Value(COLUMNNAME_StartTime);
	}

	/** Set Order References.
		@param Z_OrderRef_ID Order References	  */
	public void setZ_OrderRef_ID (int Z_OrderRef_ID)
	{
		if (Z_OrderRef_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_Z_OrderRef_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_Z_OrderRef_ID, Integer.valueOf(Z_OrderRef_ID));
	}

	/** Get Order References.
		@return Order References	  */
	public int getZ_OrderRef_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Z_OrderRef_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Z_OrderRef_UU.
		@param Z_OrderRef_UU Z_OrderRef_UU	  */
	public void setZ_OrderRef_UU (String Z_OrderRef_UU)
	{
		set_Value (COLUMNNAME_Z_OrderRef_UU, Z_OrderRef_UU);
	}

	/** Get Z_OrderRef_UU.
		@return Z_OrderRef_UU	  */
	public String getZ_OrderRef_UU () 
	{
		return (String)get_Value(COLUMNNAME_Z_OrderRef_UU);
	}
}