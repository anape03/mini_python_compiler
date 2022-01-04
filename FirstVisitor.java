import java.util.*;

import minipython.analysis.DepthFirstAdapter;
import minipython.node.*;


public class FirstVisitor extends DepthFirstAdapter {
	private final Hashtable<String, Node> variables;
	private final Hashtable<String, Node> functions;

	private final Hashtable<Node, VAR_TYPES> variableTypes;

	public static enum ERROR_TYPES {
		UNDECLARED_VARIABLE,
		UNDEFINED_FUNCTION,
		UNORDERED_PARAMS,
		WRONG_PARAMS,
		TYPE_MISSMATCH,
		ADD_TYPE_MISSMATCH,
	}

	public static enum VAR_TYPES {
		NUMBER,
		STRING,
		BOOLEAN,
		NONE,
	}

	public FirstVisitor(Hashtable<String, Node> variables, Hashtable<String, Node> functions) {
		this.variables = variables;
		this.functions = functions;
		this.variableTypes = new Hashtable<>();
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

	@Override
	public void outAAssignmentStatement(AAssignmentStatement node) {
		PArithmetics arithmetics = node.getArithmetics();
		Class arithmeticsClass = arithmetics.getClass();
		variableTypes.put(node, variableTypes.get(arithmeticsClass.cast(arithmetics)));
	}

	@Override
	public void outANumberArithmetics(ANumberArithmetics node) {
		variableTypes.put(node, VAR_TYPES.NUMBER);
	}

	@Override
	public void outAStrlitArithmetics(AStrlitArithmetics node) {
		variableTypes.put(node, VAR_TYPES.STRING);
	}

	@Override
	public void outANoneArithmetics(ANoneArithmetics node) {
		variableTypes.put(node, VAR_TYPES.NONE);
	}

	@Override
	public void outALenArithmetics(ALenArithmetics node) {
		variableTypes.put(node, VAR_TYPES.NUMBER);
	}

	@Override
	public void outAMaxminArithmetics(AMaxminArithmetics node) {
		variableTypes.put(node, VAR_TYPES.NUMBER);
	}

	@Override
	public void outAIdentifierArithmetics(AIdentifierArithmetics node) {
		String id = ((AIdentifier) node.getIdentifier()).getId().getText();

		// Iterate all varibles present and find a match
		for (Node n : variableTypes.keySet()) {
			if (n instanceof AAssignmentStatement) {
				if (id.equals(((AIdentifier) ((AAssignmentStatement) n).getIdentifier()).getId().getText())) {
					// Same type as defined in the assignment statement
					variableTypes.put(node, variableTypes.get(n));
					break;
				}
			}
		}
	}

	@Override
	public void outAExpArithmetics(AExpArithmetics node) {
		// Find the class type of left and right children
		Class lClass = node.getL().getClass();
		Class rClass = node.getR().getClass();

		// All children must return a number for the expression to be valid
		if (variableTypes.get(lClass.cast(node.getL())) == VAR_TYPES.NUMBER &&
				variableTypes.get(rClass.cast(node.getR())) == VAR_TYPES.NUMBER) {
			variableTypes.put(node, VAR_TYPES.NUMBER);
		} else if (variableTypes.get(lClass.cast(node.getL())) != VAR_TYPES.NUMBER) {
			printError(node.getL(), ERROR_TYPES.TYPE_MISSMATCH);
		} else if (variableTypes.get(rClass.cast(node.getR())) != VAR_TYPES.NUMBER) {
			printError(node.getR(), ERROR_TYPES.TYPE_MISSMATCH);
		}
	}

	// TODO: Should we allow string plus string additions?
	@Override
	public void outAPlusminusArithmetics(APlusminusArithmetics node) {
		// Find the class type of left and right children
		Class lClass = node.getL().getClass();
		Class rClass = node.getR().getClass();

		// The childrens' types must match
		if (variableTypes.get(lClass.cast(node.getL())) == variableTypes.get(rClass.cast(node.getR()))) {
			variableTypes.put(node, variableTypes.get(lClass.cast(node.getL())));
		} else {
			printError(node, ERROR_TYPES.ADD_TYPE_MISSMATCH);
		}
	}

	@Override
	public void outAMultdivArithmetics(AMultdivArithmetics node) {
		// Same as outAExpArithmetics
		// Find the class type of left and right children
		Class lClass = node.getL().getClass();
		Class rClass = node.getR().getClass();

		// All children must return a number for the expression to be valid
		if (variableTypes.get(lClass.cast(node.getL())) == VAR_TYPES.NUMBER &&
				variableTypes.get(rClass.cast(node.getR())) == VAR_TYPES.NUMBER) {
			variableTypes.put(node, VAR_TYPES.NUMBER);
		} else if (variableTypes.get(lClass.cast(node.getL())) != VAR_TYPES.NUMBER) {
			printError(node.getL(), ERROR_TYPES.TYPE_MISSMATCH);
		} else if (variableTypes.get(rClass.cast(node.getR())) != VAR_TYPES.NUMBER) {
			printError(node.getR(), ERROR_TYPES.TYPE_MISSMATCH);
		}
	}

	// Helper methods
	// Print the appropriate error message
	public static void printError(Node node, ERROR_TYPES type) {
		String message = "Error[";
		AIdentifier id;
		switch (type) {
			case UNDECLARED_VARIABLE:
				id = (AIdentifier) node;
				message += id.getId().getLine() + "]: Undeclared variable named \'" + id.getId().getText() + "\'.";
				break;

			case UNDEFINED_FUNCTION:
				id = (AIdentifier) node;
				message += id.getId().getLine() + "]: Undefined function named \'" + id.getId().getText() + "\'.";
				break;

			case UNORDERED_PARAMS:
				AFunction function = (AFunction) node;
				id = (AIdentifier) function.getIdentifier();
				message += id.getId().getLine() + "]: Parameter after default parameter in function \'" + id.getId().getText() + "\'.";
				break;

			case WRONG_PARAMS:
				id = (AIdentifier) ((AFunctionCall) node).getIdentifier();
				message += id.getId().getLine() + "]: Wrong parameters given for function named \'" + id.getId().getText() + "\'.";
				break;

			case TYPE_MISSMATCH:
				message += "]: Variable type missmatch at \'" + node.toString().stripTrailing() + "\'.";
				break;

			case ADD_TYPE_MISSMATCH:
				message += "]: Variable type missmatch in addition/substraction.";
				break;

			default:
				message += "]: Unknown error.";
		}

		System.err.println(message);
		System.exit(-1);
	}

	// Getters
	public Hashtable<String, Node> getVariables() {
		return variables;
	}

	public Hashtable<String, Node> getFunctions() {
		return functions;
	}
}
