package id.co.databiz.awn.model;

public interface I_C_InvoiceCustom 
{
    
	public void setZ_TaxInvoice_ID (int Z_TaxInvoice_ID);
	public int getZ_TaxInvoice_ID();
	
	public void setZ_Kwitansi_ID (int Z_Kwitansi_ID);
	public int getZ_Kwitansi_ID();

	public int getZ_WFScenario_ID();
	public void setZ_WFScenario_ID(int Z_WFScenario_ID);
	
	public String getTaxInvoiceNo();
	public void setTaxInvoiceNo(String taxInvoiceNo);
	
	public String getCreateTaxInvoice();
	public void setCreateTaxInvoice(String createTaxInvoice);
	
	public String getPrefix();
	public void setPrefix(String prefix);
}