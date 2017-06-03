# Grammar-Recognizer-with-Recursive-Descent-Parsing-in-Java

Name:		Kushal Shinde
B-Number:	XXX
Email:		kshinde1@binghamton.edu

For detecting the semantic errors, the grammar creates sets for used
and defined non-terminals (the start symbol is added into the
uses). Then simple set operations are used to detect the syntax
errors.

For better error reporting, the sets of non-terminal definitions is
maintained as a map from non-terminals to the coordinate at which
the first definition of the non-terminal was encountered.
