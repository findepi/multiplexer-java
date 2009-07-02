package multiplexer.jmx.tools.rulesconsts;

public class Constant<E> {

	public final String name;
	public final E value;

	public Constant(String name, E value) {
		super();
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public E getValue() {
		return value;
	}
}
