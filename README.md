# Compiler for MiniPython

## Description

This project's goal was to build a compiler's grammar as well as create the compiler's Lexer, Syntax and Semantics check. 

This compiler was made for a simplified version of the python language (MiniPython), and was created with the help of SableCC. The compiler is based on the following BNF.

![BNF](docs\BNF.png)


## How to Run

1. Open command prompt
2. Navigate to the project's directory
3. Execute the grammar by running the following command: ```sablecc minipython.grammar```
4. Compile all Java files by executing: ```javac *.java```
5. Execute the Semantics Test on the python test file of your choice by running: ```java Semantics [yourTest.py]```

## Team Members

* [Anastasia Petroulaki](https://github.com/anape03)
* [Christos Oliver Pavlidis](https://github.com/01iverr)
* [Despoina Georgiadi](https://github.com/DebsTheLemon)
* [Konstantinos Vasilopoulos](https://github.com/KonstantinosVasilopoulos)

