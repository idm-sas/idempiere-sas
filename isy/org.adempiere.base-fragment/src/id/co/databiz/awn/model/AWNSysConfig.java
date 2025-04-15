package id.co.databiz.awn.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.MSysConfig;

public class AWNSysConfig extends MSysConfig {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7492068712363674839L;
	
	public static final String AWN_MAX_SESSION = "AWN_MAX_SESSION";
	public static final String AWN_MAX_USERROLE = "AWN_MAX_USERROLE";
	
	public static final String ISY_ACCESS_FILE_PATH = "ISY_ACCESS_FILE_PATH";
	public static final String ISY_STD_PRICELIST_VERSION = "ISY_STD_PRICELIST_VERSION";
	
	public static final String Acct_WIPVarianceAllocation = "SYNC_Acct_WIPVarianceAllocation";
	public static final String Acct_FGVarianceAllocation = "SYNC_Acct_FGVarianceAllocation";
	public static final String Acct_RMVarianceAllocation = "SYNC_Acct_RMVarianceAllocation";
	
	public static final String ISY_StandardCostingUseCurrentCost = "ISY_StandardCostingUseCurrentCost";
	public static final String ISY_STANDARD_COSTING_ALLOW_ZERO_WITHOUT_PURCHASE = "ISY_STANDARD_COSTING_ALLOW_ZERO_WITHOUT_PURCHASE";
	public static final String	ISY_STANDARD_COSTING_DISTRIBUTE_PPV_OR_IPV = "ISY_STANDARD_COSTING_DISTRIBUTE_PPV_OR_IPV"; //true means PPV , false means IPV https://databiz.atlassian.net/browse/ISY-388?focusedCommentId=106016

	public static final String PROJECT_ID_COMMENTS = "PROJECT_ID_COMMENTS";
	
	public static final String DICTIONARY_ID_COMMENTS = "DICTIONARY_ID_COMMENTS";
	
	public static final String ISY_VALIDATION_DP_INVOICE = "ISY_VALIDATION_DP_INVOICE";
	
	public static final String ISY_WF_NODE_ACTION_EMAIL_VALIDATION = "ISY_WF_NODE_ACTION_EMAIL_VALIDATION"; //only for sab8 
	
	public static final String ISY_CUSTOMER_RETURN_CREATE_LINES = "ISY_CUSTOMER_RETURN_CREATE_LINES"; //only for sab8 
	
    public static final String LOGIN_WITH_TENANT_PREFIX = "LOGIN_WITH_TENANT_PREFIX"; //SysConfig IDM 11
    
    public static final String LOGIN_PREFIX_SEPARATOR = "LOGIN_PREFIX_SEPARATOR"; //SysConfig IDM 11

	public static final String ISY_ALLOW_AUTO_APPROVAL = "ISY_ALLOW_AUTO_APPROVAL"; //for auto approval
	
	public static final String ISY_GENERATE_INVOICE_CHECK_RMA_INOUTLINE = "ISY_GENERATE_INVOICE_CHECK_RMA_INOUTLINE"; //for auto approval
	
	public static final String ISY_GENERATE_TAXINVOICE_OVER_MAX_VALUE = "ISY_GENERATE_TAXINVOICE_OVER_MAX_VALUE"; 
	//Brilly ISY-37 > Option 1 = Set Exception di MWFActivity ACTION_AppsProcess (A). Option 2 Update Invoice Description update errornya (B)

	public static final String ISY_REQUEST_SEND_EMAIL_ON_UPDATE = "ISY_REQUEST_SEND_EMAIL_ON_UPDATE";
	
	public static final String ISY_TRADE_DISCOUNT_VENDOR_PRODUCTTYPE_ITEM = "ISY_TRADE_DISCOUNT_VENDOR_PRODUCTTYPE_ITEM";
	
	public static final String ISY_MAX_PANEL_TABLESIZE = "ISY_MAX_PANEL_TABLESIZE";

	
	//ISY-375
	public static int DOCTYPE_CC_MaterialReceipt = 900001;
	public static int DOCTYPE_CC_ComponentIssue = 900002;
	public static int DOCTYPE_CC_UsegeVariance = 900003;
	public static int DOCTYPE_CC_MethodChangeVariance = 900004;
	public static int DOCTYPE_CC_RateVariance = 900005;
	public static int DOCTYPE_CC_MixVariance = 900006;
	public static int DOCTYPE_CC_ActivityControl = 900007;
	
	public static final String SYS_CostCollectorStandardCostingUseCurrentCost = 
			"SYNC_CostCollectorStandardCostingUseCurrentCost";

	
	public AWNSysConfig(Properties ctx, int AD_SysConfig_ID, String trxName) {
		super(ctx, AD_SysConfig_ID, trxName);
		// TODO Auto-generated constructor stub
	}
	
	public AWNSysConfig(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
		// TODO Auto-generated constructor stub
	}


}
