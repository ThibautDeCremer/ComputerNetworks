
public class NotFoundException extends RequestException {

	public NotFoundException(String extraInfo) {
		super(404, extraInfo,"404 Not Found");
	}
}
