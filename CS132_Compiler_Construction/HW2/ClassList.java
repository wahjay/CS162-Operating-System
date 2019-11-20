import syntaxtree.*;
import java.util.HashMap;
import java.util.Map;
import java.util.*;

class ClassList {
  HashMap<String, ClassTable> C_List;
  private static ClassList instance;
  public static Object cur;   // for recording current method environment
  private static Object cur_class;  // for recording cur class environment

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

  public boolean CheckOverload(String child_id, String parent_id) {
    ClassTable child_table = C_List.get(child_id);
    ClassTable parent_table = C_List.get(parent_id);

    for(String name : child_table.methodTables.keySet()) {
      while(parent_table.methodTables.containsKey(name)) {
        MethodTable child_MT = child_table.get_method(name);
        MethodTable parent_MT = parent_table.get_method(name);
        //retype type and number of parameters must be the same
        if (child_MT.get_ret().getClass().equals(parent_MT.get_ret().getClass())
            && child_MT.parameters.size() == parent_MT.parameters.size()) {
            //now check the name and type of each parameter
            //naive comparison without considering subtype
            for(String p : child_MT.parameters.keySet()) {
              if(!child_MT.parameters.get(p).getClass().equals(parent_MT.parameters.get(p).getClass()))
                return true;
            }

            for(String p : parent_MT.parameters.keySet()) {
              if(!child_MT.parameters.get(p).getClass().equals(parent_MT.parameters.get(p).getClass()))
                return true;
            }

            //check next super class
            String super_id = parent_table.get_super_id();
            if (super_id == null)
              break;
            parent_table = C_List.get(super_id);
        }
        else if (parent_table.get_super_id() == null)
          return true;

        else if (parent_table.get_super_id() != null) {
          String super_id = parent_table.get_super_id();
          parent_table = C_List.get(super_id);
        }
      }
    }

    return false;
  }

  //for debug reference
  public void print() {
    for (Map.Entry<String, ClassTable> entry : C_List.entrySet()) {
          ClassTable CT = entry.getValue();
          System.out.println("Class Name = " + entry.getKey());
          System.out.println("Super Class Name = " +  CT.Super_ID);

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
    Identifier ID;  //class id
    String Super_ID; //super class id

    public ClassTable() {
      methodTables = new HashMap<>();
      fields = new HashMap<>();
    }

    public void set_super_id(String id){
      Super_ID = id;
    }

    public String get_super_id() {
      return Super_ID;
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
      //should have access to super class field
      /*
      ClassList instance = ClassList.getInstance();
      Node type = fields.get(fn);
      String super_id = Super_ID;

      ClassTable CT = instance.get(super_id);
      while(super_id != null && type == null) {
        type = CT.get_field(fn);
        super_id = CT.get_super_id();
        CT = instance.get(super_id);
      }

      return type;
      */
      return fields.get(fn);
    }

    public void insert_method(String mn, MethodTable mt) {
      methodTables.put(mn, mt);
    }

    public MethodTable get_method(String mn) {
      MethodTable MT = methodTables.get(mn);

      //method table is not in current class
      //try its super class
      ClassList instance = ClassList.getInstance();
      String super_id = Super_ID;
      ClassTable super_CT = instance.get(super_id);
      while(super_id != null && MT == null) {
        MT = super_CT.get_method(mn);

        //advance class env
        super_id = super_CT.get_super_id();
        super_CT = instance.get(super_id);
      }

      return MT;
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
      ClassList instance = ClassList.getInstance();
      ClassTable CT = (ClassTable) instance.get_cur_class();
      if(temp == null)
        temp = CT.get_field(ln);

      //main function exit point
      //because main calss has no fields
      //return sooner the better
      if(CT == null)
        return temp;

      //check super class fields
      String super_id = CT.get_super_id();
      ClassTable super_CT = instance.get(super_id);
      while(super_id != null && temp == null) {
        temp = super_CT.get_field(ln);

        //advance class env
        super_CT = instance.get(super_id);
        super_id = super_CT.get_super_id();
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
