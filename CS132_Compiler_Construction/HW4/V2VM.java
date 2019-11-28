import cs132.util.ProblemException;
import cs132.vapor.ast.*;
import cs132.vapor.ast.VInstr.VisitorPR;
import cs132.vapor.ast.VaporProgram;
import cs132.vapor.parser.VaporParser;
import cs132.vapor.ast.VBuiltIn.Op;
import java.util.*;
import java.io.*;

public class V2VM {

  public static String fun;
  public static VaporProgram parseVapor(InputStream in, PrintStream err) throws IOException {
    Op[] ops = {
      Op.Add, Op.Sub, Op.MulS, Op.Eq, Op.Lt, Op.LtS,
      Op.PrintIntS, Op.HeapAllocZ, Op.Error,
    };
    boolean allowLocals = true;
    String[] registers = null;
    boolean allowStack = false;

    VaporProgram program;
    try {
      program = VaporParser.run(new InputStreamReader(in), 1, 1,
                                java.util.Arrays.asList(ops),
                                allowLocals, registers, allowStack);
    }
    catch (ProblemException ex) {
      err.println(ex.getMessage());
      return null;
    }

    return program;
  }

  public static int getInt(String s) {
    s = s.substring(0, s.indexOf("."));
    return Integer.parseInt(s);
  }


  public static void main (String [] args) throws IOException {
    InputStream in = System.in;
    PrintStream cout = new PrintStream(System.out);
    VaporProgram program = parseVapor(in, cout);

    String code = "";
    String globalConst = "";
    Pair p = new Pair();
    VisitorPR VR = new VMVisitor();

    //get # out for all the functions
    int out = 0;
    for(VFunction func : program.functions) {
      if(func.params.length > out)
        out = func.params.length;
    }

    //class and method labels at the top
    for(VDataSegment data : program.dataSegments) {
      String classLabel = "const " + data.ident + "\n";
      for(VOperand.Static operand : data.values) {
        classLabel += operand + "\n";
      }

      classLabel = new VMVisitor().format(classLabel);
      globalConst += classLabel;
    }

    for(VFunction func : program.functions) {
      VMTable VMT = VMTable.getInstance();
      VMT.curMethod = func.ident;
      MethodTable MT = VMT.get(VMT.curMethod);

      //map parameters to $a# regs
      //if params > 4, map the rest to out[]
      int count = 0;
      for(VVarRef.Local param : func.params) {
        //if(count < 4)
        MT.insert(param + "", "$a"+count);
        count++;
      }

      for(VCodeLabel vl : func.labels){
        String pos = vl.sourcePos + "";
        pos = pos.substring(0, pos.indexOf("."));
        int key = Integer.parseInt(pos);
        new VMVisitor().labels.put(key, vl.ident + ":");
      }

      //HashMap<Integer, String> foo = new HashMap<>(new VMVisitor().labels);
      //for(Map.Entry<Integer, String> entry : foo.entrySet()){
      //  System.out.println(entry.getKey() + " : " + entry.getValue());
      //}

      //get # in, out, local
      MT.out = out > 4 ? out - 4 : 0;
      MT.in = func.params.length > 4 ? func.params.length - 4 : 0;
      //MT.local = func.vars.length - func.params.length;
      MT.local = func.vars.length;

      //filter out parameters from local vars
      count = 0;
      for(String var : func.vars) {
        boolean exist = false;
        for(VVarRef.Local param : func.params) {
          if(param.toString() == var) {
            exist = true;
          }
        }

        if(!exist) {
          String key = var;
          String value = "local[" + count + "]";
          MT.insert(key, value);
          count++;
        }
      }

      fun = "func " + func.ident + " [in "
              + MT.in + ", out " + MT.out + ", local " + MT.local + "]\n";

      //load parameter registers to allocated registers
      //count holds the number of local vars now
      int limit = count + 4;
      for(VVarRef.Local param : func.params) {
          String left = "local[" + count + "]";
          if(count < limit)
            fun += left + " = " + MT.get(param+"") + "\n";

          else {
            String reg = MT.regAlloc('t');
            fun += reg + " = " + "in[" + (count - limit) + "]\n";
            fun += left + " = " + reg + "\n";
            MT.incre_t(reg);
          }

          MT.insert(param+"", left); //update the correspondingly value
          count++;
      }

      int fun_pos = getInt(func.sourcePos+"");
      Object para = null;
      for(VInstr ins : func.body) {
        p = (Pair)ins.accept(para, VR);

        //special case: label maybe right after the func singature
        if(new VMVisitor().labels.get(fun_pos+1) != null) {
          new VMVisitor().label = new VMVisitor().labels.get(fun_pos+1);
          new VMVisitor().labels.remove(fun_pos+1);   // this block should only execute once
          fun = new VMVisitor().format(fun);
          code += fun;
          fun = new VMVisitor().label + "\n";
          new VMVisitor().reset();
        }

        fun += p.code + "\n";

        //check label position
        while(new VMVisitor().label != null) {
          fun = new VMVisitor().format(fun);
          code += fun;
          fun = new VMVisitor().label + "\n";

          //check nested end labels
          if(new VMVisitor().label.indexOf("end") != -1) {
            new VMVisitor().getNextLabel(new VMVisitor().line);
          }

          else
            new VMVisitor().reset();
        }

        //pass the result to the next visitor
        para = p.result == null ? null : p.result;
      }

      //cout.println("________________________");

      //clear all the labels for current func
      new VMVisitor().labels.clear();
      //format the this function block
      code += new VMVisitor().format(fun) + "\n";
    }

    //merge
    code = globalConst + "\n" + code;
    cout.println(code);
    cout.close();
  }
}
