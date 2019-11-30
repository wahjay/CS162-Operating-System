import java.util.HashMap;
import java.util.Map;
import java.util.*;

class VMTable {
  HashMap<String, MethodTable> Table;   //mapping t.# to registers
  HashMap<String, HashMap<String, Integer>> Liveness;    //maping regs to the last line they are alive
  private static VMTable instance;
  public static String curMethod;       //store current method

  private VMTable() {
    Table = new HashMap<>();
    Liveness = new HashMap<String, HashMap<String, Integer>>();
  }

  public static VMTable getInstance() {
    if(instance == null)
      instance = new VMTable();

    return instance;
  }

  public void insert(String func, MethodTable MT) {
    Table.put(func, MT);
  }

  public MethodTable get(String func) {
    MethodTable MT = Table.get(func);
    if(MT == null) {
      MT = new MethodTable();
      Table.put(func, MT);
    }

    return MT;
  }
}

class MethodTable {
  HashMap<String, String> Vars;
  int in;
  int out;
  int local;

  int count_t = -1;   //counter for caller saved regs $t0..$t8
  int count_s = -1;   //counter for callee saved regs $s0..$s7
  int count_a = -1;   //counter for argument passing regs $a0..$a3
  int count_v = -1;   //counter for temporary regs $v0, $v1

  List<String> caller_regs = new ArrayList<String>();
  List<String> callee_regs = new ArrayList<String>();

  public String regAlloc(char type) {
    String reg = null;
    switch(type) {
      case 't':
          if(caller_regs.isEmpty()) {
            //reg = "$t" + count_s;
            if(!callee_regs.isEmpty())
              regAlloc('s');
          }

          else {
            reg = caller_regs.get(0);
            caller_regs.remove(0);
          }
          count_t++;
          break;
      case 's':
          if(callee_regs.isEmpty()) {
            //reg = "$s" + count_s;
            if(!caller_regs.isEmpty())
              regAlloc('t');

            else
              regAlloc('v');
          }

          else {
            reg = callee_regs.get(0);
            callee_regs.remove(0);
          }

          count_s++;
          break;
      case 'a':
          count_a++;
          reg = "$a" + count_a;
          break;
      case 'v':
          count_v++;
          reg = "$v" + count_v;
          break;
    }
    return reg;
  }


  public MethodTable() {
    Vars = new HashMap<>();
    caller_regs.add("$t0");
    caller_regs.add("$t1");
    caller_regs.add("$t2");
    caller_regs.add("$t3");
    caller_regs.add("$t4");
    caller_regs.add("$t5");
    caller_regs.add("$t6");
    caller_regs.add("$t7");
    caller_regs.add("$t8");
    callee_regs.add("$s0");
    callee_regs.add("$s1");
    callee_regs.add("$s2");
    callee_regs.add("$s3");
    callee_regs.add("$s4");
    callee_regs.add("$s5");
    callee_regs.add("$s6");
    callee_regs.add("$s7");
  }

  public void insert(String id, String reg) {
    Vars.put(id, reg);
  }

  public String get(String id) {
    return Vars.get(id);
  }

  public void print() {
    for(Map.Entry entry : Vars.entrySet())
      System.out.println(entry.getKey() + " : " + entry.getValue());
  }

  public boolean containsKey(String key) {
    return Vars.containsKey(key);
  }

  public void reset_a() {
    count_a = -1;
  }

  public void incre_t(String reg) {
    caller_regs.add(reg);
  }

  public void incre_s(String reg) {
    callee_regs.add(reg);
  }
}
