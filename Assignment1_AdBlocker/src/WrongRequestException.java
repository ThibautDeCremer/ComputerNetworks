
public class WrongRequestException extends Exception{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String reason; 
	
	public String getReason() {
		return this.reason;
	}
	
	public void setReason(String reason) {
		this.reason = reason;
	}
	
	public WrongRequestException(String reason) {
		this.setReason(reason);
	}
}
