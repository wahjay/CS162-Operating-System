import syntaxtree.Node;
import visitor.GJDepthFirst;
import java.io.*;
import java.util.*;

public class J2V {
  public static void main(String [] args) throws ParseException {
    InputStream in = System.in;

    Node root = new MiniJavaParser(in).Goal();

    //build the method table first
    GJDepthFirst methodTable = new VTableVisitor();
    root.accept(methodTable, in);
    MethodTable instance = MethodTable.getInstance();
    //instance.print();
    //System.out.println("\n");
    //instance.printTypes();

    //do the translation
    GJDepthFirst vVisitor = new VaporVisitor();
    Object C = root.accept(vVisitor, in);
    System.out.println((String) C);
  }
}
