import java.io.*;
import minipython.lexer.Lexer;
import minipython.parser.Parser;
import minipython.node.*;
import java.util.*;

public class Semantics {
    public static void main(String[] args) {
        try {
            Parser parser =
                new Parser(
                new Lexer(
                new PushbackReader(
                new FileReader(args[0].toString()), 1024)));

            // Hash tables for storing declared variables and functions
            Hashtable<String, Node> variables =  new Hashtable<>();
            Hashtable<String, Node> functions =  new Hashtable<>();
            Hashtable<Node, FirstVisitor.VAR_TYPES> variableTypes = new Hashtable<>();

            Start ast = parser.parse();

            // Apply the visitors
            FirstVisitor firstVisitor = new FirstVisitor(variables, functions, variableTypes);
            ast.apply(firstVisitor);
            SecondVisitor secondVisitor = new SecondVisitor(firstVisitor.getVariables(), firstVisitor.getFunctions(), firstVisitor.getVariableTypes());
            ast.apply(secondVisitor);

        } catch (Exception e) {
            System.err.println(e);
        }
    }
}
