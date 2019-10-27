
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.LinkedList;

public class Parse {
  Queue<String> tokens;
  String token;
  boolean matched;

  // terminal symbols
  final String LPAREN = "(";
  final String RPAREN = ")";
  final String LCURL = "{";
  final String RCURL = "}";
  final String PRINT = "System.out.println";
  final String SEMI = ";";
  final String IF = "if";
  final String ELSE = "else";
  final String WHILE = "while";
  final String TRUE = "true";
  final String FALSE = "false";
  final String EXCLA = "!";

  //constructor
  public Parse() {
    matched = true;
    Scanner scanner = new Scanner();

    try {
      tokens = scanner.tokens();
      token = tokens.poll();

      //for(String temp : tokens) {
      //  System.out.println(temp);
      //}

      //start symbol
      S();
      if(token != null || !tokens.isEmpty())
	  error();
      
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  //the name of the main class(class which contain main method)
  //should match the name of the file that holds the program.
  //public: So that JVM can execute the method from anywhere.
  //static: Main method is to be called without object.
  //there is at most one public class which contain main() method.
  public static void main(String[] args){
    Parse parser = new Parse();
    System.out.println("Program parsed successfully");
  }

  private void error() {
    System.out.println("Parse error");
    System.exit(0);
  }

  private void eat(String tk){
    if (token == tk)
      token = tokens.poll();

    else
      error();
  }

  private void S() {
    if (token == LCURL) {
      eat(LCURL); L(); eat(RCURL);
    }

    else if (token == PRINT) {
      eat(PRINT); eat(LPAREN); E(); eat(RPAREN); eat(SEMI);
    }

    else if (token == IF) {
      eat(IF); eat(LPAREN); E(); eat(RPAREN); S(); eat(ELSE); S();
    }

    else if (token == WHILE) {
      eat(WHILE); eat(LPAREN); E(); eat(RPAREN); S();
    }

    else { matched = !matched; }
  }

  private void L() {
    boolean temp = matched;
    S();

    // S() succesfully matched
    if (temp == matched)
      L();
    // otherwise, epsilon $
    else {}

  }

  private void E() {
    if (token == TRUE)
      eat(TRUE);

    else if (token == FALSE)
      eat(FALSE);

    else {
      eat(EXCLA); E();
    }
  }
}

class Scanner {

  Map<Input_pair, Integer> transition_list;
  Map<Integer, String> accepting_states;
  int initial_state;

  //constructor
  public Scanner(){
    //transistions
    transition_list = new HashMap<>();

    //tokens list
    transition_list.put(new Input_pair(0, 'S'), 1);
    transition_list.put(new Input_pair(1, 'y'), 2);
    transition_list.put(new Input_pair(2, 's'), 3);
    transition_list.put(new Input_pair(3, 't'), 4);
    transition_list.put(new Input_pair(4, 'e'), 5);
    transition_list.put(new Input_pair(5, 'm'), 6);
    transition_list.put(new Input_pair(6, '.'), 7);
    transition_list.put(new Input_pair(7, 'o'), 8);
    transition_list.put(new Input_pair(8, 'u'), 9);
    transition_list.put(new Input_pair(9, 't'), 10);
    transition_list.put(new Input_pair(10, '.'), 11);
    transition_list.put(new Input_pair(11, 'p'), 12);
    transition_list.put(new Input_pair(12, 'r'), 13);
    transition_list.put(new Input_pair(13, 'i'), 14);
    transition_list.put(new Input_pair(14, 'n'), 15);
    transition_list.put(new Input_pair(15, 't'), 16);
    transition_list.put(new Input_pair(16, 'l'), 17);
    transition_list.put(new Input_pair(17, 'n'), 18);

    // (
    transition_list.put(new Input_pair(0, '('), 19);

    // )
    transition_list.put(new Input_pair(0, ')'), 20);

    // {
    transition_list.put(new Input_pair(0, '{'), 21);

    // }
    transition_list.put(new Input_pair(0, '}'), 22);

    // if
    transition_list.put(new Input_pair(0, 'i'), 23);
    transition_list.put(new Input_pair(23, 'f'), 24);

    // else
    transition_list.put(new Input_pair(0, 'e'), 25);
    transition_list.put(new Input_pair(25, 'l'), 26);
    transition_list.put(new Input_pair(26, 's'), 27);
    transition_list.put(new Input_pair(27, 'e'), 28);

    // while
    transition_list.put(new Input_pair(0, 'w'), 29);
    transition_list.put(new Input_pair(29, 'h'), 30);
    transition_list.put(new Input_pair(30, 'i'), 31);
    transition_list.put(new Input_pair(31, 'l'), 32);
    transition_list.put(new Input_pair(32, 'e'), 33);

    // true
    transition_list.put(new Input_pair(0, 't'), 34);
    transition_list.put(new Input_pair(34, 'r'), 35);
    transition_list.put(new Input_pair(35, 'u'), 36);
    transition_list.put(new Input_pair(36, 'e'), 37);

    //false
    transition_list.put(new Input_pair(0, 'f'), 38);
    transition_list.put(new Input_pair(38, 'a'), 39);
    transition_list.put(new Input_pair(39, 'l'), 40);
    transition_list.put(new Input_pair(40, 's'), 41);
    transition_list.put(new Input_pair(41, 'e'), 42);

    // !
    transition_list.put(new Input_pair(0, '!'), 43);

    // ;
    transition_list.put(new Input_pair(0, ';'), 44);

    accepting_states = new HashMap<>();
    accepting_states.put(18, "System.out.println");
    accepting_states.put(19, "(");
    accepting_states.put(20, ")");
    accepting_states.put(21, "{");
    accepting_states.put(22, "}");
    accepting_states.put(24, "if");
    accepting_states.put(28, "else");
    accepting_states.put(33, "while");
    accepting_states.put(37, "true");
    accepting_states.put(42, "false");
    accepting_states.put(43, "!");
    accepting_states.put(44, ";");

    initial_state = 0;
  }

  private void error() {
    System.out.println("Parse error");
    System.exit(0);
  }

  public Queue<String> tokens() throws IOException {
    Queue<String> tokens = new LinkedList<>();
    int input = System.in.read();

    Input_pair cur_state = new Input_pair(0, (char)input);
    int next_state = transition_list.containsKey(cur_state) ? transition_list.get(cur_state) : -1;

    if(next_state == -1)
      error();

    while(input != -1) {
      if(accepting_states.containsKey(next_state)) {
        tokens.add(accepting_states.get(next_state));
        next_state = 0;
      }

      if (next_state != 0 && (input == 32 || input == 9 || input == 12 || input == 10))
        error();
      
      input = System.in.read();

      while(next_state == 0 && (input == 32 || input == 9 || input == 12 || input == 10))
        input = System.in.read();
      
      //out of string?
      if (input == -1)
        break;

      cur_state = new Input_pair(next_state, (char) input);
      //in case the token does not match
      if(!transition_list.containsKey(cur_state))
        error();

      next_state = transition_list.get(cur_state);
    }

    return tokens;
  }
}

class Input_pair {
  int cur_state;
  char input;

  public Input_pair(int cs, char in){
    cur_state = cs;
    input = in;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 31 * hash + (int) input;
    hash = 31 * hash + cur_state;
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    Input_pair ip = (Input_pair) obj;
    return (ip.cur_state == this.cur_state && ip.input == this.input);
  }
}
