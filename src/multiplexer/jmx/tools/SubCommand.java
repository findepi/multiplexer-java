package multiplexer.jmx.tools;

abstract class SubCommand {

	private final String name;
	private final String description;

	public SubCommand(String name, String description) {
		super();
		if (name == null)
			throw new NullPointerException("name");
		if (description == null)
			throw new NullPointerException("description");
		this.name = name;
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	abstract void run(String[] args) throws Exception;
}
