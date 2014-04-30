

@SuppressWarnings("serial")
public class EmptyException extends Exception {
	public EmptyException () {
		super("The queue is empty");
	}
}
