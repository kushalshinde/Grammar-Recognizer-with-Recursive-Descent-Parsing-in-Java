package edu.binghamton.cs571;

/** A simple struct used to specify the coordinates within a file */
class Coords {
  String fileName;
  final int lineN;   //1-based
  final int colN;    //0-based

  Coords(String fileName, int lineN, int colN) {
    this.fileName = fileName; this.lineN = lineN; this.colN = colN;
  }

  Coords(String fileName) {
    this(fileName, 0, 0); //assume lineN incremented on reading first line
  }

  public String toString() {
    return String.format("%s:%d:%d", fileName, lineN, colN);
  }

}
