import java.util.*;

import minipython.analysis.DepthFirstAdapter;
import minipython.node.*;


public class FirstVisitor extends DepthFirstAdapter {
	private final Hashtable<String, Node> variables;
	private final Hashtable<String, Node> functions;

	public static enum ERROR_TYPES {
		UNDECLARED_VARIABLE,
		UNDEFINED_FUNCTION,
	}

	public FirstVisitor(Hashtable<String, Node> variables, Hashtable<String, Node> functions) {
		this.variables = variables;
		this.functions = functions;
	}

	@Override
	public void inAIdentifier(AIdentifier node) {
		// Get the name, the line and the parent of the node
		String name = node.getId().getText();
		Node parent = node.parent();

		// Check for undeclared variables
		if (parent instanceof AIdentifierArithmetics) {
			if (!variables.containsKey(name)) {
				// Print error message
				printError(node, ERROR_TYPES.UNDECLARED_VARIABLE);
			}

		} else if (parent instanceof AForStatement) {
			AForStatement forLoop = (AForStatement) parent;

			// Check that the second identifer is an existing variable
			AIdentifier id2 = (AIdentifier) forLoop.getId2();
			if (name.equals(id2.getId().getText()) && !variables.containsKey(name)) {
				// Print error message
				printError(node, ERROR_TYPES.UNDECLARED_VARIABLE);
			}
		}
	}

	@Override
	public void outAIdentifier(AIdentifier node) {
		// Get the name and the parent of the node
		String name = node.getId().getText();
		Node parent = node.parent();

		// Check the parent's type and react accordingly
		if (parent instanceof AAssignmentStatement
			|| parent instanceof AMoreAssignments
			|| parent instanceof AArgument) {
			// Create a new variable
			variables.put(name, node);

		} else if (parent instanceof AFunction) {
			// Create a new function
			functions.put(name, node);

		} else if (parent instanceof AForStatement) {
			AForStatement forLoop = (AForStatement) parent;

			// Create a variable for the first identifier
			AIdentifier id1 = (AIdentifier) forLoop.getId1();
			if (name.equals(id1.getId().getText())) {
				variables.put(name, node);
			}
		}
	}

	// Helper methods
	// Print the appropriate error message
	public static void printError(AIdentifier node, ERROR_TYPES type) {
		String message = "Error[" + node.getId().getLine() + "]: ";
		switch (type) {
			case UNDECLARED_VARIABLE:
				message += "Undeclared variable named \'" + node.getId().getText() + "\'.";
				break;

			case UNDEFINED_FUNCTION:
				message += "Undefined function named \'" + node.getId().getText() + "\'.";
				break;

			default:
				message += "]: Unknown error.";
		}

		System.err.println(message);
	}

	// Getters
	public Hashtable<String, Node> getVariables() {
		return variables;
	}

	public Hashtable<String, Node> getFunctions() {
		return functions;
	}
}
