import syntaxtree.*;
import visitor.GJDepthFirst;
import java.util.HashMap;
import java.io.IOException;
import java.util.*;

public class MyVisitor extends GJDepthFirst {

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
    }

    @Override
    public Object visit(Goal n, Object argu) {
        return super.visit(n, argu);
    }

    @Override
    public Object visit(MainClass n, Object argu) {
        ClassList instance = ClassList.getInstance();
        String className = (String) n.f1.accept(this,argu);
        ClassTable CT = instance.get(className);
        String mainName = (String) n.f6.accept(this,argu);
        MethodTable MT = CT.get_method(mainName);
        instance.set_cur(MT); //set 'this' class as current environment

        return super.visit(n, argu);
    }

    @Override
    public Object visit(TypeDeclaration n, Object argu) {
        //System.out.println(n.f0.choice.accept(this,argu));
        return n.f0.choice.accept(this, argu);
        //return super.visit(n, argu);
    }

    @Override
    public Object visit(ClassDeclaration n, Object argu) {
        ClassList instance = ClassList.getInstance();
        String className = (String) n.f1.accept(this,argu);
        ClassTable CT = instance.get(className);
        instance.set_cur(CT); //set 'this' class as current environment

        return super.visit(n, argu);
    }

    @Override
    public Object visit(ClassExtendsDeclaration n, Object argu) {
        return super.visit(n, argu);
    }

    @Override
    public Object visit(VarDeclaration n, Object argu) {
        //return the type node
        return n.f0.f0.choice;
    }

    @Override
    public Object visit(MethodDeclaration n, Object argu) {
        ClassList instance = ClassList.getInstance();
        String MethodName = (String) n.f2.accept(this,argu);

        //just enter into a new scope
        //need to update cur environment
        Object temp = instance.get_cur_class();
        if (temp != null) {
          ClassTable Ctemp = (ClassTable) temp;
          MethodTable Mtemp = Ctemp.get_method(MethodName);

          //Mtemp == null means we do enter into a new scope
          //and its from a class env to another class env
          //the folliwng line of code will only update env
          //in the same class env, we skip this, we are fine.
          if(Mtemp != null)
            instance.set_cur(Mtemp);
        }

        //just in case the pre env is null,
        //the following lines of code are needed
        //get class table from current environment
        Object Table = instance.get_cur();
        ClassTable CT = null;
        MethodTable MT = null;

        // get method table
        if (Table instanceof ClassTable) {
          CT = (ClassTable) instance.get_cur();
          MT =  CT.get_method(MethodName);
        }
        else if (Table instanceof MethodTable)
          MT =  (MethodTable) Table;

        //update the current environment
        instance.set_cur(MT);

        //type check return type
        Object obj = n.f10.f0.choice.accept(this,argu);
        Node ret_type = MT.get_ret();
        Node ret = null;

        //Note!!!: the return statement could be a BooleanLiteral like 'true' or 'false'
        //check if the return statement is an ID or a IntegerLiteral
        if(obj instanceof String)
          ret = MT.l_get((String) obj);
        else
          ret = (Node) obj;

        if(!ret_type.getClass().equals(ret.getClass())) {
          System.out.println("Type error");
          System.exit(0);
        }

        return super.visit(n,argu);
    }

    @Override
    public Object visit(FormalParameterList n, Object argu) {
        return super.visit(n, argu);
    }

    @Override
    public Object visit(FormalParameter n, Object argu) {
        return super.visit(n, argu);
    }

    @Override
    public Object visit(FormalParameterRest n, Object argu) {
        return super.visit(n, argu);
    }

    /**
     * f0 -> ArrayType()
     *       | BooleanType()
     *       | IntegerType()
     *       | Identifier()
     */
    @Override
    public Object visit(Type n, Object argu) {
        //System.out.println(n.f0.choice.getClass());
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
    }

    @Override
    public Object visit(Statement n, Object argu) {
        //System.out.println("stmt: " + n.f0.choice);
        return n.f0.choice.accept(this,argu);
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
        ClassList instance = ClassList.getInstance();

        //get class table from current environment
        MethodTable MT = (MethodTable) instance.get_cur();

        //retrieve local type and compare
        String id = (String) n.f0.accept(this,argu);
        Node left = MT.p_get(id) == null ? MT.l_get(id) : MT.p_get(id);

        //right hand side could be IntegerLiteral/expression/ID
        //so handle it accordingly
        Object obj = n.f2.accept(this,argu);
        Node right = null;

        if (obj instanceof String) {
          String r_id = (String) obj;
          right = MT.p_get(r_id) == null ? MT.l_get(r_id) : MT.p_get(r_id);
        }
        else
          right = (Node) obj;

        //System.out.println("id: " + id);
        //System.out.println("left: " + left);
        //System.out.println("right: " + right);
        //ret_type == null means variable does not exist or is declared before
        if(left == null || right == null || !right.getClass().equals(left.getClass())) {
          System.out.println("Type error");
          System.exit(0);
        }

        return left;
        //return super.visit(n, argu);
    }

    @Override
    public Object visit(ArrayAssignmentStatement n, Object argu) {
        return super.visit(n, argu);
    }

    @Override
    public Object visit(IfStatement n, Object argu) {
        ClassList instance = ClassList.getInstance();
        MethodTable MT = (MethodTable) instance.get_cur();
        Object obj = n.f2.accept(this,argu);
        Node type = null;

        if (obj == null)
          System.out.println(n.f2.f0.choice);

        if(obj instanceof Identifier) {
          System.out.println("if fix this");
          System.exit(0);
        }

        if(obj instanceof String && (String) obj != "true" && (String) obj != "false")
          type = MT.p_get((String) obj) == null ? MT.l_get((String) obj) : MT.p_get((String) obj);
        else if (obj instanceof BooleanType)
          {}
        else if ((String) obj != "true" || (String) obj != "false") {
          System.out.println("Type error");
          System.exit(0);
        }

        //type check if statement
        n.f4.accept(this,argu);
        //type check else statement
        n.f6.accept(this,argu);

        return null;
    }

    @Override
    public Object visit(WhileStatement n, Object argu) {
        //System.out.println(n.f2.f0.choice.accept(this,argu));
        return super.visit(n, argu);
    }

    @Override
    public Object visit(PrintStatement n, Object argu) {
        return n.f2.f0.choice.accept(this,argu);
        //return super.visit(n, argu);
    }

    @Override
    public Object visit(Expression n, Object argu) {
        //System.out.println(n.f0.choice);
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public Object visit(AndExpression n, Object argu) {
        ClassList instance = ClassList.getInstance();
        MethodTable MT = (MethodTable) instance.get_cur();
        Object left_obj = n.f0.accept(this,argu);
        Object right_obj = n.f0.accept(this,argu);
        String lid = null;
        String rid = null;
        Node left = null;
        Node right = null;

        if(left_obj instanceof Identifier || right_obj instanceof Identifier) {
          System.out.println("and fix this");
          System.exit(0);
        }

        if (left_obj instanceof String) {
          lid = (String) left_obj;
          left = MT.l_get(lid) == null? MT.p_get(lid) : MT.l_get(lid);
        }
        else if (left_obj instanceof BooleanType)
          left = (Node) left_obj;

        if (right_obj instanceof String) {
          rid = (String) right_obj;
          right = MT.l_get(rid) == null? MT.p_get(rid) : MT.l_get(rid);
        }
        else if (right_obj instanceof BooleanType)
          right = (Node) right_obj;

        if(!(left instanceof BooleanType && right instanceof BooleanType)) {
          System.out.println("Type error");
          System.exit(0);
        }

        return new BooleanType();
    }

    @Override
    public Object visit(CompareExpression n, Object argu) {
        ClassList instance = ClassList.getInstance();
        MethodTable MT = (MethodTable) instance.get_cur();

        Object left_obj = n.f0.accept(this,argu);
        Object right_obj = n.f2.accept(this,argu);
        String lid = null;
        String rid = null;
        Node left = null;
        Node right = null;

        if (left_obj instanceof Identifier || right_obj instanceof Identifier) {
          System.out.println("compare fix this");
          System.exit(0);
        }

        if (left_obj instanceof String) {
          lid = (String) left_obj;
          left = MT.l_get(lid) == null? MT.p_get(lid) : MT.l_get(lid);
        }
        else if (left_obj instanceof IntegerType)
          left = (Node) left_obj;

        if (right_obj instanceof String) {
          rid = (String) right_obj;
          right = MT.l_get(rid) == null? MT.p_get(rid) : MT.l_get(rid);
        }
        else if (right_obj instanceof IntegerType)
          right = (Node) right_obj;

        if(!(left instanceof IntegerType && right instanceof IntegerType)) {
          System.out.println("Type error");
          System.exit(0);
        }

        return new BooleanType();
    }

    @Override
    public Object visit(PlusExpression n, Object argu) {
        ClassList instance = ClassList.getInstance();

        //get class table from current environment
        MethodTable MT = (MethodTable) instance.get_cur();

        //get left and right side type, then compare
        Object left = n.f0.f0.choice.accept(this,argu);
        Object right = n.f2.f0.choice.accept(this,argu);
        Node left_type = null;
        Node right_type = null;


        if (left instanceof IntegerType) {
          left_type = (Node) left;
        }
        //if not, then it should be an id
        else if (left instanceof String) {
          String id = (String) left;
          left_type = MT.l_get(id);
        }
        //or else it should be type error
        else {
          System.out.println("Type error");
          System.exit(0);
        }

        if (right instanceof IntegerType) {
          right_type = (Node) right;
        }
        //if not, then it should be an id
        else if (right instanceof String) {
          String id = (String) right;
          right_type = MT.l_get(id);
        }
        //or else it should be type error
        else {
          System.out.println("Type error");
          System.exit(0);
        }

        if(left_type == null || right_type == null || !left_type.getClass().equals(right_type.getClass())) {
          System.out.println("Type error");
          System.exit(0);
        }

        //System.out.println(n.f0.f0.choice.accept(this,argu));
        //System.out.println(n.f2.f0.choice.accept(this,argu));
        return left_type;
    }

    @Override
    public Object visit(MinusExpression n, Object argu) {
      ClassList instance = ClassList.getInstance();

      //get class table from current environment
      //System.out.println(instance.get_cur());
      MethodTable MT = (MethodTable) instance.get_cur();


      /*
      //get class table from current environment
      Object Table = instance.get_cur();
      ClassTable CT = null;
      MethodTable MT = null;
      // get method table
      if (Table instanceof ClassTable) {
        CT = (ClassTable) instance.get_cur();
        MT =  CT.get_method(MethodName);
      }
      else if (Table instanceof MethodTable)
        MT =  (MethodTable) Table;
      */



      //get left and right side type, then compare
      Object left = n.f0.f0.choice.accept(this,argu);
      Object right = n.f2.f0.choice.accept(this,argu);
      Node left_type = null;
      Node right_type = null;

      if (left instanceof IntegerType) {
        left_type = (Node) left;
      }
      //if not, then it should be an id
      else if (left instanceof String) {
        String id = (String) left;
        left_type = MT.l_get(id);
      }
      //or else it should be type error
      else {
        System.out.println("Type error");
        System.exit(0);
      }

      if (right instanceof IntegerType) {
        right_type = (Node) right;
      }
      //if not, then it should be an id
      else if (right instanceof String) {
        String id = (String) right;
        right_type = MT.l_get(id);
      }
      //or else it should be type error
      else {
        System.out.println("Type error");
        System.exit(0);
      }

      if(left_type == null || right_type == null || !left_type.getClass().equals(right_type.getClass())) {
        System.out.println("Type error");
        System.exit(0);
      }

      //System.out.println(n.f0.f0.choice.accept(this,argu));
      //System.out.println(n.f2.f0.choice.accept(this,argu));
      return left_type;
    }

    @Override
    public Object visit(TimesExpression n, Object argu) {
        ClassList instance = ClassList.getInstance();

        //get class table from current environment
        MethodTable MT = (MethodTable) instance.get_cur();

        //get left and right side type, then compare
        Object left = n.f0.f0.choice.accept(this,argu);
        Object right = n.f2.f0.choice.accept(this,argu);
        Node left_type = null;
        Node right_type = null;

        if (left instanceof IntegerType) {
          left_type = (Node) left;
        }
        //if not, then it should be an id
        else if (left instanceof String) {
          String id = (String) left;
          left_type = MT.l_get(id);
        }
        //or else it should be type error
        else {
          System.out.println("Type error");
          System.exit(0);
        }

        if (right instanceof IntegerType) {
          right_type = (Node) right;
        }
        //if not, then it should be an id
        else if (right instanceof String) {
          String id = (String) right;
          right_type = MT.l_get(id);
        }
        //or else it should be type error
        else {
          System.out.println("Type error");
          System.exit(0);
        }

        if(left_type == null || right_type == null || !left_type.getClass().equals(right_type.getClass())) {
          System.out.println("Type error");
          System.exit(0);
        }

        //System.out.println(n.f0.f0.choice.accept(this,argu));
        //System.out.println(n.f2.f0.choice.accept(this,argu));
        return left_type;
    }

    @Override
    public Object visit(ArrayLookup n, Object argu) {
        ClassList instance = ClassList.getInstance();
        MethodTable MT = (MethodTable) instance.get_cur();

        String arr_id = (String) n.f0.accept(this,argu);
        String ele_id = (String) n.f2.accept(this,argu);

        //System.out.println("array: " + n.f0.accept(this,argu));
        //System.out.println("element " + n.f2.accept(this,argu));
        //System.out.println("cur method table: " + MT);

        //look up in method parameters first
        Node arr_type = null;
        Node ele_type = null;

        //check for parameters first, if nothing found, check locals
        arr_type =  MT.p_get(arr_id) == null ? MT.l_get(arr_id) : MT.p_get(arr_id);
        ele_type =  MT.p_get(ele_id) == null ? MT.l_get(ele_id) : MT.p_get(ele_id);

        //if still nothing, ID does not exist
        if(arr_type == null || ele_type == null) {
          System.out.println("Type error");
          System.exit(0);
        }

        if(!(arr_type instanceof ArrayType && ele_type instanceof IntegerType)) {
          System.out.println("Type error");
          System.exit(0);
        }

        return ele_type;
    }

    @Override
    public Object visit(ArrayLength n, Object argu) {
        return super.visit(n, argu);
    }

    @Override
    public Object visit(MessageSend n, Object argu) {
        ClassList instance = ClassList.getInstance();
        String MethodName = null;
        String ClassName = null;
        ClassTable CT = null;

        Object caller = n.f0.accept(this,argu);
        Object callee = n.f2.accept(this,argu);

        //System.out.println("______________________________");
        //System.out.println("caller method: " + caller);
        //System.out.println("callee method: " + callee);

        if (caller instanceof Identifier) {
          Identifier caller_id = (Identifier) caller;
          ClassName = (String) caller_id.accept(this,argu);
        }
        else
          ClassName = (String) caller;

        if (callee instanceof Identifier) {
          Identifier callee_id = (Identifier) callee;
          MethodName = (String) callee_id.accept(this,argu);
        }
        else
          MethodName = (String) callee;

        //System.out.println("cur class: " + instance.get_cur_class());
        //System.out.println("cur method: " + instance.get_cur());


        //get class table from current environment
        MethodTable MT = (MethodTable)instance.get_cur();

        //that means we are in the main function :)
        //so we need to manually retrieve the table we need
        if (MT == null) {
          ClassName = (String) n.f0.accept(this,argu);
          MethodName = (String) n.f2.accept(this,argu);
          CT = instance.get(ClassName);
          MT = CT.get_method(MethodName);

          //if method Table is still null,
          //that means the method does not exists
          if (MT == null) {
            System.out.println("Type error");
            System.exit(0);
          }
        }

        //switch environment, roll back first
        //if cannot rollback, which mean we are at main class/func
        MethodTable new_MT = null;
        if(instance.get_cur_class() != null) {
          //System.out.println("roll!!!!!!!!!!!!!!!");
          instance.rollback();
          CT = (ClassTable) instance.get_cur();
          new_MT = CT.get_method(MethodName);
        }

        else {
          CT = (ClassTable) instance.get(ClassName);
          new_MT = CT.get_method(MethodName);
        }

        //if the reference method is null
        //probably because the method is define in other classes
        if (new_MT == null) {
          //this will only get us an Identifier ID
          //we'll need to extract the String manually
          Node ID = MT.l_get(ClassName);
          String id = (String) ID.accept(this,argu);
          new_MT = instance.cross_get_method(id, MethodName);
        }

        //switch back to pre env
        //in case the accept method transfers control to visitors
        instance.set_cur(MT);
        //System.out.println("back!!!!!!!!!!!!!!!");
        //System.out.println("______________________________");

        //get parameter type
        ExpressionList EL = (ExpressionList)n.f4.node;

        //if it's a method wihout taking any argument
        //no need to continue, just return the method's return type
        if(EL == null) {
          return new_MT == null ? null : new_MT.get_ret();
        }

        Vector<Node> para_list = EL.f1.nodes;
        Expression E = EL.f0;

        //the first parameter type
        Object obj = E.f0.choice.accept(this,argu);
        Node type = null;

        //Note!!:could be BooleanLiteral, 'true' or 'false'
        //check if the parameter is an ID or IntegerLiteral
        if (obj instanceof String) {
          type = MT.p_get((String) obj) == null ? MT.l_get((String) obj) : MT.p_get((String) obj);
        }
        else
          type = (Node) obj;

        //type check number of parameters
        int para_nums = para_list.size() + (E == null ? 0 : 1);
        if(para_nums != new_MT.parameters.size()) {
          System.out.println("Type error");
          System.exit(0);
        }
        //correct number of parameters
        else {
          //only one parameter
          if(para_list.isEmpty()) {
            for(Node pt : new_MT.parameters.values()) {
              if(!type.getClass().equals(pt.getClass())) {
                System.out.println("Type error");
                System.exit(0);
              }
            }
          }
          //handle multiple arguments/parameters
          else {
              List<Node> paras = new ArrayList<Node>();
              paras.add(type);  //add the first parameter to the parameter list
              for(int i= 0; i<para_list.size(); i++) {
                Node ER = para_list.elementAt(i);
                Object temp = ER.accept(this,argu);   //unkown danger?
                Node pt = null;

                if(temp instanceof String)
                  pt = MT.p_get((String) temp) == null ? MT.l_get((String) temp) : MT.p_get((String) temp);
                else if (temp instanceof Identifier)
                  pt = (Identifier) temp;
                else if (temp instanceof IntegerType)
                  pt = (IntegerType) temp;
                else if (temp instanceof BooleanType)
                  pt = (BooleanType) temp;
                else {
                  System.out.println("Type error");
                  System.exit(0);
                }
                paras.add(pt);
              }

              List<Node> p = new ArrayList<Node>(new_MT.parameters.values());
              for(int i = 0; i<paras.size(); i++) {
                if(!(paras.get(i).getClass().equals(p.get(i).getClass()))) {
                  System.out.println("Type error");
                  System.exit(0);
                }
              }
          }
        }

        return new_MT == null ? null : new_MT.get_ret();
    }

    @Override
    public Object visit(ExpressionList n, Object argu) {
        return super.visit(n, argu);
    }

    @Override
    public Object visit(ExpressionRest n, Object argu) {
        return n.f1.accept(this,argu);
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
        ClassList instance = ClassList.getInstance();
        ClassTable CT = (ClassTable) instance.get_cur_class();
        return CT.get_id();
        //return new ThisExpression();
    }

    @Override
    public Object visit(ArrayAllocationExpression n, Object argu) {
        ClassList instance = ClassList.getInstance();
        //get class table from current environment
        MethodTable MT = (MethodTable) instance.get_cur();
        String ele_id = (String)n.f3.accept(this,argu);
        Node ele_type = MT.p_get(ele_id) == null ? MT.l_get(ele_id) : MT.p_get(ele_id);


        if (ele_type == null || !(ele_type instanceof IntegerType)) {
          System.out.println("Type error");
          System.exit(0);
        }

        return new ArrayType();
    }

    @Override
    public Object visit(AllocationExpression n, Object argu) {
        return n.f1;
    }

    @Override
    public Object visit(NotExpression n, Object argu) {
        ClassList instance = ClassList.getInstance();
        MethodTable MT = (MethodTable) instance.get_cur();
        Node type = null;
        Object obj = n.f1.accept(this,argu);

        if(obj instanceof Identifier) {
          System.out.println("not fix this");
          System.exit(0);
        }

        if(obj instanceof String && (String) obj != "true" && (String) obj != "false")
          type = MT.p_get((String) obj) == null ? MT.l_get((String) obj) : MT.p_get((String) obj);
        else if (obj instanceof BooleanType)
          {}
        else if ((String) obj != "true" || (String) obj != "false") {
          System.out.println("Type error");
          System.exit(0);
        }

        return new BooleanType();
    }

    @Override
    public Object visit(BracketExpression n, Object argu) {
        return n.f1.accept(this,argu);
    }
}
