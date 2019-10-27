import syntaxtree.Node;
import visitor.GJDepthFirst;
import java.io.*;
import java.util.*;

public class Typecheck {

    public static void main (String [] args) throws ParseException  {

        //get input from stdin
        InputStream in = System.in;

        //build an AST in memory for the input from stdin
        Node root = new MiniJavaParser(in).Goal();

        //Setting up the Symbol tables first
        GJDepthFirst visTable = new TableVisitor();
        root.accept(visTable,in);

        //print out the symbol table
        //ClassList instance = ClassList.getInstance();
        //instance.print();

        //now, Setting up another visitor for type check
        GJDepthFirst vis = new MyVisitor();
        root.accept(vis, in);


        System.out.println("Program type checked successfully");
    }

}
