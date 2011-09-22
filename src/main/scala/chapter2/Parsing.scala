package chapter2

import scala.util.parsing.combinator.RegexParsers

abstract class LispVal
case class Atom(name: String) extends LispVal
case class LispList(elems: List[LispVal]) extends LispVal
case class DottedList(elems: List[LispVal], last: LispVal) extends LispVal
case class Number(i: Int) extends LispVal
case class LispString(s: String) extends LispVal
case class Bool(b: Boolean) extends LispVal

object Parser extends RegexParsers {

  override val skipWhitespace = false

  val symbol: Parser[String] = regex("[!#$%&|\\*-+/:<=>?@^_~]".r)

  def readExpr(input: String) = {
    parse(parseExpr, input) match {
      case Success(res, _) => "Found value: «"+ res +"»"
      case NoSuccess(msg, _) => "No match: «"+ msg +"»"
    }
  }

  def main(args: Array[String]) {
    println(readExpr(args(0)))
  }
  
  val space = rep1(" ")
  
  val parseString: Parser[LispString] = "\"" ~> rep("[^\"]".r) <~ "\"" map {
    ss => LispString(ss.mkString)
  }

  val letter = regex("[a-zA-Z]".r)

  val digit = regex("[0-9]".r)

  val parseAtom = (letter | symbol) ~ rep(letter | symbol | digit) map {
    case first ~ rest =>
      val atom = first + rest.mkString
      atom match {
        case "#t" => Bool(true)
        case "#f" => Bool(false)
        case _ => Atom(atom)
      }
  }

  val parseNumber = rep1(digit) map {
    n => Number(n.mkString.toInt)
  }

  val parseExpr: Parser[LispVal] = parseAtom | parseNumber | parseString | parseQuoted |
  "(" ~> (parseDottedList | parseList) <~ ")"
  
  val parseList = repsep(parseExpr, space) map {
    exprs => LispList(exprs)
  }
    
  val parseDottedList = rep1(parseExpr <~ space) ~ ("." ~ space ~> parseExpr) map {
    case head ~ tail => DottedList(head, tail)
  }
    
  val parseQuoted = "'" ~> parseExpr map {
    expr => LispList(List(Atom("quote"), expr))
  }
}