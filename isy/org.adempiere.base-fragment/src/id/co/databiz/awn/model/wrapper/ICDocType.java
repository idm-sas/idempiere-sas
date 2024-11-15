
package id.co.databiz.awn.model.wrapper;


public interface ICDocType 
{
	public void setIsAROther (boolean IsAROther);
	public boolean isAROther();
	
	public void setC_DocTypeTaxInvoice_ID (int C_DocTypeTaxInvoice_ID);
	public int getC_DocTypeTaxInvoice_ID();
	
	public void setIsInternal (boolean IsInternal);
	public boolean isInternal();
	
	public boolean getCreateTaxInvoice();
	public void setCreateTaxInvoice(boolean createTaxInvoice);
}
