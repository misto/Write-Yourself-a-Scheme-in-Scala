package chapter3

import chapter2._

object Evaluator {
    
  def showVal(lv: LispVal): String = lv match {
    case LispString(contents) => 
      "\"" + contents + "\""
    case Atom(name) => 
      name
    case Number(contents) => 
      contents.toString
    case Bool(true) => 
      "#t"
    case Bool(false) => 
      "#f"
      
    case LispList(contents) => 
      "(" + unwordsList(contents) + ")"
    case DottedList(head, tail) => 
      "(" + unwordsList(head) + " . " + showVal(tail) + ")"
  }

  def unwordsList(lvs: List[LispVal]): String = {
    lvs.map(showVal).mkString(" ")
  }
  
  def eval(lv: LispVal): LispVal = lv match {
    case v @ LispString(_) => v
    case v @ Number(_) => v
    case v @ Bool(_) => v
    case LispList(Atom("quote") :: v :: Nil) => v    
    case LispList(Atom(func) :: args) => 
      apply(func, args map eval)
  }
  
  def readExpr(input: String): LispVal = {
    import Parser._
    parse(parseExpr, input) match {
      case Success(res, _) => 
        res
      case NoSuccess(msg, _) => 
        LispString("No match: «" + msg + "»")
    }
  }
  
  def main(args: Array[String]) {
    println(showVal(eval(readExpr(args(0)))))
  }
  
  def apply(funName: String, lvs: List[LispVal]): LispVal = {
    val fun = primitives.get(funName)
    val result = fun.map(f =>  f(lvs))
    result.getOrElse(Bool(false))
  }

  val primitives: Map[String, List[LispVal] => LispVal] = Map(
    "+" -> numericBinop((x, y) => x + y),
    "-" -> numericBinop((x, y) => x - y),
    "*" -> numericBinop((x, y) => x * y),
    "/" -> numericBinop((x, y) => x / y),
    "remainder" -> numericBinop((x, y) => x % y)
  )
  
  def numericBinop(op: (Int, Int) => Int)(args: List[LispVal]): LispVal = {
    def unpackNum(lv: LispVal): Int = lv match {
      case Number(n) => 
        n
      case LispString(s) if s.matches("\\d+") => 
        s.toInt
      case LispList(List(lv)) => 
        unpackNum(lv)
      case _ => 
        0
    }
    Number(args.map(unpackNum).reduceRight(op))
  }
}