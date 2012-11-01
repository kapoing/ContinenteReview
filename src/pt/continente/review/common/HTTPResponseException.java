package pt.continente.review.common;

public class HTTPResponseException extends Exception {

	String detailMessage ="";
	String url = "";
	/**
	 * 
	 */
	private static final long serialVersionUID = 3703279369859671570L;

	public HTTPResponseException() {
		super();
		// TODO Auto-generated constructor stub
	}

	public HTTPResponseException(String detailMessage) {
		super(detailMessage);
		// TODO Auto-generated constructor stub
	}

	public HTTPResponseException(Throwable throwable) {
		super(throwable);
		// TODO Auto-generated constructor stub
	}

	public HTTPResponseException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
		// TODO Auto-generated constructor stub
	}
	public void setUrl(String url) {
		
	}
	public void setDetailMessage(String message) {
		detailMessage = message;
	}
	public String getMessage() {
		return "URL:"+url+"\n"+detailMessage;
	}
	public String toString()	{
		return this.getMessage();
	}

}