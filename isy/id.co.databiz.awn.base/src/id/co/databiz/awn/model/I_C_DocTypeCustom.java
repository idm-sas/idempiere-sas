package id.co.databiz.awn.model;

public interface I_C_DocTypeCustom 
{
    
	public void setC_DocTypeTaxInvoice_ID (int C_DocTypeTaxInvoice_ID);
	public int getC_DocTypeTaxInvoice_ID();
	public void setC_DocTypeKwitansi_ID (int C_DocTypeKwitansi_ID);
	public int getC_DocTypeKwitansi_ID();
	
	public void setIsInternal (boolean IsInternal);
	public boolean isInternal();
	
	public void setIsAROther (boolean IsAROther);
	public boolean isAROther();
	
	public void setIsMakeToOrder (boolean IsMakeToOrder);
	public boolean isMakeToOrder();

	public boolean getCreateTaxInvoice();
	public void setCreateTaxInvoice(boolean createTaxInvoice);
}