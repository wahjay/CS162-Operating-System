import syntaxtree.*;
import visitor.GJDepthFirst;
import java.util.HashMap;
import java.io.IOException;
import java.util.*;

public class TableVisitor extends GJDepthFirst {

  @Override
    public Object visit(NodeList n, Object argu) {
        return super.visit(n, argu);
    }

    @Override
    public Object visit(NodeListOptional n, Object argu) {
        return super.visit(n, argu);
    }

    @Override
    public Object visit(NodeOptional n, Object argu) {
        return super.visit(n, argu);
    }

    @Override
    public Object visit(NodeSequence n, Object argu) {
        return super.visit(n, argu);
    }

    @Override
    public Object visit(NodeToken n, Object argu) {
        return n.tokenImage;
        //return super.visit(n, argu);
    }

    @Override
    public Object visit(Goal n, Object argu) {
        return super.visit(n, argu);
    }

    @Override
    public Object visit(MainClass n, Object argu) {
        ClassList instance = ClassList.getInstance();
        ClassTable C_Table = new ClassTable();    //store main class
        MethodTable M_Table = new MethodTable();  // store main function
        String  mainName = (String) n.f6.accept(this,argu); // main name
        String className = (String) n.f1.accept(this,argu); //class name

        //Class identifier, for 'this' purpose
        C_Table.set_id(n.f1);

        //Store String[] args in main as ArrayType in table
        //since there is no String[] type in miniJAVA
        //this is just temporary, 'args' may be for assignment later
        String pn = (String) n.f11.accept(this,argu);
        M_Table.p_insert(pn, new ArrayType());

        //insert locals variables from main function
        Vector<Node> vec =  n.f14.nodes;
        Iterator it = vec.iterator();
        while(it.hasNext()) {
          VarDeclaration vd = (VarDeclaration) it.next();
          String id = (String) vd.f1.accept(this,argu);
          Node type = vd.f0.f0.choice;
          if(M_Table.locals.containsKey(id) || M_Table.parameters.containsKey(id)) {
            System.out.println("Type error");
            System.exit(0);
          }
          M_Table.l_insert(id, type);
        }

        C_Table.insert_method(mainName, M_Table);
        instance.insert(className, C_Table);

        //return super.visit(n, argu);
        return n.f15.accept(this,argu);
    }

    @Override
    public Object visit(TypeDeclaration n, Object argu) {
        //System.out.println(n.f0.choice.accept(this,argu));
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public Object visit(ClassDeclaration n, Object argu) {
        ClassList instance = ClassList.getInstance();
        ClassTable C_Table = new ClassTable();
        MethodTable M_Table = new MethodTable();
        String ClassName = (String) n.f1.accept(this,argu);

        //Class identifier, for 'this' purpose
        C_Table.set_id(n.f1);

        //Class fields
        Vector<Node> vec = n.f3.nodes;
        Iterator it = vec.iterator();

        //System.out.println("class name: " + n.f1.accept(this,argu));
        while(it.hasNext()) {
          VarDeclaration paras = (VarDeclaration)it.next();
          String id = (String) paras.f1.accept(this,argu);
          Node type = paras.f0.f0.choice;

          //check if inserted before
          if(C_Table.fields.containsKey(id)) {
            System.out.println("Type error");
            System.exit(0);
          }

          C_Table.insert_field(id, type);
        }

        //Class methods
        vec = n.f4.nodes;
        it = vec.iterator();
        while(it.hasNext()) {
          MethodDeclaration md = (MethodDeclaration)it.next();
          String id = (String) md.f2.accept(this,argu);
          MethodTable mt = (MethodTable)md.accept(this,argu);

          //check if method name has been used already
          if(C_Table.methodTables.containsKey(id)){
            System.out.println("Type error");
            System.exit(0);
          }

          C_Table.insert_method(id, mt);
        }

        //After finished setting up the class table
        //insert the class name and its class table to ClassList
        //but lets check if classname has already been used
        if(instance.C_List.containsKey(ClassName)) {
          System.out.println("Type error");
          System.exit(0);
        }

        instance.insert(ClassName, C_Table);
        return super.visit(n,argu);
    }

    @Override
    public Object visit(ClassExtendsDeclaration n, Object argu) {
        ClassList instance = ClassList.getInstance();
        ClassTable C_Table = new ClassTable();
        MethodTable M_Table = new MethodTable();
        String ClassName = (String) n.f1.accept(this,argu);

        //Class identifier, for 'this' purpose
        C_Table.set_id(n.f1);

        //Super Class identifier, for 'extends' purpose
        C_Table.set_super_id((String)n.f3.accept(this,argu));

        //Class fields
        Vector<Node> vec = n.f5.nodes;
        Iterator it = vec.iterator();

        //System.out.println("class name: " + n.f1.accept(this,argu));
        while(it.hasNext()) {
          VarDeclaration paras = (VarDeclaration)it.next();
          String id = (String) paras.f1.accept(this,argu);
          Node type = paras.f0.f0.choice;

          //check if inserted before
          if(C_Table.fields.containsKey(id)) {
            System.out.println("Type error");
            System.exit(0);
          }

          C_Table.insert_field(id, type);
        }

        //Class methods
        vec = n.f6.nodes;
        it = vec.iterator();
        while(it.hasNext()) {
          MethodDeclaration md = (MethodDeclaration)it.next();
          String id = (String) md.f2.accept(this,argu);
          MethodTable mt = (MethodTable)md.accept(this,argu);

          //check if method name has been used already
          if(C_Table.methodTables.containsKey(id)){
            System.out.println("Type error");
            System.exit(0);
          }

          C_Table.insert_method(id, mt);
        }

        //After finished setting up the class table
        //insert the class name and its class table to ClassList
        if(instance.C_List.containsKey(ClassName)) {
          System.out.println("Type error");
          System.exit(0);
        }

        instance.insert(ClassName, C_Table);
        return super.visit(n,argu);
    }

    @Override
    public Object visit(VarDeclaration n, Object argu) {
        return super.visit(n,argu);
    }

    @Override
    public Object visit(MethodDeclaration n, Object argu) {
        MethodTable M_Table = new MethodTable();

        //insert treturn type
        Node ret_type = n.f1.f0.choice;
        if (!(ret_type instanceof BooleanType || ret_type instanceof ArrayType
            || ret_type instanceof IntegerType || ret_type instanceof Identifier) ) {
              System.out.println("Type error");
              System.exit(0);
        }
        M_Table.set_ret(ret_type);

        //insert parameters list
        if(n.f4.node != null) {
          LinkedHashMap<String, Node> paras_list = (LinkedHashMap<String, Node>)n.f4.node.accept(this,argu);
          M_Table.parameters.putAll(paras_list);
        }

        //insert locals
        Vector<Node> vec = n.f7.nodes;
        Iterator it = vec.iterator();

        while(it.hasNext()) {
          VarDeclaration locals = (VarDeclaration)it.next();
          String id = (String) locals.f1.accept(this,argu);
          Node type = locals.f0.f0.choice;

          //locals name and parameters name should all be distinct
          if(M_Table.locals.containsKey(id) || M_Table.parameters.containsKey(id)) {
            System.out.println("Type error");
            System.exit(0);
          }

          M_Table.l_insert(id, type);
        }

        return M_Table;
    }

    @Override
    public Object visit(FormalParameterList n, Object argu) {
        LinkedHashMap<String, Node> lhm = new LinkedHashMap<String, Node>();
        String pn  = (String) n.f0.f1.accept(this,argu);
        Node pt = n.f0.f0.f0.choice;
        lhm.put(pn,pt);

        Vector<Node> vec = n.f1.nodes;
        Iterator it = vec.iterator();
        while (it.hasNext()){
          FormalParameterRest para_rest = (FormalParameterRest)it.next();
          LinkedHashMap<String, Node> parameter = (LinkedHashMap<String, Node>)para_rest.f1.accept(this,argu);

          //all parameter names must be distinct
          for(String id: parameter.keySet()) {
            if(lhm.containsKey(id)){
              System.out.println("Type error");
              System.exit(0);
            }
          }

          lhm.putAll(parameter);
        }

        return lhm;
    }

    @Override
    public Object visit(FormalParameter n, Object argu) {
        LinkedHashMap<String, Node> para = new LinkedHashMap<>();
        String pn = (String) n.f1.accept(this,argu);
        Node pt = (Node) n.f0.f0.choice;
        para.put(pn, pt);
        return para;
    }

    @Override
    public Object visit(FormalParameterRest n, Object argu) {
        return n.f1.accept(this,argu);
    }

    /**
     * f0 -> ArrayType()
     *       | BooleanType()
     *       | IntegerType()
     *       | Identifier()
     */
    @Override
    public Object visit(Type n, Object argu) {
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public Object visit(ArrayType n, Object argu) {
        return super.visit(n, argu);
    }

    @Override
    public Object visit(BooleanType n, Object argu) {
        return n.f0.accept(this, argu);
    }

    @Override
    public Object visit(IntegerType n, Object argu) {
        return n.f0.accept(this, argu);
        //return super.visit(n, argu);
    }

    @Override
    public Object visit(Statement n, Object argu) {
        //System.out.println(n.f0.choice);
        return n.f0.choice.accept(this,argu);
        //return super.visit(n, argu);
    }

    @Override
    public Object visit(Block n, Object argu) {
        return super.visit(n, argu);
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    @Override
    public Object visit(AssignmentStatement n, Object argu) {
        return super.visit(n,argu);
    }

    @Override
    public Object visit(ArrayAssignmentStatement n, Object argu) {
        return super.visit(n, argu);
    }

    @Override
    public Object visit(IfStatement n, Object argu) {
        return super.visit(n, argu);
    }

    @Override
    public Object visit(WhileStatement n, Object argu) {
        return super.visit(n, argu);
    }

    @Override
    public Object visit(PrintStatement n, Object argu) {
        return n.f2.f0.choice.accept(this,argu);
        //return super.visit(n, argu);
    }

    @Override
    public Object visit(Expression n, Object argu) {
        return super.visit(n,argu);
        //return n.f0.choice.accept(this, argu);
    }

    @Override
    public Object visit(AndExpression n, Object argu) {
        return super.visit(n, argu);
    }

    @Override
    public Object visit(CompareExpression n, Object argu) {
        return super.visit(n, argu);
    }

    @Override
    public Object visit(PlusExpression n, Object argu) {
        return super.visit(n, argu);
    }

    @Override
    public Object visit(MinusExpression n, Object argu) {
        return super.visit(n,argu);
    }

    @Override
    public Object visit(TimesExpression n, Object argu) {
        return super.visit(n, argu);
    }

    @Override
    public Object visit(ArrayLookup n, Object argu) {
        return super.visit(n, argu);
    }

    @Override
    public Object visit(ArrayLength n, Object argu) {
        return super.visit(n, argu);
    }

    @Override
    public Object visit(MessageSend n, Object argu) {
        ExpressionList EL = (ExpressionList)n.f4.node;

        if (EL == null)
          return EL;

        Expression E = EL.f0;
        return E.f0.choice.accept(this,argu);
        //return super.visit(n, argu);
    }

    @Override
    public Object visit(ExpressionList n, Object argu) {
        return super.visit(n, argu);
    }

    @Override
    public Object visit(ExpressionRest n, Object argu) {
        return super.visit(n, argu);
    }

    @Override
    public Object visit(PrimaryExpression n, Object argu) {
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public Object visit(IntegerLiteral n, Object argu) {
        return new IntegerType();
        //return super.visit(n, argu);
    }

    @Override
    public Object visit(TrueLiteral n, Object argu) {
        return new BooleanType();
        //return super.visit(n, argu);
    }

    @Override
    public Object visit(FalseLiteral n, Object argu) {
        return new BooleanType();
        //return super.visit(n, argu);
    }

    @Override
    public Object visit(Identifier n, Object argu) {
        //System.out.println(n.f0.tokenImage instanceof String);
        return n.f0.tokenImage;
        //return super.visit(n, argu);
    }

    @Override
    public Object visit(ThisExpression n, Object argu) {
        return new ThisExpression();
        //return super.visit(n, argu);
    }

    @Override
    public Object visit(ArrayAllocationExpression n, Object argu) {
        //return n.f1.accept(this,argu);
        return super.visit(n, argu);
    }

    @Override
    public Object visit(AllocationExpression n, Object argu) {
        //System.out.println(n.f1.f0.accept(this,argu));
        return n.f1.f0.accept(this,argu);
    }

    @Override
    public Object visit(NotExpression n, Object argu) {
        return super.visit(n, argu);
    }

    @Override
    public Object visit(BracketExpression n, Object argu) {
        //System.out.println(n.f1.f0.choice.getClass());
        return n.f1.f0.choice.accept(this,argu);
    }
}
