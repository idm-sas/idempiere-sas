/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package id.co.databiz.sas.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.adempiere.base.Core;
import org.adempiere.base.IProductPricing;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.ProductNotOnPriceListException;
import org.adempiere.model.ITaxProvider;
import org.compiere.model.MCharge;
import org.compiere.model.MCurrency;
import org.compiere.model.MDocType;
import org.compiere.model.MLandedCost;
import org.compiere.model.MOrderTax;
import org.compiere.model.MPriceList;
import org.compiere.model.MProduct;
import org.compiere.model.MRequisitionLine;
import org.compiere.model.MResourceAssignment;
import org.compiere.model.MRole;
import org.compiere.model.MTax;
import org.compiere.model.MTaxProvider;
import org.compiere.model.MUOM;
import org.compiere.process.DocAction;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;

/**
 *  Order Line Model.
 * 	<code>
 * 			MOrderLine ol = new MOrderLine(m_order);
			ol.setM_Product_ID(wbl.getM_Product_ID());
			ol.setQtyOrdered(wbl.getQuantity());
			ol.setPrice();
			ol.setPriceActual(wbl.getPrice());
			ol.setTax();
			ol.saveEx();

 *	</code>
 *  @author Jorg Janke
 *  @version $Id: MOrderLine.java,v 1.6 2006/10/02 05:18:39 jjanke Exp $
 * 
 * @author Teo Sarca, SC ARHIPAC SERVICE SRL
 *			<li>BF [ 2588043 ] Insufficient message ProductNotOnPriceList
 */
public class MOrderLine extends org.compiere.model.MOrderLine
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 7994694334621222461L;

	/**
	 * 	Get Order Unreserved Qty
	 *	@param ctx context
	 *	@param M_Warehouse_ID wh
	 *	@param M_Product_ID product
	 *	@param M_AttributeSetInstance_ID asi
	 *	@param excludeC_OrderLine_ID exclude C_OrderLine_ID
	 *	@return Unreserved Qty
	 */
	public static BigDecimal getNotReserved (Properties ctx, int M_Warehouse_ID, 
		int M_Product_ID, int M_AttributeSetInstance_ID, int excludeC_OrderLine_ID)
	{
		BigDecimal retValue = Env.ZERO;
		String sql = "SELECT SUM(QtyOrdered-QtyDelivered-QtyReserved) "
			+ "FROM C_OrderLine ol"
			+ " INNER JOIN C_Order o ON (ol.C_Order_ID=o.C_Order_ID) "
			+ "WHERE ol.M_Warehouse_ID=?"	//	#1
			+ " AND M_Product_ID=?"			//	#2
			+ " AND o.IsSOTrx='Y' AND o.DocStatus='DR'"
			+ " AND QtyOrdered-QtyDelivered-QtyReserved<>0"
			+ " AND ol.C_OrderLine_ID<>?";
		if (M_AttributeSetInstance_ID != 0)
			sql += " AND M_AttributeSetInstance_ID=?";
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql, null);
			pstmt.setInt (1, M_Warehouse_ID);
			pstmt.setInt (2, M_Product_ID);
			pstmt.setInt (3, excludeC_OrderLine_ID);
			if (M_AttributeSetInstance_ID != 0)
				pstmt.setInt (4, M_AttributeSetInstance_ID);
			rs = pstmt.executeQuery ();
			if (rs.next ())
				retValue = rs.getBigDecimal(1);
		}
		catch (Exception e)
		{
			s_log.log (Level.SEVERE, sql, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		if (retValue == null)
			s_log.fine("-");
		else
			if (s_log.isLoggable(Level.FINE)) s_log.fine(retValue.toString());
		return retValue;
	}	//	getNotReserved
	
	/**	Logger	*/
	protected static CLogger s_log = CLogger.getCLogger (MOrderLine.class);
	
    /**
    * UUID based Constructor
    * @param ctx  Context
    * @param C_OrderLine_UU  UUID key
    * @param trxName Transaction
    */
    public MOrderLine(Properties ctx, String C_OrderLine_UU, String trxName) {
        super(ctx, C_OrderLine_UU, trxName);
		if (Util.isEmpty(C_OrderLine_UU))
			setInitialDefaults();
    }

	/**************************************************************************
	 *  Default Constructor
	 *  @param ctx context
	 *  @param  C_OrderLine_ID  order line to load
	 *  @param trxName trx name
	 */
	public MOrderLine (Properties ctx, int C_OrderLine_ID, String trxName)
	{
		this (ctx, C_OrderLine_ID, trxName, (String[]) null);
	}	//	MOrderLine

	public MOrderLine(Properties ctx, int C_OrderLine_ID, String trxName, String... virtualColumns) {
		super(ctx, C_OrderLine_ID, trxName, virtualColumns);
		if (C_OrderLine_ID == 0)
			setInitialDefaults();
	}

	/**
	 * Set the initial defaults for a new record
	 */
	private void setInitialDefaults() {
		setFreightAmt (Env.ZERO);
		setLineNetAmt (Env.ZERO);
		//
		setPriceEntered(Env.ZERO);
		setPriceActual (Env.ZERO);
		setPriceLimit (Env.ZERO);
		setPriceList (Env.ZERO);
		//
		setM_AttributeSetInstance_ID(0);
		//
		setQtyEntered (Env.ZERO);
		setQtyOrdered (Env.ZERO);	// 1
		setQtyDelivered (Env.ZERO);
		setQtyInvoiced (Env.ZERO);
		setQtyReserved (Env.ZERO);
		//
		setIsDescription (false);	// N
		setProcessed (false);
		setLine (0);
	}

	/**
	 *  Parent Constructor.
	 		ol.setM_Product_ID(wbl.getM_Product_ID());
			ol.setQtyOrdered(wbl.getQuantity());
			ol.setPrice();
			ol.setPriceActual(wbl.getPrice());
			ol.setTax();
			ol.saveEx();
	 *  @param  order parent order
	 */
	public MOrderLine (MOrder order)
	{
		this (order.getCtx(), 0, order.get_TrxName());
		if (order.get_ID() == 0)
			throw new IllegalArgumentException("Header not saved");
		setC_Order_ID (order.getC_Order_ID());	//	parent
		setOrder(order);
	}	//	MOrderLine

	/**
	 *  Load Constructor
	 *  @param ctx context
	 *  @param rs result set record
	 *  @param trxName transaction
	 */
	public MOrderLine (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MOrderLine

	protected int 			m_M_PriceList_ID = 0;
	//
	protected boolean			m_IsSOTrx = true;
	//	Product Pricing
	protected IProductPricing	m_productPrice = null;

	/** Tax							*/
	protected MTax 		m_tax = null;
	
	/** Cached Currency Precision	*/
	protected Integer			m_precision = null;
	/**	Product					*/
	protected MProduct 		m_product = null;
	/**	Charge					*/
	protected MCharge 		m_charge = null;
	/** Parent					*/
	protected MOrder			m_parent = null;
	
	/**
	 * 	Set Defaults from Order.
	 * 	Does not set Parent !!
	 * 	@param order order
	 */
	public void setOrder (MOrder order)
	{
		setClientOrg(order);
		setC_BPartner_ID(order.getC_BPartner_ID());
		setC_BPartner_Location_ID(order.getC_BPartner_Location_ID());
		setM_Warehouse_ID(order.getM_Warehouse_ID());
		setDateOrdered(order.getDateOrdered());
		setDatePromised(order.getDatePromised());
		setC_Currency_ID(order.getC_Currency_ID());
		//
		setHeaderInfo(order);	//	sets m_order
		//	Don't set Activity, etc as they are overwrites
	}	//	setOrder

	/**
	 * 	Set Header Info
	 *	@param order order
	 */
	public void setHeaderInfo (MOrder order)
	{
		m_parent = order;
		m_precision = Integer.valueOf(order.getPrecision());
		m_M_PriceList_ID = order.getM_PriceList_ID();
		m_IsSOTrx = order.isSOTrx();
	}	//	setHeaderInfo
	
	/**
	 * 	Get Parent
	 *	@return parent
	 */
	public MOrder getParent()
	{
		if (m_parent == null)
			m_parent = new MOrder(getCtx(), getC_Order_ID(), get_TrxName());
		return m_parent;
	}	//	getParent
	
	/**
	 * 	Set Price Entered/Actual.
	 * 	Use this Method if the Line UOM is the Product UOM 
	 *	@param PriceActual price
	 */
	public void setPrice (BigDecimal PriceActual)
	{
		setPriceEntered(PriceActual);
		setPriceActual (PriceActual);
	}	//	setPrice

	/**
	 * 	Set Price Actual.
	 * 	(actual price is not updateable)
	 *	@param PriceActual actual price
	 */
	public void setPriceActual (BigDecimal PriceActual)
	{
		if (PriceActual == null) 
			throw new IllegalArgumentException ("PriceActual is mandatory");
		set_ValueNoCheck("PriceActual", PriceActual);
	}	//	setPriceActual

	/**
	 * 	Set Price for Product and PriceList.
	 * 	Use only if newly created.
	 * 	Uses standard price list of not set by order constructor
	 */
	public void setPrice()
	{
		if (getM_Product_ID() == 0)
			return;
		if (m_M_PriceList_ID == 0)
			throw new IllegalStateException("PriceList unknown!");
		setPrice (m_M_PriceList_ID);
	}	//	setPrice

	/**
	 * 	Set Price for Product and PriceList
	 * 	@param M_PriceList_ID price list
	 */
	public void setPrice (int M_PriceList_ID)
	{
		if (getM_Product_ID() == 0)
			return;
		//
		if (log.isLoggable(Level.FINE)) log.fine(toString() + " - M_PriceList_ID=" + M_PriceList_ID);
		getProductPricing (M_PriceList_ID);
		setPriceActual (m_productPrice.getPriceStd());
		setPriceList (m_productPrice.getPriceList());
		setPriceLimit (m_productPrice.getPriceLimit());
		//
		if (getQtyEntered().compareTo(getQtyOrdered()) == 0)
			setPriceEntered(getPriceActual());
		else
			setPriceEntered(getPriceActual().multiply(getQtyOrdered()
				.divide(getQtyEntered(), 12, RoundingMode.HALF_UP)));	//	recision
		
		//	Calculate Discount
		setDiscount(m_productPrice.getDiscount());
		//	Set UOM
		if (getC_UOM_ID()==0)
			setC_UOM_ID(m_productPrice.getC_UOM_ID());
	}	//	setPrice

	/**
	 * 	Get and calculate Product Pricing
	 *	@param M_PriceList_ID id
	 *	@return product pricing
	 */
	protected IProductPricing getProductPricing (int M_PriceList_ID)
	{
		m_productPrice = Core.getProductPricing();
		m_productPrice.setOrderLine(this, get_TrxName());
		m_productPrice.setM_PriceList_ID(M_PriceList_ID);
		//
		m_productPrice.calculatePrice();
		return m_productPrice;
	}	//	getProductPrice
	
	/**
	 *	Set Tax
	 *	@return true if tax is set
	 */
	public boolean setTax()
	{
		int ii = Core.getTaxLookup().get(getCtx(), getM_Product_ID(), getC_Charge_ID(), getDateOrdered(), getDateOrdered(),
			getAD_Org_ID(), getM_Warehouse_ID(),
			getC_BPartner_Location_ID(),		//	should be bill to
			getC_BPartner_Location_ID(), m_IsSOTrx, getParent().getDeliveryViaRule(), get_TrxName());
		if (ii == 0)
		{
			log.log(Level.SEVERE, "No Tax found");
			return false;
		}
		setC_Tax_ID (ii);
		return true;
	}	//	setTax
	
	/**
	 * 	Calculate Extended Amt.
	 * 	May or may not include tax
	 */
	public void setLineNetAmt ()
	{
		BigDecimal bd = getPriceActual().multiply(getQtyOrdered()); ///SAS Minta Agar Sama Seperti SAS3 LinenetAmt = PriceActual * QtyOrdered
		int precision = getPrecision();
		if (bd.scale() > precision)
			bd = bd.setScale(precision, RoundingMode.HALF_UP);
		super.setLineNetAmt (bd);
	}	//	setLineNetAmt
	
	/**
	 * 	Get Charge
	 *	@return product or null
	 */
	public MCharge getCharge()
	{
		if (m_charge == null && getC_Charge_ID() != 0)
			m_charge =  MCharge.getCopy(getCtx(), getC_Charge_ID(), get_TrxName());
		return m_charge;
	}
	/**
	 * 	Get Tax (immutable)
	 *	@return tax
	 */
	protected MTax getTax()
	{
		if (m_tax == null)
			m_tax = MTax.get(getCtx(), getC_Tax_ID());
		return m_tax;
	}	//	getTax

	/**
	 * 	Get Currency Precision from Currency
	 *	@return precision
	 */
	public int getPrecision()
	{
		if (m_precision != null)
			return m_precision.intValue();
		//
		if (getC_Currency_ID() == 0)
		{
			setOrder (getParent());
			if (m_precision != null)
				return m_precision.intValue();
		}
		if (getC_Currency_ID() != 0)
		{
			MCurrency cur = MCurrency.get(getCtx(), getC_Currency_ID());
			if (cur.get_ID() != 0)
			{
				m_precision = Integer.valueOf(cur.getStdPrecision());
				return m_precision.intValue();
			}
		}
		//	Fallback
		String sql = "SELECT c.StdPrecision "
			+ "FROM C_Currency c INNER JOIN C_Order x ON (x.C_Currency_ID=c.C_Currency_ID) "
			+ "WHERE x.C_Order_ID=?";
		int i = DB.getSQLValue(get_TrxName(), sql, getC_Order_ID());
		m_precision = Integer.valueOf(i);
		return m_precision.intValue();
	}	//	getPrecision
	
	/**
	 * 	Set Product
	 *	@param product product
	 */
	public void setProduct (MProduct product)
	{
		m_product = product;
		if (m_product != null)
		{
			setM_Product_ID(m_product.getM_Product_ID());
			setC_UOM_ID (m_product.getC_UOM_ID());
		}
		else
		{
			setM_Product_ID(0);
			set_ValueNoCheck ("C_UOM_ID", null);
		}
		setM_AttributeSetInstance_ID(0);
	}	//	setProduct

	
	/**
	 * 	Set M_Product_ID
	 *	@param M_Product_ID product
	 *	@param setUOM set also UOM
	 */
	public void setM_Product_ID (int M_Product_ID, boolean setUOM)
	{
		if (setUOM)
			setProduct(MProduct.get(getCtx(), M_Product_ID));
		else
			super.setM_Product_ID (M_Product_ID);
		setM_AttributeSetInstance_ID(0);
	}	//	setM_Product_ID
	
	/**
	 * 	Set Product and UOM
	 *	@param M_Product_ID product
	 *	@param C_UOM_ID uom
	 */
	public void setM_Product_ID (int M_Product_ID, int C_UOM_ID)
	{
		super.setM_Product_ID (M_Product_ID);
		if (C_UOM_ID != 0)
			super.setC_UOM_ID(C_UOM_ID);
		setM_AttributeSetInstance_ID(0);
	}	//	setM_Product_ID
	
	
	/**
	 * 	Get Product
	 *	@return product or null
	 */
	public MProduct getProduct()
	{
		if (m_product == null && getM_Product_ID() != 0)
			m_product =  MProduct.getCopy(getCtx(), getM_Product_ID(), get_TrxName());
		return m_product;
	}	//	getProduct
	
	/**
	 * 	Set M_AttributeSetInstance_ID
	 *	@param M_AttributeSetInstance_ID id
	 */
	public void setM_AttributeSetInstance_ID (int M_AttributeSetInstance_ID)
	{
		if (M_AttributeSetInstance_ID == 0)		//	 0 is valid ID
			set_Value("M_AttributeSetInstance_ID", Integer.valueOf(0));
		else
			super.setM_AttributeSetInstance_ID (M_AttributeSetInstance_ID);
	}	//	setM_AttributeSetInstance_ID
	
	/**
	 * 	Set Warehouse
	 *	@param M_Warehouse_ID warehouse
	 */
	public void setM_Warehouse_ID (int M_Warehouse_ID)
	{
		if (getM_Warehouse_ID() > 0
			&& getM_Warehouse_ID() != M_Warehouse_ID
			&& !canChangeWarehouse())
			log.severe("Ignored - Already Delivered/Invoiced/Reserved");
		else
			super.setM_Warehouse_ID (M_Warehouse_ID);
	}	//	setM_Warehouse_ID
	
	/**
	 * 	Can Change Warehouse
	 *	@return true if warehouse can be changed
	 */
	public boolean canChangeWarehouse()
	{
		if (getQtyDelivered().signum() != 0)
		{
			log.saveError("Error", Msg.translate(getCtx(), "QtyDelivered") + "=" + getQtyDelivered());
			return false;
		}
		if (getQtyInvoiced().signum() != 0)
		{
			log.saveError("Error", Msg.translate(getCtx(), "QtyInvoiced") + "=" + getQtyInvoiced());
			return false;
		}
		if (getQtyReserved().signum() != 0)
		{
			log.saveError("Error", Msg.translate(getCtx(), "QtyReserved") + "=" + getQtyReserved());
			return false;
		}
		//	We can change
		return true;
	}	//	canChangeWarehouse
	
	/**
	 * 	Get C_Project_ID
	 *	@return project
	 */
	public int getC_Project_ID()
	{
		int ii = super.getC_Project_ID ();
		if (ii == 0)
			ii = getParent().getC_Project_ID();
		return ii;
	}	//	getC_Project_ID
	
	/**
	 * 	Get C_Activity_ID
	 *	@return Activity
	 */
	public int getC_Activity_ID()
	{
		int ii = super.getC_Activity_ID ();
		if (ii == 0)
			ii = getParent().getC_Activity_ID();
		return ii;
	}	//	getC_Activity_ID
	
	/**
	 * 	Get C_Campaign_ID
	 *	@return Campaign
	 */
	public int getC_Campaign_ID()
	{
		int ii = super.getC_Campaign_ID ();
		if (ii == 0)
			ii = getParent().getC_Campaign_ID();
		return ii;
	}	//	getC_Campaign_ID
	
	/**
	 * 	Get User2_ID
	 *	@return User2
	 */
	public int getUser1_ID ()
	{
		int ii = super.getUser1_ID ();
		if (ii == 0)
			ii = getParent().getUser1_ID();
		return ii;
	}	//	getUser1_ID

	/**
	 * 	Get User2_ID
	 *	@return User2
	 */
	public int getUser2_ID ()
	{
		int ii = super.getUser2_ID ();
		if (ii == 0)
			ii = getParent().getUser2_ID();
		return ii;
	}	//	getUser2_ID

	/**
	 * 	Get AD_OrgTrx_ID
	 *	@return trx org
	 */
	public int getAD_OrgTrx_ID()
	{
		int ii = super.getAD_OrgTrx_ID();
		if (ii == 0)
			ii = getParent().getAD_OrgTrx_ID();
		return ii;
	}	//	getAD_OrgTrx_ID

	/**************************************************************************
	 * 	String Representation
	 * 	@return info
	 */
	public String toString ()
	{
		StringBuilder sb = new StringBuilder ("MOrderLine[")
			.append(get_ID())
			.append(", Line=").append(getLine())
			.append(", Ordered=").append(getQtyOrdered())
			.append(", Delivered=").append(getQtyDelivered())
			.append(", Invoiced=").append(getQtyInvoiced())
			.append(", Reserved=").append(getQtyReserved())
			.append(", LineNet=").append(getLineNetAmt())
			.append ("]");
		return sb.toString ();
	}	//	toString

	/**
	 * 	Add to Description
	 *	@param description text
	 */
	public void addDescription (String description)
	{
		String desc = getDescription();
		if (desc == null)
			setDescription(description);
		else
			setDescription(desc + " | " + description);
	}	//	addDescription
	
	/**
	 * 	Get Description Text.
	 * 	For jsp access (vs. isDescription)
	 *	@return description
	 */
	public String getDescriptionText()
	{
		return super.getDescription();
	}	//	getDescriptionText
	
	/**
	 * 	Get Name
	 *	@return get the name of the line (from Product)
	 */
	public String getName()
	{
		getProduct();
		if (m_product != null)
			return m_product.getName();
		if (getC_Charge_ID() != 0)
		{
			MCharge charge = MCharge.get(getCtx(), getC_Charge_ID());
			return charge.getName();
		}
		return "";
	}	//	getName

	/**
	 * 	Set C_Charge_ID
	 *	@param C_Charge_ID charge
	 */
	public void setC_Charge_ID (int C_Charge_ID)
	{
		super.setC_Charge_ID (C_Charge_ID);
		if (C_Charge_ID > 0)
			set_ValueNoCheck ("C_UOM_ID", null);
	}	//	setC_Charge_ID
	/**
	 *	Set Discount
	 */
	public void setDiscount()
	{
		BigDecimal list = getPriceList();
		//	No List Price
		if (Env.ZERO.compareTo(list) == 0)
			return;
		BigDecimal discount = list.subtract(getPriceActual())
			.multiply(Env.ONEHUNDRED)
			.divide(list, getPrecision(), RoundingMode.HALF_UP);
		setDiscount(discount);
	}	//	setDiscount

	/**
	 *	Is Tax Included in Amount
	 *	@return true if tax calculated
	 */
	public boolean isTaxIncluded()
	{
		if (m_M_PriceList_ID == 0)
		{
			m_M_PriceList_ID = DB.getSQLValue(get_TrxName(),
				"SELECT M_PriceList_ID FROM C_Order WHERE C_Order_ID=?",
				getC_Order_ID());
		}
		MPriceList pl = MPriceList.get(getCtx(), m_M_PriceList_ID, get_TrxName());
		return pl.isTaxIncluded();
	}	//	isTaxIncluded

	
	/**
	 * 	Set Qty Entered/Ordered.
	 * 	Use this Method if the Line UOM is the Product UOM 
	 *	@param Qty QtyOrdered/Entered
	 */
	public void setQty (BigDecimal Qty)
	{
		super.setQtyEntered (Qty);
		super.setQtyOrdered (getQtyEntered());
	}	//	setQty

	/**
	 * 	Set Qty Entered - enforce entered UOM 
	 *	@param QtyEntered
	 */
	public void setQtyEntered (BigDecimal QtyEntered)
	{
		if (QtyEntered != null && getC_UOM_ID() != 0)
		{
			int precision = MUOM.getPrecision(getCtx(), getC_UOM_ID());
			QtyEntered = QtyEntered.setScale(precision, RoundingMode.HALF_UP);
		}
		super.setQtyEntered (QtyEntered);
	}	//	setQtyEntered

	/**
	 * 	Set Qty Ordered - enforce Product UOM 
	 *	@param QtyOrdered
	 */
	public void setQtyOrdered (BigDecimal QtyOrdered)
	{
		MProduct product = getProduct();
		if (QtyOrdered != null && product != null)
		{
			int precision = product.getUOMPrecision();
			QtyOrdered = QtyOrdered.setScale(precision, RoundingMode.HALF_UP);
		}
		super.setQtyOrdered(QtyOrdered);
	}	//	setQtyOrdered

	/**
	 * 	Get Base value for Cost Distribution
	 *	@param CostDistribution cost Distribution
	 *	@return base number
	 */
	public BigDecimal getBase (String CostDistribution)
	{
		if (MLandedCost.LANDEDCOSTDISTRIBUTION_Costs.equals(CostDistribution))
		{
			return this.getQtyOrdered().multiply(getPriceActual());  // Actual delivery
		}
		else if (MLandedCost.LANDEDCOSTDISTRIBUTION_Line.equals(CostDistribution))
			return Env.ONE;
		else if (MLandedCost.LANDEDCOSTDISTRIBUTION_Quantity.equals(CostDistribution))
			return getQtyOrdered();
		else if (MLandedCost.LANDEDCOSTDISTRIBUTION_Volume.equals(CostDistribution))
		{
			MProduct product = getProduct();
			if (product == null)
			{
				log.severe("No Product");
				return Env.ZERO;
			}
			return getQtyOrdered().multiply(product.getVolume());
		}
		else if (MLandedCost.LANDEDCOSTDISTRIBUTION_Weight.equals(CostDistribution))
		{
			MProduct product = getProduct();
			if (product == null)
			{
				log.severe("No Product");
				return Env.ZERO;
			}
			return getQtyOrdered().multiply(product.getWeight());
		}
		//
		log.severe("Invalid Criteria: " + CostDistribution);
		return Env.ZERO;
	}	//	getBase
	
	/**************************************************************************
	 * 	Before Save
	 *	@param newRecord
	 *	@return true if it can be saved
	 */
	protected boolean beforeSave (boolean newRecord)
	{
		if (newRecord && getParent().isProcessed()) {
			log.saveError("ParentComplete", Msg.translate(getCtx(), "C_Order_ID"));
			return false;
		}
		//	Get Defaults from Parent
		if (getC_BPartner_ID() == 0 || getC_BPartner_Location_ID() == 0
			|| getM_Warehouse_ID() == 0 
			|| getC_Currency_ID() == 0)
			setOrder (getParent());
		if (m_M_PriceList_ID == 0)
			setHeaderInfo(getParent());

		
		//	R/O Check - Product/Warehouse Change
		if (!newRecord 
			&& (is_ValueChanged("M_Product_ID") || is_ValueChanged("M_Warehouse_ID") || 
			(!getParent().isProcessed() && is_ValueChanged(COLUMNNAME_M_AttributeSetInstance_ID)))) 
		{
			if (!canChangeWarehouse())
				return false;
		}	//	Product Changed
		
		//	Charge
		if (getC_Charge_ID() != 0 && getM_Product_ID() != 0)
				setM_Product_ID(0);
		//	No Product
		if (getM_Product_ID() == 0)
			setM_AttributeSetInstance_ID(0);
		//	Product
		else if (!isProcessed())	//	Set/check Product Price
		{
			//	Set Price if Actual = 0
			if (m_productPrice == null 
				&&  Env.ZERO.compareTo(getPriceActual()) == 0
				&&  Env.ZERO.compareTo(getPriceList()) == 0)
				setPrice();
			//	Check if on Price list
			if (m_productPrice == null)
				getProductPricing(m_M_PriceList_ID);
			// IDEMPIERE-1574 Sales Order Line lets Price under the Price Limit when updating
			//	Check PriceLimit
			boolean enforce = m_IsSOTrx && getParent().getM_PriceList().isEnforcePriceLimit();
			if (enforce && MRole.getDefault().isOverwritePriceLimit())
				enforce = false;
			//	Check Price Limit?
			if (enforce && getPriceLimit() != Env.ZERO
			  && getPriceActual().compareTo(getPriceLimit()) < 0)
			{
				log.saveError("UnderLimitPrice", "PriceEntered=" + getPriceEntered() + ", PriceLimit=" + getPriceLimit()); 
				return false;
			}
			int C_DocType_ID = getParent().getDocTypeID();
			MDocType docType = MDocType.get(getCtx(), C_DocType_ID);
			//
			if (!docType.isNoPriceListCheck() && !m_productPrice.isCalculated())
			{
				throw new ProductNotOnPriceListException(m_productPrice, getLine());
			}
		}

		//	UOM
		if (getC_UOM_ID() == 0)
			setDefaultC_UOM_ID();
		//	Qty Precision
		if (newRecord || is_ValueChanged("QtyEntered"))
			setQtyEntered(getQtyEntered());
		if (newRecord || is_ValueChanged("QtyOrdered"))
			setQtyOrdered(getQtyOrdered());
		
		//	FreightAmt Not used
		if (Env.ZERO.compareTo(getFreightAmt()) != 0)
			setFreightAmt(Env.ZERO);

		//	Set Tax
		if (getC_Tax_ID() == 0)
			setTax();

		//	Get Line No
		if (getLine() == 0)
		{
			String sql = "SELECT COALESCE(MAX(Line),0)+10 FROM C_OrderLine WHERE C_Order_ID=?";
			int ii = DB.getSQLValue (get_TrxName(), sql, getC_Order_ID());
			setLine (ii);
		}
		
		//	Calculations & Rounding
		setLineNetAmt();	//	extended Amount with or without tax
		setDiscount();

		/* Carlos Ruiz - globalqss
		 * IDEMPIERE-178 Orders and Invoices must disallow amount lines without product/charge
		 */
		if (getParent().getC_DocTypeTarget().isChargeOrProductMandatory()) {
			if (getC_Charge_ID() == 0 && getM_Product_ID() == 0 && (getPriceEntered().signum() != 0 || getQtyEntered().signum() != 0)) {
				log.saveError("FillMandatory", Msg.translate(getCtx(), "ChargeOrProductMandatory"));
				return false;
			}
		}
		
		//sync qtyordered and qtylostsales for closed order
		if (!newRecord && DocAction.STATUS_Closed.equals(getParent().getDocStatus()) && is_ValueChanged(COLUMNNAME_QtyDelivered)
			&& !getParent().is_ValueChanged(MOrder.COLUMNNAME_DocStatus)) {
			if (getQtyOrdered().compareTo(getQtyDelivered()) > 0)
			{
				setQtyLostSales(getQtyLostSales().add(getQtyOrdered().subtract(getQtyDelivered())));
				setQtyOrdered(getQtyDelivered());
			}
			else
			{
				setQtyLostSales(Env.ZERO);
			}
		}
		
		return true;
	}	//	beforeSave
	
	/***
	 * Sets the default unit of measure
	 * If there's a product, it sets the UOM of the product
	 * If not, it sets the default UOM of the client
	 */
	private void setDefaultC_UOM_ID() {
		int C_UOM_ID = 0;
		
		if (MProduct.get(getCtx(), getM_Product_ID()) != null) {
			C_UOM_ID = MProduct.get(getCtx(), getM_Product_ID()).getC_UOM_ID();	
		} else {
			C_UOM_ID = MUOM.getDefault_UOM_ID(getCtx());
		}

		if (C_UOM_ID > 0)
			setC_UOM_ID (C_UOM_ID);
	}
	
	/**
	 * 	Before Delete
	 *	@return true if it can be deleted
	 */
	protected boolean beforeDelete ()
	{
		//	R/O Check - Something delivered. etc.
		if (Env.ZERO.compareTo(getQtyDelivered()) != 0)
		{
			log.saveError("DeleteError", Msg.translate(getCtx(), "QtyDelivered") + "=" + getQtyDelivered());
			return false;
		}
		if (Env.ZERO.compareTo(getQtyInvoiced()) != 0)
		{
			log.saveError("DeleteError", Msg.translate(getCtx(), "QtyInvoiced") + "=" + getQtyInvoiced());
			return false;
		}
		if (Env.ZERO.compareTo(getQtyReserved()) != 0)
		{
			//	For PO should be On Order
			log.saveError("DeleteError", Msg.translate(getCtx(), "QtyReserved") + "=" + getQtyReserved());
			return false;
		}
		
		// UnLink All Requisitions
		MRequisitionLine.unlinkC_OrderLine_ID(getCtx(), get_ID(), get_TrxName());
		
		return true;
	}	//	beforeDelete
	
	/**
	 * 	After Save
	 *	@param newRecord new
	 *	@param success success
	 *	@return saved
	 */
	protected boolean afterSave (boolean newRecord, boolean success)
	{
		if (!success)
			return success;
		if (getParent().isProcessed())
			return success;
		if (   newRecord
			|| is_ValueChanged(MOrderLine.COLUMNNAME_C_Tax_ID)
			|| is_ValueChanged(MOrderLine.COLUMNNAME_LineNetAmt)) {
			MTax tax = new MTax(getCtx(), getC_Tax_ID(), get_TrxName());
	        MTaxProvider provider = new MTaxProvider(tax.getCtx(), tax.getC_TaxProvider_ID(), tax.get_TrxName());
			ITaxProvider calculator = Core.getTaxProvider(provider);
			if (calculator == null)
				throw new AdempiereException(Msg.getMsg(getCtx(), "TaxNoProvider"));
	    	return calculator.recalculateTax(provider, this, newRecord);
		}
		return success;
	}	//	afterSave

	/**
	 * 	After Delete
	 *	@param success success
	 *	@return deleted
	 */
	protected boolean afterDelete (boolean success)
	{
		if (!success)
			return success;
		if (getS_ResourceAssignment_ID() != 0)
		{
			MResourceAssignment ra = new MResourceAssignment(getCtx(), getS_ResourceAssignment_ID(), get_TrxName());
			ra.delete(true);
		}
		
		return updateHeaderTax();
	}	//	afterDelete
	
	/**
	 * Recalculate order tax
	 * @param oldTax true if the old C_Tax_ID should be used
	 * @return true if success, false otherwise
	 * 
	 * author teo_sarca [ 1583825 ]
	 */
	public boolean updateOrderTax(boolean oldTax) {
		int C_Tax_ID = getC_Tax_ID();
		boolean isOldTax = oldTax && is_ValueChanged(MOrderLine.COLUMNNAME_C_Tax_ID); 
		if (isOldTax)
		{
			Object old = get_ValueOld(MOrderLine.COLUMNNAME_C_Tax_ID);
			if (old == null)
			{
				return true;
			}
			C_Tax_ID = ((Integer)old).intValue();
		}
		if (C_Tax_ID == 0)
		{
			return true;
		}
		
		MTax t = MTax.get(C_Tax_ID);
		if (t.isSummary())
		{
			MOrderTax[] taxes = MOrderTax.getChildTaxes(this, getPrecision(), isOldTax, get_TrxName());
			if (taxes != null && taxes.length > 0)
			{
				for(MOrderTax tax : taxes)
				{
					if (!tax.calculateTaxFromLines())
						return false;
					if (tax.getTaxAmt().signum() != 0) {
						if (!tax.save(get_TrxName()))
							return false;
					}
					else {
						if (!tax.is_new() && !tax.delete(false, get_TrxName()))
							return false;
					}
				}
			}
		}
		else
		{
			MOrderTax tax = MOrderTax.get (this, getPrecision(), oldTax, get_TrxName());
			if (tax != null) {
				if (!tax.calculateTaxFromLines())
					return false;
				if (tax.getTaxAmt().signum() != 0) {
					if (!tax.save(get_TrxName()))
						return false;
				}
				else {
					if (!tax.is_new() && !tax.delete(false, get_TrxName()))
						return false;
				}
			}
		}
		return true;
	}
	
	/**
	 *	Update Tax and Header
	 *	@return true if header updated
	 */
	public boolean updateHeaderTax()
	{

		// Update header only if the document is not processed
		if (isProcessed() && !is_ValueChanged(COLUMNNAME_Processed))
			return true;

		MTax tax = new MTax(getCtx(), getC_Tax_ID(), get_TrxName());
        MTaxProvider provider = new MTaxProvider(tax.getCtx(), tax.getC_TaxProvider_ID(), tax.get_TrxName());
		ITaxProvider calculator = Core.getTaxProvider(provider);
		if (calculator == null)
			throw new AdempiereException(Msg.getMsg(getCtx(), "TaxNoProvider"));
    	if (!calculator.updateOrderTax(provider, this))
			return false;

    	return calculator.updateHeaderTax(provider, this);

	}	//	updateHeaderTax

	public void clearParent()
	{
		this.m_parent = null;
	}

	/**
	 * Get the description stripping the Close tag that was created when closing the order
	 * @return
	 */
	public String getDescriptionStrippingCloseTag() {
		String description = getDescription();
		if (description == null)
			return description;
		Pattern pattern = Pattern.compile("( \\| )?Close \\(.*\\)");
		String[] parts = pattern.split(description);
		StringBuilder description_sb = new StringBuilder();
		for (String s : parts)
			description_sb.append(s);
		return description_sb.toString();
	}

}	//	MOrderLine

