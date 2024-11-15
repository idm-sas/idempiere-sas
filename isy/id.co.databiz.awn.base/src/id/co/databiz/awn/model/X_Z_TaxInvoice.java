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

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;
import org.compiere.model.*;
import org.compiere.util.Env;

/** Generated Model for Z_TaxInvoice
 *  @author iDempiere (generated) 
 *  @version Release 1.0a b185 - $Id$ */
public class X_Z_TaxInvoice extends PO implements I_Z_TaxInvoice, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20130610L;

    /** Standard Constructor */
    public X_Z_TaxInvoice (Properties ctx, int Z_TaxInvoice_ID, String trxName)
    {
      super (ctx, Z_TaxInvoice_ID, trxName);
      /** if (Z_TaxInvoice_ID == 0)
        {
			setDocumentNo (null);
			setIsRevision (false);
// N
			setZ_TaxInvoice_ID (0);
        } */
    }

    /** Load Constructor */
    public X_Z_TaxInvoice (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_Z_TaxInvoice[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_AD_User getAD_User() throws RuntimeException
    {
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_Name)
			.getPO(getAD_User_ID(), get_TrxName());	}

	/** Set User/Contact.
		@param AD_User_ID 
		User within the system - Internal or Business Partner Contact
	  */
	public void setAD_User_ID (int AD_User_ID)
	{
		if (AD_User_ID < 1) 
			set_Value (COLUMNNAME_AD_User_ID, null);
		else 
			set_Value (COLUMNNAME_AD_User_ID, Integer.valueOf(AD_User_ID));
	}

	/** Get User/Contact.
		@return User within the system - Internal or Business Partner Contact
	  */
	public int getAD_User_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_User_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
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

	public org.compiere.model.I_C_Currency getC_Currency() throws RuntimeException
    {
		return (org.compiere.model.I_C_Currency)MTable.get(getCtx(), org.compiere.model.I_C_Currency.Table_Name)
			.getPO(getC_Currency_ID(), get_TrxName());	}

	/** Set Currency.
		@param C_Currency_ID 
		The Currency for this record
	  */
	public void setC_Currency_ID (int C_Currency_ID)
	{
		if (C_Currency_ID < 1) 
			set_Value (COLUMNNAME_C_Currency_ID, null);
		else 
			set_Value (COLUMNNAME_C_Currency_ID, Integer.valueOf(C_Currency_ID));
	}

	/** Get Currency.
		@return The Currency for this record
	  */
	public int getC_Currency_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Currency_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_DocType getC_DocType() throws RuntimeException
    {
		return (org.compiere.model.I_C_DocType)MTable.get(getCtx(), org.compiere.model.I_C_DocType.Table_Name)
			.getPO(getC_DocType_ID(), get_TrxName());	}

	/** Set Document Type.
		@param C_DocType_ID 
		Document type or rules
	  */
	public void setC_DocType_ID (int C_DocType_ID)
	{
		if (C_DocType_ID < 0) 
			set_Value (COLUMNNAME_C_DocType_ID, null);
		else 
			set_Value (COLUMNNAME_C_DocType_ID, Integer.valueOf(C_DocType_ID));
	}

	/** Get Document Type.
		@return Document type or rules
	  */
	public int getC_DocType_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_DocType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_Period getC_Period() throws RuntimeException
    {
		return (org.compiere.model.I_C_Period)MTable.get(getCtx(), org.compiere.model.I_C_Period.Table_Name)
			.getPO(getC_Period_ID(), get_TrxName());	}

	/** Set Period.
		@param C_Period_ID 
		Period of the Calendar
	  */
	public void setC_Period_ID (int C_Period_ID)
	{
		if (C_Period_ID < 1) 
			set_Value (COLUMNNAME_C_Period_ID, null);
		else 
			set_Value (COLUMNNAME_C_Period_ID, Integer.valueOf(C_Period_ID));
	}

	/** Get Period.
		@return Period of the Calendar
	  */
	public int getC_Period_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Period_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** CustomerTaxStatus AD_Reference_ID=550003 */
	public static final int CUSTOMERTAXSTATUS_AD_Reference_ID=550003;
	/** 001. Ekspor BKP Berwujud = 001 */
	public static final String CUSTOMERTAXSTATUS_001EksporBKPBerwujud = "001";
	/** 002. Ekspor BKP tidak Berwujud = 002 */
	public static final String CUSTOMERTAXSTATUS_002EksporBKPTidakBerwujud = "002";
	/** 003. Ekpor JKP = 003 */
	public static final String CUSTOMERTAXSTATUS_003EkporJKP = "003";
	/** 01. Kepada Pihak Yang Bukan Pemungut PPN = 01 */
	public static final String CUSTOMERTAXSTATUS_01KepadaPihakYangBukanPemungutPPN = "01";
	/** 02. Kepada Pemungut Bendaharawan = 02 */
	public static final String CUSTOMERTAXSTATUS_02KepadaPemungutBendaharawan = "02";
	/** 03. Kepada Pemungut Selain Bendaharawan = 03 */
	public static final String CUSTOMERTAXSTATUS_03KepadaPemungutSelainBendaharawan = "03";
	/** 04. DPP Nilai Lain = 04 */
	public static final String CUSTOMERTAXSTATUS_04DPPNilaiLain = "04";
	/** 06. Penyerahan Lainnya = 06 */
	public static final String CUSTOMERTAXSTATUS_06PenyerahanLainnya = "06";
	/** 07. Penyerahan yang PPN-nya tidak Dipungut = 07 */
	public static final String CUSTOMERTAXSTATUS_07PenyerahanYangPPN_NyaTidakDipungut = "07";
	/** 08. Penyerahan yang PPN-nya Dibebaskan = 08 */
	public static final String CUSTOMERTAXSTATUS_08PenyerahanYangPPN_NyaDibebaskan = "08";
	/** 09. Penyerahan Aktiva (Pasal 16D UU PPN) = 09 */
	public static final String CUSTOMERTAXSTATUS_09PenyerahanAktivaPasal16DUUPPN = "09";
	/** Set Customer Tax Status.
		@param CustomerTaxStatus Customer Tax Status	  */
	public void setCustomerTaxStatus (String CustomerTaxStatus)
	{

		set_Value (COLUMNNAME_CustomerTaxStatus, CustomerTaxStatus);
	}

	/** Get Customer Tax Status.
		@return Customer Tax Status	  */
	public String getCustomerTaxStatus () 
	{
		return (String)get_Value(COLUMNNAME_CustomerTaxStatus);
	}

	/** Set Custom Prefix.
		@param CustomPrefix 
		Prefix for Custom entities
	  */
	public void setCustomPrefix (String CustomPrefix)
	{
		set_Value (COLUMNNAME_CustomPrefix, CustomPrefix);
	}

	/** Get Custom Prefix.
		@return Prefix for Custom entities
	  */
	public String getCustomPrefix () 
	{
		return (String)get_Value(COLUMNNAME_CustomPrefix);
	}

	/** Set Date Invoiced.
		@param DateInvoiced 
		Date printed on Invoice
	  */
	public void setDateInvoiced (Timestamp DateInvoiced)
	{
		set_Value (COLUMNNAME_DateInvoiced, DateInvoiced);
	}

	/** Get Date Invoiced.
		@return Date printed on Invoice
	  */
	public Timestamp getDateInvoiced () 
	{
		return (Timestamp)get_Value(COLUMNNAME_DateInvoiced);
	}

	/** Set Description.
		@param Description 
		Optional short description of the record
	  */
	public void setDescription (String Description)
	{
		set_Value (COLUMNNAME_Description, Description);
	}

	/** Get Description.
		@return Optional short description of the record
	  */
	public String getDescription () 
	{
		return (String)get_Value(COLUMNNAME_Description);
	}

	/** Set Document No.
		@param DocumentNo 
		Document sequence number of the document
	  */
	public void setDocumentNo (String DocumentNo)
	{
		set_Value (COLUMNNAME_DocumentNo, DocumentNo);
	}

	/** Get Document No.
		@return Document sequence number of the document
	  */
	public String getDocumentNo () 
	{
		return (String)get_Value(COLUMNNAME_DocumentNo);
	}

	/** Set Revision.
		@param IsRevision Revision	  */
	public void setIsRevision (boolean IsRevision)
	{
		set_Value (COLUMNNAME_IsRevision, Boolean.valueOf(IsRevision));
	}

	/** Get Revision.
		@return Revision	  */
	public boolean isRevision () 
	{
		Object oo = get_Value(COLUMNNAME_IsRevision);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Sales Transaction.
		@param IsSOTrx 
		This is a Sales Transaction
	  */
	public void setIsSOTrx (boolean IsSOTrx)
	{
		set_Value (COLUMNNAME_IsSOTrx, Boolean.valueOf(IsSOTrx));
	}

	/** Get Sales Transaction.
		@return This is a Sales Transaction
	  */
	public boolean isSOTrx () 
	{
		Object oo = get_Value(COLUMNNAME_IsSOTrx);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Processed.
		@param Processed 
		The document has been processed
	  */
	public void setProcessed (boolean Processed)
	{
		set_Value (COLUMNNAME_Processed, Boolean.valueOf(Processed));
	}

	/** Get Processed.
		@return The document has been processed
	  */
	public boolean isProcessed () 
	{
		Object oo = get_Value(COLUMNNAME_Processed);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Tax Amount.
		@param TaxAmt 
		Tax Amount for a document
	  */
	public void setTaxAmt (BigDecimal TaxAmt)
	{
		set_Value (COLUMNNAME_TaxAmt, TaxAmt);
	}

	/** Get Tax Amount.
		@return Tax Amount for a document
	  */
	public BigDecimal getTaxAmt () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_TaxAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Tax base Amount.
		@param TaxBaseAmt 
		Base for calculating the tax amount
	  */
	public void setTaxBaseAmt (BigDecimal TaxBaseAmt)
	{
		set_Value (COLUMNNAME_TaxBaseAmt, TaxBaseAmt);
	}

	/** Get Tax base Amount.
		@return Base for calculating the tax amount
	  */
	public BigDecimal getTaxBaseAmt () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_TaxBaseAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Tax ID.
		@param TaxID 
		Tax Identification
	  */
	public void setTaxID (String TaxID)
	{
		set_Value (COLUMNNAME_TaxID, TaxID);
	}

	/** Get Tax ID.
		@return Tax Identification
	  */
	public String getTaxID () 
	{
		return (String)get_Value(COLUMNNAME_TaxID);
	}

	/** Set Tax Invoice.
		@param Z_TaxInvoice_ID Tax Invoice	  */
	public void setZ_TaxInvoice_ID (int Z_TaxInvoice_ID)
	{
		if (Z_TaxInvoice_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_Z_TaxInvoice_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_Z_TaxInvoice_ID, Integer.valueOf(Z_TaxInvoice_ID));
	}

	/** Get Tax Invoice.
		@return Tax Invoice	  */
	public int getZ_TaxInvoice_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Z_TaxInvoice_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}