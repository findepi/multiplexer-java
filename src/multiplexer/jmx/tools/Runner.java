package multiplexer.jmx.tools;

import multiplexer.jmx.server.JmxServer;

/**
 * Simple class to be used as a command line/script entry-point of the
 * Multiplexer jar, so that users don't need to know all the class names.
 * 
 * @author Piotr Findeisen
 */
public class Runner {

	final static private int SUBCOMMAND_NAME_PADDING = 3;

	private static SubCommand[] subCommands = {

		new SubCommand(
			"server",
			"runs Multiplexer server (use `jmx-*-withdeps.jar' if using java's `-jar' option)") {
			@Override
			void run(String[] args) throws Exception {
				JmxServer.main(args);
			}
		},

		new SubCommand("compile-constants",
			"compiles Multiplexer rules file into Java constants definitions") {
			@Override
			void run(String[] args) throws Exception {
				CompileConstants.main(args);
			}
		},

	};

	protected static void printHelpAndExit() {
		System.err.println("Usage: java ... ( -jar <this-jar> | "
			+ Runner.class.getName() + " ) subcommand args...");
		System.err.println();
		System.err.println("Available subcommands:");
		int maxlen = 0;
		for (SubCommand subCommand : subCommands)
			maxlen = subCommand.getName().length() > maxlen ? subCommand
				.getName().length() : maxlen;
		for (final SubCommand subCommand : subCommands) {
			StringBuilder nameAndDescription = new StringBuilder();
			nameAndDescription.ensureCapacity(maxlen + SUBCOMMAND_NAME_PADDING
				+ 6 /* for extra characters */
				+ subCommand.getDescription().length());
			nameAndDescription.append(" * ");
			nameAndDescription.append(subCommand.getName());
			for (int i = maxlen + SUBCOMMAND_NAME_PADDING
				- subCommand.getName().length(); i > 0; i--) {
				nameAndDescription.append(" ");
			}
			nameAndDescription.append(" - ");
			nameAndDescription.append(subCommand.getDescription());
			System.err.println(nameAndDescription.toString());
		}
		System.exit(1);
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		if (args.length == 0) {
			printHelpAndExit();
			return;
		}

		// take the first argument
		String subCommandName = args[0];
		assert subCommandName != null;

		// Strip subCommandName from args.
		final int stripCount = 1;
		assert args.length >= stripCount;
		String[] subCommandArgs = new String[args.length - stripCount];
		System.arraycopy(args, stripCount, subCommandArgs, 0, args.length
			- stripCount);
		
		if (subCommandName.equals("help")) {
			if (subCommandArgs.length == 0) {
				printHelpAndExit();
				return;
			} else {
				System.err
					.println("Help about subcommands is not available. Try ... "
						+ subCommandArgs[0] + " --help instead.");
				System.exit(1);
			}
		}

		for (SubCommand subCommand : subCommands) {
			if (subCommandName.equals(subCommand.getName())) {
				subCommand.run(subCommandArgs);
				return;
			}
		}

		printHelpAndExit();
		return;
	}
}
