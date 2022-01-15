import java.util.*;

import minipython.analysis.DepthFirstAdapter;
import minipython.node.*;


public class SecondVisitor extends DepthFirstAdapter {
    private final Hashtable<String, Node> variables;
    private final Hashtable<String, Node> functions;
    private final Hashtable<Node, FirstVisitor.VAR_TYPES> variableTypes;

    public SecondVisitor(Hashtable<String, Node> variables, Hashtable<String, Node> functions, 
            Hashtable<Node, FirstVisitor.VAR_TYPES> variableTypes) {
		this.variables = variables;
		this.functions = functions;
        this.variableTypes = variableTypes;
	}

    @Override
    @SuppressWarnings("unchecked")
    public void outAFunctionCall(AFunctionCall node) {
        // Get the function's name
        AIdentifier identifier = (AIdentifier) node.getIdentifier();
        String name = identifier.getId().getText();

        // Ensure that the function has been defined
        if (!functions.containsKey(name)) {
            FirstVisitor.printError(identifier, FirstVisitor.ERROR_TYPES.UNDEFINED_FUNCTION);
            return;
        }

        // Retrieve the arguments from the function's definition
        AFunction function = (AFunction) (functions.get(name).parent());
        LinkedList<AArgument> arguments = function.getArgument();
        List<String> expectedArguments = new ArrayList<>();

        // Cast the first argument into an AIdentifier and then retrieve the name of the token
        int defaultIndex = 0;
        if (arguments.size() > 0) {
            expectedArguments.add(((AIdentifier) arguments.get(0).getIdentifier()).getId().getText());
            if (arguments.get(0).getAssignValue().size() > 0) {
                defaultIndex = 1;
            }

            for (AMoreAssignments argument : ((LinkedList<AMoreAssignments>) arguments.get(0).getMoreAssignments())) {
                expectedArguments.add(((AIdentifier) argument.getIdentifier()).getId().getText());

                // Check for default parameters
                LinkedList<AAssignValue> value = argument.getAssignValue();
                if (value.size() > 0 && defaultIndex == 0) {
                    defaultIndex = expectedArguments.size();

                } else if (value.size() == 0 && defaultIndex > 0) {
                    FirstVisitor.printError(function, FirstVisitor.ERROR_TYPES.UNORDERED_PARAMS);
                }
            }
        }

        // Get the arguments from the call statement
        LinkedList<AArgList> argumentsCall = node.getArgList();
        List<PArithmetics> givenArguments = new ArrayList<>();

        // Cast and then get the arguments' values from the call statement
        if (argumentsCall.size() > 0) {
            givenArguments.add(argumentsCall.get(0).getArithmetics());
            for (ACommaExpr argument : (LinkedList<ACommaExpr>) argumentsCall.get(0).getCommaExpr()) {
                givenArguments.add(argument.getArithmetics());
            }
        }

        // Compare the arguments from the definition with the ones from the call
        if (givenArguments.size() < defaultIndex - 1 || givenArguments.size() > expectedArguments.size()) {
            FirstVisitor.printError(node, FirstVisitor.ERROR_TYPES.WRONG_PARAMS);
        }

        // ********************************* \\
        // Check that the correct number of parameters has been given
        FirstVisitor.VAR_TYPES type;
        if (argumentsCall.size() > 0) {
            LinkedList<AMoreAssignments> params = arguments.get(0).getMoreAssignments();
            LinkedList<ACommaExpr> args = argumentsCall.get(0).getCommaExpr();
            for (int i = 0; i < expectedArguments.size(); i++) {
                if (i < givenArguments.size()) {
                    // Get the argument's type from the function's call
                    if (i == 0) {
                        type = variableTypes.get(givenArguments.get(i));
                        variableTypes.put(arguments.get(0).getIdentifier(), type);
                    } else {
                        type = variableTypes.get(args.get(i - 1).getArithmetics());
                        variableTypes.put(params.get(i - 1).getIdentifier(), type);
                    }

                } else {  // Default values included
                    // Use the default value to set the variable's type
                    AMoreAssignments value = params.get(i - 1);
                    type = variableTypes.get(((LinkedList<AAssignValue>) value.getAssignValue()).get(0).getArithmetics());
                    variableTypes.put(params.get(i - 1).getIdentifier(), type);
                }
            }
        } else if (arguments.size() > 0) {  // Only default parameters present
            LinkedList<AAssignValue> firstValue = arguments.get(0).getAssignValue();
            if (firstValue.size() > 0) {
                type = variableTypes.get(firstValue.get(0).getArithmetics());
                variableTypes.put(arguments.get(0).getIdentifier(), type);
            }

            LinkedList<AAssignValue> value;
            for (AMoreAssignments param : (LinkedList<AMoreAssignments>) arguments.get(0).getMoreAssignments()) {
                value = param.getAssignValue();
                type = variableTypes.get(value.get(0).getArithmetics());
                variableTypes.put(param.getIdentifier(), type);
            }
        }

        // ********************************* \\
        if (function.getStatement() != null) {
            function.getStatement().apply(this);
        }
    }

    @Override
    public void outANumberArithmetics(ANumberArithmetics node) {
        variableTypes.put(node, FirstVisitor.getNumberSubtype(node.getNumber()));
    }

    @Override
    public void outAStrlitArithmetics(AStrlitArithmetics node) {
        variableTypes.put(node, FirstVisitor.VAR_TYPES.STRING);
    }

    @Override
    public void outANoneArithmetics(ANoneArithmetics node) {
        variableTypes.put(node, FirstVisitor.VAR_TYPES.NONE);
    }

    @Override
    public void outALenArithmetics(ALenArithmetics node) {
        variableTypes.put(node, FirstVisitor.VAR_TYPES.INTEGER);
    }

    @Override
    public void outAMaxminArithmetics(AMaxminArithmetics node) {
        variableTypes.put(node, FirstVisitor.VAR_TYPES.DOUBLE);
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
        FirstVisitor.VAR_TYPES lType = variableTypes.get(lClass.cast(node.getL()));
        FirstVisitor.VAR_TYPES rType = variableTypes.get(rClass.cast(node.getR()));

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
        if (rType == FirstVisitor.VAR_TYPES.NONE || lType == FirstVisitor.VAR_TYPES.NONE) {
            FirstVisitor.printError(node, FirstVisitor.ERROR_TYPES.NONE_OPERATION);
        } else if (lType == FirstVisitor.VAR_TYPES.UNKNOWN || rType == FirstVisitor.VAR_TYPES.UNKNOWN) {
            variableTypes.put(node, FirstVisitor.VAR_TYPES.UNKNOWN);
        } else if (lType == FirstVisitor.VAR_TYPES.INTEGER && rType == FirstVisitor.VAR_TYPES.INTEGER) {
            variableTypes.put(node, FirstVisitor.VAR_TYPES.INTEGER);
        } else if (FirstVisitor.isNumber(lType) && FirstVisitor.isNumber(rType)) {
            variableTypes.put(node, FirstVisitor.VAR_TYPES.DOUBLE);
        } else {
            FirstVisitor.printError(node.getR(), FirstVisitor.ERROR_TYPES.TYPE_MISSMATCH);
        }
    }

    @Override
    public void outAPlusArithmetics(APlusArithmetics node) {
        // Find the class type of left and right children
        Class<?> lClass = node.getL().getClass();
        Class<?> rClass = node.getR().getClass();
        FirstVisitor.VAR_TYPES lType = variableTypes.get(lClass.cast(node.getL()));
        FirstVisitor.VAR_TYPES rType = variableTypes.get(rClass.cast(node.getR()));

        if (node.getL() instanceof AIdentifierArithmetics) {
            lType = findVariableType(
                    ((AIdentifier) ((AIdentifierArithmetics) node.getL()).getIdentifier()).getId().getText());
        }

        if (node.getR() instanceof AIdentifierArithmetics) {
            rType = findVariableType(
                    ((AIdentifier) ((AIdentifierArithmetics) node.getR()).getIdentifier()).getId().getText());
        }

        // The childrens' types must match
        if (rType == FirstVisitor.VAR_TYPES.NONE || lType == FirstVisitor.VAR_TYPES.NONE) {
            FirstVisitor.printError(node, FirstVisitor.ERROR_TYPES.NONE_OPERATION);
        } else if (lType == FirstVisitor.VAR_TYPES.UNKNOWN || rType == FirstVisitor.VAR_TYPES.UNKNOWN) {
            variableTypes.put(node, FirstVisitor.VAR_TYPES.UNKNOWN);
        } else if (lType == rType) {
            variableTypes.put(node, lType);
        } else if (FirstVisitor.isNumber(lType) && FirstVisitor.isNumber(rType)) {
            variableTypes.put(node, FirstVisitor.VAR_TYPES.DOUBLE);
        } else {
            FirstVisitor.printError(node, FirstVisitor.ERROR_TYPES.ADD_TYPE_MISSMATCH);
        }
    }

    @Override
    public void outAMinusArithmetics(AMinusArithmetics node) {
        // Find the class type of left and right children
        Class<?> lClass = node.getL().getClass();
        Class<?> rClass = node.getR().getClass();
        FirstVisitor.VAR_TYPES lType = variableTypes.get(lClass.cast(node.getL()));
        FirstVisitor.VAR_TYPES rType = variableTypes.get(rClass.cast(node.getR()));

        if (node.getL() instanceof AIdentifierArithmetics) {
            lType = findVariableType(
                    ((AIdentifier) ((AIdentifierArithmetics) node.getL()).getIdentifier()).getId().getText());
        }

        if (node.getR() instanceof AIdentifierArithmetics) {
            rType = findVariableType(
                    ((AIdentifier) ((AIdentifierArithmetics) node.getR()).getIdentifier()).getId().getText());
        }

        if (rType == FirstVisitor.VAR_TYPES.NONE || lType == FirstVisitor.VAR_TYPES.NONE) {
            FirstVisitor.printError(node, FirstVisitor.ERROR_TYPES.NONE_OPERATION);
        } else if (lType == FirstVisitor.VAR_TYPES.UNKNOWN || rType == FirstVisitor.VAR_TYPES.UNKNOWN) {
			variableTypes.put(node, FirstVisitor.VAR_TYPES.UNKNOWN);
        } else if (lType == FirstVisitor.VAR_TYPES.INTEGER && rType == FirstVisitor.VAR_TYPES.INTEGER) {
            variableTypes.put(node, FirstVisitor.VAR_TYPES.INTEGER);
        } else if (lType == FirstVisitor.VAR_TYPES.STRING || rType == FirstVisitor.VAR_TYPES.STRING) {
            FirstVisitor.printError(node, FirstVisitor.ERROR_TYPES.MINUS_TYPE_MISSMATCH);
        } else if (FirstVisitor.isNumber(lType) && FirstVisitor.isNumber(rType)) {
            variableTypes.put(node, FirstVisitor.VAR_TYPES.DOUBLE);
        } else {
            FirstVisitor.printError(node, FirstVisitor.ERROR_TYPES.MINUS_TYPE_MISSMATCH);
        }
    }

    @Override
    public void outAMultArithmetics(AMultArithmetics node) {
        // Same as outAExpArithmetics
        // Find the class type of left and right children
        Class<?> lClass = node.getL().getClass();
        Class<?> rClass = node.getR().getClass();
        FirstVisitor.VAR_TYPES lType = variableTypes.get(lClass.cast(node.getL()));
        FirstVisitor.VAR_TYPES rType = variableTypes.get(rClass.cast(node.getR()));

        if (node.getL() instanceof AIdentifierArithmetics) {
            lType = findVariableType(
                    ((AIdentifier) ((AIdentifierArithmetics) node.getL()).getIdentifier()).getId().getText());
        }

        if (node.getR() instanceof AIdentifierArithmetics) {
            rType = findVariableType(
                    ((AIdentifier) ((AIdentifierArithmetics) node.getR()).getIdentifier()).getId().getText());
        }

        // All children must return a number for the expression to be valid
        if (rType == FirstVisitor.VAR_TYPES.NONE || lType == FirstVisitor.VAR_TYPES.NONE) {
            FirstVisitor.printError(node, FirstVisitor.ERROR_TYPES.NONE_OPERATION);
        } else if (lType == FirstVisitor.VAR_TYPES.UNKNOWN || rType == FirstVisitor.VAR_TYPES.UNKNOWN) {
            variableTypes.put(node, FirstVisitor.VAR_TYPES.UNKNOWN);
        } else if (lType == FirstVisitor.VAR_TYPES.INTEGER && rType == FirstVisitor.VAR_TYPES.INTEGER) {
            variableTypes.put(node, FirstVisitor.VAR_TYPES.INTEGER);
        } else if (lType == FirstVisitor.VAR_TYPES.INTEGER && rType == FirstVisitor.VAR_TYPES.STRING
                || lType == FirstVisitor.VAR_TYPES.STRING && rType == FirstVisitor.VAR_TYPES.INTEGER) {
            variableTypes.put(node, FirstVisitor.VAR_TYPES.STRING);
        } else if (FirstVisitor.isNumber(lType) && FirstVisitor.isNumber(rType)) {
            variableTypes.put(node, FirstVisitor.VAR_TYPES.DOUBLE);
        } else {
            FirstVisitor.printError(node, FirstVisitor.ERROR_TYPES.TYPE_MISSMATCH);
        }
    }

    @Override
    public void outADivArithmetics(ADivArithmetics node) {
        // Same as outAExpArithmetics
        // Find the class type of left and right children
        Class<?> lClass = node.getL().getClass();
        Class<?> rClass = node.getR().getClass();
        FirstVisitor.VAR_TYPES lType = variableTypes.get(lClass.cast(node.getL()));
        FirstVisitor.VAR_TYPES rType = variableTypes.get(rClass.cast(node.getR()));

        if (node.getL() instanceof AIdentifierArithmetics) {
            lType = findVariableType(
                    ((AIdentifier) ((AIdentifierArithmetics) node.getL()).getIdentifier()).getId().getText());
        }

        if (node.getR() instanceof AIdentifierArithmetics) {
            rType = findVariableType(
                    ((AIdentifier) ((AIdentifierArithmetics) node.getR()).getIdentifier()).getId().getText());
        }

        if (rType == FirstVisitor.VAR_TYPES.NONE || lType == FirstVisitor.VAR_TYPES.NONE) {
            FirstVisitor.printError(node, FirstVisitor.ERROR_TYPES.NONE_OPERATION);
        } else if (lType == FirstVisitor.VAR_TYPES.UNKNOWN || rType == FirstVisitor.VAR_TYPES.UNKNOWN) {
            variableTypes.put(node, FirstVisitor.VAR_TYPES.UNKNOWN);
        } else if (FirstVisitor.isNumber(lType) && FirstVisitor.isNumber(rType)) {
            variableTypes.put(node, FirstVisitor.VAR_TYPES.DOUBLE);
        } else {
            FirstVisitor.printError(node, FirstVisitor.ERROR_TYPES.TYPE_MISSMATCH);
        }
    }

    @Override
    public void outAFunctionArithmetics(AFunctionArithmetics node) {
        // Find the function's type using it's identifier
        String id = ((AIdentifier) ((AFunctionCall) node.getFunctionCall()).getIdentifier()).getId().getText();
        
        // Find the function's definition
        if (findVariableType(id) != FirstVisitor.VAR_TYPES.NONE) {
            AFunction function = (AFunction) (functions.get(id).parent());
            if (function.getStatement() instanceof AReturnStatement) {
                function.getStatement().apply(new FirstVisitor(variables, functions, variableTypes));

                // The function call has the same type as the function's return statement
                PArithmetics arithmetics = ((AReturnStatement) function.getStatement()).getArithmetics();
                // variableTypes.put(function.getIdentifier(), variableTypes.get(arithmetics));  // -0
                variableTypes.put(node, variableTypes.get(arithmetics));
            }
        }
    }

    // Helper methods
    private FirstVisitor.VAR_TYPES findVariableType(String token) {
        for (Node node : variableTypes.keySet()) {
            if (node instanceof AIdentifier) {
                if (token.trim().equals(((AIdentifier) node).getId().getText().trim())) {
                    return variableTypes.get(node);
                }
            }
        }

        return null;
    }

    // Getters
    public Hashtable<String, Node> getVariables() {
        return variables;
    }

    public Hashtable<String, Node> getFunctions() {
        return functions;
    }

    public Hashtable<Node, FirstVisitor.VAR_TYPES> getVariableTypes() {
        return variableTypes;
    }
}
