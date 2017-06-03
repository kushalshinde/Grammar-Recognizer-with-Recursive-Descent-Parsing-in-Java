package edu.binghamton.cs571;

import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Recognize grammars specified using following grammar:

  grammar
    : EOF
    | ruleSet grammar
    ;

  ruleSet
    : NON_TERMINAL ':' rightHandSide restRightHandSides
    ;

  restRightHandSides
    : ';'
    | '|' rightHandSide restRightHandSides
    ;

  rightHandSide
   : TERMINAL rightHandSide
   | NON_TERMINAL rightHandSide
   | //empty
   ;
 */

public class GrammarStats {

  private final Scanner _scanner;
  private Token _lookahead;
  private Map<String, Coords> _nonTerminalDefs;
  private Set<String> _nonTerminalUses;
  private int _nNonTerminals;
  private int _nTerminals;

  GrammarStats(String fileName) {
    _scanner = new Scanner(fileName, PATTERNS_MAP);
    _nonTerminalDefs = new HashMap<>();
    _nonTerminalUses = new HashSet<>();
    nextToken();
    _nonTerminalUses.add(_lookahead.lexeme);
  }

  /** Recognize a grammar specification.  Return silently if ok, else
   *  signal an error.
   */
  Stats getStats() {
    Stats stats = null;
    try {
      grammar();
      if (_nonTerminalDefs.size() == 0) {
        throw new GrammarParseException("no ruleSets found");
      }
      Set<String> undefined = new HashSet<>(_nonTerminalUses);
      undefined.removeAll(_nonTerminalDefs.keySet());
      if (undefined.size() > 0) {
        throw new GrammarParseException("no rules for " + undefined);
      }
      Set<String> defined = new HashSet<>(_nonTerminalDefs.keySet());
      defined.removeAll(_nonTerminalUses);
      if (defined.size() > 0) {
        throw new GrammarParseException("no uses of rules for " + defined);
      }
      stats = new Stats(_nonTerminalDefs.size(), _nNonTerminals, _nTerminals);
    }
    catch (GrammarParseException e) {
      System.err.println(e.getMessage());
    }
    return stats;
  }

  /** Recognize grammar:
   *  grammar
   *    : EOF
   *    | ruleSet grammar
   *    ;
   */
  private void grammar() {
    if (_lookahead.kind == TokenKind.EOF) {
      match(TokenKind.EOF);
    }
    else {
      ruleSet();
      grammar();
    }
  }

  /** Recognize a ruleSet:
   *  ruleSet
   *    : NON_TERMINAL ':' rightHandSide restRightHandSides
   *    ;
   */
  private void ruleSet() {
    String nonTerminal = _lookahead.lexeme;
    Coords lastDef = _nonTerminalDefs.get(nonTerminal);
    if (lastDef != null) {
      String message = String.format("%s: multiple rule-sets for %s;" +
                                     "first defined at %s",
                                     _lookahead.coords, nonTerminal, lastDef);
      throw new GrammarParseException(message);
    }
    _nonTerminalDefs.put(nonTerminal, _lookahead.coords);
    match(TokenKind.NON_TERMINAL);
    _nNonTerminals++;
    match(TokenKind.COLON);
    rightHandSide();
    restRightHandSides();
  }

  /** Recognize restRightHandSides
   *  restRightHandSides
   *    : ';'
   *    | '|' rightHandSide restRightHandSides
   *    ;
   */
  private void restRightHandSides() {
    if (_lookahead.kind == TokenKind.SEMI) {
      match(TokenKind.SEMI);
    }
    else if (_lookahead.kind == TokenKind.PIPE) {
      match(TokenKind.PIPE);
      rightHandSide();
      restRightHandSides();
    }
    else {
      syntaxError();
    }
  }


  /** Recognize rightHandSide
   *  rightHandSide
   *    : TERMINAL rightHandSide
   *    | NON_TERMINAL rightHandSide
   *    | //empty
   *    ;
   */
  private void rightHandSide() {
    if (_lookahead.kind == TokenKind.TERMINAL) {
      _nTerminals++;
      match(TokenKind.TERMINAL);
      rightHandSide();
    }
    else if (_lookahead.kind == TokenKind.NON_TERMINAL) {
      _nNonTerminals++;
      _nonTerminalUses.add(_lookahead.lexeme);
      match(TokenKind.NON_TERMINAL);
      rightHandSide();
    }
    else {
      //empty
    }
  }


  //We extend RuntimeException since Java's checked exceptions are //@exception@
  //very cumbersome
  static class GrammarParseException extends RuntimeException {
    GrammarParseException(String message) {
      super(message);
    }
  }

  private void match(TokenKind kind) { //@match@
    if (kind != _lookahead.kind) {
      syntaxError();
    }
    if (kind != TokenKind.EOF) {
      nextToken();
    }
  }

  /** Skip to end of current line and then throw exception */ //@syntaxError@
  private void syntaxError() {
    String message = String.format("%s: syntax error at '%s'",
                                   _lookahead.coords, _lookahead.lexeme);
    throw new GrammarParseException(message);
  }

  private static final boolean DO_TOKEN_TRACE = false; //@nextToken@

  private void nextToken() {
    _lookahead = _scanner.nextToken();
    if (DO_TOKEN_TRACE) System.err.println("token: " + _lookahead);
  }



  /** token kinds for arith tokens*/ //@tokenKind@
  private static enum TokenKind {
    EOF,
    COLON,
    PIPE,
    SEMI,
    NON_TERMINAL,
    TERMINAL,
    ERROR,
  }

  /** Simple structure to collect grammar statistics */
  private static class Stats {
    final int nRuleSets;
    final int nNonTerminals;
    final int nTerminals;
    Stats(int nRuleSets, int nNonTerminals, int nTerminals) {
      this.nRuleSets = nRuleSets;
      this.nNonTerminals = nNonTerminals;
      this.nTerminals = nTerminals;
    }
    public String toString() {
      return String.format("%d %d %d", nRuleSets, nNonTerminals, nTerminals);
    }
  }

  /** Map from regex to token-kind */ //@tokenMap@
  private static final LinkedHashMap<String, Enum> PATTERNS_MAP =
    new LinkedHashMap<String, Enum>() {{
      put("", TokenKind.EOF);
      put("\\s+", null);  //ignore whitespace.
      put("\\//.*", null);
      put("\\:", TokenKind.COLON);
      put("\\|", TokenKind.PIPE);
      put("\\;", TokenKind.SEMI);
      put("[a-z]\\w*", TokenKind.NON_TERMINAL);
      put("[A-Z]\\w*", TokenKind.TERMINAL);
      put(".", TokenKind.ERROR);  //catch lexical error in parser
    }};


  private static final String USAGE =
    String.format("usage: java %s GRAMMAR_FILE",
                  GrammarStats.class.getName());

  /** Main program for testing */
  public static void main(String[] args) {
    if (args.length != 1) {
      System.err.println(USAGE);
      System.exit(1);
    }
    GrammarStats grammarStats = new GrammarStats(args[0]);
    Stats stats = grammarStats.getStats();
    if (stats != null) {
      System.out.println(stats);
    }
  }



}
