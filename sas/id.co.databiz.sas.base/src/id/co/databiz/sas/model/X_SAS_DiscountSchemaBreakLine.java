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

/** Generated Model for SAS_DiscountSchemaBreakLine
 *  @author iDempiere (generated) 
 *  @version Release 3.1 - $Id$ */
public class X_SAS_DiscountSchemaBreakLine extends PO implements I_SAS_DiscountSchemaBreakLine, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20160905L;

    /** Standard Constructor */
    public X_SAS_DiscountSchemaBreakLine (Properties ctx, int SAS_DiscountSchemaBreakLine_ID, String trxName)
    {
      super (ctx, SAS_DiscountSchemaBreakLine_ID, trxName);
      /** if (SAS_DiscountSchemaBreakLine_ID == 0)
        {
			setDiscount (Env.ZERO);
// 0
			setSAS_DiscountSchemaBreakLine_ID (0);
			setSeqNo (0);
// @SQL=SELECT NVL(MAX(SeqNo),0)+10 AS DefaultValue FROM SAS_DiscountSchemaBreakLine WHERE SAS_DiscountList_ID=@SAS_DiscountList_ID@
        } */
    }

    /** Load Constructor */
    public X_SAS_DiscountSchemaBreakLine (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_SAS_DiscountSchemaBreakLine[")
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

	public org.compiere.model.I_M_DiscountSchemaBreak getM_DiscountSchemaBreak() throws RuntimeException
    {
		return (org.compiere.model.I_M_DiscountSchemaBreak)MTable.get(getCtx(), org.compiere.model.I_M_DiscountSchemaBreak.Table_Name)
			.getPO(getM_DiscountSchemaBreak_ID(), get_TrxName());	}

	/** Set Discount Schema Break.
		@param M_DiscountSchemaBreak_ID 
		Trade Discount Break
	  */
	public void setM_DiscountSchemaBreak_ID (int M_DiscountSchemaBreak_ID)
	{
		if (M_DiscountSchemaBreak_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_M_DiscountSchemaBreak_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_M_DiscountSchemaBreak_ID, Integer.valueOf(M_DiscountSchemaBreak_ID));
	}

	/** Get Discount Schema Break.
		@return Trade Discount Break
	  */
	public int getM_DiscountSchemaBreak_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_DiscountSchemaBreak_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_SAS_DiscountList getSAS_DiscountList() throws RuntimeException
    {
		return (I_SAS_DiscountList)MTable.get(getCtx(), I_SAS_DiscountList.Table_Name)
			.getPO(getSAS_DiscountList_ID(), get_TrxName());	}

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

	/** Set Discount Schema Break Line.
		@param SAS_DiscountSchemaBreakLine_ID Discount Schema Break Line	  */
	public void setSAS_DiscountSchemaBreakLine_ID (int SAS_DiscountSchemaBreakLine_ID)
	{
		if (SAS_DiscountSchemaBreakLine_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_SAS_DiscountSchemaBreakLine_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_SAS_DiscountSchemaBreakLine_ID, Integer.valueOf(SAS_DiscountSchemaBreakLine_ID));
	}

	/** Get Discount Schema Break Line.
		@return Discount Schema Break Line	  */
	public int getSAS_DiscountSchemaBreakLine_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_SAS_DiscountSchemaBreakLine_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Sequence.
		@param SeqNo 
		Method of ordering records; lowest number comes first
	  */
	public void setSeqNo (int SeqNo)
	{
		set_Value (COLUMNNAME_SeqNo, Integer.valueOf(SeqNo));
	}

	/** Get Sequence.
		@return Method of ordering records; lowest number comes first
	  */
	public int getSeqNo () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_SeqNo);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}