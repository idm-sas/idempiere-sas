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
package org.compiere.model;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;

/** Generated Model for Z_WFScenarioLine
 *  @author iDempiere (generated) 
 *  @version Release 1.0b - $Id$ */
public class X_Z_WFScenarioLine extends PO implements I_Z_WFScenarioLine, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20130523L;

    /** Standard Constructor */
    public X_Z_WFScenarioLine (Properties ctx, int Z_WFScenarioLine_ID, String trxName)
    {
      super (ctx, Z_WFScenarioLine_ID, trxName);
      /** if (Z_WFScenarioLine_ID == 0)
        {
			setZ_WFScenario_ID (0);
			setZ_WFScenarioLine_ID (0);
        } */
    }

    /** Load Constructor */
    public X_Z_WFScenarioLine (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 6 - System - Client 
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
      StringBuffer sb = new StringBuffer ("X_Z_WFScenarioLine[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_AD_WF_Responsible getAD_WF_Responsible() throws RuntimeException
    {
		return (org.compiere.model.I_AD_WF_Responsible)MTable.get(getCtx(), org.compiere.model.I_AD_WF_Responsible.Table_Name)
			.getPO(getAD_WF_Responsible_ID(), get_TrxName());	}

	/** Set Workflow Responsible.
		@param AD_WF_Responsible_ID 
		Responsible for Workflow Execution
	  */
	public void setAD_WF_Responsible_ID (int AD_WF_Responsible_ID)
	{
		if (AD_WF_Responsible_ID < 1) 
			set_Value (COLUMNNAME_AD_WF_Responsible_ID, null);
		else 
			set_Value (COLUMNNAME_AD_WF_Responsible_ID, Integer.valueOf(AD_WF_Responsible_ID));
	}

	/** Get Workflow Responsible.
		@return Responsible for Workflow Execution
	  */
	public int getAD_WF_Responsible_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_WF_Responsible_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Grand Total.
		@param GrandTotal 
		Total amount of document
	  */
	public void setGrandTotal (BigDecimal GrandTotal)
	{
		set_Value (COLUMNNAME_GrandTotal, GrandTotal);
	}

	/** Get Grand Total.
		@return Total amount of document
	  */
	public BigDecimal getGrandTotal () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_GrandTotal);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Line No.
		@param Line 
		Unique line for this document
	  */
	public void setLine (BigDecimal Line)
	{
		set_Value (COLUMNNAME_Line, Line);
	}

	/** Get Line No.
		@return Unique line for this document
	  */
	public BigDecimal getLine () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_Line);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	public org.compiere.model.I_R_MailText getR_MailText() throws RuntimeException
    {
		return (org.compiere.model.I_R_MailText)MTable.get(getCtx(), org.compiere.model.I_R_MailText.Table_Name)
			.getPO(getR_MailText_ID(), get_TrxName());	}

	/** Set Mail Template.
		@param R_MailText_ID 
		Text templates for mailings
	  */
	public void setR_MailText_ID (int R_MailText_ID)
	{
		if (R_MailText_ID < 1) 
			set_Value (COLUMNNAME_R_MailText_ID, null);
		else 
			set_Value (COLUMNNAME_R_MailText_ID, Integer.valueOf(R_MailText_ID));
	}

	/** Get Mail Template.
		@return Text templates for mailings
	  */
	public int getR_MailText_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_R_MailText_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_Z_WFScenario getZ_WFScenario() throws RuntimeException
    {
		return (I_Z_WFScenario)MTable.get(getCtx(), I_Z_WFScenario.Table_Name)
			.getPO(getZ_WFScenario_ID(), get_TrxName());	}

	/** Set Workflow Scenario.
		@param Z_WFScenario_ID Workflow Scenario	  */
	public void setZ_WFScenario_ID (int Z_WFScenario_ID)
	{
		if (Z_WFScenario_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_Z_WFScenario_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_Z_WFScenario_ID, Integer.valueOf(Z_WFScenario_ID));
	}

	/** Get Workflow Scenario.
		@return Workflow Scenario	  */
	public int getZ_WFScenario_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Z_WFScenario_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

    /** Get Record ID/ColumnName
        @return ID/ColumnName pair
      */
    public KeyNamePair getKeyNamePair() 
    {
        return new KeyNamePair(get_ID(), String.valueOf(getZ_WFScenario_ID()));
    }

	/** Set Workflow Scenario Line.
		@param Z_WFScenarioLine_ID Workflow Scenario Line	  */
	public void setZ_WFScenarioLine_ID (int Z_WFScenarioLine_ID)
	{
		if (Z_WFScenarioLine_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_Z_WFScenarioLine_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_Z_WFScenarioLine_ID, Integer.valueOf(Z_WFScenarioLine_ID));
	}

	/** Get Workflow Scenario Line.
		@return Workflow Scenario Line	  */
	public int getZ_WFScenarioLine_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Z_WFScenarioLine_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}