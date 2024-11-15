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
import java.sql.Timestamp;
import java.util.Properties;
import org.compiere.model.*;

/** Generated Model for SAS_RequestBundle
 *  @author iDempiere (generated) 
 *  @version Release 3.1 - $Id$ */
public class X_SAS_RequestBundle extends PO implements I_SAS_RequestBundle, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20160815L;

    /** Standard Constructor */
    public X_SAS_RequestBundle (Properties ctx, int SAS_RequestBundle_ID, String trxName)
    {
      super (ctx, SAS_RequestBundle_ID, trxName);
      /** if (SAS_RequestBundle_ID == 0)
        {
			setAD_User_ID (0);
// -1
			setDateDoc (new Timestamp( System.currentTimeMillis() ));
// @#Date@
			setProcessed (false);
// N
			setSAS_RequestBundle_ID (0);
        } */
    }

    /** Load Constructor */
    public X_SAS_RequestBundle (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_SAS_RequestBundle[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Add Invoices.
		@param AddInvoices Add Invoices	  */
	public void setAddInvoices (String AddInvoices)
	{
		set_Value (COLUMNNAME_AddInvoices, AddInvoices);
	}

	/** Get Add Invoices.
		@return Add Invoices	  */
	public String getAddInvoices () 
	{
		return (String)get_Value(COLUMNNAME_AddInvoices);
	}

	/** Set Add Shipments.
		@param AddShipments Add Shipments	  */
	public void setAddShipments (String AddShipments)
	{
		set_Value (COLUMNNAME_AddShipments, AddShipments);
	}

	/** Get Add Shipments.
		@return Add Shipments	  */
	public String getAddShipments () 
	{
		return (String)get_Value(COLUMNNAME_AddShipments);
	}

	public org.compiere.model.I_AD_User getAD_User2() throws RuntimeException
    {
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_Name)
			.getPO(getAD_User2_ID(), get_TrxName());	}

	/** Set User 2.
		@param AD_User2_ID 
		User within the system - Internal or Business Partner Contact
	  */
	public void setAD_User2_ID (int AD_User2_ID)
	{
		if (AD_User2_ID < 1) 
			set_Value (COLUMNNAME_AD_User2_ID, null);
		else 
			set_Value (COLUMNNAME_AD_User2_ID, Integer.valueOf(AD_User2_ID));
	}

	/** Get User 2.
		@return User within the system - Internal or Business Partner Contact
	  */
	public int getAD_User2_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_User2_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_AD_User getAD_User3() throws RuntimeException
    {
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_Name)
			.getPO(getAD_User3_ID(), get_TrxName());	}

	/** Set User 3.
		@param AD_User3_ID 
		User within the system - Internal or Business Partner Contact
	  */
	public void setAD_User3_ID (int AD_User3_ID)
	{
		if (AD_User3_ID < 1) 
			set_Value (COLUMNNAME_AD_User3_ID, null);
		else 
			set_Value (COLUMNNAME_AD_User3_ID, Integer.valueOf(AD_User3_ID));
	}

	/** Get User 3.
		@return User within the system - Internal or Business Partner Contact
	  */
	public int getAD_User3_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_User3_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
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

	/** Set Area Code.
		@param AreaCode 
		Phone Area Code
	  */
	public void setAreaCode (String AreaCode)
	{
		set_Value (COLUMNNAME_AreaCode, AreaCode);
	}

	/** Get Area Code.
		@return Phone Area Code
	  */
	public String getAreaCode () 
	{
		return (String)get_Value(COLUMNNAME_AreaCode);
	}

	/** Set Arrival Time.
		@param ArrivalTime Arrival Time	  */
	public void setArrivalTime (Timestamp ArrivalTime)
	{
		set_Value (COLUMNNAME_ArrivalTime, ArrivalTime);
	}

	/** Get Arrival Time.
		@return Arrival Time	  */
	public Timestamp getArrivalTime () 
	{
		return (Timestamp)get_Value(COLUMNNAME_ArrivalTime);
	}

	/** Set Arrive.
		@param Arrive Arrive	  */
	public void setArrive (String Arrive)
	{
		set_Value (COLUMNNAME_Arrive, Arrive);
	}

	/** Get Arrive.
		@return Arrive	  */
	public String getArrive () 
	{
		return (String)get_Value(COLUMNNAME_Arrive);
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
			set_ValueNoCheck (COLUMNNAME_C_DocType_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_C_DocType_ID, Integer.valueOf(C_DocType_ID));
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

	public org.compiere.model.I_C_Job getC_Job2() throws RuntimeException
    {
		return (org.compiere.model.I_C_Job)MTable.get(getCtx(), org.compiere.model.I_C_Job.Table_Name)
			.getPO(getC_Job2_ID(), get_TrxName());	}

	/** Set Position 2.
		@param C_Job2_ID 
		Job Position 2
	  */
	public void setC_Job2_ID (int C_Job2_ID)
	{
		if (C_Job2_ID < 1) 
			set_Value (COLUMNNAME_C_Job2_ID, null);
		else 
			set_Value (COLUMNNAME_C_Job2_ID, Integer.valueOf(C_Job2_ID));
	}

	/** Get Position 2.
		@return Job Position 2
	  */
	public int getC_Job2_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Job2_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_Job getC_Job3() throws RuntimeException
    {
		return (org.compiere.model.I_C_Job)MTable.get(getCtx(), org.compiere.model.I_C_Job.Table_Name)
			.getPO(getC_Job3_ID(), get_TrxName());	}

	/** Set Position 3.
		@param C_Job3_ID 
		Job Position 3
	  */
	public void setC_Job3_ID (int C_Job3_ID)
	{
		if (C_Job3_ID < 1) 
			set_Value (COLUMNNAME_C_Job3_ID, null);
		else 
			set_Value (COLUMNNAME_C_Job3_ID, Integer.valueOf(C_Job3_ID));
	}

	/** Get Position 3.
		@return Job Position 3
	  */
	public int getC_Job3_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Job3_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_Job getC_Job() throws RuntimeException
    {
		return (org.compiere.model.I_C_Job)MTable.get(getCtx(), org.compiere.model.I_C_Job.Table_Name)
			.getPO(getC_Job_ID(), get_TrxName());	}

	/** Set Position.
		@param C_Job_ID 
		Job Position
	  */
	public void setC_Job_ID (int C_Job_ID)
	{
		if (C_Job_ID < 1) 
			set_Value (COLUMNNAME_C_Job_ID, null);
		else 
			set_Value (COLUMNNAME_C_Job_ID, Integer.valueOf(C_Job_ID));
	}

	/** Get Position.
		@return Job Position
	  */
	public int getC_Job_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Job_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Document Date.
		@param DateDoc 
		Date of the Document
	  */
	public void setDateDoc (Timestamp DateDoc)
	{
		set_Value (COLUMNNAME_DateDoc, DateDoc);
	}

	/** Get Document Date.
		@return Date of the Document
	  */
	public Timestamp getDateDoc () 
	{
		return (Timestamp)get_Value(COLUMNNAME_DateDoc);
	}

	/** Set Depart.
		@param Depart Depart	  */
	public void setDepart (String Depart)
	{
		set_Value (COLUMNNAME_Depart, Depart);
	}

	/** Get Depart.
		@return Depart	  */
	public String getDepart () 
	{
		return (String)get_Value(COLUMNNAME_Depart);
	}

	/** Set Departure Time.
		@param DepartureTime Departure Time	  */
	public void setDepartureTime (Timestamp DepartureTime)
	{
		set_Value (COLUMNNAME_DepartureTime, DepartureTime);
	}

	/** Get Departure Time.
		@return Departure Time	  */
	public Timestamp getDepartureTime () 
	{
		return (Timestamp)get_Value(COLUMNNAME_DepartureTime);
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
		set_ValueNoCheck (COLUMNNAME_DocumentNo, DocumentNo);
	}

	/** Get Document No.
		@return Document sequence number of the document
	  */
	public String getDocumentNo () 
	{
		return (String)get_Value(COLUMNNAME_DocumentNo);
	}

	/** Set Estimation.
		@param Estimation Estimation	  */
	public void setEstimation (String Estimation)
	{
		set_Value (COLUMNNAME_Estimation, Estimation);
	}

	/** Get Estimation.
		@return Estimation	  */
	public String getEstimation () 
	{
		return (String)get_Value(COLUMNNAME_Estimation);
	}

	/** Set Phone.
		@param Phone 
		Identifies a telephone number
	  */
	public void setPhone (String Phone)
	{
		set_Value (COLUMNNAME_Phone, Phone);
	}

	/** Get Phone.
		@return Identifies a telephone number
	  */
	public String getPhone () 
	{
		return (String)get_Value(COLUMNNAME_Phone);
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

	/** Set Remote Organization.
		@param Remote_Org_ID 
		Remote Organization to be used to replicate / synchronize data with.
	  */
	public void setRemote_Org_ID (int Remote_Org_ID)
	{
		if (Remote_Org_ID < 1) 
			set_Value (COLUMNNAME_Remote_Org_ID, null);
		else 
			set_Value (COLUMNNAME_Remote_Org_ID, Integer.valueOf(Remote_Org_ID));
	}

	/** Get Remote Organization.
		@return Remote Organization to be used to replicate / synchronize data with.
	  */
	public int getRemote_Org_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Remote_Org_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_R_RequestType getR_RequestType() throws RuntimeException
    {
		return (org.compiere.model.I_R_RequestType)MTable.get(getCtx(), org.compiere.model.I_R_RequestType.Table_Name)
			.getPO(getR_RequestType_ID(), get_TrxName());	}

	/** Set Request Type.
		@param R_RequestType_ID 
		Type of request (e.g. Inquiry, Complaint, ..)
	  */
	public void setR_RequestType_ID (int R_RequestType_ID)
	{
		if (R_RequestType_ID < 1) 
			set_Value (COLUMNNAME_R_RequestType_ID, null);
		else 
			set_Value (COLUMNNAME_R_RequestType_ID, Integer.valueOf(R_RequestType_ID));
	}

	/** Get Request Type.
		@return Type of request (e.g. Inquiry, Complaint, ..)
	  */
	public int getR_RequestType_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_R_RequestType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_R_Status getR_Status() throws RuntimeException
    {
		return (org.compiere.model.I_R_Status)MTable.get(getCtx(), org.compiere.model.I_R_Status.Table_Name)
			.getPO(getR_Status_ID(), get_TrxName());	}

	/** Set Status.
		@param R_Status_ID 
		Request Status
	  */
	public void setR_Status_ID (int R_Status_ID)
	{
		if (R_Status_ID < 1) 
			set_Value (COLUMNNAME_R_Status_ID, null);
		else 
			set_Value (COLUMNNAME_R_Status_ID, Integer.valueOf(R_Status_ID));
	}

	/** Get Status.
		@return Request Status
	  */
	public int getR_Status_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_R_Status_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Request Bundle.
		@param SAS_RequestBundle_ID Request Bundle	  */
	public void setSAS_RequestBundle_ID (int SAS_RequestBundle_ID)
	{
		if (SAS_RequestBundle_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_SAS_RequestBundle_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_SAS_RequestBundle_ID, Integer.valueOf(SAS_RequestBundle_ID));
	}

	/** Get Request Bundle.
		@return Request Bundle	  */
	public int getSAS_RequestBundle_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_SAS_RequestBundle_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set SAS_RequestBundle_UU.
		@param SAS_RequestBundle_UU SAS_RequestBundle_UU	  */
	public void setSAS_RequestBundle_UU (String SAS_RequestBundle_UU)
	{
		set_Value (COLUMNNAME_SAS_RequestBundle_UU, SAS_RequestBundle_UU);
	}

	/** Get SAS_RequestBundle_UU.
		@return SAS_RequestBundle_UU	  */
	public String getSAS_RequestBundle_UU () 
	{
		return (String)get_Value(COLUMNNAME_SAS_RequestBundle_UU);
	}

	/** Set Vehicle No.
		@param VehicleNo Vehicle No	  */
	public void setVehicleNo (String VehicleNo)
	{
		set_Value (COLUMNNAME_VehicleNo, VehicleNo);
	}

	/** Get Vehicle No.
		@return Vehicle No	  */
	public String getVehicleNo () 
	{
		return (String)get_Value(COLUMNNAME_VehicleNo);
	}

	/** VehicleType AD_Reference_ID=550084 */
	public static final int VEHICLETYPE_AD_Reference_ID=550084;
	/** Car = Car */
	public static final String VEHICLETYPE_Car = "Car";
	/** Motor = Motor */
	public static final String VEHICLETYPE_Motor = "Motor";
	/** Trolly = Trolly */
	public static final String VEHICLETYPE_Trolly = "Trolly";
	/** Walk = Walk */
	public static final String VEHICLETYPE_Walk = "Walk";
	/** Set Vehicle Type.
		@param VehicleType Vehicle Type	  */
	public void setVehicleType (String VehicleType)
	{

		set_Value (COLUMNNAME_VehicleType, VehicleType);
	}

	/** Get Vehicle Type.
		@return Vehicle Type	  */
	public String getVehicleType () 
	{
		return (String)get_Value(COLUMNNAME_VehicleType);
	}
}