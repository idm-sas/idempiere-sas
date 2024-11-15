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
package id.co.databiz.sas.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import org.compiere.model.*;
import org.compiere.util.KeyNamePair;

/** Generated Interface for SAS_RequestBundle
 *  @author iDempiere (generated) 
 *  @version Release 3.1
 */
@SuppressWarnings("all")
public interface I_SAS_RequestBundle 
{

    /** TableName=SAS_RequestBundle */
    public static final String Table_Name = "SAS_RequestBundle";

    /** AD_Table_ID=550068 */
    public static final int Table_ID = 550068;

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

    /** Column name AddInvoices */
    public static final String COLUMNNAME_AddInvoices = "AddInvoices";

	/** Set Add Invoices	  */
	public void setAddInvoices (String AddInvoices);

	/** Get Add Invoices	  */
	public String getAddInvoices();

    /** Column name AddShipments */
    public static final String COLUMNNAME_AddShipments = "AddShipments";

	/** Set Add Shipments	  */
	public void setAddShipments (String AddShipments);

	/** Get Add Shipments	  */
	public String getAddShipments();

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

    /** Column name AD_User2_ID */
    public static final String COLUMNNAME_AD_User2_ID = "AD_User2_ID";

	/** Set User 2.
	  * User within the system - Internal or Business Partner Contact
	  */
	public void setAD_User2_ID (int AD_User2_ID);

	/** Get User 2.
	  * User within the system - Internal or Business Partner Contact
	  */
	public int getAD_User2_ID();

	public org.compiere.model.I_AD_User getAD_User2() throws RuntimeException;

    /** Column name AD_User3_ID */
    public static final String COLUMNNAME_AD_User3_ID = "AD_User3_ID";

	/** Set User 3.
	  * User within the system - Internal or Business Partner Contact
	  */
	public void setAD_User3_ID (int AD_User3_ID);

	/** Get User 3.
	  * User within the system - Internal or Business Partner Contact
	  */
	public int getAD_User3_ID();

	public org.compiere.model.I_AD_User getAD_User3() throws RuntimeException;

    /** Column name AD_User_ID */
    public static final String COLUMNNAME_AD_User_ID = "AD_User_ID";

	/** Set User/Contact.
	  * User within the system - Internal or Business Partner Contact
	  */
	public void setAD_User_ID (int AD_User_ID);

	/** Get User/Contact.
	  * User within the system - Internal or Business Partner Contact
	  */
	public int getAD_User_ID();

	public org.compiere.model.I_AD_User getAD_User() throws RuntimeException;

    /** Column name AreaCode */
    public static final String COLUMNNAME_AreaCode = "AreaCode";

	/** Set Area Code.
	  * Phone Area Code
	  */
	public void setAreaCode (String AreaCode);

	/** Get Area Code.
	  * Phone Area Code
	  */
	public String getAreaCode();

    /** Column name ArrivalTime */
    public static final String COLUMNNAME_ArrivalTime = "ArrivalTime";

	/** Set Arrival Time	  */
	public void setArrivalTime (Timestamp ArrivalTime);

	/** Get Arrival Time	  */
	public Timestamp getArrivalTime();

    /** Column name Arrive */
    public static final String COLUMNNAME_Arrive = "Arrive";

	/** Set Arrive	  */
	public void setArrive (String Arrive);

	/** Get Arrive	  */
	public String getArrive();

    /** Column name C_DocType_ID */
    public static final String COLUMNNAME_C_DocType_ID = "C_DocType_ID";

	/** Set Document Type.
	  * Document type or rules
	  */
	public void setC_DocType_ID (int C_DocType_ID);

	/** Get Document Type.
	  * Document type or rules
	  */
	public int getC_DocType_ID();

	public org.compiere.model.I_C_DocType getC_DocType() throws RuntimeException;

    /** Column name C_Job2_ID */
    public static final String COLUMNNAME_C_Job2_ID = "C_Job2_ID";

	/** Set Position 2.
	  * Job Position 2
	  */
	public void setC_Job2_ID (int C_Job2_ID);

	/** Get Position 2.
	  * Job Position 2
	  */
	public int getC_Job2_ID();

	public org.compiere.model.I_C_Job getC_Job2() throws RuntimeException;

    /** Column name C_Job3_ID */
    public static final String COLUMNNAME_C_Job3_ID = "C_Job3_ID";

	/** Set Position 3.
	  * Job Position 3
	  */
	public void setC_Job3_ID (int C_Job3_ID);

	/** Get Position 3.
	  * Job Position 3
	  */
	public int getC_Job3_ID();

	public org.compiere.model.I_C_Job getC_Job3() throws RuntimeException;

    /** Column name C_Job_ID */
    public static final String COLUMNNAME_C_Job_ID = "C_Job_ID";

	/** Set Position.
	  * Job Position
	  */
	public void setC_Job_ID (int C_Job_ID);

	/** Get Position.
	  * Job Position
	  */
	public int getC_Job_ID();

	public org.compiere.model.I_C_Job getC_Job() throws RuntimeException;

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

    /** Column name DateDoc */
    public static final String COLUMNNAME_DateDoc = "DateDoc";

	/** Set Document Date.
	  * Date of the Document
	  */
	public void setDateDoc (Timestamp DateDoc);

	/** Get Document Date.
	  * Date of the Document
	  */
	public Timestamp getDateDoc();

    /** Column name Depart */
    public static final String COLUMNNAME_Depart = "Depart";

	/** Set Depart	  */
	public void setDepart (String Depart);

	/** Get Depart	  */
	public String getDepart();

    /** Column name DepartureTime */
    public static final String COLUMNNAME_DepartureTime = "DepartureTime";

	/** Set Departure Time	  */
	public void setDepartureTime (Timestamp DepartureTime);

	/** Get Departure Time	  */
	public Timestamp getDepartureTime();

    /** Column name Description */
    public static final String COLUMNNAME_Description = "Description";

	/** Set Description.
	  * Optional short description of the record
	  */
	public void setDescription (String Description);

	/** Get Description.
	  * Optional short description of the record
	  */
	public String getDescription();

    /** Column name DocumentNo */
    public static final String COLUMNNAME_DocumentNo = "DocumentNo";

	/** Set Document No.
	  * Document sequence number of the document
	  */
	public void setDocumentNo (String DocumentNo);

	/** Get Document No.
	  * Document sequence number of the document
	  */
	public String getDocumentNo();

    /** Column name Estimation */
    public static final String COLUMNNAME_Estimation = "Estimation";

	/** Set Estimation	  */
	public void setEstimation (String Estimation);

	/** Get Estimation	  */
	public String getEstimation();

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

    /** Column name Phone */
    public static final String COLUMNNAME_Phone = "Phone";

	/** Set Phone.
	  * Identifies a telephone number
	  */
	public void setPhone (String Phone);

	/** Get Phone.
	  * Identifies a telephone number
	  */
	public String getPhone();

    /** Column name Processed */
    public static final String COLUMNNAME_Processed = "Processed";

	/** Set Processed.
	  * The document has been processed
	  */
	public void setProcessed (boolean Processed);

	/** Get Processed.
	  * The document has been processed
	  */
	public boolean isProcessed();

    /** Column name Remote_Org_ID */
    public static final String COLUMNNAME_Remote_Org_ID = "Remote_Org_ID";

	/** Set Remote Organization.
	  * Remote Organization to be used to replicate / synchronize data with.
	  */
	public void setRemote_Org_ID (int Remote_Org_ID);

	/** Get Remote Organization.
	  * Remote Organization to be used to replicate / synchronize data with.
	  */
	public int getRemote_Org_ID();

    /** Column name R_RequestType_ID */
    public static final String COLUMNNAME_R_RequestType_ID = "R_RequestType_ID";

	/** Set Request Type.
	  * Type of request (e.g. Inquiry, Complaint, ..)
	  */
	public void setR_RequestType_ID (int R_RequestType_ID);

	/** Get Request Type.
	  * Type of request (e.g. Inquiry, Complaint, ..)
	  */
	public int getR_RequestType_ID();

	public org.compiere.model.I_R_RequestType getR_RequestType() throws RuntimeException;

    /** Column name R_Status_ID */
    public static final String COLUMNNAME_R_Status_ID = "R_Status_ID";

	/** Set Status.
	  * Request Status
	  */
	public void setR_Status_ID (int R_Status_ID);

	/** Get Status.
	  * Request Status
	  */
	public int getR_Status_ID();

	public org.compiere.model.I_R_Status getR_Status() throws RuntimeException;

    /** Column name SAS_RequestBundle_ID */
    public static final String COLUMNNAME_SAS_RequestBundle_ID = "SAS_RequestBundle_ID";

	/** Set Request Bundle	  */
	public void setSAS_RequestBundle_ID (int SAS_RequestBundle_ID);

	/** Get Request Bundle	  */
	public int getSAS_RequestBundle_ID();

    /** Column name SAS_RequestBundle_UU */
    public static final String COLUMNNAME_SAS_RequestBundle_UU = "SAS_RequestBundle_UU";

	/** Set SAS_RequestBundle_UU	  */
	public void setSAS_RequestBundle_UU (String SAS_RequestBundle_UU);

	/** Get SAS_RequestBundle_UU	  */
	public String getSAS_RequestBundle_UU();

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

    /** Column name VehicleNo */
    public static final String COLUMNNAME_VehicleNo = "VehicleNo";

	/** Set Vehicle No	  */
	public void setVehicleNo (String VehicleNo);

	/** Get Vehicle No	  */
	public String getVehicleNo();

    /** Column name VehicleType */
    public static final String COLUMNNAME_VehicleType = "VehicleType";

	/** Set Vehicle Type	  */
	public void setVehicleType (String VehicleType);

	/** Get Vehicle Type	  */
	public String getVehicleType();
}
