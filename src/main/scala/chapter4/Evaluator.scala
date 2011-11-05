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

  def throwError(e: LispError): ThrowsError[Nothing] = Left(e)
  
  def trapError(action: ThrowsError[String]): ThrowsError[String] = {
    action match {
      case Left(error) => Right(showError(error))
      case right => right
    }
  }

  def extractValue[T](te: ThrowsError[T]): T = te.right.get
  
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
  
  def eval(lv: LispVal): ThrowsError[LispVal] = lv match {
    case v @ LispString(_) => Right(v)
    case v @ Number(_) => Right(v)
    case v @ Bool(_) => Right(v)
    case LispList(Atom("quote") :: v :: Nil) => Right(v)  
    case LispList(Atom(func) :: args) => 
      
      val acc: ThrowsError[List[LispVal]] = Right(Nil)
      
      val evaluatedArgs = args.foldRight(acc) { (lv, acc) =>
        eval(lv).fold(throwError, lv => acc.fold(throwError, lvs => Right(lv :: lvs)))
      }
      
      val evaluatedArgs2 = args.foldRight(acc) { (lv, acc) =>
        (eval(lv), acc) match {
          case (Right(lv), Right(lvs)) =>
            Right(lv :: lvs)
          case (Left(error), _) => 
            throwError(error)
        }
      }
      
      evaluatedArgs2.fold(throwError, args => apply(func, args))
      
    case badForm =>
      throwError(BadSpecialForm("Unrecognized special form", badForm))
  }
  
  def readExpr(input: String): ThrowsError[LispVal] = {
    import Parser._
    parse(parseExpr, input) match {
      case Success(res, _) => 
        Right(res)
      case NoSuccess(msg, _) => 
        throwError(ParseError(msg))
    }
  }
  
  def main(args: Array[String]) {
    val result = readExpr(args(0)).right.map(eval).joinRight.right.map(showVal)
    println(extractValue(trapError(result)))
  }
  
  def apply(funName: String, lvs: List[LispVal]): ThrowsError[LispVal] = {
    val result = primitives.get(funName).map(f => f(lvs))
    result.getOrElse(throwError(NotFunction("Unrecognized primitive function args", funName)))
  }

  val primitives: Map[String, List[LispVal] => ThrowsError[LispVal]] = Map(
    "+" -> numericBinop((x, y) => x + y),
    "-" -> numericBinop((x, y) => x - y),
    "*" -> numericBinop((x, y) => x * y),
    "/" -> numericBinop((x, y) => x / y),
    "remainder" -> numericBinop((x, y) => x % y)
  )
  
  def numericBinop(op: (Int, Int) => Int)(args: List[LispVal]): ThrowsError[LispVal] = {
    def unpackNum(lv: LispVal): ThrowsError[Int] = lv match {
      case Number(n) => 
        Right(n)
      case LispString(s) if s.matches("\\d+") => 
        Right(s.toInt)
      case LispList(List(lv)) => 
        unpackNum(lv)
      case notNum => 
        throwError(TypeMismatch("number", notNum))
    }
    args match {
      case singleVal @ (Nil | List(_)) =>
        throwError(NumArgs(2, singleVal))
      case args =>
        val result = args.map(unpackNum).reduceRight { (unpacked1, unpacked2) =>
          (unpacked1, unpacked2) match {
            case (Right(n1), Right(n2)) =>
              Right(op(n1, n2))
            case (Left(error), _) =>
              throwError(error)
          }
        }
        result.right.map(Number)
    }
  }
}