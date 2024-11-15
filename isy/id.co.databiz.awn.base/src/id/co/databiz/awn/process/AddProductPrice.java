
package id.co.databiz.awn.process;

import java.math.BigDecimal;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MPriceListVersion;
import org.compiere.model.MProduct;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
 
/**
 *	Add Product Price
 *	
 *  @author Anozi Mada
 */
public class AddProductPrice extends SvrProcess
{
	private int 	priceListVersionID = 0;
	private int 	productID = 0;
	
	private BigDecimal priceList = null;
	private BigDecimal priceStd = null;
	private BigDecimal priceLimit = null;
	
	/**
	 *  Prepare - e.g., get Parameters.
	 */
	protected void prepare()
	{
		ProcessInfoParameter[] para = getParameter();
		
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals("M_PriceList_Version_ID"))
				priceListVersionID = para[i].getParameterAsInt();		
			else if (name.equals("M_Product_ID"))
				productID = para[i].getParameterAsInt();	
			else if (name.equals("PriceList"))
				priceList = para[i].getParameterAsBigDecimal();	
			else if (name.equals("PriceStd"))
				priceStd = para[i].getParameterAsBigDecimal();	
			else if (name.equals("PriceLimit"))
				priceLimit = para[i].getParameterAsBigDecimal();	
			else
				log.log(Level.SEVERE, "prepare - Unknown Parameter: " + name);
		}	
		
		if (priceList == null)
			priceList = Env.ZERO;
		if (priceStd == null)
			priceStd = Env.ZERO;
		if (priceLimit == null)
			priceLimit = Env.ZERO;
	}	//	prepare

	/**
	 *	@return status message
	 *	@throws Exception
	 */
	protected String doIt () throws Exception
	{
		log.info("M_PriceList_Version_ID=" + priceListVersionID + ", M_Product_ID=" + productID +
				", PriceList=" + priceList + ", PriceStd=" + priceStd +", PriceLimit=" + priceLimit);
		
		if (priceListVersionID <= 0)
			throw new AdempiereException("@Mandatory@ @M_PriceList_Version_ID@");
		if (productID <= 0)
			throw new AdempiereException("@Mandatory@ @M_Product_ID@");
		
		MPriceListVersion plv = new MPriceListVersion(getCtx(), priceListVersionID, get_TrxName());
		MProduct product = MProduct.get(getCtx(), productID);
		
		int count = DB.getSQLValue(get_TrxName(), 
				"SELECT COUNT(*) FROM M_ProductPrice WHERE M_PriceList_Version_ID = ? AND M_Product_ID = ?", 
				plv.get_ID(),product.get_ID());
		if (count > 0) {
			String errorMessage = "@AlreadyExists@. " +
					"Price List Version: " + plv.getName() +
					", Product: " + product.getName();
			throw new AdempiereException(errorMessage);
		}
		
		StringBuffer sql = new StringBuffer();
		sql.append("INSERT INTO M_ProductPrice(M_PriceList_Version_ID,M_Product_ID, ");
		sql.append("AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy, ");
		sql.append("PriceList,PriceStd,PriceLimit) ");
		sql.append("VALUES(?,?,?,?,'Y',SysDate,?,SysDate,?,?,?,?)");
		int created = DB.executeUpdate(sql.toString(), new Object[]{
			plv.get_ID(),
			product.get_ID(),
			plv.getAD_Client_ID(),
			plv.getAD_Org_ID(),
			getAD_User_ID(),
			getAD_User_ID(),
			priceList,
			priceStd,
			priceLimit
		}, false, get_TrxName());
		
		return "@Created@ " + created;
	}	//	doIt
}	
