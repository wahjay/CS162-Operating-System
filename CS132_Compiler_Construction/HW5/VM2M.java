import cs132.util.ProblemException;
import cs132.vapor.ast.*;
import cs132.vapor.ast.VInstr.VisitorPR;
import cs132.vapor.ast.VaporProgram;
import cs132.vapor.parser.VaporParser;
import cs132.vapor.ast.VBuiltIn.Op;
import java.util.*;
import java.io.*;

public class VM2M {

  public static VaporProgram parseVapor(InputStream in, PrintStream err) throws IOException {
    Op[] ops = {
      Op.Add, Op.Sub, Op.MulS, Op.Eq, Op.Lt, Op.LtS,
      Op.PrintIntS, Op.HeapAllocZ, Op.Error,
    };

    boolean allowLocals = false;
    String[] registers = {
    "v0", "v1",
    "a0", "a1", "a2", "a3",
    "t0", "t1", "t2", "t3", "t4", "t5", "t6", "t7",
    "s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7",
    "t8",
    };
    boolean allowStack = true;

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

  public static void main(String[] args) throws IOException {
    InputStream in = System.in;
    PrintStream cout = new PrintStream(System.out);
    VaporProgram program = parseVapor(in, cout);
    VisitorPR VR = new VMVisitor();

    //set up
    String print_fun = "_print:\n";
    print_fun += "li $v0 1\n";
    print_fun += "syscall\n";
    print_fun += "la $a0 _newline\n";
    print_fun += "li $v0 4\n";
    print_fun += "syscall\n";
    print_fun += "jr $ra\n";
    print_fun = new VMVisitor().format(print_fun) + "\n";

    String error_fun = "_error:\n";
    error_fun += "li $v0 4\n";
    error_fun += "syscall\n";
    error_fun += "li $v0 10\n";
    error_fun += "syscall\n";
    error_fun = new VMVisitor().format(error_fun) + "\n";

    String heapAlloc_fun = "_heapAlloc:\n";
    heapAlloc_fun += "li $v0 9\n";
    heapAlloc_fun += "syscall\n";
    heapAlloc_fun += "jr $ra\n";
    heapAlloc_fun = new VMVisitor().format(heapAlloc_fun) + "\n";

    String codeSect = ".data\n\n";
    String textSect = ".text\n\njal Main\nli $v0 10\nsyscall\n";
    textSect = new VMVisitor().format(textSect) + "\n";

    String footer = ".data\n.align 0\n_newline: .asciiz \"\\n\" \n_str0: .asciiz \"null pointer\\n\"";
    String botSect = print_fun + error_fun + heapAlloc_fun + footer;


    //class and method labels at the top
    for(VDataSegment data : program.dataSegments) {
      String classLabel = data.ident + ":\n";
      for(VOperand.Static operand : data.values) {
        classLabel += (operand+"").substring((operand+"").indexOf(":")+1) + "\n";
      }

      classLabel = new VMVisitor().format(classLabel);
      codeSect += classLabel + "\n";
    }

    String code = codeSect + textSect;
    String fun = "";

    for(VFunction func : program.functions) {
      VMTable VMT = VMTable.getInstance();
      VMT.curMethod = func.ident;
      MethodTable MT = VMT.get(VMT.curMethod);
      VMVisitor vis = new VMVisitor();  //this visitor is used to access the global label table

      //insert cur func labels into a global table
      for(VCodeLabel vl : func.labels){
       String pos = vl.sourcePos + "";
       pos = pos.substring(0, pos.indexOf("."));
       int key = Integer.parseInt(pos);
       vis.labels.put(key, vl.ident + ":");
      }

      fun = func.ident + ":\n";
      //store the frame pointer
      fun += "sw $fp -8($sp)\n";
      fun += "move $fp $sp\n";
      fun += "subu $sp $sp " + (8 + (func.stack.local+func.stack.out)*4) + "\n";
      fun += "sw $ra -4($fp)\n";

      for(VInstr ins : func.body) {
        String line = (String)ins.accept(null, VR);
        fun += (line == null ? "" : line + "\n");

        int label_pos = getInt(ins.sourcePos+"");
        label_pos += 1;
        while(vis.labels.get(label_pos) != null) {
          String label = vis.labels.get(label_pos);
          fun = vis.format(fun);
          code += fun;
          fun = label + "\n";
          label_pos++;  //handle nested labels
        }
      }


      //restore return address  and frame pointer
      fun += "lw $ra -4($fp)\n";
      fun += "lw $fp -8($fp)\n";
      fun += "addu $sp $sp " + (8 + (func.stack.local+func.stack.out)*4) + "\n";
      fun += "jr $ra\n";

      code += vis.format(fun) + "\n";
      vis.labels.clear();
      //System.out.println("__________________");
      //new VMVisitor().reset();
    }

    code += botSect;
    cout.println(code);
    cout.close();
  }
}
