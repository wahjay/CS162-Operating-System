import cs132.vapor.ast.*;
import cs132.vapor.parser.VaporParser;
import cs132.vapor.ast.VVarRef.*;
import java.util.*;
import java.io.*;

public class VMVisitor extends VInstr.VisitorPR {
  public static HashMap<Integer, String> labels = new HashMap<>();

  private String regReturn() {
    return "$v0";
  }

  private String tempReg() {
    return "$t9";
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
    //System.out.println("Assign");
    VMTable VMT = VMTable.getInstance();
    MethodTable MT = VMT.get(VMT.curMethod);
    String code = "";

    //a.dest is always a register
    //System.out.println(a.dest);
    //System.out.println(a.source);

    if(a.source instanceof VVarRef.Register) {
      code += "move " + a.dest + " " + a.source;
    }

    else if (a.source instanceof VLitInt) {
      code += "li " + a.dest + " " + a.source;
    }

    else if (a.source instanceof VLitStr) {
      code += "la " + a.dest + " " + a.source;
    }

    //System.out.println(code);
    return code;
  }

  @Override
  public Object visit(Object p, VCall c) throws RuntimeException {
    //System.out.println("Call");
    VMTable VMT = VMTable.getInstance();
    MethodTable MT = VMT.get(VMT.curMethod);
    String code = "";

    //must be func label then
    if(c.addr instanceof VAddr.Label) {
      String label = c.addr + "";
      code += "jal " + label.substring(label.indexOf(":")+1);
    }

    //must be a register then
    else if (c.addr instanceof VAddr.Var) {
      code += "jalr " + c.addr;
    }

    MT.reset_a();
    //code += "move " + (c.dest == null? c.addr : c.dest) + " " + regReturn();
    //System.out.println(c.addr);
    //System.out.println(c.dest);
    //System.out.println(code);
    return code;
  }

  @Override
  public Object visit(Object p, VBuiltIn c) throws RuntimeException {
    //System.out.println("Built In");
    VMTable VMT = VMTable.getInstance();
    MethodTable MT = VMT.get(VMT.curMethod);
    String code = "";

    if(c.op.name.indexOf("HeapAllocZ") != -1) {
      //get arguments
      //warn: arguments maybe > 4
      String args = "";
      for(VOperand arg : c.args) {
        String arg_ = arg + "";

        //check arg type, and act accordingly
        if(arg instanceof VVarRef.Register) {
          code += "move " + MT.regAlloc('a') + " " + arg_  + "\n";
        }

        else if (arg instanceof VLitInt) {
          code += "li " + MT.regAlloc('a') + " " + arg_  + "\n";
        }
      }

      code += "jal _heapAlloc" + "\n";
      //code += "move " + MT.regAlloc('t') + " " + regReturn();
      code += "move " + c.dest + " " + regReturn();
      MT.reset_a();
    }

    else if (c.op.name.indexOf("MulS") != -1) {
      //System.out.println(c.op.name);
      String args = "";
      int count = 1;
      for(VOperand arg : c.args) {
        //if the arg is an integer literal, need to
        //load it to a register, then do the operation
        if(arg instanceof VLitInt && count != c.args.length) {
          code += "li " + tempReg() + " " + arg + "\n";
          args += tempReg() + " ";
        }

        else
          args += arg + " ";

        count++;
      }

      args = args.substring(0, args.lastIndexOf(" "));
      code += "mul " + c.dest + " " + args;
    }

    else if (c.op.name.indexOf("Error") != -1) {
      code += "la " + MT.regAlloc('a') + " _str0\n";
      code += "j _error";
      MT.reset_a();
    }

    else if (c.op.name.indexOf("LtS") != -1) {
      //System.out.println(c.op.name);
      boolean litInt = false;
      String args = "";
      int count = 1;
      for(VOperand arg : c.args) {
        //if the integer literal is at the last position, use 'slti'
        if(arg instanceof VLitInt && count == c.args.length)
          litInt = true;

        //if not, use 'slt'
        if(arg instanceof VLitInt && count != c.args.length) {
          code += "li " + tempReg() + " " + arg + "\n";
          args += tempReg() + " ";
        }

        else
          args += arg + " ";

        count++;
      }

      args = args.substring(0, args.lastIndexOf(" "));
      code += (litInt ? "slti " : "slt ") + c.dest + " " + args;
    }

    else if (c.op.name.indexOf("Lt") != -1) {
      //System.out.println(c.op.name);
      boolean litInt = false;
      String args = "";
      int count = 1;
      for(VOperand arg : c.args) {
        //if the integer literal is at the last position, use 'slti'
        if(arg instanceof VLitInt && count == c.args.length)
          litInt = true;

        //if not, use 'sltu'
        if(arg instanceof VLitInt && count != c.args.length) {
          code += "li " + tempReg() + " " + arg + "\n";
          args += tempReg() + " ";
        }

        else
          args += arg + " ";

        count++;
      }

      args = args.substring(0, args.lastIndexOf(" "));
      code += (litInt ? "slti " : "sltu ") + c.dest + " " + args;
    }

    else if (c.op.name.indexOf("Sub") != -1) {
      //System.out.println(c.op.name);
      String args = "";
      int count = 1;
      for(VOperand arg : c.args) {
        //if the arg is an integer literal, need to
        //load it to a register, then do the operation
        //but we only do it for the last integer li
        if(arg instanceof VLitInt && count != c.args.length) {
          code += "li " + tempReg() + " " + arg + "\n";
          args += tempReg() + " ";
        }

        else
          args += arg + " ";

        count++;
      }

      args = args.substring(0, args.lastIndexOf(" "));
      code += "subu " + c.dest + " " + args;
    }

    else if (c.op.name.indexOf("PrintIntS") != -1) {
      VOperand arg = c.args[0];

      if(arg instanceof VVarRef.Register) {
        code += "move " + MT.regAlloc('a')+ " " + arg + "\n";
      }

      else if (arg instanceof VLitInt) {
        code += "li " + MT.regAlloc('a') + " " + arg + "\n";
      }

      code += "jal _print";
      MT.reset_a();
    }

    else if (c.op.name.indexOf("Add") != -1) {
      String args = "";
      int count = 1;
      for(VOperand arg : c.args) {
        //if the arg is an integer literal, need to
        //load it to a register, then do the operation
        if(arg instanceof VLitInt && count != c.args.length) {
          code += "li " + tempReg() + " " + arg + "\n";
          args += tempReg() + " ";
        }

        else
          args += arg + " ";

        count++;
      }

      args = args.substring(0, args.lastIndexOf(" "));
      code += "addu " + c.dest + " " + args;
    }

    else if (c.op.name.indexOf("And") != -1) {
      //System.out.println(c.op.name);
      boolean litInt = false;
      String args = "";
      int count = 1;
      for(VOperand arg : c.args) {
        if(arg instanceof VLitInt && count == c.args.length)
          litInt = true;

        if(arg instanceof VLitInt && count != c.args.length) {
          code += "li " + tempReg() + " " + arg + "\n";
          args += tempReg() + " ";
        }

        else
          args += arg + " ";

        count++;
      }

      args = args.substring(0, args.lastIndexOf(" "));
      code += (litInt ? "andi " : "and ") + c.dest + " " + args;
    }

    else if (c.op.name.indexOf("Or") != -1) {
      //System.out.println(c.op.name);
      boolean litInt = false;
      String args = "";
      int count = 1;
      for(VOperand arg : c.args) {
        if(arg instanceof VLitInt && count == c.args.length)
          litInt = true;

        if(arg instanceof VLitInt && count != c.args.length) {
          code += "li " + tempReg() + " " + arg + "\n";
          args += tempReg() + " ";
        }

        else
          args += arg + " ";

        count++;
      }

      args = args.substring(0, args.lastIndexOf(" "));
      code += (litInt ? "ori " : "or ") + c.dest + " " + args;
    }

    else if (c.op.name.indexOf("Eq") != -1) {
      //System.out.println(c.op.name);
      String args = "";
      for(VOperand arg : c.args) {
          args += arg + " ";
      }

      args = args.substring(0, args.lastIndexOf(" "));
      code += "seq " + c.dest + " " + args;
    }

    else if (c.op.name.indexOf("Not") != -1) {
      //System.out.println(c.op.name);
      String args = "";
      for(VOperand arg : c.args) {

          if(arg instanceof VLitInt) {
            code += "li " + tempReg() + " " + arg + "\n";
            args += tempReg() + " ";
          }

          else
            args += arg + " ";
      }

      args = args.substring(0, args.lastIndexOf(" "));
      code += "not " + c.dest + " " + args;
    }

    

    //System.out.println(code);
    return code;
  }

  @Override
  public Object visit(Object p, VMemWrite w) throws RuntimeException {
    //System.out.println("Memory Write");
    VMTable VMT = VMTable.getInstance();
    MethodTable MT = VMT.get(VMT.curMethod);
    String code = "";

    if(w.dest instanceof VMemRef.Global) {
      VMemRef.Global vg = (VMemRef.Global) w.dest;
      VAddr va = (VAddr)vg.base;
      String right = w.source + "";
      right = right.substring(right.indexOf(":")+1);

      //cast to VVarRef, just in case the lhs is a register
      VAddr.Var vv = (VAddr.Var)va;
      VVarRef vr = (VVarRef)vv.var;

      //we act accordingly depends on the type of the rhs
      if(w.source instanceof VVarRef.Register) {
        //code += "la " + reg + " 0(" + w.source + ")\n";
        code += "sw " + w.source + " " + vg.byteOffset + "(" + va.toString() + ")";
      }

      //rhs could be a label, like vmt_..
      else if (w.source instanceof VLabelRef) {
        code += "la " + tempReg() + " " + right + "\n";
        code += "sw " + tempReg() + " 0(" + va.toString() + ")";
      }

      //dest(left) could be a register
      else if (vr instanceof VVarRef.Register) {
        String source = null;
        if(w.source.toString().equals("0"))
          source = "$" + w.source.toString();
        else if(w.source.toString().matches("-?\\d+")){
          source = tempReg();
          code += "li " + tempReg() + " " + w.source + "\n";
        }

        //could be reg, 0, other nums
        code += "sw " + (source == null ? w.source.toString() : source) + " " + vg.byteOffset + "(" + va.toString() + ")";
      }

      //lhs could be a integer literal
      else if (w.source instanceof VLitInt) {
        code += "li " + va.toString() + " " + w.source + "";
      }
    }

    else if(w.dest instanceof VMemRef.Stack) {
      VMemRef.Stack vs = (VMemRef.Stack) w.dest;

      //source is a integer literal, need to load it
      //to a temporary register first before pushing on the stack
      if(w.source.toString().matches("-?\\d+")) {
        code += "li " + tempReg() + " " + w.source + "\n";
        code += "sw " + tempReg() + " " + (vs.index*4) + "($sp)";
      }

      else
        code += "sw " + w.source + " " + (vs.index*4) + "($sp)";
    }

    //System.out.println(code);
    return code;
  }

  @Override
  public Object visit(Object p, VMemRead r) throws RuntimeException {
    //System.out.println("Memory Read");
    VMTable VMT = VMTable.getInstance();
    MethodTable MT = VMT.get(VMT.curMethod);
    String code = "";
    String left = r.dest + "";

    //System.out.println(r.source.toString());
    //System.out.println(r.dest.toString());

    if(r.source instanceof VMemRef.Global) {
      VMemRef.Global gl = (VMemRef.Global) r.source;
      VAddr va = (VAddr)gl.base;

      //it must be register then
      if(va instanceof VAddr.Var) {
        //VAddr.Var vv = (VAddr.Var)va;
        //VVarRef.Register vr = (VVarRef.Register)vv.var;
        code += "lw " + left + " " + gl.byteOffset + "(" + va.toString() + ")";
      }

      else if(va instanceof VAddr.Label) {
        String target = va.toString();
        code += "la " + left + " " + target.substring(target.indexOf(":")+1);
      }

      //code += "lw " + left + " " + gl.byteOffset + "(" + va.toString() + ")";
    }

    else if (r.source instanceof VMemRef.Stack) {
      VMemRef.Stack vs = (VMemRef.Stack) r.source;
      //callee saved regs should be restored from the stack pointer (staic at callee side)
      //callee saved regs should be restored from the frame pointer (staic at caller side)
      code += "lw " + left + " " + (vs.index*4) + (left.indexOf("$t") != -1 ? "($fp)" : "($sp)");
    }

    //System.out.println(code);
    return code;
  }

  @Override
  public Object visit(Object p, VBranch b) throws RuntimeException {
    //System.out.println("Branch");
    VMTable VMT = VMTable.getInstance();
    MethodTable MT = VMT.get(VMT.curMethod);
    String code = "";
    String label = b.target + "";

    if(!b.positive)
      code += "beqz " + b.value + " " + label.substring(label.indexOf(":")+1);
    else
      code += "bnez " + b.value + " " + label.substring(label.indexOf(":")+1);

    //System.out.println(code);
    return code;
  }

  @Override
  public Object visit(Object p, VGoto g) throws RuntimeException {
    //System.out.println("Goto");
    String label = g.target + "";
    String code = "";
    code += "j " + label.substring(label.indexOf(":")+1);
    //System.out.println(code);
    return code;
  }

  @Override
  public Object visit(Object p, VReturn r) throws RuntimeException {
    //System.out.println("Return");
    VMTable VMT = VMTable.getInstance();
    MethodTable MT = VMT.get(VMT.curMethod);
    String code = null;
    //System.out.println(r.value);

    if(r.value != null) {
        if(r.value instanceof VVarRef.Register) {
          code += "move " + regReturn() + " " + r.value;
        }

        else if (r.value instanceof VLitInt) {
          code += "li " + regReturn() + " " + r.value;
        }
    }

    //code += "jr $ra";
    //System.out.println(code);
    return code;
  }
}
