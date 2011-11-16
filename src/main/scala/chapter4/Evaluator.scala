package chapter4

import chapter2._

abstract class LispError
case class NumArgs(num: Int, args: List[LispVal]) extends LispError
case class TypeMismatch(expected: String, actual: LispVal) extends LispError
case class ParseError(message: String) extends LispError
case class BadSpecialForm(message: String, form: LispVal) extends LispError
case class NotFunction(message: String, func: String) extends LispError
case class UnboundVar(message: String, varname: String) extends LispError
case class Default(message: String) extends LispError
 
object Evaluator {
        
  import chapter3.Evaluator.{showVal, unwordsList}

  def showError(le: LispError): String = le match {
    case UnboundVar(message, varname) =>
      message + ": " + varname
    case BadSpecialForm(message, form) =>
      message + ": " + showVal(form)
    case NotFunction(message, func) =>
      message + ": " + func
    case NumArgs(expected, found) =>
      "Expected " + expected + " args; found values " + unwordsList(found)
    case TypeMismatch(expected, found) =>
      "Invalid type: expected " + expected + ", found " ++ showVal(found)
    case ParseError(message) =>
      "Parse error at " + message
    case Default(message) =>
      message
  }
   
  type ThrowsError[T] = Either[LispError, T]
  
  def readExpr(input: String): ThrowsError[LispVal] = {
    import Parser._
    parse(parseExpr, input) match {
      case Success(res, _) => 
        Right(res)
      case NoSuccess(msg, _) => 
        Left(ParseError(msg))
    }
  }
  
  def eval(lv: LispVal): ThrowsError[LispVal] = lv match {
    case v @ LispString(_) => Right(v)
    case v @ Number(_) => Right(v)
    case v @ Bool(_) => Right(v)
    case LispList(Atom("quote") :: v :: Nil) => Right(v)  
    case LispList(Atom(func) :: args) => 
      
      def evalArg(lv: LispVal, acc: ThrowsError[List[LispVal]]) = {
        eval(lv).right.flatMap { lv =>
          acc.right.map(lvs => lv :: lvs)
        }
      }
      
      val acc: ThrowsError[List[LispVal]] = Right(Nil)
      
      val evaluatedArgs = args.foldRight(acc)(evalArg)
      
      evaluatedArgs.right.flatMap(args => apply(func, args))
      
    case badForm =>
      Left(BadSpecialForm("Unrecognized special form", badForm))
  }
  
  def apply(funName: String, lvs: List[LispVal]): ThrowsError[LispVal] = {
    val fun = primitives.get(funName)
    val result = fun.map(f => f(lvs))
    result.getOrElse(Left(NotFunction("Unrecognized primitive function args", funName)))
  }

  val primitives: Map[String, List[LispVal] => ThrowsError[LispVal]] = Map(
    "+" -> numericBinop((x, y) => x + y),
    "-" -> numericBinop((x, y) => x - y),
    "*" -> numericBinop((x, y) => x * y),
    "/" -> numericBinop((x, y) => x / y),
    "remainder" -> numericBinop((x, y) => x % y)
  )
  
  def unpackNum(lv: LispVal): ThrowsError[Int] = lv match {
    case Number(n) => 
      Right(n)
    case LispString(s) if s.matches("\\d+") => 
      Right(s.toInt)
    case LispList(List(lv)) => 
      unpackNum(lv)
    case notNum => 
      Left(TypeMismatch("number", notNum))
  }
  
  def numericBinop(op: (Int, Int) => Int)(args: List[LispVal]): ThrowsError[LispVal] = {
    args match {
      case singleVal @ List(_) =>
        Left(NumArgs(2, singleVal))
      case args => 
        def reduce(fst: ThrowsError[Int], snd: ThrowsError[Int]): ThrowsError[Int] = {
          (fst, snd) match {
            case (Right(n1), Right(n2)) =>
              Right(op(n1, n2))
            case (Left(error), _) =>
              Left(error)
            case (_, Left(error)) =>
              Left(error)
          }
        }
        args.map(unpackNum).reduceRight(reduce).right.map(n => Number(n))
    }
  }
  
  def main(args: Array[String]) {    
    val expr = readExpr(args(0))
    val evaluated = expr.right.flatMap(eval)
    val result = evaluated.right.map(showVal)
    println(result.left.map(showError).merge)
  }
}