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
package id.co.databiz.awn.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import org.compiere.model.*;
import org.compiere.util.KeyNamePair;

/** Generated Interface for Z_OrderRef
 *  @author iDempiere (generated) 
 *  @version Release 2.0 b494
 */
@SuppressWarnings("all")
public interface I_Z_OrderRef 
{

    /** TableName=Z_OrderRef */
    public static final String Table_Name = "Z_OrderRef";

    /** AD_Table_ID=550010 */
    public static final int Table_ID = 550010;

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 3 - Client - Org 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(3);

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

    /** Column name A_Asset_ID */
    public static final String COLUMNNAME_A_Asset_ID = "A_Asset_ID";

	/** Set Asset.
	  * Asset used internally or by customers
	  */
	public void setA_Asset_ID (int A_Asset_ID);

	/** Get Asset.
	  * Asset used internally or by customers
	  */
	public int getA_Asset_ID();

	public org.compiere.model.I_A_Asset getA_Asset() throws RuntimeException;

    /** Column name A_Entry_Type */
    public static final String COLUMNNAME_A_Entry_Type = "A_Entry_Type";

	/** Set Entry Type	  */
	public void setA_Entry_Type (String A_Entry_Type);

	/** Get Entry Type	  */
	public String getA_Entry_Type();

    /** Column name A_User1_ID */
    public static final String COLUMNNAME_A_User1_ID = "A_User1_ID";

	/** Set User 1	  */
	public void setA_User1_ID (int A_User1_ID);

	/** Get User 1	  */
	public int getA_User1_ID();

	public org.compiere.model.I_AD_User getA_User1() throws RuntimeException;

    /** Column name A_User2_ID */
    public static final String COLUMNNAME_A_User2_ID = "A_User2_ID";

	/** Set User 2	  */
	public void setA_User2_ID (int A_User2_ID);

	/** Get User 2	  */
	public int getA_User2_ID();

	public org.compiere.model.I_AD_User getA_User2() throws RuntimeException;

    /** Column name A_User3_ID */
    public static final String COLUMNNAME_A_User3_ID = "A_User3_ID";

	/** Set User 3	  */
	public void setA_User3_ID (int A_User3_ID);

	/** Get User 3	  */
	public int getA_User3_ID();

	public org.compiere.model.I_AD_User getA_User3() throws RuntimeException;

    /** Column name A_User4_ID */
    public static final String COLUMNNAME_A_User4_ID = "A_User4_ID";

	/** Set User 4	  */
	public void setA_User4_ID (int A_User4_ID);

	/** Get User 4	  */
	public int getA_User4_ID();

	public org.compiere.model.I_AD_User getA_User4() throws RuntimeException;

    /** Column name C_Order_ID */
    public static final String COLUMNNAME_C_Order_ID = "C_Order_ID";

	/** Set Order.
	  * Order
	  */
	public void setC_Order_ID (int C_Order_ID);

	/** Get Order.
	  * Order
	  */
	public int getC_Order_ID();

	public org.compiere.model.I_C_Order getC_Order() throws RuntimeException;

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

    /** Column name DateReceived */
    public static final String COLUMNNAME_DateReceived = "DateReceived";

	/** Set Date received.
	  * Date a product was received
	  */
	public void setDateReceived (Timestamp DateReceived);

	/** Get Date received.
	  * Date a product was received
	  */
	public Timestamp getDateReceived();

    /** Column name EndTime */
    public static final String COLUMNNAME_EndTime = "EndTime";

	/** Set End Time.
	  * End of the time span
	  */
	public void setEndTime (Timestamp EndTime);

	/** Get End Time.
	  * End of the time span
	  */
	public Timestamp getEndTime();

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

    /** Column name StartTime */
    public static final String COLUMNNAME_StartTime = "StartTime";

	/** Set Start Time.
	  * Time started
	  */
	public void setStartTime (Timestamp StartTime);

	/** Get Start Time.
	  * Time started
	  */
	public Timestamp getStartTime();

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

    /** Column name Z_OrderRef_ID */
    public static final String COLUMNNAME_Z_OrderRef_ID = "Z_OrderRef_ID";

	/** Set Order References	  */
	public void setZ_OrderRef_ID (int Z_OrderRef_ID);

	/** Get Order References	  */
	public int getZ_OrderRef_ID();

    /** Column name Z_OrderRef_UU */
    public static final String COLUMNNAME_Z_OrderRef_UU = "Z_OrderRef_UU";

	/** Set Z_OrderRef_UU	  */
	public void setZ_OrderRef_UU (String Z_OrderRef_UU);

	/** Get Z_OrderRef_UU	  */
	public String getZ_OrderRef_UU();
}
