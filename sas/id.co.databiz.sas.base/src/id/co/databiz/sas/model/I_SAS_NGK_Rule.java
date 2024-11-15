package id.co.databiz.sas.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

import org.compiere.model.MTable;
import org.compiere.util.KeyNamePair;

public interface I_SAS_NGK_Rule {
	
	/** TableName=SAS_NGK_Rule */
    public static final String Table_Name = "SAS_NGK_Rule";

//    public static final int Table_ID = 550169;
    public static final int Table_ID = MTable.getTable_ID(Table_Name);

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 7 - System - Client - Org 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(7);

    /** Load Meta Data */

    /** Column name AD_Client_ID */
    public static final String COLUMNNAME_AD_Client_ID = "AD_Client_ID";

	/** Get Client.
	  * Client/Tenant for this installation.
	  */
	public int getAD_Client_ID();

    /** Column name AD_Org_ID */
    public static final String COLUMNNAME_AD_Org_ID = "AD_Org_ID";

	/** Set Organization.
	  * Organizational entity within client
	  */
	public void setAD_Org_ID (int AD_Org_ID);

	/** Get Organization.
	  * Organizational entity within client
	  */
	public int getAD_Org_ID();
	
	 /** Column name AD_OrgTrx_ID */
    public static final String COLUMNNAME_AD_OrgTrx_ID = "AD_OrgTrx_ID";

	/** Set Trx Organization.
	  * Performing or initiating organization
	  */
	public void setAD_OrgTrx_ID (int AD_OrgTrx_ID);

	/** Get Trx Organization.
	  * Performing or initiating organization
	  */
	public int getAD_OrgTrx_ID();
	
	/** Column name SAS_C_BPARTNERNGK_ID */
    public static final String COLUMNNAME_SAS_C_BPARTNERNGK_ID = "SAS_C_BPartnerNGK_ID";

	/** Set SAS_C_BPARTNERNGK_ID	  */
	public void setSAS_C_BPartnerNGK_ID (int SAS_C_BPartnerNGK_ID);

	/** Get SAS_C_BPARTNERNGK_ID	  */
	public int getSAS_C_BPartnerNGK_ID();
	
	public id.co.databiz.sas.model.I_SAS_C_BPartnerNGK getSAS_C_BPartnerNGK() throws RuntimeException;
	
	/** Column name C_Campaign_ID */
    public static final String COLUMNNAME_C_Campaign_ID = "C_Campaign_ID";

	/** Set Campaign.
	  * Marketing Campaign
	  */
	public void setC_Campaign_ID (int C_Campaign_ID);

	/** Get Campaign.
	  * Marketing Campaign
	  */
	public int getC_Campaign_ID();

	public org.compiere.model.I_C_Campaign getC_Campaign() throws RuntimeException;
	
	/** Column name Name */
    public static final String COLUMNNAME_Name = "Name";

	/** Set Name.
	  * Alphanumeric identifier of the entity
	  */
	public void setName (String Name);

	/** Get Name.
	  * Alphanumeric identifier of the entity
	  */
	public String getName();
	
	/** Column name Value */
    public static final String COLUMNNAME_Value = "Value";

	/** Set Search Key.
	  * Search key for the record in the format required - must be unique
	  */
	public void setValue (String Value);

	/** Get Search Key.
	  * Search key for the record in the format required - must be unique
	  */
	public String getValue();
	
	/** Column name Percentage */
    public static final String COLUMNNAME_Percentage = "Percentage";

	/** Set Percentage	  */
	public void setPercentage (BigDecimal Percentage);

	/** Get Percentage	  */
	public BigDecimal getPercentage();
	
	/** Column name ValueMax */
    public static final String COLUMNNAME_ValueMax = "ValueMax";

	/** Set ValueMax	  */
	public void setValueMax (BigDecimal ValueMax);

	/** Get ValueMax	  */
	public BigDecimal getValueMax();
	
	/** Column name ValueMin */
    public static final String COLUMNNAME_ValueMin = "ValueMin";

	/** Set ValueMin	  */
	public void setValueMin (BigDecimal ValueMin);

	/** Get ValueMin	  */
	public BigDecimal getValueMin();
	
	/** Column name LockInValue */
    public static final String COLUMNNAME_LockInValue = "LockInValue";

	/** Set LockInValue	  */
	public void setLockInValue (BigDecimal LockInValue);

	/** Get LockInValue	  */
	public BigDecimal getLockInValue();
	
	/** Column name VoucherValue */
    public static final String COLUMNNAME_VoucherValue = "VoucherValue";

	/** Set VoucherValue	  */
	public void setVoucherValue (BigDecimal VoucherValue);

	/** Get VoucherValue	  */
	public BigDecimal getVoucherValue();
	
	/** Column name StartDate */
    public static final String COLUMNNAME_StartDate = "StartDate";

	/** Set Start Date.
	  * First effective day (inclusive)
	  */
	public void setStartDate (Timestamp StartDate);

	/** Get Start Date.
	  * First effective day (inclusive)
	  */
	public Timestamp getStartDate();
	
	/** Column name EndDate */
    public static final String COLUMNNAME_EndDate = "EndDate";

	/** Set End Date.
	  * Last effective date (inclusive)
	  */
	public void setEndDate (Timestamp EndDate);

	/** Get End Date.
	  * Last effective date (inclusive)
	  */
	public Timestamp getEndDate();

    /** Column name Created */
    public static final String COLUMNNAME_Created = "Created";

	/** Get Created.
	  * Date this record was created
	  */
	public Timestamp getCreated();

    /** Column name CreatedBy */
    public static final String COLUMNNAME_CreatedBy = "CreatedBy";

	/** Get Created By.
	  * User who created this records
	  */
	public int getCreatedBy();

    /** Column name IsActive */
    public static final String COLUMNNAME_IsActive = "IsActive";

	/** Set Active.
	  * The record is active in the system
	  */
	public void setIsActive (boolean IsActive);

	/** Get Active.
	  * The record is active in the system
	  */
	public boolean isActive();

    /** Column name SAS_NGK_Rule_ID */
    public static final String COLUMNNAME_SAS_NGK_Rule_ID= "SAS_NGK_Rule_ID";

	/** Set SAS_NGK_Rule_ID	  */
	public void setSAS_NGK_Rule_ID (int SAS_NGK_Rule_ID);

	/** Get SAS_NGK_Rule_ID	  */
	public int getSAS_NGK_Rule_ID();

    /** Column name Updated */
    public static final String COLUMNNAME_Updated = "Updated";

	/** Get Updated.
	  * Date this record was updated
	  */
	public Timestamp getUpdated();

    /** Column name UpdatedBy */
    public static final String COLUMNNAME_UpdatedBy = "UpdatedBy";

	/** Get Updated By.
	  * User who updated this records
	  */
	public int getUpdatedBy();

}