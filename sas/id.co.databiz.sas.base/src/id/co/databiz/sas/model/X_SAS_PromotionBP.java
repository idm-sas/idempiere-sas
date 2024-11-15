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

import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.model.*;

/** Generated Model for SAS_PromotionBP
 *  @author iDempiere (generated) 
 *  @version Release 3.1 - $Id$ */
public class X_SAS_PromotionBP extends PO implements I_SAS_PromotionBP, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20160805L;

    /** Standard Constructor */
    public X_SAS_PromotionBP (Properties ctx, int SAS_PromotionBP_ID, String trxName)
    {
      super (ctx, SAS_PromotionBP_ID, trxName);
      /** if (SAS_PromotionBP_ID == 0)
        {
			setC_BPartner_ID (0);
			setIsExclude (false);
// N
			setM_Promotion_ID (0);
			setSAS_PromotionBP_ID (0);
        } */
    }

    /** Load Constructor */
    public X_SAS_PromotionBP (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_SAS_PromotionBP[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_C_BPartner getC_BPartner() throws RuntimeException
    {
		return (org.compiere.model.I_C_BPartner)MTable.get(getCtx(), org.compiere.model.I_C_BPartner.Table_Name)
			.getPO(getC_BPartner_ID(), get_TrxName());	}

	/** Set Business Partner .
		@param C_BPartner_ID 
		Identifies a Business Partner
	  */
	public void setC_BPartner_ID (int C_BPartner_ID)
	{
		if (C_BPartner_ID < 1) 
			set_Value (COLUMNNAME_C_BPartner_ID, null);
		else 
			set_Value (COLUMNNAME_C_BPartner_ID, Integer.valueOf(C_BPartner_ID));
	}

	/** Get Business Partner .
		@return Identifies a Business Partner
	  */
	public int getC_BPartner_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_BPartner_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Exclude.
		@param IsExclude 
		Exclude access to the data - if not selected Include access to the data
	  */
	public void setIsExclude (boolean IsExclude)
	{
		set_Value (COLUMNNAME_IsExclude, Boolean.valueOf(IsExclude));
	}

	/** Get Exclude.
		@return Exclude access to the data - if not selected Include access to the data
	  */
	public boolean isExclude () 
	{
		Object oo = get_Value(COLUMNNAME_IsExclude);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	public org.compiere.model.I_M_Promotion getM_Promotion() throws RuntimeException
    {
		return (org.compiere.model.I_M_Promotion)MTable.get(getCtx(), org.compiere.model.I_M_Promotion.Table_Name)
			.getPO(getM_Promotion_ID(), get_TrxName());	}

	/** Set Promotion.
		@param M_Promotion_ID Promotion	  */
	public void setM_Promotion_ID (int M_Promotion_ID)
	{
		if (M_Promotion_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_M_Promotion_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_M_Promotion_ID, Integer.valueOf(M_Promotion_ID));
	}

	/** Get Promotion.
		@return Promotion	  */
	public int getM_Promotion_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Promotion_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Promotion BP Filter.
		@param SAS_PromotionBP_ID Promotion BP Filter	  */
	public void setSAS_PromotionBP_ID (int SAS_PromotionBP_ID)
	{
		if (SAS_PromotionBP_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_SAS_PromotionBP_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_SAS_PromotionBP_ID, Integer.valueOf(SAS_PromotionBP_ID));
	}

	/** Get Promotion BP Filter.
		@return Promotion BP Filter	  */
	public int getSAS_PromotionBP_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_SAS_PromotionBP_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set SAS_PromotionBP_UU.
		@param SAS_PromotionBP_UU SAS_PromotionBP_UU	  */
	public void setSAS_PromotionBP_UU (String SAS_PromotionBP_UU)
	{
		set_Value (COLUMNNAME_SAS_PromotionBP_UU, SAS_PromotionBP_UU);
	}

	/** Get SAS_PromotionBP_UU.
		@return SAS_PromotionBP_UU	  */
	public String getSAS_PromotionBP_UU () 
	{
		return (String)get_Value(COLUMNNAME_SAS_PromotionBP_UU);
	}
}