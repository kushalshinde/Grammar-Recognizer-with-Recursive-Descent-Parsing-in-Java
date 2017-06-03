package edu.binghamton.cs571;

/** A simple struct representing a token produced by the scanner */
class Token {
  final Enum kind;       /** what kind of token is this */
  final String lexeme;   /** the actual text of this token */
  final Coords coords;   /** where did this token occur */

  Token(Enum kind, String lexeme, Coords coords) {
    this.kind = kind; this.lexeme = lexeme; this.coords = coords;
  }

  public String toString() {
    return String.format("%s: %s \"%s\"", coords, kind, lexeme);
  }

}
