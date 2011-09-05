package chapter2

import scala.util.parsing.combinator.RegexParsers
import scala.{ List => ScalaList }
import java.lang.{ String => ScalaString }

sealed abstract class LispVal
case class Atom(name: ScalaString) extends LispVal
case class List(elems: ScalaList[LispVal]) extends LispVal
case class DottedList(elems: ScalaList[LispVal], last: LispVal) extends LispVal
case class Number(i: Int) extends LispVal
case class String(s: ScalaString) extends LispVal
case class Bool(b: Boolean) extends LispVal

object Parser extends RegexParsers {

  def readExpr(source: ScalaString): ScalaString = {
    parse(parseExpr, source) match {
      case Success(res, _) => "Found value: «%s»" format res
      case NoSuccess(msg, _) => "No match: «%s»" format msg
    }
  }

  val parseString = "\"" ~> rep("[^\"]".r) <~ "\"" ^^ {
    str => String(str.mkString)
  }

  val letter = "[a-zA-Z]".r

  val digit = "[0-9]".r

  val symbol = "[!#$%&|\\*-+/:<=>?@^_~]".r

  val parseAtom = (letter | symbol) ~ rep(letter | symbol | digit) ^^ {
    case first ~ rest =>
      val atom = first + rest.mkString
      atom match {
        case "#t" => Bool(true)
        case "#f" => Bool(false)
        case _ => Atom(atom)
      }
  }

  val parseNumber = digit ^^ {
    n => Number(n.toInt)
  }

  val parseAnyList = "(" ~> repsep(parseExpr, space) ~ opt(space ~ "." ~ space ~> parseExpr) <~ ")" ^^ {
    case head ~ Some(tail) => DottedList(head, tail)
    case head ~ None => List(head)
  }

  val parseExpr: Parser[LispVal] = parseAtom | parseNumber | parseString | parseQuoted |
    "(" ~> (parseDottedList | parseList) <~ ")"

  val parseDottedList = rep1(parseExpr <~ space) ~ ("." ~ space ~> parseExpr) ^^ {
    case head ~ tail => DottedList(head, tail)
  }

  val parseQuoted = "'" ~> parseExpr ^^ {
    expr => List(Atom("quote") :: expr :: Nil)
  }

  val parseList = repsep(parseExpr, space) ^^ List

  val space = rep1(" ")

  override val skipWhitespace = false

  def main(args: Array[ScalaString]) {
    val input = "(#t '(quoted (dotted * 5)) test)"

    println(readExpr(input))
  }
}