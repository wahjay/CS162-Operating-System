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
        //check acyclic here
        ClassList instance = ClassList.getInstance();
        for(ClassTable CT : instance.C_List.values()) {
          //System.out.println(CT.ID.accept(this,argu));
          String child_id = (String)CT.ID.accept(this,argu);
          ClassTable child_CT = CT;
          String parent_id = CT.get_super_id();

          while(parent_id != null) {
            if(child_id.equals(parent_id)) {
              System.out.println("Type error");
              System.exit(0);
            }

            //keep searching for parent_id until null
            child_CT = instance.get(parent_id);
            parent_id = child_CT.get_super_id();
          }
       }

        return super.visit(n, argu);
    }

    @Override
    public Object visit(MainClass n, Object argu) {
        ClassList instance = ClassList.getInstance();
        String className = (String) n.f1.accept(this,argu);
        ClassTable CT = instance.get(className);
        String mainName = (String) n.f6.accept(this,argu);
        MethodTable MT = CT.get_method(mainName);
        instance.set_cur(MT); //set 'this' method as current scope
        instance.set_cur_class(CT); //set 'this' class as current env

        return super.visit(n, argu);
    }

    @Override
    public Object visit(TypeDeclaration n, Object argu) {
        //System.out.println(n.f0.choice.accept(this,argu));
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public Object visit(ClassDeclaration n, Object argu) {
        ClassList instance = ClassList.getInstance();
        String className = (String) n.f1.accept(this,argu);
        ClassTable CT = instance.get(className);
        instance.set_cur_class(CT); //set 'this' class as current environment
        return super.visit(n, argu);
    }

    @Override
    public Object visit(ClassExtendsDeclaration n, Object argu) {
        ClassList instance = ClassList.getInstance();
        String className = (String) n.f1.accept(this,argu);
        String superClassName = (String)n.f3.accept(this,argu);
        ClassTable CT = instance.get(className);
        instance.set_cur_class(CT); //set 'this' class as current environment

        //check if the super class exists
        if(!instance.C_List.containsKey(superClassName)) {
          System.out.println("Type error");
          System.exit(0);
        }

        if(instance.CheckOverload(className, superClassName)) {
          System.out.println("Type error");
          System.exit(0);
        }

        return super.visit(n, argu);
    }

    @Override
    public Object visit(VarDeclaration n, Object argu) {
        //Node type = n.f0.f0.choice;
        //make sure ID exists
        ClassList instance = ClassList.getInstance();
        Object obj = instance.get_cur();
        MethodTable MT = null;
        ClassTable CT = null;
        String id = (String) n.f1.accept(this,argu);
        Node type = null;


        if(obj instanceof MethodTable) {
          MT = (MethodTable) obj;
          type = MT.p_get(id) == null ? MT.l_get(id) : MT.p_get(id);
          ClassTable ct = (ClassTable) instance.get_cur_class();
        }
        else if (obj instanceof ClassTable) {
          CT = (ClassTable) obj;
          type = CT.get_field(id);
        }

        if (!(type instanceof BooleanType || type instanceof ArrayType
            || type instanceof IntegerType || type instanceof Identifier) ) {
              System.out.println("1Type error");
              System.exit(0);
        }

        //return the type node
        return null;
    }

    @Override
    public Object visit(MethodDeclaration n, Object argu) {
        ClassList instance = ClassList.getInstance();
        String MethodName = (String) n.f2.accept(this,argu);

        //just enter into a new scope
        //need to update cur environment
        Object temp = instance.get_cur_class();
        ClassTable Ctemp = null;
        MethodTable Mtemp = null;
        if (temp != null) {
          Ctemp = (ClassTable) temp;
          Mtemp = Ctemp.get_method(MethodName);

          //Mtemp == null means we do enter into a new scope
          //and its from a class env to another class env
          //so cur class does not know cur method name.
          //the folliwng line of code will only update env
          //if cur method is inside its class env,
          //if it's not null, we will set cur method as cur scope.
          if(Mtemp != null)
            instance.set_cur(Mtemp);
        }

        //just in case the cur class is null,
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

        //check if the return statement is an ID or a IntegerLiteral
        if(obj instanceof String)
          ret = MT.l_get((String) obj);
        else
          ret = (Node) obj;


        if(ret == null || ret_type == null ||!ret_type.getClass().equals(ret.getClass())) {
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
        else if (obj instanceof Identifier)
          right = (Identifier) obj;
        else
          right = (Node) obj;

        //check if left or right ID exists
        if(left == null || right == null) {
          System.out.println("Type error");
          System.exit(0);
        }

        String left_type = (String)left.accept(this,argu);
        String right_type = (String)right.accept(this,argu);
        //compare type String name (i.e int, boolean, Tree, etc)
        //if either type is null, maybe because the type is array,
        //so we need to check that, too
        if(left_type != null && right_type != null && left_type.equals(right_type))
          return null;

        else {
          //compare array type
          if (right instanceof ArrayType) {
            if(!right.getClass().equals(left.getClass())) {
              System.out.println("Type error");
              System.exit(0);
            }
            else
              return null;
          }

          //handle subtype here
          else {
              if(left_type == null || right_type == null) {
                System.out.println("Type error");
                System.exit(0);
              }
              //get both-side class table
              ClassTable left_ct = instance.get(left_type);
              ClassTable right_ct = instance.get(right_type);

              if(left_ct == null || right_ct == null) {
                System.out.println("Type error");
                System.exit(0);
              }

              //then get both side super class
              String left_super_type = left_ct.get_super_id();
              String right_super_type = right_ct.get_super_id();

              //compare
              if(left_type.equals(right_super_type))
                return null;
              //transitive compare
              else if (instance.get(right_super_type) != null) {
                while (right_super_type != null && !left_type.equals(right_super_type)) {
                  right_ct = instance.get(right_super_type);
                  right_super_type = right_ct.get_super_id();

                  if(left_type.equals(right_super_type))
                    return null;
                }

                //after the while loop, if still not equal, then error
                if(right_super_type == null && !left_type.equals(right_super_type))
                 {
                   System.out.println("Type error");
                   System.exit(0);
                 }
              }

              else {
                System.out.println("Type error");
                System.exit(0);
              }
          }
        }

        return null;
    }

    @Override
    public Object visit(ArrayAssignmentStatement n, Object argu) {
        ClassList instance = ClassList.getInstance();
        MethodTable MT = (MethodTable) instance.get_cur();
        Object arr_obj = n.f0.accept(this,argu);
        Object ele_obj = n.f2.accept(this,argu);
        Object rhs_obj = n.f5.accept(this,argu);
        Node arr_type = null;
        Node ele_type = null;
        Node rhs_type = null;

        //array ID must be array type
        if (arr_obj instanceof String) {
          String arr_id = (String) arr_obj;
          arr_type = MT.p_get(arr_id) == null ? MT.l_get(arr_id) : MT.p_get(arr_id);
        }
        else if (arr_obj instanceof ArrayType)
          arr_type = (ArrayType) arr_obj;

        //array element must be an IntegerType
        if (ele_obj instanceof String) {
          String ele_id = (String) ele_obj;
          ele_type = MT.p_get(ele_id) == null ? MT.l_get(ele_id) : MT.p_get(ele_id);
        }
        else if (ele_obj instanceof IntegerType)
          ele_type = (IntegerType) ele_obj;

        //rhs must be an IntegerType
        if (rhs_obj instanceof String) {
          String rhs_id = (String) rhs_obj;
          rhs_type = MT.p_get(rhs_id) == null ? MT.l_get(rhs_id) : MT.p_get(rhs_id);
        }
        else if (rhs_obj instanceof IntegerType)
          rhs_type = (IntegerType) rhs_obj;

        if(!(arr_type instanceof ArrayType && ele_type instanceof IntegerType
          && rhs_type instanceof IntegerType)) {
            System.out.println("Type error");
            System.exit(0);
          }

        return null;
    }

    @Override
    public Object visit(IfStatement n, Object argu) {
        ClassList instance = ClassList.getInstance();
        MethodTable MT = (MethodTable) instance.get_cur();
        Object obj = n.f2.accept(this,argu);
        Node type = null;

        if(obj instanceof String)
          type = MT.p_get((String) obj) == null ? MT.l_get((String) obj) : MT.p_get((String) obj);
        else if (obj instanceof BooleanType)
          type = (BooleanType) obj;

        if(!(type instanceof BooleanType)){
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
        ClassList instance = ClassList.getInstance();
        MethodTable MT = (MethodTable) instance.get_cur();
        Object obj = n.f2.accept(this,argu);
        Node type = null;

        if(obj instanceof String)
          type = MT.p_get((String) obj) == null ? MT.l_get((String) obj) : MT.p_get((String) obj);
        else if (obj instanceof BooleanType)
          type = (BooleanType) obj;

        if(!(type instanceof BooleanType)){
          System.out.println("Type error");
          System.exit(0);
        }

        n.f4.accept(this,argu);
        return null;
    }

    @Override
    public Object visit(PrintStatement n, Object argu) {
        ClassList instance = ClassList.getInstance();
        MethodTable MT = (MethodTable) instance.get_cur();
        Node type = null;
        Object obj = n.f2.accept(this,argu);

        if (obj instanceof String)
          type = MT.p_get((String) obj) == null ? MT.l_get((String) obj) : MT.p_get((String) obj);
        else if (obj instanceof IntegerType)
          type = (IntegerType) obj;

        if(!(type instanceof IntegerType)) {
          System.out.println("Type error");
          System.exit(0);
        }

        return new IntegerType();
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

        if (right instanceof IntegerType) {
          right_type = (Node) right;
        }
        //if not, then it should be an id
        else if (right instanceof String) {
          String id = (String) right;
          right_type = MT.l_get(id);
        }

        if(left_type == null || right_type == null || !left_type.getClass().equals(right_type.getClass())) {
          System.out.println("Type error");
          System.exit(0);
        }

        return new IntegerType();
    }

    @Override
    public Object visit(MinusExpression n, Object argu) {
      ClassList instance = ClassList.getInstance();

      //get class table from current environment
      //System.out.println(instance.get_cur());
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

      if (right instanceof IntegerType) {
        right_type = (Node) right;
      }
      //if not, then it should be an id
      else if (right instanceof String) {
        String id = (String) right;
        right_type = MT.l_get(id);
      }

      if(left_type == null || right_type == null || !left_type.getClass().equals(right_type.getClass())) {
        System.out.println("Type error");
        System.exit(0);
      }

      return new IntegerType();
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

        if (right instanceof IntegerType) {
          right_type = (Node) right;
        }
        //if not, then it should be an id
        else if (right instanceof String) {
          String id = (String) right;
          right_type = MT.l_get(id);
        }

        if(left_type == null || right_type == null || !left_type.getClass().equals(right_type.getClass())) {
          System.out.println("Type error");
          System.exit(0);
        }

        return new IntegerType();
    }

    @Override
    public Object visit(ArrayLookup n, Object argu) {
        ClassList instance = ClassList.getInstance();
        MethodTable MT = (MethodTable) instance.get_cur();

        String arr_id = (String) n.f0.accept(this,argu);
        //String ele_id = (String) n.f2.accept(this,argu);

        //System.out.println("array: " + n.f0.accept(this,argu));
        //System.out.println("element " + n.f2.accept(this,argu));
        //System.out.println("cur method table: " + MT);

        //look up in method parameters first
        Node arr_type = null;
        Node ele_type = null;
        //check for parameters first, if nothing found, check locals
        arr_type =  MT.p_get(arr_id) == null ? MT.l_get(arr_id) : MT.p_get(arr_id);

        Object obj = n.f2.accept(this,argu);
        if (obj instanceof String) {
          String ele_id = (String) obj;
          ele_type =  MT.p_get(ele_id) == null ? MT.l_get(ele_id) : MT.p_get(ele_id);
        }
        else if (obj instanceof IntegerType)
          ele_type = (IntegerType) obj;

        //if still nothing, ID does not exist
        if(arr_type == null || ele_type == null) {
          System.out.println("Type error");
          System.exit(0);
        }

        if(!(arr_type instanceof ArrayType && ele_type instanceof IntegerType)) {
          System.out.println("Type error");
          System.exit(0);
        }

        return new IntegerType();
    }

    @Override
    public Object visit(ArrayLength n, Object argu) {
        ClassList instance = ClassList.getInstance();
        MethodTable MT = (MethodTable) instance.get_cur();
        Object obj = n.f0.accept(this,argu);
        Node type = null;

        if (obj instanceof String)
          type = MT.p_get((String) obj) == null ? MT.l_get((String) obj) : MT.p_get((String) obj);
        else if (obj instanceof ArrayType)
          type = (ArrayType) obj;

        if(!(type instanceof ArrayType)){
          System.out.println("Type error");
          System.exit(0);
        }

        return new IntegerType();
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

        //switch back to pre env
        //in case the accept method transfers control to visitors
        instance.set_cur(MT);

        //if the callee method is null
        //probably because the method is define in other classes
        if (new_MT == null) {
          //this will only get us an Identifier ID
          //we'll need to extract the String manually
          Node class_id = MT.l_get(ClassName);

          //class_id == null, meaning ClassName could
          //hold the class name type, we could directly pass the name
          if(class_id == null)
            new_MT = instance.cross_get_method(ClassName, MethodName);

          //else we need to search the class name type
          //corresponding to the class ID
          else {
            String id = (String) class_id.accept(this,argu);
            new_MT = instance.cross_get_method(id, MethodName);
          }
        }

        //at this point, if new_MT, AKA the callee method,
        //is null, probably because it does not exist
        if(new_MT == null) {
          System.out.println("Type error");
          System.exit(0);
        }

        //get parameter type
        ExpressionList EL = (ExpressionList)n.f4.node;

        //if it's a method wihout taking any argument
        //no need to continue, just return the method's return type
        //However, we still need to double check if this is true
        if(EL == null) {
          if(new_MT.parameters.size() != 0) {
            System.out.println("Type error");
            System.exit(0);
          }

          return new_MT.get_ret();
        }

        Vector<Node> para_list = EL.f1.nodes;
        Expression E = EL.f0;

        //the first argument type
        Object obj = E.f0.choice.accept(this,argu);
        Node arg_type = null;

        //check if the parameter is an ID or IntegerLiteral
        if (obj instanceof String) {
          arg_type = MT.p_get((String) obj) == null ? MT.l_get((String) obj) : MT.p_get((String) obj);
        }
        else
          arg_type = (Node) obj;

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
              //System.out.println("arugument: "+ arg_type.accept(this,argu));
              //System.out.println("parameter: " + arg_type.accept(this,argu));

              //subtype passing
              if (pt instanceof Identifier && arg_type instanceof Identifier) {
                String para_type = (String) pt.accept(this,argu);
                String argu_type = (String) arg_type.accept(this,argu);

                //type check argument vs parament finished
                //exit early here
                if(para_type.equals(argu_type)) {
                  return new_MT.get_ret();
                }

                ClassTable cur_table = instance.get(argu_type);
                String argu_super_type = cur_table.get_super_id();
                while(argu_super_type != null && !para_type.equals(argu_super_type)) {
                  cur_table = instance.get(argu_super_type);
                  argu_super_type = cur_table.get_super_id();
                }

                //check the result type after the while loop
                if(argu_super_type == null && !para_type.equals(argu_super_type)){
                  System.out.println("Type error");
                  System.exit(0);
                }
              }
              //primitive type passing
              else if(!arg_type.getClass().equals(pt.getClass())) {
                System.out.println("Type error");
                System.exit(0);
              }
            }
          }
          //handle multiple arguments/parameters
          else {
              List<Node> paras = new ArrayList<Node>();
              paras.add(arg_type);  //add the first parameter to the parameter list
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
                if (paras.get(i) instanceof Identifier && p.get(i) instanceof Identifier) {
                  String argu_type = (String) paras.get(i).accept(this,argu);
                  String para_type = (String) p.get(i).accept(this,argu);

                  while(argu_type != null && !argu_type.equals(para_type)) {
                    ClassTable cur_table = instance.get(argu_type);
                    argu_type = cur_table.get_super_id();
                  }

                  if(argu_type == null && !para_type.equals(argu_type)) {
                    System.out.println("Type error");
                    System.exit(0);
                  }
                }

                else if(paras.get(i) == null || !(paras.get(i).getClass().equals(p.get(i).getClass()))) {
                  System.out.println("Type error");
                  System.exit(0);
                }
              }
          }
        }

        return new_MT.get_ret();
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

        //class must exist
        if(CT == null) {
          System.out.println("Type error");
          System.exit(0);
        }

        return CT.get_id();
        //return new ThisExpression();
    }

    @Override
    public Object visit(ArrayAllocationExpression n, Object argu) {
        ClassList instance = ClassList.getInstance();
        //get class table from current environment
        MethodTable MT = (MethodTable) instance.get_cur();
        //String ele_id = (String)n.f3.accept(this,argu);
        Object obj  = n.f3.accept(this,argu);
        Node ele_type = null;

        if (obj instanceof IntegerType)
          ele_type = (IntegerType) obj;
        else if (obj instanceof String) {
          String ele_id = (String) obj;
          ele_type = MT.p_get(ele_id) == null ? MT.l_get(ele_id) : MT.p_get(ele_id);
        }

        if (ele_type == null || !(ele_type instanceof IntegerType)) {
          System.out.println("Type error");
          System.exit(0);
        }

        return new ArrayType();
    }

    @Override
    public Object visit(AllocationExpression n, Object argu) {
        //System.out.println("1 : "+n.f1.accept(this,argu));
        //System.out.println("2 : " +n.f1);
        return n.f1;
    }

    @Override
    public Object visit(NotExpression n, Object argu) {
        ClassList instance = ClassList.getInstance();
        MethodTable MT = (MethodTable) instance.get_cur();
        Node type = null;
        Object obj = n.f1.accept(this,argu);

        if(obj instanceof String)
          type = MT.p_get((String) obj) == null ? MT.l_get((String) obj) : MT.p_get((String) obj);
        else if (obj instanceof BooleanType)
          type = (BooleanType) obj;

        if(!(type instanceof BooleanType)){
          System.out.println("Type error");
          System.exit(0);
        }

        return new BooleanType();
    }

    @Override
    public Object visit(BracketExpression n, Object argu) {
        //could be literal type or ID
        ClassList instance = ClassList.getInstance();
        MethodTable MT = (MethodTable) instance.get_cur();
        Node type = null;
        Object obj = n.f1.accept(this,argu);

        if (obj instanceof String)
          type = MT.p_get((String) obj) == null ? MT.l_get((String) obj) : MT.p_get((String) obj);
        else if (obj instanceof IntegerType)
          type = (IntegerType) obj;
        else if (obj instanceof BooleanType)
          type = (BooleanType) obj;
        else if (obj instanceof ArrayType)
          type = (ArrayType) obj;
        else if (obj instanceof Identifier)
          type = (Identifier) obj;
        else{
          System.out.println("Type error");
          System.exit(0);
        }

        return type;
    }
}
