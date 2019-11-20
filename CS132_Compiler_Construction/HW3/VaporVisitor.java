import syntaxtree.*;
import visitor.GJDepthFirst;
import java.util.HashMap;
import java.io.IOException;
import java.util.*;

public class VaporVisitor extends GJDepthFirst {

  //used to store array base addresses
  public static HashMap<String, String> Arrays = new HashMap<>();
  //used to build array allocation helper function
  public static String arrAllocHelper = "";
  //global counter for label generation
  public static int counter = -1;

  //helper functions
  private static String unique() {
    counter++;
    return "t." + counter;
  }

  public static String getIfLabel(String t) {
    return "if" + t + "_else";
  }

  public static String getElseLabel(String t) {
    return "if" + t + "_end";
  }

  public static String getNullLabel(String t) {
    return "null" + t;
  }

  public static String getNullEnd(String t) {
    return "nullend" + t;
  }

  public static String getEndLabel(String t) {
    return "end" + t;
  }

  public static String getWhileTop(String t) {
    return "while" + t + "_top";
  }

  public static String getWhileBot(String t) {
    return "while" + t + "_end";
  }

  public static boolean isNum(String t){
    return t.matches("-?\\d+(\\.\\d+)?");
  }

  //indent block format
  public static String format(String C) {
    String a = "\n";    //separator
    String b = "\n  ";  //replacement
    String result = C.substring(0,C.lastIndexOf(a)).replaceAll(a , b).concat(C.substring(C.lastIndexOf(a)));
    result = result.replaceAll("\n  $", "");
    return result;
  }

  public static boolean containsOp(String t) {
    if(t.indexOf("Add") != -1 || t.indexOf("Sub") != -1 || t.indexOf("MulS") != -1
      || t.indexOf("LtS") != -1 || t.indexOf("Eq") != -1) {
          return true;
    }

    return false;
  }

  //override functions

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
        //return super.visit(n, argu);
        return n.tokenImage;
    }

    @Override
    public Object visit(Goal n, Object argu) {
        MethodTable instance = MethodTable.getInstance();
        String global ="";
        for(Map.Entry<String, List<String>> entry : instance.methodTable.entrySet()) {
          global += "const vmt_" + entry.getKey() + "\n  ";
          for(String method : entry.getValue()){
            global += ":" + method + "\n  ";
          }
          global += "\n";
        }

        String C1 = (String) n.f0.accept(this,argu);
        String C2 = "";
        Vector<Node> vec = n.f1.nodes;
        Iterator it = vec.iterator();
        while(it.hasNext()) {
          TypeDeclaration cd = (TypeDeclaration)it.next();
          C2 += (String) cd.accept(this,argu);
        }

        String code = global + "\n" + C1 + "\n\n" + C2 + arrAllocHelper + "\n";
        return code;
    }

    @Override
    public Object visit(MainClass n, Object argu) {
        //methods init
        MethodTable instance = MethodTable.getInstance();

        //head
        String code = "func Main()\n";

        //body
        Vector<Node> vec = n.f15.nodes;
        Iterator it = vec.iterator();
        while(it.hasNext()) {
          Statement stmt = (Statement)it.next();
          Pair p = new Pair();
          p = (Pair)stmt.f0.accept(this,argu);
          code += p.code;
        }

        code = code + "ret\n";
        code = format(code);
        return code;
        //return super.visit(n, argu);
    }

    @Override
    public Object visit(TypeDeclaration n, Object argu) {
        return n.f0.accept(this,argu);
        //return super.visit(n, argu);
    }

    @Override
    public Object visit(ClassDeclaration n, Object argu) {
        MethodTable instance = MethodTable.getInstance();
        //get cur class name
        Pair p = new Pair();
        p = (Pair)n.f1.accept(this,argu);
        instance.cur_class = p.code;  //set this class as cur class

        String code = "";
        Vector<Node> vec = n.f4.nodes;
        Iterator it = vec.iterator();
        while(it.hasNext()) {
          MethodDeclaration md = (MethodDeclaration)it.next();
          String p1 = (String)md.accept(this,argu);
          code += p1 + "\n";
        }

        //System.out.println(code);
        return code;
        //return super.visit(n, argu);
    }

    @Override
    public Object visit(ClassExtendsDeclaration n, Object argu) {
        MethodTable instance = MethodTable.getInstance();
        //get cur class name
        Pair p = new Pair();
        p = (Pair)n.f1.accept(this,argu);
        instance.cur_class = p.code;  //set this class as cur class

        String code = "";
        Vector<Node> vec = n.f6.nodes;
        Iterator it = vec.iterator();
        while(it.hasNext()) {
          MethodDeclaration md = (MethodDeclaration)it.next();
          String p1 = (String)md.accept(this,argu);
          code += p1 + "\n";
        }

        //System.out.println(code);
        return code;

        //return super.visit(n, argu);
    }

    @Override
    public Object visit(VarDeclaration n, Object argu) {
        return super.visit(n, argu);
    }

    @Override
    public Object visit(MethodDeclaration n, Object argu) {
      //function signature
      MethodTable instance = MethodTable.getInstance();
      String cur_class = instance.cur_class;
      Pair p1 = new Pair();
      p1 = (Pair)n.f2.accept(this,argu);
      String method_name = cur_class + "." + p1.code;
      instance.cur_method = p1.code;  //set this method as cur method

      //get paras name
      String paras;
      //check if arg is null
      if (n.f4.node == null)
        paras = "";
      else
        paras = (String)n.f4.node.accept(this,argu);

      String code = "func " + method_name + "(this" + paras + ")\n";

      //function body
      String body = "";
      Vector<Node> vec = n.f8.nodes;
      Iterator it = vec.iterator();
      while(it.hasNext()) {
        Statement stmt = (Statement)it.next();
        Pair p = new Pair();
        p = (Pair)stmt.f0.accept(this,argu);
        body += p.code;
      }

      //return statement
      Pair p = new Pair();
      p = (Pair)n.f10.accept(this,argu);
      String ret = p.result == null? "" : p.code;

      ///////////////////////////////////////////////////
      //first check if the var is in local
      //if so, get its type and go to that type class and get its position
      String t = null;
      if(p.result == null) {
        String type = instance.getLocalType(cur_class + "." + instance.cur_method, p.code);
        int pos;
        if(type != null)
          pos = instance.getFPosition(type, p.code);
        //if not, check current class field
        else
          pos = instance.getFPosition(instance.cur_class, p.code);
        if(pos != -1) {
          t = unique();
          ret += t + " = " + "[this+" + pos + "]\n";
        }
      }
      //////////////////////////////////////////////////

      /*
      //check if the var is in class field
      //if so, act accordingly
      String t = null;
      if(p.result == null) {
        int pos = instance.getFPosition(instance.cur_class, p.code);
        if(pos != -1) {
          t = unique();
          ret += t + " = " + "[this+" + pos + "]\n";
        }
      }
      */

      ret += "\nret " + (t == null? (p.result == null ? p.code : p.result) : t) + "\n";
      code = code + body + ret;
      code = format(code);
      return code + "\n";
      //return super.visit(n, argu);
    }

    @Override
    public Object visit(FormalParameterList n, Object argu) {
      String code = (String)n.f0.accept(this,argu);

      Vector<Node> vec = n.f1.nodes;
      Iterator it = vec.iterator();
      while(it.hasNext()) {
        FormalParameterRest para_rest = (FormalParameterRest)it.next();
        String para = (String)para_rest.f1.accept(this,argu);
        code = code + " " + para;
      }

      return code == null? "" : (" " + code);
      //return super.visit(n, argu);
    }

    @Override
    public Object visit(FormalParameter n, Object argu) {
        //since identifier return a pair
        //we split it and return the part we want
        Pair p = new Pair();
        p = (Pair)n.f1.accept(this,argu);
        return p.code;
    }

    @Override
    public Object visit(FormalParameterRest n, Object argu) {
        return n.f1.accept(this, argu);
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
        return n.f0.choice.accept(this, argu);
        //return super.visit(n, argu);
    }

    @Override
    public Object visit(Block n, Object argu) {
        MethodTable instance = MethodTable.getInstance();
        Pair p = new Pair();

        String code = "";
        Vector<Node> vec = n.f1.nodes;
        Iterator it = vec.iterator();
        while(it.hasNext()) {
          Statement stmt = (Statement)it.next();
          //System.out.println(stmt.f0.choice);
          p = (Pair)stmt.accept(this,argu);
          code += p.code + "\n";
          //System.out.println(p.code);
          //System.out.println("_______________________________");
        }

        //System.out.println(code);
        return new Pair(code, null);
        //return super.visit(n, argu);
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    @Override
    public Object visit(AssignmentStatement n, Object argu) {
        MethodTable instance = MethodTable.getInstance();
        Pair p1 = new Pair();
        Pair p2 = new Pair();
        p1 = (Pair) n.f0.accept(this,argu);
        p2 = (Pair) n.f2.accept(this,argu);

        //if this is an new array allocation assignment,
        //we insert it into the global array construction table
        if(n.f2.f0.choice instanceof PrimaryExpression) {
          PrimaryExpression PE = (PrimaryExpression)n.f2.f0.choice;
          //only insert into the global table if it's a local array
          if(PE.f0.choice instanceof ArrayAllocationExpression) {
            //insert local arrays in this format: "method.arrayname, reg"
            Arrays.put(instance.cur_method + "." + p1.code, p2.result);
          }
        }

        //check expression type, and respond accordingly
        String code = "";
        code = p2.result == null? "" : p2.code;

        String t1 = null;
        String t2 = null;
        String left = null;
        String right = null;
        //check if variable is a class field
        //potential bug: local vars may share the same name with instance vars
        if(p1.result == null) {
          int pos = instance.getFPosition(instance.cur_class, p1.code);
          if(pos != -1) {
            t1 = unique();
            left = "[this+" + pos + "]";
            //code += t1 + " = " + left + "\n";
          }
        }

        if(p2.result == null) {
          int pos = instance.getFPosition(instance.cur_class, p2.code);
          if(pos != -1) {
            t2 = unique();
            right = "[this+" + pos + "]";
            //code += t1 + " = " + right + "\n";
          }
        }

        code += (t1==null? (p1.result==null? p1.code: p1.result) : left) + " = " + (t2 == null? (p2.result==null? p2.code: p2.result) : right) + "\n";
        //code += p1.code + " = " + (p2.result == null? p2.code : p2.result) + "\n";

        //System.out.println(code);
        //System.out.println("_______________________________");
        return new Pair(code, null);
    }

    @Override
    public Object visit(ArrayAssignmentStatement n, Object argu) {
        MethodTable instance = MethodTable.getInstance();
        Pair array = new Pair();
        Pair index = new Pair();
        Pair rhs = new Pair();
        array = (Pair)n.f0.accept(this,argu);
        index = (Pair)n.f2.accept(this,argu);
        rhs = (Pair)n.f5.accept(this,argu);

        /*
        int pos = instance.getFPosition(instance.cur_class, array.code);
        String base = null;
        if (pos == -1)
          base = Arrays.get(instance.cur_method + "." + array.code);
        */

        //////////////////////////////////////////////////////////////////
        String base = Arrays.get(instance.cur_method + "." + array.code);
        int pos = -1;
        if (base == null)
          pos = instance.getFPosition(instance.cur_class, array.code);
        //////////////////////////////////////////////////////////////////


        //potential bug below: may need to check if index.result is null
        //get base address
        String len = unique();
        //String load = len + " = " + "[" + (pos != -1 ? ("this+" + pos) : base) + "]\n";
        String load = len + " = " + (pos != -1 ? ("[this+" + pos + "]") : base) + "\n";
        //String load = len + " = " + "[" + (base == null ? ("this+" + pos) : base) + "]\n";
        String code = "if0 " + len + " goto :" + getNullLabel(len) + "\n";
        code += index.result == null ? "" : index.code; // in case index is an expression

        //////////////////////////////////////////////////////////////////
        //get [this+size] format for index if index is a class field
        String t1 = null;
        if(index.result == null) {
          int position = instance.getFPosition(instance.cur_class, index.code);
          if(position != -1) {
            t1 = unique();
            code += t1 + " = " + "[this+" + position + "]\n";
          }
        }
        //////////////////////////////////////////////////////////////////


        //load base address/register
        //check if index < array length
        String temp = unique();
        code += temp + " = " + "[" + len + "]\n";
        code += temp + " = LtS(" + (t1 == null? (index.result == null? index.code : index.result) : t1) + " " + temp + ")\n";

        ////////////////////////////////////////////////////////
        //check if the index >= 0
        //implement greate than and equal to logic using
        //Eq and LtS built-in operator in vapor
        String Lts = unique();
        code += Lts + " = LtS(0 " + (t1 == null? (index.result == null? index.code : index.result) : t1)+ ")\n";
        String Eq = unique();
        code += Eq + " = Eq(0 " + (t1 == null? (index.result == null? index.code : index.result) : t1) + ")\n";
        code += Eq + " = Add(" + Lts + " " + Eq + ")\n";
        code += temp + " = MulS(" + temp + " " + Eq + ")\n";
        ////////////////////////////////////////////////////////

        //array lookup
        String if_ = "if0 " + temp + " goto :" + getIfLabel(len) + "\n";
        //naive just for now, need to deal with expression later
        //calculate offset
        if_ += temp + " = MulS(" + (t1 == null? (index.result == null? index.code : index.result) : t1) + " 4)\n";
        //add offset to base to get the desired element
        if_ += temp + " = " + "Add(" + temp + " " + (base == null? len : base) + ")\n";

        //rhs append here
        if_ += rhs.result == null? "" : rhs.code;


        //String t2 = unique();
        //retrieve the element, "+4" because the 0 index is used to store length
        if_ += "[" + temp + "+4] = " + (rhs.result==null? rhs.code : rhs.result) + "\n";
        if_ += "goto :" + getEndLabel(len) + "\n";
        if_ = format(if_);

        //append array look
        code += if_;

        String checkBound = getIfLabel(len) + ":\n";
        checkBound += "  Error(\"array index out of bounds\")\n";
        checkBound += getEndLabel(len) + ":\n";

        //append bound check end label, as well jump null label
        code += checkBound;
        code += "goto :" + getNullEnd(len) + "\n";

        //indent the block one more time
        code = format(code);
        code = load + code;

        //append null check end label
        //String end  = "goto :" + getNullEnd(len) + "\n";
        String end = getNullLabel(len) + ":\n";
        end += "  Error(\"null pointer\")\n";
        end += getNullEnd(len) + ":\n";
        code += end;
        return new Pair(code, null);
    }

    @Override
    public Object visit(IfStatement n, Object argu) {
        MethodTable instance = MethodTable.getInstance();
        Pair p1 = new Pair();
        Pair p2 = new Pair();
        Pair p3 = new Pair();
        p1 = (Pair)n.f2.accept(this,argu);
        p2 = (Pair)n.f4.accept(this,argu);
        p3 = (Pair)n.f6.accept(this,argu);
        String else_stmt = p3.code;
        String t = unique();

        String if_ = (p1.result == null? "" : (isNum(p1.code) ? "" : p1.code + "\n"));

        //handle [this+size] for IfStatement
        String t1 = null;
        if(p1.result == null) {
          int pos = instance.getFPosition(instance.cur_class, p1.code);
          if(pos != -1) {
            t1 = unique();
            if_ += t1 + " = " + "[this+" + pos + "]\n";
          }
        }

        String exp_ = "if0" + " " + (t1==null? (p1.result==null? p1.code : p1.result) : t1) + " goto :" + getIfLabel(t) + "\n";
        exp_ = exp_ + p2.code + "goto :" + getElseLabel(t) + "\n";

        //if block format
        exp_ = format(exp_);

        //merge if and else block
        String code = if_ + exp_;

        String else_ = getIfLabel(t) + ":" + "\n" + else_stmt;

        //else block format
        else_ = format(else_);
        else_ = else_ + getElseLabel(t) + ":\n";
        //System.out.println(else_);
        code = code + else_;

        //System.out.println(code);
        //return super.visit(n, argu);
        return new Pair(code, null);
        //return super.visit(n, argu);
    }

    @Override
    public Object visit(WhileStatement n, Object argu) {
        Pair exp = new Pair();
        Pair stmt = new Pair();
        exp = (Pair)n.f2.accept(this,argu);
        stmt = (Pair)n.f4.accept(this,argu);
        String code = "";
        String t1 = unique();

        code += getWhileTop(t1) + ":\n";
        code += (exp.result == null? "" : exp.code);
        String block = "if0 " + (exp.result==null? exp.code : exp.result) + " goto :" + getWhileBot(t1) + "\n";
        block += stmt.code;
        block = format(block);
        block += "goto :" + getWhileTop(t1) + "\n";
        code += block;
        code += getWhileBot(t1) + ":\n";

        return new Pair(code, null);
        //return super.visit(n, argu);
    }

    @Override
    public Object visit(PrintStatement n, Object argu) {
        MethodTable instance = MethodTable.getInstance();
        Pair p = new Pair();
        p = (Pair)n.f2.accept(this,argu);

        //if the expression inside the print statement is an expression,
        //we treat it with "t = a + b"
        String code = "";
        code += p.result == null? "" : p.code;

        ////////////////////////////////////////////
        String t = null;
        if(p.result == null) {
          int pos = instance.getFPosition(instance.cur_class, p.code);
          if(pos != -1) {
            t = unique();
            code += t + " = " + "[this+" + pos + "]\n";
          }
        }
        ////////////////////////////////////////////

        //check if there is any computation needed to
        //be do first inside the print statement
        //String code = p.result == null? "" : (p.result + " = " + p.code);
        code += "PrintIntS(";
        code += (t == null? (p.result == null? p.code : p.result) : t) + ")\n";
        return new Pair(code,null);
        //return n.f2.accept(this, argu);
    }

    @Override
    public Object visit(Expression n, Object argu) {
        //System.out.println(n.f0.choice.accept(this,argu));
        return n.f0.accept(this,argu);
        //return n.f0.choice.accept(this, argu);
    }

    @Override
    public Object visit(AndExpression n, Object argu) {
      MethodTable instance = MethodTable.getInstance();
      Pair p1 = new Pair();
      Pair p2 = new Pair();
      p1 = (Pair)n.f0.accept(this,argu);
      p2 = (Pair)n.f2.accept(this,argu);

      String code = "";
      code += (p1.result == null ? "" : p1.code);
      code += (p2.result == null ? "" : p2.code);

      //if p.result is null, then probably this is a varaible
      //get its address
      String t = unique();
      String t1 = null;
      String t2 = null;
      //check if variable is a class field
      //potential bug: local vars may share the same name with instance vars
      if(p1.result == null) {
        int pos = instance.getFPosition(instance.cur_class, p1.code);
        if(pos != -1) {
          t1 = unique();
          code += t1 + " = " + "[this+" + pos + "]\n";
        }
      }

      if(p2.result == null) {
        int pos = instance.getFPosition(instance.cur_class, p2.code);
        if(pos != -1) {
          t2 = unique();
          code += t2 + " = " + "[this+" + pos + "]\n";
        }
      }

      //use MulS built-in operator to implement logical and operator
      code += t + " = MulS(" + (t1==null? (p1.result==null? p1.code: p1.result) : t1)
            + " " + (t2 == null? (p2.result==null? p2.code: p2.result) : t2) + ")\n";
      return new Pair(code, t);
    }

    @Override
    public Object visit(CompareExpression n, Object argu) {
        MethodTable instance = MethodTable.getInstance();
        Pair p1 = new Pair();
        Pair p2 = new Pair();
        p1 = (Pair)n.f0.accept(this,argu);
        p2 = (Pair)n.f2.accept(this,argu);

        String code = "";
        code += (p1.result == null ? "" : p1.code);
        code += (p2.result == null ? "" : p2.code);

        //if p.result is null, then probably this is a varaible
        //get its address
        String t = unique();
        String t1 = null;
        String t2 = null;
        //check if variable is a class field
        //potential bug: local vars may share the same name with instance vars
        if(p1.result == null) {
          int pos = instance.getFPosition(instance.cur_class, p1.code);
          if(pos != -1) {
            t1 = unique();
            code += t1 + " = " + "[this+" + pos + "]\n";
          }
        }

        if(p2.result == null) {
          int pos = instance.getFPosition(instance.cur_class, p2.code);
          if(pos != -1) {
            t2 = unique();
            code += t2 + " = " + "[this+" + pos + "]\n";
          }
        }

        code += t + " = LtS(" + (t1==null? (p1.result==null? p1.code: p1.result) : t1)
              + " " + (t2 == null? (p2.result==null? p2.code: p2.result) : t2) + ")\n";
        return new Pair(code, t);
    }

    @Override
    public Object visit(PlusExpression n, Object argu) {
        MethodTable instance = MethodTable.getInstance();
        Pair p1 = new Pair();
        Pair p2 = new Pair();
        p1 = (Pair)n.f0.accept(this,argu);
        p2 = (Pair)n.f2.accept(this,argu);

        String code = "";
        code += (p1.result == null ? "" : p1.code);
        code += (p2.result == null ? "" : p2.code);

        //System.out.println(1);

        //if p.result is null, then probably this is a varaible
        //get its address
        String t = unique();
        String t1 = null;
        String t2 = null;
        //check if variable is a class field
        //potential bug: local vars may share the same name with instance vars
        if(p1.result == null) {
          int pos = instance.getFPosition(instance.cur_class, p1.code);
          if(pos != -1) {
            t1 = unique();
            code += t1 + " = " + "[this+" + pos + "]\n";
          }
        }

        if(p2.result == null) {
          int pos = instance.getFPosition(instance.cur_class, p2.code);
          if(pos != -1) {
            t2 = unique();
            code += t2 + " = " + "[this+" + pos + "]\n";
          }
        }

        code += t + " = Add(" + (t1==null? (p1.result==null? p1.code: p1.result) : t1)
              + " " + (t2 == null? (p2.result==null? p2.code: p2.result) : t2) + ")\n";
        return new Pair(code, t);
    }

    @Override
    public Object visit(MinusExpression n, Object argu) {
        MethodTable instance = MethodTable.getInstance();
        Pair p1 = new Pair();
        Pair p2 = new Pair();
        p1 = (Pair)n.f0.accept(this,argu);
        p2 = (Pair)n.f2.accept(this,argu);

        String code = "";
        code += (p1.result == null ? "" : p1.code);
        code += (p2.result == null ? "" : p2.code);

        //if p.result is null, then probably this is a varaible
        //get its address
        String t = unique();
        String t1 = null;
        String t2 = null;
        //check if variable is a class field
        //potential bug: local vars may share the same name with instance vars
        if(p1.result == null) {
          int pos = instance.getFPosition(instance.cur_class, p1.code);
          if(pos != -1) {
            t1 = unique();
            code += t1 + " = " + "[this+" + pos + "]\n";
          }
        }

        if(p2.result == null) {
          int pos = instance.getFPosition(instance.cur_class, p2.code);
          if(pos != -1) {
            t2 = unique();
            code += t2 + " = " + "[this+" + pos + "]\n";
          }
        }

        code += t + " = Sub(" + (t1==null? (p1.result==null? p1.code: p1.result) : t1)
              + " " + (t2 == null? (p2.result==null? p2.code: p2.result) : t2) + ")\n";
        return new Pair(code, t);
        //return super.visit(n, argu);
    }

    @Override
    public Object visit(TimesExpression n, Object argu) {
        MethodTable instance = MethodTable.getInstance();
        Pair p1 = new Pair();
        Pair p2 = new Pair();

        p1 = (Pair)n.f0.accept(this,argu);
        p2 = (Pair)n.f2.accept(this,argu);

        //append previous statements
        String code = "";
        code += (p1.result == null ? "" : p1.code);
        code += (p2.result == null ? "" : p2.code);

        String t = unique();
        String t1 = null;
        String t2 = null;
        //check if variable is a class field
        //potential bug: local vars may share the same name with instance vars
        if(p1.result == null) {
          int pos = instance.getFPosition(instance.cur_class, p1.code);
          if(pos != -1) {
            t1 = unique();
            //left = "[this+" + pos + "]";
            code += t1 + " = " + "[this+" + pos + "]\n";
          }
        }

        if(p2.result == null) {
          int pos = instance.getFPosition(instance.cur_class, p2.code);
          if(pos != -1) {
            t2 = unique();
            //right = "[this+" + pos + "]";
            code += t2 + " = " + "[this+" + pos + "]\n";
          }
        }

        code += t + " = MulS(" + (t1==null? (p1.result==null? p1.code: p1.result) : t1)
              + " " + (t2 == null? (p2.result==null? p2.code: p2.result) : t2) + ")\n";

        return new Pair(code, t);
        //return super.visit(n, argu);
    }

    @Override
    public Object visit(ArrayLookup n, Object argu) {
        MethodTable instance = MethodTable.getInstance();
        Pair array = new Pair();
        Pair index = new Pair();
        array = (Pair)n.f0.accept(this,argu);
        index = (Pair)n.f2.accept(this,argu);


        //check the global array table that is used to store local array
        //then check if the array is in the class field, if not then
        //////////////////////////////////////////////////////////////////
        String base = Arrays.get(instance.cur_method + "." + array.code);
        int pos = -1;
        if (base == null)
          pos = instance.getFPosition(instance.cur_class, array.code);
        //////////////////////////////////////////////////////////////////


        //get base address
        String len = unique();

        //get array length, used for bound checking and null checking
        String load = len + " = "  + (pos != -1? ("[this+" + pos + "]") : base) + "\n";
        String code = "if0 " + len + " goto :" + getNullLabel(len) + "\n";
        code += index.result == null ? "" : index.code; // in case index is an expression

        //////////////////////////////////////////////////////////////////
        //get [this+size] format for index if index is a class field
        String t1 = null;
        if(index.result == null) {
          int position = instance.getFPosition(instance.cur_class, index.code);
          if(position != -1) {
            t1 = unique();
            code += t1 + " = " + "[this+" + position + "]\n";
          }
        }
        //////////////////////////////////////////////////////////////////

        //load base address/register
        //which is just the length of the array
        //check if the index is less the array length
        String temp = unique();
        code += temp + " = " + "[" + len + "]\n";
        code += temp + " = LtS(" + (t1 == null? (index.result == null? index.code : index.result) : t1) + " " + temp + ")\n";

        ////////////////////////////////////////////////////////
        //check if the index >= 0
        //implement greate than and equal to logic using
        //Eq and LtS built-in operator in vapor
        String Lts = unique();
        code += Lts + " = LtS(0 " + (t1 == null? (index.result == null? index.code : index.result) : t1) + ")\n";
        String Eq = unique();
        code += Eq + " = Eq(0 " + (t1 == null? (index.result == null? index.code : index.result) : t1) + ")\n";
        code += Eq + " = Add(" + Lts + " " + Eq + ")\n";
        code += temp + " = MulS(" + temp + " " + Eq + ")\n";
        ////////////////////////////////////////////////////////

        //array lookup
        String if_ = "if0 " + temp + " goto :" + getIfLabel(len) + "\n";
        //naive just for now, need to deal with expression later
        //calculate offset
        if_ += temp + " = MulS(" + (t1 == null? (index.result == null? index.code : index.result) : t1) + " 4)\n";
        //add offset to base to get the desired element
        if_ += temp + " = " + "Add(" + temp + " " + (base==null? len :base) + ")\n";
        String t2 = unique();
        //retrieve the element, "+4" because the 0 index is used to store length
        if_ += t2 + " = " +  "[" + temp + "+" + 4 + "]\n";
        if_ += "goto :" + getEndLabel(len) + "\n";
        if_ = format(if_);

        //append array look
        code += if_;

        String checkBound = getIfLabel(len) + ":\n";
        checkBound += "  Error(\"array index out of bounds\")\n";
        checkBound += getEndLabel(len) + ":\n";

        //append bound check end label, as well jump null label
        code += checkBound;
        code += "goto :" + getNullEnd(len) + "\n";

        //indent the block one more time
        code = format(code);
        code = load + code;

        //append null check end label
        //String end  = "goto :" + getNullEnd(len) + "\n";
        String end = getNullLabel(len) + ":\n";
        end += "  Error(\"null pointer\")\n";
        end += getNullEnd(len) + ":\n";
        code += end;

        return new Pair(code, t2);
        //return super.visit(n, argu);
    }

    @Override
    public Object visit(ArrayLength n, Object argu) {
        MethodTable instance = MethodTable.getInstance();
        //first lookup in the local array table
        Pair p = new Pair();
        String t = unique();
        p = (Pair)n.f0.accept(this,argu);
        String reg = Arrays.get(instance.cur_method + "." + p.code);
        String code = "";

        //if it's not a local array, then it must be
        //a class filed level array, act accordingly
        if (reg == null) {
          ///////////////////////////////////////////////////
          //first check if the var is in local
          //if so, get its type and go to that type class and get its position
          int pos;
          if(p.result == null) {
            String type = instance.getLocalType(instance.cur_class + "." + instance.cur_method, p.code);
            if(type != null)
              pos = instance.getFPosition(type, p.code);
            //if not, check current class field
            else
              pos = instance.getFPosition(instance.cur_class, p.code);

            if(pos != -1) {
              code += t + " = " + "[this+" + pos + "]\n";
            }
          }
          //////////////////////////////////////////////////
        }

        else {
          code += t + " = " + "[" + reg+ "]\n";
        }

        return new Pair(code, t);
        //return super.visit(n, argu);
    }

    @Override
    public Object visit(MessageSend n, Object argu) {
        MethodTable instance = MethodTable.getInstance();

        //get args name
        String func_arg = "";
        String arg_stack = "";
        Pair p1 = new Pair();
        //check if arg is null
        if (n.f4.node != null) {
          ExpressionList EL = (ExpressionList)n.f4.node;
          p1 = (Pair)EL.f0.accept(this,argu);
          arg_stack += (p1.result == null? "" : p1.code + "\n");

          /*
          ///////////////////////////////////////////////////
          //first check if the var is in local
          //if so, get its type and go to that type class and get its position
          String t1 = null;
          if(p1.result == null) {
            String type = instance.getLocalType(instance.cur_class + "." + instance.cur_method, p1.code);
            int pos;
            if(type != null)
              pos = instance.getFPosition(type, p1.code);
            //if not, check current class field
            else
              pos = instance.getFPosition(instance.cur_class, p1.code);

            if(pos != -1) {
              t1 = unique();
              arg_stack += t1 + " = " + "[this+" + pos + "]\n";
            }
          }
          //////////////////////////////////////////////////
          */

          //handle [this+size] for arguments
          String t1 = null;
          if(p1.result == null) {
            int pos = instance.getFPosition(instance.cur_class, p1.code);
            if(pos != -1) {
              t1 = unique();
              arg_stack += t1 + " = " + "[this+" + pos + "]\n";
            }
          }

          func_arg += (t1==null? (p1.result == null? p1.code : p1.result) : t1) + " ";

          Vector<Node> vec = EL.f1.nodes;
          Iterator it = vec.iterator();
          while(it.hasNext()) {
            ExpressionRest args = (ExpressionRest)it.next();
            p1 = (Pair)args.f1.accept(this,argu);
            arg_stack += (p1.result == null? "" : p1.code + "\n");

            /*
            ///////////////////////////////////////////////////
            //first check if the var is in local
            //if so, get its type and go to that type class and get its position
            String t2 = null;
            if(p1.result == null) {
              String type = instance.getLocalType(instance.cur_class + "." + instance.cur_method, p1.code);
              int pos;
              if(type != null)
                pos = instance.getFPosition(type, p1.code);
              //if not, check current class field
              else
                pos = instance.getFPosition(instance.cur_class, p1.code);

              if(pos != -1) {
                t2 = unique();
                arg_stack += t2 + " = " + "[this+" + pos + "]\n";
              }
            }
            //////////////////////////////////////////////////
            */

            String t2 = null;
            if(p1.result == null) {
              int pos = instance.getFPosition(instance.cur_class, p1.code);
              if(pos != -1) {
                t2 = unique();
                arg_stack += t2 + " = " + "[this+" + pos + "]\n";
              }
            }


            func_arg += (t2==null? (p1.result == null? p1.code : p1.result) : t2) + " ";
          }
        }

        //caller either from 'new exp' or not
        String t2 = unique();
        Pair p2 = new Pair();
        p2 = (Pair)n.f0.accept(this,argu);

        //get callee address/position
        Pair p3 = new Pair();
        p3 = (Pair)n.f2.accept(this,argu);
        String cur_class = null;
        if(p2.code.indexOf("HeapAllocZ") != -1)
          cur_class = instance.new_class;
        else {
          cur_class = instance.cur_class;
        }

        int pos = instance.getMPosition(cur_class, p3.code);
        //if pos == -1, probably because caller is not defined
        //in the current class, check one more time using types hash
        if(pos == -1) {
          String cur_method = instance.cur_method;
          //format: "cur_class.cur_method.caller"
          //check if this callee is in local method level
          String caller = cur_class + "." + cur_method + "." + p2.code;
          String Obj_type = instance.getType(caller);

          /////////////////////////
          //if obj_type is null, probably because the var is
          //defined at the class field level, format: "cur_class.caller"
          if(Obj_type == null) {
            caller = cur_class + "." + p2.code;
            Obj_type = instance.getType(caller);
          }

          //if still null, probably because it's defined in its super class
          //check super class fields, and retrieve the name and type, then get its pos
          if(Obj_type == null) {
            List<String> fields = instance.fields.get(cur_class);
            for(int i = 0; i < fields.size(); i++) {
              int index = fields.get(i).indexOf(".");
              String var = fields.get(i).substring(index+1);
              if(var.equals(p2.code)) {
                caller = fields.get(i);
                break;
              }
            }
            Obj_type = instance.getType(caller);
          }

          pos = instance.getMPosition(Obj_type, p3.code);
          /////////////////////////
        }

        //load load call
        String body = "";
        String func_setup = "";
        String func_end = "";
        String load_func = "";
        String func_call = "";
        String t0 = unique();
        if (p2.code.equals("this")) {
          load_func = t0 + " = " + "[this] \n";
          load_func += t0 + " = " + "[" + t0 + "+" + pos + "] \n";

          //check if arg is null
          if(n.f4.node == null)
            func_call = t2 + " = " + "call " + t0 + "(" + p2.code + ")\n";
          else
            func_call = t2 + " = " + "call " + t0 + "(" + p2.code + " " + func_arg + ")\n";

          body = load_func + arg_stack + func_call;
        }
        else {
          String t1 = unique();
          String t3 = unique();
          func_setup = (p2.result == null? "" : p2.code);

          /*
          ///////////////////////////////////////////////////
          //first check if the var is in local
          //if so, get its type and go to that type class and get its position
          String t4 = null;
          if(p2.result == null) {
            String type = instance.getLocalType(instance.cur_class + "." + instance.cur_method, p2.code);
            int position;
            if(type != null)
              position = instance.getFPosition(type, p2.code);
            //if not, check current class field
            else
              position = instance.getFPosition(instance.cur_class, p2.code);

            if(position != -1) {
              t4 = unique();
              func_setup += t4 + " = " + "[this+" + position + "]\n";
            }
          }
          //////////////////////////////////////////////////
          */

          //handle [this+size] for IfStatement
          String t4 = null;
          if(p2.result == null) {
            int position = instance.getFPosition(instance.cur_class, p2.code);
            if(position != -1) {
              t4 = unique();
              func_setup += t4 + " = " + "[this+" + position + "]\n";
            }
          }

          func_setup += "if0 " + (t4 == null?(p2.result==null? p2.code : p2.result): t4) + " goto :" + getNullLabel(t1);

          //func_end = "goto :" + getEndLabel(t3) + "\n";
          func_end += getNullLabel(t1) + ":\n";
          func_end += "  Error(\"null pointer\")\n";
          func_end += getEndLabel(t3) + ":\n";

          String t = unique();
          //get table
          load_func = "\n" + t + " = " + "[" + (t4 == null?(p2.result==null? p2.code : p2.result): t4) + "]\n";
          //get func
          load_func += t + " = " + "[" + t + "+" + pos + "]\n";

          //double check if arg is null
          //func call
          if(n.f4.node == null)
            func_call = t2 + " = " + "call " + t + "(" + (t4 == null?(p2.result==null? p2.code : p2.result): t4) + ")\n";
          else
            func_call = t2 + " = " + "call " + t + "(" + (t4 == null?(p2.result==null? p2.code : p2.result): t4) + " " + func_arg + ")\n";

          func_call += "goto :" + getEndLabel(t3) + "\n";

          //format
          body = load_func + arg_stack + func_call;
          body = format(body);
        }

        String code = func_setup + body + func_end;
        return new Pair(code, t2);
    }

    @Override
    public Object visit(ExpressionList n, Object argu) {
        return n.f0.accept(this,argu);
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
        return new Pair(n.f0.tokenImage, null);
    }

    @Override
    public Object visit(TrueLiteral n, Object argu) {
        return new Pair("1", null);
    }

    @Override
    public Object visit(FalseLiteral n, Object argu) {
        return new Pair("0", null);
    }

    @Override
    public Object visit(Identifier n, Object argu) {
        return new Pair(n.f0.tokenImage, null);
    }

    @Override
    public Object visit(ThisExpression n, Object argu) {
        //return new ThisExpression();
        return new Pair(n.f0.tokenImage, null);
    }

    @Override
    public Object visit(ArrayAllocationExpression n, Object argu) {
        MethodTable instance = MethodTable.getInstance();
        Pair p = new Pair();
        p = (Pair)n.f3.accept(this,argu);
        String t = unique();
        String code = (p.result == null ? "" : p.code);

        /*
        ///////////////////////////////////////////////////
        //first check if the var is in local
        //if so, get its type and go to that type class and get its position
        String t1 = null;
        if(p.result == null) {
          String type = instance.getLocalType(instance.cur_class + "." + instance.cur_method, p.code);
          int position;
          if(type != null)
            position = instance.getFPosition(type, p.code);
          //if not, check current class field
          else
            position = instance.getFPosition(instance.cur_class, p.code);

          if(position != -1) {
            t1 = unique();
            code += t1 + " = " + "[this+" + position + "]\n";
          }
        }
        //////////////////////////////////////////////////
        */


        String t1 = null;
        if(p.result == null) {
          int pos = instance.getFPosition(instance.cur_class, p.code);
          if(pos != -1) {
            t1 = unique();
            code += t1 + " = " + "[this+" + pos + "]\n";
          }
        }


        ///////////////////////////////////////////
        //check if size > 0
        String negative = unique();
        code += negative + " = LtS(0 " + (t1 == null? (p.result == null ? p.code : p.result) : t1) + ")\n";
        code += "if0 " + negative + " goto :" + getIfLabel(negative) + "\n";
        ///////////////////////////////////////////

        code += "  " + t + " = call :AllocArray(" + (t1 == null? (p.result == null ? p.code : p.result) : t1) + ")\n";

        ///////////////////////////////////////////
        code += "goto :" + getEndLabel(negative) + "\n";
        code += getIfLabel(negative) + ":\n";
        code += "  Error(\"array index out of bounds\")\n";
        code += getEndLabel(negative) + ":\n";
        ///////////////////////////////////////////

        //reset the helper function, we dont want more than one exists
        arrAllocHelper = "";
        arrAllocHelper += "func AllocArray(size)\n  ";
        arrAllocHelper += "bytes = MulS(size 4)\n  ";
        arrAllocHelper += "bytes = Add(bytes 4)\n  ";
        arrAllocHelper += "v = HeapAllocZ(bytes)\n  ";
        arrAllocHelper += "[v] = size\n  ret v\n";
        return new Pair(code, t);
        //return super.visit(n, argu);
    }

    @Override
    public Object visit(AllocationExpression n, Object argu) {
        MethodTable instance = MethodTable.getInstance();
        Pair p = new Pair();
        p = (Pair)n.f1.accept(this,argu);
        int size = instance.getOSize(p.code);
        //allocate
        String t = unique();
        String code = t + " = " + "HeapAllocZ(" + size + ")\n";
        code += "[" + t + "]" + " = :vmt_" + p.code + "\n";

        //store the class name, used for retrieve obj size later
        instance.new_class = p.code;
        return new Pair(code, t);
        //return n.f1.accept(this, argu);
    }

    @Override
    public Object visit(NotExpression n, Object argu) {
        MethodTable instance = MethodTable.getInstance();
        Pair p = new Pair();
        p = (Pair)n.f1.accept(this,argu);
        String code = "";
        String t1 = unique();
        code += p.result == null? "" : p.code;

        ////////////////////////////////////////////////////////
        String t2 = null;
        if(p.result == null) {
          int pos = instance.getFPosition(instance.cur_class, p.code);
          if(pos != -1) {
            t2 = unique();
            code += t2 + " = " + "[this+" + pos + "]\n";
          }
        }
        ////////////////////////////////////////////////////////

        code += t1 + " = " + "Sub(1 " + (t2==null?(p.result == null? p.code : p.result):t2) + ")\n";
        return new Pair(code, t1);
    }

    @Override
    public Object visit(BracketExpression  n, Object argu) {
        MethodTable instance = MethodTable.getInstance();
        Pair p = new Pair();
        p = (Pair)n.f1.accept(this,argu);
        String code = "";

        /*
        ///////////////////////////////////////////////////
        //first check if the var is in local
        //if so, get its type and go to that type class and get its position
        String t = null;
        if(p.result == null) {
          String type = instance.getLocalType(instance.cur_class + "." + instance.cur_method, p.code);
          int position;
          if(type != null)
            position = instance.getFPosition(type, p.code);
          //if not, check current class field
          else
            position = instance.getFPosition(instance.cur_class, p.code);

          if(position != -1) {
            t = unique();
            p.code = t + " = " + "[this+" + position + "]\n";
            p.result = t;
          }
        }
        //////////////////////////////////////////////////
        */

        //be ware of complex expression
        //if p.result is null, then probably this is a varaible
        //get its address
        String t = "";
        if(p.result == null) {
          int pos = instance.getFPosition(instance.cur_class, p.code);
          if(pos != -1) {
            t = unique();
            p.code = t + " = " + "[this+" + pos + "]\n";
            p.result = t;
          }
        }


        return new Pair(p.code, p.result);
        //return n.f1.f0.choice.accept(this, argu);
    }
}
