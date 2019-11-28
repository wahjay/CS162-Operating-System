import cs132.vapor.ast.*;
import cs132.vapor.parser.VaporParser;
import cs132.vapor.ast.VVarRef.*;
import java.util.*;
import java.io.*;

public class VMVisitor extends VInstr.VisitorPR {

  public static String label = null;
  public static int line = 0;
  public static HashMap<Integer, String> labels = new HashMap<>();

  public static void reset() {
    label = null;
    line = 0;
  }

  private String regReturn() {
    return "$v0";
  }

  public static void getNextLabel(int key) {
    if(labels.get(key+1) != null) {
      label = labels.get(key+1);
      line++;
    }

    else
      reset();
  }

  //get label pos
  public static void placeLabel(Object obj){
    //place label
    String pos = null;

    if(obj instanceof VAssign)
      pos = ((VAssign)obj).sourcePos + "";
    else if(obj instanceof VCall)
      pos = ((VCall)obj).sourcePos + "";
    else if(obj instanceof VBuiltIn)
      pos = ((VBuiltIn)obj).sourcePos + "";
    else if(obj instanceof VGoto)
      pos = ((VGoto)obj).sourcePos + "";
    else if(obj instanceof VMemWrite)
      pos = ((VMemWrite)obj).sourcePos + "";
    else if(obj instanceof VMemRead)
      pos = ((VMemRead)obj).sourcePos + "";
    else if(obj instanceof VReturn)
      pos = ((VReturn)obj).sourcePos + "";
    else if(obj instanceof VBranch)
      pos = ((VBranch)obj).sourcePos + "";

    pos = pos.substring(0, pos.indexOf("."));
    int key = Integer.parseInt(pos);
    line = key + 1;
    if(labels.get(key+1) != null) {
      label = labels.get(key+1);
    }
  }

  //indent block format
  public static String format(String C) {
    String a = "\n";    //separator
    String b = "\n  ";  //replacement
    String result = C.substring(0,C.lastIndexOf(a)).replaceAll(a , b).concat(C.substring(C.lastIndexOf(a)));
    result = result.replaceAll("\n  $", "");
    return result;
  }

  //overridden methods
  @Override
  public Object visit(Object p, VAssign a) throws RuntimeException {
    VMTable VMT = VMTable.getInstance();
    MethodTable MT = VMT.get(VMT.curMethod);
    Pair p1 = new Pair();
    p1.code = "";

    //place label
    placeLabel(a);

    String right = MT.get(a.source+"") != null ? MT.get(a.source+"") : a.source + "";

    //check if left dest(left) is local
    if(a.dest instanceof Local) {

      if(MT.get(a.dest.toString()) != null) {
        String left = MT.get(a.dest.toString());

        if(right.indexOf("local") != -1) {
          String reg = MT.regAlloc('t');
          p1.code += reg + " = " + right + "\n";
          p1.code += left + " = " + reg;
          MT.incre_t(reg);
        }

        else {
          p1.code += left + " = " + right;
        }

        p1.result = left;
      }

      else {
        String reg = MT.regAlloc('t');
        p1.code = reg + " = " + a.source;
        p1.result = reg;
      }

      //MT.insert(a.dest.toString(), p1.result);
    }

    else if (a.dest instanceof Register) {
      p1.code = "[" + a.dest + "] = " + a.source;
    }

    //System.out.println(a.dest.toString() + " " + p1.result);
    //System.out.println("~~~~~~");
    //System.out.println(a.dest.toString() + " " + a.source);
    //System.out.println(p1.code + " " + a.sourcePos);
    //System.out.println(p1.code);
    return p1;
  }

  @Override
  public Object visit(Object p, VCall c) throws RuntimeException {
    //System.out.println("Call");
    VMTable VMT = VMTable.getInstance();
    MethodTable MT = VMT.get(VMT.curMethod);

    //place label
    placeLabel(c);

    Pair p1 = new Pair();
    String args = "";
    String ret = null;
    String caller = null;

    if(MT.get(c.addr.toString()) != null)
      caller = MT.get(c.addr.toString());

    else if (c.addr instanceof VAddr.Label) {
      caller = c.addr.toString();
    }

    else if (c.addr instanceof VAddr.Var) {
      //nested cast
      VAddr.Var va_var = (VAddr.Var)c.addr;
      VVarRef vr = (VVarRef)va_var.var;
      if(vr instanceof Register)
        caller = vr.toString();

      else if (vr instanceof Local) {
        caller = MT.regAlloc('t');
        MT.insert(vr.toString(), caller);
      }
    }

    else if (p!=null)
      caller = (String)p;

    int count = 0;
    for(VOperand arg : c.args) {
      String arg_ = arg + "";
      if(count < 4) {
        args += MT.regAlloc('a') + " = " + (MT.get(arg_) == null ? arg : MT.get(arg_)) + "\n";
      }

      else {
        String key = "out[" + (count - 4) + "]";
        String val = MT.get(arg_) == null ? arg + "" : MT.get(arg_);

        if(val.indexOf("local") != -1) {
          String reg = MT.regAlloc('t');
          args += reg + " = " + val + "\n";
          args += key + " = " + reg + "\n";
          MT.incre_t(reg);
        }

        else
          args += key + " = " + val + "\n";

        MT.insert(key, val);
      }

      count++;
    }

    p1.code = args;
    p1.result = caller;

    if(caller.indexOf("local") != -1) {
      String reg = MT.regAlloc('t');
      p1.code += reg + " = " + caller + "\n";
      p1.code += "call " + reg + "\n";
      MT.incre_t(reg);
    }

    else {
      p1.code += "call " + caller + "\n";
    }

    //if its a label, like :AllocArray
    if(c.addr instanceof VAddr.Label) {
      String reg = MT.regAlloc('t');
      p1.result = reg;
      ret = reg+ " = " + regReturn();
      MT.insert(c.dest.toString(), reg);
    }

    else {
      p1.result = caller;
      String ret_val = MT.get(c.dest.toString());
      ret = ret_val == null ? caller :ret_val + " = " + regReturn();
    }


    p1.code += ret;
    //since after this call, the previous allocated
    //argument registers are freed, we could reset the counter
    MT.reset_a();
    //System.out.println(p1.code + " " + c.sourcePos);
    return p1;
  }

  @Override
  public Object visit(Object p, VBuiltIn c) throws RuntimeException {
    //System.out.println("Built In");
    VMTable VMT = VMTable.getInstance();
    MethodTable MT = VMT.get(VMT.curMethod);

    Pair p1 = new Pair();
    p1.code = "";

    //get builtin function
    String Op = c.op.name;

    //place label
    placeLabel(c);

    //get arguments
    String args = "";
    for(VOperand arg : c.args) {
      String arg_ = arg + "";
      String value = (MT.get(arg_) == null ? arg +"" : MT.get(arg_));

      if(value.indexOf("local") != -1) {
        String reg = MT.regAlloc('t');
        p1.code += reg + " = " + value + "\n";
        args += reg + " ";
        MT.incre_t(reg);
      }

      else
        args += value + " ";
    }

    //strip the last space
    args = args.substring(0, args.lastIndexOf(" "));

    if(c.dest != null && MT.get(c.dest.toString()) != null)
      p1.result = MT.get(c.dest.toString());

    else if(c.dest != null && c.dest instanceof Local) {
      p1.result = MT.regAlloc('t');
      MT.insert(c.dest.toString(), p1.result);
    }

    else if(c.dest != null && c.dest instanceof Register)
      p1.result = c.dest.toString();

    //combine
    if(p1.result != null && p1.result.indexOf("local") != -1) {
        String reg = MT.regAlloc('t');
        p1.code += reg + " = " + Op + "(" + args + ")\n";
        p1.code += p1.result + " = " + reg;
        MT.incre_t(reg);
    }

    else {
      p1.code += (p1.result==null? "" : p1.result + " = ") + Op + "(" + args + ")";
    }
    
    //System.out.println(p1.code + " " + c.sourcePos);
    //System.out.println(p1.code);
    //System.out.println("~~~~~~");
    //System.out.println(p1.code);
    return p1;
  }

  @Override
  public Object visit(Object p, VMemWrite w) throws RuntimeException {
    //System.out.println("Memory Write");
    //System.out.println((String)p);
    VMTable VMT = VMTable.getInstance();
    MethodTable MT = VMT.get(VMT.curMethod);

    //place label
    placeLabel(w);

    Pair p1 = new Pair();
    String left = p == null ? "" : (String)p;
    String right = w.source + "";
    int offset = 0;

    //RHS breakdown
    if(MT.get(w.source.toString()) != null)
      right = MT.get(w.source.toString());
    else
      right = w.source.toString();

    //LHS breakdown
    if(w.dest instanceof VMemRef.Global) {
      VMemRef.Global gl = (VMemRef.Global)w.dest;
      offset = gl.byteOffset;

      if (MT.get(gl.base.toString()) != null)
        left = MT.get(gl.base.toString());

      else
        left = gl.base.toString();
    }

    else if(w.dest instanceof VMemRef.Stack) {
      VMemRef.Stack st = (VMemRef.Stack)w.dest;
      System.out.println(st.region);
      System.out.println(st.index);
    }


    //check if lhs is local var
    if(left.indexOf("local") != -1) {
      String reg = MT.regAlloc('t');
      p1.code = reg + " = " + left + "\n";

      //need to check rhs, too
      if(right.indexOf("local") != -1) {
        String reg2 = MT.regAlloc('t');
        p1.code += reg2 + " = " + right + "\n";
        p1.code += "[" + reg + (offset == 0 ? "" : "+" + offset) + "] = " + reg2;
        MT.incre_t(reg2);
      }

      else
        p1.code += "[" + reg + (offset == 0 ? "" : "+" + offset) + "] = " + right;

      p1.result = reg;
      MT.incre_t(reg);  //put the reg back to the regAlloc
    }

    else {
      //if lhs is just a reg, need to check if rhs is local var
      if(right.indexOf("local") != -1) {
        String reg = MT.regAlloc('t');
        p1.code = reg + " = " + right + "\n";
        p1.code += "[" + left + (offset == 0 ? "" : "+" + offset) + "] = " + reg;
        MT.incre_t(reg);
      }

      else
        p1.code = "[" + left + (offset == 0 ? "" : "+" + offset) + "] = " + right;

      p1.result = left;
    }

    //System.out.println("write: " + p1.code);
    //System.out.println("~~~~~~~");
    //System.out.println(p1.code + " " + w.sourcePos);
    return p1;
  }

  @Override
  public Object visit(Object p, VMemRead r) throws RuntimeException {
    //System.out.println("Memory Read");

    //place label
    placeLabel(r);

    VMTable VMT = VMTable.getInstance();
    MethodTable MT = VMT.get(VMT.curMethod);

    Pair p1 = new Pair();
    String left = p == null ? "" : (String)p;
    String right = "";
    int offset = 0;

    //LHS breakdown
    if(MT.get(r.dest.toString()) != null)
      left = MT.get(r.dest.toString());

    else if(r.dest instanceof VVarRef.Local) {
      left = MT.regAlloc('t');
      MT.insert(r.dest.toString(), left);
    }

    else if(r.dest instanceof VVarRef.Register) {
      left = r.dest.toString();
    }

    //RHS breakdown
    if(r.source instanceof VMemRef.Global) {
      VMemRef.Global gl = (VMemRef.Global)r.source;
      offset = gl.byteOffset;

      if (MT.get(gl.base.toString()) != null)
        right = MT.get(gl.base.toString());

      else if(p != null)
        right = (String)p;

      else
        right = gl.base.toString();
    }

    else if (r.source instanceof VMemRef.Stack) {
      System.out.println("stack");
      VMemRef.Stack st = (VMemRef.Stack)r.source;
      System.out.println(st.region);
      System.out.println(st.index);
    }


    if(right.indexOf("local") != -1) {
      String reg = MT.regAlloc('t');
      p1.code = reg + " = " + right + "\n";


      if(offset == 0) {
        p1.code += reg + " = ["+ reg + "]\n";
        p1.code += left + " = " + reg;
      }

      else {
        p1.code += reg + " = " + "[" + reg + (offset == 0 ? "" : "+" + offset) + "]\n";
        p1.code += left + " = " + reg;
      }

      //p1.code += left + " = [" + reg + (offset == 0 ? "" : "+" + offset) + "]";
      p1.result = left;
      MT.incre_t(reg);
    }

    else {
      if(left.indexOf("local") != -1) {
        if(offset == 0) {
          String reg = MT.regAlloc('t');
          p1.code = reg + " = [" + right + "]\n";
          p1.code += left + " = " + reg;
          MT.incre_t(reg);
        }

        else {
          p1.code = right + " = " + "[" + right + (offset == 0 ? "" : "+" + offset) + "]\n";
          p1.code += left + " = " + right;
        }

      }

      else {
        p1.code = left + " = [" + right + (offset == 0 ? "" : "+" + offset) + "]";
      }

      p1.result = left;
    }

    //System.out.println("read: " + p1.code);
    //System.out.println("~~~");
    return p1;
  }

  @Override
  public Object visit(Object p, VBranch b) throws RuntimeException {
    //System.out.println("Branch");
    VMTable VMT = VMTable.getInstance();
    MethodTable MT = VMT.get(VMT.curMethod);

    //place label
    placeLabel(b);

    Pair p1 = new Pair();
    p1.code = "";

    String branch = (String)p;
    String reg = null;

    if (MT.get(b.value.toString()) != null) {
      reg = MT.regAlloc('t');
      p1.code += reg + " = " + MT.get(b.value.toString()) + "\n";
      MT.incre_t(reg);
    }

    else if (branch != null && branch.indexOf("local") != -1) {
      reg = MT.regAlloc('t');
      p1.code += reg + " = " + branch + "\n";
      MT.incre_t(reg);
    }

    //if branch
    if(b.positive) {
      p1.code += "if " + (reg == null? branch : reg) + " goto " + b.target.toString();
      //p1.result = b.target.ident + ":";
    }

    //if0 branch
    else {
      p1.code += "if0 " + (reg == null? branch : reg) + " goto " + b.target.toString();
      //p1.result = b.target.ident + ":";
    }

    //System.out.println(MT.get(b.value.toString()) + " " + b.value.toString());
    //System.out.println((String)p);
    //System.out.println("~~~~~");
    //System.out.println(b.sourcePos + " " + p1.code);
    return p1;
  }

  @Override
  public Object visit(Object p, VGoto g) throws RuntimeException {
    //System.out.println("Goto");
    placeLabel(g);

    Pair p1 = new Pair();
    String label = g.target + "";
    p1.code = "goto " + g.target;
    //p1.code = label.substring(label.indexOf(":")+1) + ":";
    //System.out.println(g.target);

    //System.out.println(p1.code + " " + g.sourcePos);
    return p1;
  }

  @Override
  public Object visit(Object p, VReturn r) throws RuntimeException {
    //System.out.println("Return");
    placeLabel(r);

    VMTable VMT = VMTable.getInstance();
    MethodTable MT = VMT.get(VMT.curMethod);
    Pair p1 = new Pair();

    if(r.value == null) {
      p1.code = "ret";
    }

    else {
      p1.code = regReturn() + " = " + (MT.get(r.value+"") == null ? r.value : MT.get(r.value+"")) + "\n";
      p1.code += "ret";
    }

    //System.out.println(p1.code + " " + r.sourcePos);
    return p1;
  }

}
