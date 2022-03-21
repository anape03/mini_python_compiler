import java.util.*;

import minipython.analysis.DepthFirstAdapter;
import minipython.node.*;

/**
 * The FirstVisitor does a first traversal of the code and stores the variables
 * and functions to their hashtables accordingly
 * 
 * @param variables     hashtable containing the name and node of the variables read
 * @param funcitons     hashtable containing the name and node of the functions read
 * @param variableTypes hashtable containing the node and type of the variables read
 */
public class FirstVisitor extends DepthFirstAdapter {

	private final Hashtable<String, Node> variables;
	private final Hashtable<String, Node> functions;

	private final Hashtable<Node, VAR_TYPES> variableTypes;
	private final List<Function> functionList;

	/**
	* All the recognisable potential error types listed
	*/
	public static enum ERROR_TYPES {
		UNDECLARED_VARIABLE,
		UNDEFINED_FUNCTION,
		UNORDERED_PARAMS,
		WRONG_PARAMS,
		TYPE_MISSMATCH,
		ADD_TYPE_MISSMATCH,
		MINUS_TYPE_MISSMATCH,
		NONE_OPERATION,
		IDENTICAL_FUNCTIONS,
	}

	/**
	* All the potential variable types listed
	*/
	public static enum VAR_TYPES {
		INTEGER,
		DOUBLE,
		STRING,
		NONE,
		UNKNOWN,
	}

	public FirstVisitor(Hashtable<String, Node> variables, Hashtable<String, Node> functions, 
			Hashtable<Node, VAR_TYPES> variableTypes) {
		this.variables = variables;
		this.functions = functions;
		this.variableTypes = variableTypes;
		this.functionList = new ArrayList<>();
	}

	@Override
	public void outAGoal(AGoal node) {
		// Check for functions with identical names and parameters' names
		for (Function function : functionList) {
			for (Function f : functionList) {
				if (function != f && function.getName().equals(f.getName())) {
					if (function.getTotalParams() == f.getTotalParams() || function.getParams() == f.getParams()) {
						printError(node, ERROR_TYPES.IDENTICAL_FUNCTIONS, function.getName());
					}
				}
			}
		}
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
			variableTypes.put(node, VAR_TYPES.UNKNOWN);

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

	/**
	* Make sure no two functions share the same name and parameters' name
	*/
	@Override
	@SuppressWarnings("unchecked")
	public void inAFunction(AFunction node) {
		// Get the function's name
		String name = ((AIdentifier) node.getIdentifier()).getId().getText();
		Function function = new Function(name);

		// Get the function's parameter count
		LinkedList<AArgument> arguments = node.getArgument();
		if (arguments.size() > 0) {
			if (arguments.get(0).getAssignValue().size() > 0) {
				function.setDefaultParams(1);
			} else {
				function.setParams(1);
			}

			for (AMoreAssignments argument : ((LinkedList<AMoreAssignments>) arguments.get(0).getMoreAssignments())) {
				// Check for default parameters
				LinkedList<AAssignValue> value = argument.getAssignValue();
				if (value.size() > 0) {
					function.setDefaultParams(function.getDefaultParams() + 1);
				} else {
					function.setParams(function.getParams() + 1);
				}
			}
		}

		functionList.add(function);
	}

	@Override
	public void outAFunction(AFunction node) {
		// Retrieve the function's return type from it's return statement(if it exists)
		if (node.getStatement() instanceof AReturnStatement) {
			PArithmetics arithmetics = ((AReturnStatement) node.getStatement()).getArithmetics();
			variableTypes.put(node.getIdentifier(), variableTypes.get(arithmetics));

		} else {  // Otheriwse None return type
			variableTypes.put(node.getIdentifier(), VAR_TYPES.NONE);
		}
	}

	@Override
	public void outAAssignmentStatement(AAssignmentStatement node) {
		// Replace previous assignment type
		String name = ((AIdentifier) node.getIdentifier()).getId().getText();
		String varName;
		List<Node> toRemove = new ArrayList<>();
		for (Node variable  : variableTypes.keySet()) {
			if (variable instanceof AIdentifier) {
				varName = ((AIdentifier) variable).getId().getText();
				if (varName.equals(name)) {
					toRemove.add(variable);
					break;
				}
			}
		}
		for (Node n : toRemove) {
			variableTypes.remove(n);
		}

		PArithmetics arithmetics = node.getArithmetics();
		Class<?> arithmeticsClass = arithmetics.getClass();
		variableTypes.put(node.getIdentifier(), variableTypes.get(arithmeticsClass.cast(arithmetics)));
	}

	@Override
	public void outANumberArithmetics(ANumberArithmetics node) {
		variableTypes.put(node, getNumberSubtype(node.getNumber()));
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
		variableTypes.put(node, VAR_TYPES.INTEGER);
	}

	@Override
	public void outAMaxminArithmetics(AMaxminArithmetics node) {
		variableTypes.put(node, VAR_TYPES.DOUBLE);
	}

	@Override
	public void outAIdentifierArithmetics(AIdentifierArithmetics node) {
		String id = ((AIdentifier) node.getIdentifier()).getId().getText().trim();

		// Iterate all varibles present and find a match
		for (Node n : variableTypes.keySet()) {
			if (n instanceof AIdentifier) {
				if (id.equals(((AIdentifier) n).getId().getText().trim())) {
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
		Class<?> lClass = node.getL().getClass();
		Class<?> rClass = node.getR().getClass();
		VAR_TYPES lType = variableTypes.get(lClass.cast(node.getL()));
		VAR_TYPES rType = variableTypes.get(rClass.cast(node.getR()));

		// AIdentifierArithmetics inserts Aidentifier node and not itself
		// Hence, the AIdentifier's type should be retrieved
		if (node.getL() instanceof AIdentifierArithmetics) {
			lType = findVariableType(
					((AIdentifier) ((AIdentifierArithmetics) node.getL()).getIdentifier()).getId().getText());
		}

		if (node.getR() instanceof AIdentifierArithmetics) {
			rType = findVariableType(
					((AIdentifier) ((AIdentifierArithmetics) node.getR()).getIdentifier()).getId().getText());
		}

		// All children must return a number for the expression to be valid
		if (rType == VAR_TYPES.NONE || lType == VAR_TYPES.NONE) {
			printError(node, ERROR_TYPES.NONE_OPERATION);
		} else if (lType == VAR_TYPES.UNKNOWN || rType == VAR_TYPES.UNKNOWN) {
			variableTypes.put(node, VAR_TYPES.UNKNOWN);
		} else if (lType == VAR_TYPES.INTEGER && rType == VAR_TYPES.INTEGER) {
			variableTypes.put(node, VAR_TYPES.INTEGER);
		} else if (isNumber(lType) && isNumber(rType)) {
			variableTypes.put(node, VAR_TYPES.DOUBLE);
		} else {
			printError(node.getR(), ERROR_TYPES.TYPE_MISSMATCH);
		}
	}

	@Override
	public void outAPlusArithmetics(APlusArithmetics node) {
		// Find the class type of left and right children
		Class<?> lClass = node.getL().getClass();
		Class<?> rClass = node.getR().getClass();
		VAR_TYPES lType = variableTypes.get(lClass.cast(node.getL()));
		VAR_TYPES rType = variableTypes.get(rClass.cast(node.getR()));

		if (node.getL() instanceof AIdentifierArithmetics) {
			lType = findVariableType(
					((AIdentifier) ((AIdentifierArithmetics) node.getL()).getIdentifier()).getId().getText());
		}

		if (node.getR() instanceof AIdentifierArithmetics) {
			rType = findVariableType(
					((AIdentifier) ((AIdentifierArithmetics) node.getR()).getIdentifier()).getId().getText());
		}

		// The childrens' types must match
		if (rType == VAR_TYPES.NONE || lType == VAR_TYPES.NONE) {
			printError(node, ERROR_TYPES.NONE_OPERATION);
		} else if (lType == VAR_TYPES.UNKNOWN || rType == VAR_TYPES.UNKNOWN) {
			variableTypes.put(node, VAR_TYPES.UNKNOWN);
		} else if (lType == rType) {
			variableTypes.put(node, lType);
		} else if (isNumber(lType) && isNumber(rType)) {
			variableTypes.put(node, VAR_TYPES.DOUBLE);
		} else {
			printError(node, ERROR_TYPES.ADD_TYPE_MISSMATCH);
		}
	}

	@Override
	public void outAMinusArithmetics(AMinusArithmetics node) {
		// Find the class type of left and right children
		Class<?> lClass = node.getL().getClass();
		Class<?> rClass = node.getR().getClass();
		VAR_TYPES lType = variableTypes.get(lClass.cast(node.getL()));
		VAR_TYPES rType = variableTypes.get(rClass.cast(node.getR()));

		if (node.getL() instanceof AIdentifierArithmetics) {
			lType = findVariableType(
					((AIdentifier) ((AIdentifierArithmetics) node.getL()).getIdentifier()).getId().getText());
		}

		if (node.getR() instanceof AIdentifierArithmetics) {
			rType = findVariableType(
					((AIdentifier) ((AIdentifierArithmetics) node.getR()).getIdentifier()).getId().getText());
		}

		if (rType == VAR_TYPES.NONE || lType == VAR_TYPES.NONE) {
			printError(node, ERROR_TYPES.NONE_OPERATION);
		} else if (lType == VAR_TYPES.UNKNOWN || rType == VAR_TYPES.UNKNOWN) {
			variableTypes.put(node, VAR_TYPES.UNKNOWN);
		} else if (lType == VAR_TYPES.INTEGER && rType == VAR_TYPES.INTEGER) {
			variableTypes.put(node, VAR_TYPES.INTEGER);
		} else if (lType == VAR_TYPES.STRING || rType == VAR_TYPES.STRING) {
			printError(node, ERROR_TYPES.MINUS_TYPE_MISSMATCH);
		} else if (FirstVisitor.isNumber(lType) && FirstVisitor.isNumber(rType)) {
			variableTypes.put(node, VAR_TYPES.DOUBLE);
		} else {
			printError(node, ERROR_TYPES.MINUS_TYPE_MISSMATCH);
		}
	}

	@Override
	public void outAMultArithmetics(AMultArithmetics node) {
		// Same as outAExpArithmetics
		// Find the class type of left and right children
		Class<?> lClass = node.getL().getClass();
		Class<?> rClass = node.getR().getClass();
		VAR_TYPES lType = variableTypes.get(lClass.cast(node.getL()));
		VAR_TYPES rType = variableTypes.get(rClass.cast(node.getR()));

		if (node.getL() instanceof AIdentifierArithmetics) {
			lType = findVariableType(
					((AIdentifier) ((AIdentifierArithmetics) node.getL()).getIdentifier()).getId().getText());
		}

		if (node.getR() instanceof AIdentifierArithmetics) {
			rType = findVariableType(
					((AIdentifier) ((AIdentifierArithmetics) node.getR()).getIdentifier()).getId().getText());
		}

		// All children must return a number for the expression to be valid
		if (rType == VAR_TYPES.NONE || lType == VAR_TYPES.NONE) {
			printError(node, ERROR_TYPES.NONE_OPERATION);
		} else if (lType == VAR_TYPES.UNKNOWN || rType == VAR_TYPES.UNKNOWN) {
			variableTypes.put(node, VAR_TYPES.UNKNOWN);
		} else if (lType == VAR_TYPES.INTEGER && rType == VAR_TYPES.INTEGER) {
			variableTypes.put(node, VAR_TYPES.INTEGER);
		} else if (lType == VAR_TYPES.INTEGER && rType == VAR_TYPES.STRING
				|| lType == VAR_TYPES.STRING && rType == VAR_TYPES.INTEGER) {
			variableTypes.put(node, VAR_TYPES.STRING);
		} else if (isNumber(lType) && isNumber(rType)) {
			variableTypes.put(node, VAR_TYPES.DOUBLE);
		} else {
			printError(node, ERROR_TYPES.TYPE_MISSMATCH);
		}
	}

	@Override
	public void outAFunctionArithmetics(AFunctionArithmetics node) {
		// Find the function's type using it's identifier
		String id = ((AIdentifier) ((AFunctionCall) node.getFunctionCall()).getIdentifier()).getId().getText();
		VAR_TYPES type = findVariableType(id);
		variableTypes.put(node, type);
	}

	@Override
	public void outADivArithmetics(ADivArithmetics node) {
		// Same as outAExpArithmetics
		// Find the class type of left and right children
		Class<?> lClass = node.getL().getClass();
		Class<?> rClass = node.getR().getClass();
		VAR_TYPES lType = variableTypes.get(lClass.cast(node.getL()));
		VAR_TYPES rType = variableTypes.get(rClass.cast(node.getR()));

		if (node.getL() instanceof AIdentifierArithmetics) {
			lType = findVariableType(
					((AIdentifier) ((AIdentifierArithmetics) node.getL()).getIdentifier()).getId().getText());
		}

		if (node.getR() instanceof AIdentifierArithmetics) {
			rType = findVariableType(
					((AIdentifier) ((AIdentifierArithmetics) node.getR()).getIdentifier()).getId().getText());
		}

		if (rType == VAR_TYPES.NONE || lType == VAR_TYPES.NONE) {
			printError(node, ERROR_TYPES.NONE_OPERATION);
		} else if (lType == VAR_TYPES.UNKNOWN || rType == VAR_TYPES.UNKNOWN) {
			variableTypes.put(node, VAR_TYPES.UNKNOWN);
		} else if (isNumber(lType) && isNumber(rType)) {
			variableTypes.put(node, VAR_TYPES.DOUBLE);
		} else {
			printError(node, ERROR_TYPES.TYPE_MISSMATCH);
		}
	}

	// Helper methods

	/**
	* Prints the appropriate error message
	* @param node the node causing the error
	* @param type the error's type
	*/
	public static void printError(Node node, ERROR_TYPES type) {
		String message = "Error";
		AIdentifier id;
		switch (type) {
			case UNDECLARED_VARIABLE:
				id = (AIdentifier) node;
				message += "[" + id.getId().getLine() + "]: Undeclared variable named \'" + id.getId().getText() + "\'.";
				break;

			case UNDEFINED_FUNCTION:
				id = (AIdentifier) node;
				message += "[" + id.getId().getLine() + "]: Undefined function named \'" + id.getId().getText() + "\'.";
				break;

			case UNORDERED_PARAMS:
				AFunction function = (AFunction) node;
				id = (AIdentifier) function.getIdentifier();
				message += "[" + id.getId().getLine() + "]: Parameter after default parameter in function \'" + id.getId().getText() + "\'.";
				break;

			case WRONG_PARAMS:
				id = (AIdentifier) ((AFunctionCall) node).getIdentifier();
				message += "[" + id.getId().getLine() + "]: Wrong parameters given for function named \'" + id.getId().getText() + "\'.";
				break;

			case TYPE_MISSMATCH:
				message += ": Variable type missmatch at \'" + node.toString().stripTrailing() + "\'.";
				break;

			case ADD_TYPE_MISSMATCH:
				message += ": Variable type missmatch in addition.";
				break;

			case MINUS_TYPE_MISSMATCH:
				message += ": Variable type missmatch in substraction.";
				break;

			case NONE_OPERATION:
				message += ": Illegal operation with None.";
				break;

			case IDENTICAL_FUNCTIONS:
				break;

			default:
				message += ": Unknown error.";
		}

		if (!(node instanceof AGoal)) {
			System.err.println(message);
			System.exit(-1);
		}
	}

	/**
	* Prints the appropriate error messages for more advanced cases
	* @param node the node causing the error
	* @param type the error's type
	* @param name the function's name
	*/
	public static void printError(Node node, ERROR_TYPES type, String name) {
		String message = "Error";
		switch (type) {
			case IDENTICAL_FUNCTIONS:
				message += ": Function \'" + name + "\' already defined with same parameter number.";
				break;

			default:
				message += ": Unknown error.";
		}

		System.err.println(message);
		System.exit(-1);
	}

	/**
	* Given a token's name find the matching variable's type (for AIdentifier)
	* @param token the given token's mame
	* @return the variable's type
	*/
	private VAR_TYPES findVariableType(String token) {
		for (Node node : variableTypes.keySet()) {
			if (node instanceof AIdentifier) {
				if (token.trim().equals(((AIdentifier) node).getId().getText().trim())) {
					return variableTypes.get(node);
				}
			}
		}

		return null;
	}

	/**
	* Finds a number's subtype
	* @param number the given number
	* @return the number's type (INTEGER or DOUBLE)
	*/
	public static VAR_TYPES getNumberSubtype(PNumber number) {
		if (number instanceof AIntNumber) {
			return VAR_TYPES.INTEGER;
		} else {
			return VAR_TYPES.DOUBLE;
		}
	}

	/**
	* Checks if a given variable type is that of a number
	* @param type the given variable type
	* @return true or false
	*/
	public static boolean isNumber(VAR_TYPES type) {
		return type == VAR_TYPES.INTEGER || type == VAR_TYPES.DOUBLE;
	}

	// Getters
	public Hashtable<String, Node> getVariables() {
		return variables;
	}

	public Hashtable<String, Node> getFunctions() {
		return functions;
	}

	public Hashtable<Node, VAR_TYPES> getVariableTypes() {
		return variableTypes;
	}

	public List<Function> getFunctionList() {
		return functionList;
	}
}
