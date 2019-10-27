import syntaxtree.*;
import java.util.HashMap;
import java.util.Map;
import java.util.*;

class ClassList {
  HashMap<String, ClassTable> C_List;
  private static ClassList instance;
  public static Object cur;   // for recording current environment
  //private static Object pre;  // for recording previous environment
  private static Object cur_class;  // for recording previous environment

  private ClassList() {
    C_List = new HashMap<>();
  }

  public static ClassList getInstance() {
     if (instance == null)
      instance = new ClassList();

     return instance;
  }

  //get method table outside of current environment
  public MethodTable cross_get_method(String cn, String mn) {
    ClassTable CT = C_List.get(cn);
    return CT.get_method(mn);
  }

  //back up to previous environment
  public void rollback() {
    cur = cur_class;
  }

  public void set_cur(Object cur_env) {
    //only classTable can be pre_env for now
    if (cur != null && cur instanceof ClassTable)
      cur_class = cur;

    cur = cur_env;
  }

  public void set_cur_class(Object cur_env) {
    cur_class = cur_env;
  }

  //for testing
  public Object get_cur_class() {
    return cur_class;
  }

  public Object get_cur() {
    return cur;
  }

  public void insert(String cn, ClassTable ct) {
    C_List.put(cn, ct);
  }

  public ClassTable get(String cn) {
    return C_List.get(cn);
  }

  public void print() {
    for (Map.Entry<String, ClassTable> entry : C_List.entrySet()) {
          System.out.println("Class Name = " + entry.getKey());

          ClassTable CT = entry.getValue();
          for (Map.Entry<String, Node> field : CT.fields.entrySet()) {
            System.out.println("Field Name = " +field.getKey() + " | Field Type = " + field.getValue());
          }

          System.out.println("-----------------------------");
          for (Map.Entry<String, MethodTable> method : CT.methodTables.entrySet()) {
            System.out.println("Method Name = " +method.getKey());

            MethodTable MT = method.getValue();
            for (Map.Entry<String, Node> para : MT.parameters.entrySet()) {
              System.out.println("Para Name = " + para.getKey() + " | Para Type = " + para.getValue());
            }

            for (Map.Entry<String, Node> local : MT.locals.entrySet()) {
              System.out.println("local Name = " + local.getKey() + " | local Type = " + local.getValue());
            }

            System.out.println("return type = " + MT.ret_type);
            System.out.println("-----------------------------");
          }
    }
  }
}


class ClassTable {
    HashMap<String, MethodTable> methodTables;
    HashMap<String, Node> fields;
    Identifier ID;

    public ClassTable() {
      methodTables = new HashMap<>();
      fields = new HashMap<>();
    }

    public void set_id(Identifier id){
      ID = id;
    }

    public Identifier get_id(){
      return ID;
    }

    public void insert_field(String fn, Node ft) {
      fields.put(fn, ft);
    }

    public Node get_field(String fn) {
      return fields.get(fn);
    }

    public void insert_method(String mn, MethodTable mt) {
      methodTables.put(mn, mt);
    }

    public MethodTable get_method(String mn) {
      return methodTables.get(mn);
    }
}


class MethodTable {
    LinkedHashMap<String, Node> parameters;
    HashMap<String, Node> locals;
    Node ret_type;

    public MethodTable() {
      parameters = new LinkedHashMap<>();
      locals = new HashMap<>();
      ret_type = null;
    }

    public void p_insert(String pn, Node pt) {
      parameters.put(pn, pt);
    }

    public Node p_get(String pn) {
      return parameters.get(pn);
    }

    public void l_insert(String ln, Node lt) {
      locals.put(ln, lt);
    }

    public Node l_get(String ln) {
      //check locals first
      Node temp = locals.get(ln);

      //check parameters
      if (temp == null)
        temp = parameters.get(ln);

      //check class fields
      if(temp == null) {
        ClassList instance = ClassList.getInstance();
        ClassTable CT = (ClassTable) instance.get_cur_class();
        temp = CT.get_field(ln);
      }

      return temp;
    }

    public void set_ret(Node ret) {
      ret_type = ret;
    }

    public Node get_ret() {
      return ret_type;
    }
}
