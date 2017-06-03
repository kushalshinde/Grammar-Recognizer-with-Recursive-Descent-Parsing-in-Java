package edu.binghamton.cs571;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.StringReader;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Scanner built from a mapping of regex strings to some enum of token-kinds.
 *  It regex string is empty, then used for EOF token.  If mapped enum is
 *  null, then input matching the corresponding regex is ignored.
 *
 *  Quite practical except for following problems:
 *
 *    1) Difficult to have different scanning rules depending on context
 *       (as per the states of lex-like scanners).  This can be worked-around
 *       by defining pseudo-token-kinds with state arguments (regular tokens
 *       would have null state arguments).  If such a token is produced, then
 *       the scanner would switch tables to that specified by the state.
 *
 *    2) Matching regex is found using linear search over all patterns.
 *       This is a problem with the Java Matcher API and does not seem
 *       to have any easy workaround.
 *
 *    3) Lexemes cannot cross multiple lines.  This should have a
 *       easy workaround by sucking in entire file into memory (though
 *       this will not work for gargantuan files).
 */
class Scanner {

  private final BufferedReader _reader;
  private final ScanMapInfo _scanMapInfo;
  private String _line;
  private Coords _coords; //coordinates of end of last token


  /** Create a scanner for reading from filename with tokens given by scanMap. */
  Scanner(String fileName, LinkedHashMap<String, Enum> scanMap) {
    this(openFile(fileName), new Coords(fileName), scanMap);
  }

  /** Create a scanner for reading from stdin with tokens given by scanMap. */
  Scanner(LinkedHashMap<String, Enum> scanMap) {
    this(new BufferedReader(new InputStreamReader(System.in)),
         new Coords("<stdin>"), scanMap);
  }

  /** Create a scanner for reader with previous coordinates coords.
   *  scanMap is a map from regex-strings to corresponding token kinds
   *  (the token kind is null if the lexeme should be skipped).
   */
  Scanner(BufferedReader reader, Coords coords,
          LinkedHashMap<String, Enum> scanMap) {
    _reader = reader;
    _coords = coords;
    _scanMapInfo = analyzeScanMap(scanMap);
    _line = null;
  }

  /** Return next token for this scanner.  Returns null on EOF. */
  Token nextToken() {
    int lineN = _coords.lineN;
    int colN = _coords.colN;
    Token token = null;
    while (token == null) {
      Coords coords = new Coords(_coords.fileName, lineN, colN);
      try {
        if (_line == null || colN >= _line.length()) {
          _line = _reader.readLine();
          if (_line == null) {
            _reader.close();
            return new Token(_scanMapInfo.eofKind, "<EOF>", coords);
          }
          _line = _line + "\n"; //readLine() does not include terminating "\n"
          lineN++; colN = 0;
          coords = new Coords(_coords.fileName, lineN, colN);
        }
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
      Matcher matcher =
        _scanMapInfo.pattern.matcher(_line).region(colN, _line.length());
      if (!matcher.lookingAt()) {
        throw new RuntimeException(coords + ": no match at '" +
                                   _line.substring(colN) + "'");
      }
      for (String name : _scanMapInfo.nameKinds.keySet()) {
        //linear search because of limitations of Matcher api
        String lexeme = matcher.group(name);
        if (lexeme != null) {
          if (lexeme.length() == 0) {
            throw new RuntimeException(coords + ": empty match");
          }
          Enum e = _scanMapInfo.nameKinds.get(name);
          if (e == null) {
            colN += lexeme.length();
          }
          else {
            token = new Token(e, lexeme, coords);
            _coords = new Coords(_coords.fileName, lineN,
                                 colN + lexeme.length());
          }
        }
      }
    }
    return token;
  }


  private static BufferedReader openFile(String fileName) {
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new FileReader(fileName));
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
    return reader;
  }

  private static ScanMapInfo
    analyzeScanMap(LinkedHashMap<String, Enum> scanMap)
  {
    Map<String, Enum> nameKinds = new HashMap<>();
    String nameFormat = "T%d";
    int i = 0;
    StringBuilder b = new StringBuilder();
    Enum eofKind = null;
    for (String regex : scanMap.keySet()) {
      Enum e = scanMap.get(regex);
      if (regex.length() == 0) {
        if (eofKind != null) {
          throw new RuntimeException("multiple empty patterns for EOF token");
        }
        eofKind = e;
        continue;
      }
      String name = String.format(nameFormat, i++);
      if (b.length() > 0) b.append("|");
      b.append("(?<").append(name).append(">").append(regex).append(")");
      nameKinds.put(name, e);
    }
    return new ScanMapInfo(Pattern.compile(b.toString()), nameKinds, eofKind);
  }

  //A struct used for a return-value
  private static class ScanMapInfo {
    final Pattern pattern;
    final Map<String, Enum> nameKinds;
    final Enum eofKind;
    ScanMapInfo(Pattern pattern, Map<String, Enum> nameKinds, Enum eofKind) {
      this.pattern = pattern; this.nameKinds = nameKinds;
      this.eofKind = eofKind;
    }
  }

  private static enum T { EOF, ID, INT, STRING, MISC };
  public static void main(String[] args) {
    LinkedHashMap<String, Enum> patternsMap = new LinkedHashMap<String, Enum>() {{
        put("", T.EOF);
        put("\\s+", null);
        put("#.*", null);
        put("[a-zA-Z_]\\w*", T.ID);
        put("\\d+", T.INT);
        put("\\\"([^\\\"]|\\\\.)*\\\"", T.STRING);
        put(".", T.MISC);
      }};
    String test =
      "id_123 \"some string\", #21 34\n" +
      "   234 \"asd\\ndef\"  \n" +
      " another_id 342 # xx yy\n";
    BufferedReader reader = new BufferedReader(new StringReader(test));
    Scanner scanner =
      new Scanner(reader, new Coords("<testString>"), patternsMap);
    Token t;
    do {
      t = scanner.nextToken();
      System.out.println(t);
    } while (t.kind != T.EOF);
  }

}
