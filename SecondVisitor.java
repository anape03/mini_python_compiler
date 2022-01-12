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
    public void inAFunctionCall(AFunctionCall node) {
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
                        System.out.println("in normal zero");  // -0
                        type = variableTypes.get(givenArguments.get(i));
                        variableTypes.put(arguments.get(0).getIdentifier(), type);
                    } else {
                        System.out.println("in normal no zero");  // -0
                        type = variableTypes.get(args.get(i - 1).getArithmetics());
                        variableTypes.put(params.get(i - 1).getIdentifier(), type);
                    }

                } else {  // Default values included
                    System.out.println("in default no zero");  // -0
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
        // Find the function's return type
        
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
