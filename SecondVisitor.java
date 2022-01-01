import java.util.*;

import minipython.analysis.DepthFirstAdapter;
import minipython.node.*;


public class SecondVisitor extends DepthFirstAdapter {
    private final Hashtable<String, Node> variables;
    private final Hashtable<String, Node> functions;

    public SecondVisitor(Hashtable<String, Node> variables, Hashtable<String, Node> functions) {
		this.variables = variables;
		this.functions = functions;
	}

    @Override
    public void inAFunctionCall(AFunctionCall node) {
        // Get the function's name
        AIdentifier identifier = (AIdentifier) node.getIdentifier();
        String name = identifier.getId().getText();

        // Ensure that the function has been defined
        if (!functions.containsKey(name)) {
            FirstVisitor.printError(identifier, FirstVisitor.ERROR_TYPES.UNDEFINED_FUNCTION);
        }

        // Retrieve the arguments from the function's definition
        AFunction function = (AFunction) (functions.get(name).parent());
        LinkedList<AArgument> arguments = function.getArgument();
        List<String> expectedArguments = new ArrayList<>();

        // Cast the first argument into an AIdentifier and then retrieve the name of the token
        expectedArguments.add(((AIdentifier) arguments.get(0).getIdentifier()).getId().getText());
        for (AMoreAssignments argument : ((LinkedList<AMoreAssignments>) arguments.get(0).getMoreAssignments())) {
            expectedArguments.add(((AIdentifier) argument.getIdentifier()).getId().getText());
        }

        // Get the arguments from the call statement
        LinkedList<AArgList> argumentsCall = node.getArgList();
        List<PArithmetics> givenArguments = new ArrayList<>();

        // Cast and then get the arguments' values from the call statement
        givenArguments.add(argumentsCall.get(0).getArithmetics());
        for (ACommaExpr argument : (LinkedList<ACommaExpr>) argumentsCall.get(0).getCommaExpr()) {
            givenArguments.add(argument.getArithmetics());
        }

        // TODO: Compare the arguments from the definition with the ones from the call

    }

    // Getters
    public Hashtable<String, Node> getVariables() {
        return variables;
    }

    public Hashtable<String, Node> getFunctions() {
        return functions;
    }
}
