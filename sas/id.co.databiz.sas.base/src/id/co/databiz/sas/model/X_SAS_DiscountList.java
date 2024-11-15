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
package id.co.databiz.sas.model;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.model.*;
import org.compiere.util.Env;

/** Generated Model for SAS_DiscountList
 *  @author iDempiere (generated) 
 *  @version Release 3.1 - $Id$ */
public class X_SAS_DiscountList extends PO implements I_SAS_DiscountList, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20160905L;

    /** Standard Constructor */
    public X_SAS_DiscountList (Properties ctx, int SAS_DiscountList_ID, String trxName)
    {
      super (ctx, SAS_DiscountList_ID, trxName);
      /** if (SAS_DiscountList_ID == 0)
        {
			setDiscount (Env.ZERO);
// 0
			setSAS_DiscountList_ID (0);
        } */
    }

    /** Load Constructor */
    public X_SAS_DiscountList (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_SAS_DiscountList[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Discount %.
		@param Discount 
		Discount in percent
	  */
	public void setDiscount (BigDecimal Discount)
	{
		set_Value (COLUMNNAME_Discount, Discount);
	}

	/** Get Discount %.
		@return Discount in percent
	  */
	public BigDecimal getDiscount () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_Discount);
		if (bd == null)
			 return Env.ZERO;
		return bd;
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

	/** Set Name 2.
		@param Name2 
		Additional Name
	  */
	public void setName2 (String Name2)
	{
		set_Value (COLUMNNAME_Name2, Name2);
	}

	/** Get Name 2.
		@return Additional Name
	  */
	public String getName2 () 
	{
		return (String)get_Value(COLUMNNAME_Name2);
	}

	/** Set Discount List.
		@param SAS_DiscountList_ID Discount List	  */
	public void setSAS_DiscountList_ID (int SAS_DiscountList_ID)
	{
		if (SAS_DiscountList_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_SAS_DiscountList_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_SAS_DiscountList_ID, Integer.valueOf(SAS_DiscountList_ID));
	}

	/** Get Discount List.
		@return Discount List	  */
	public int getSAS_DiscountList_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_SAS_DiscountList_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set SAS_DiscountList_UU.
		@param SAS_DiscountList_UU SAS_DiscountList_UU	  */
	public void setSAS_DiscountList_UU (String SAS_DiscountList_UU)
	{
		set_Value (COLUMNNAME_SAS_DiscountList_UU, SAS_DiscountList_UU);
	}

	/** Get SAS_DiscountList_UU.
		@return SAS_DiscountList_UU	  */
	public String getSAS_DiscountList_UU () 
	{
		return (String)get_Value(COLUMNNAME_SAS_DiscountList_UU);
	}
}