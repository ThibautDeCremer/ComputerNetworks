
public class RequestException extends Exception{

	/**
	 * Needed to remove errors
	 */
	private static final long serialVersionUID = 1L;
	
	private final int statuscode;
	
	private final String extraInfo;
	
	private final String responseHeader;
	
	public RequestException(int statuscode, String extraInfo, String responseheader) {
		this.statuscode = statuscode;
		this.extraInfo = extraInfo;
		this.responseHeader = responseheader;
	}
	
	public int setStatuscode() {
		return this.statuscode;
	}
	
	public String getExtraInfo() {
		return this.extraInfo;
	}
	
	public String getResponseHeader() {
		return this.responseHeader;
	}
		

}
