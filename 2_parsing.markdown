---
layout: default
---

## Writing a Simple Parser

Now, let's try writing a very simple parser. The Scala standard library already
comes with a parser-combinators library, so we won't need to install anything.

We start with a new package and add an import to the parsing library.

{% highlight scala %}
package chapter2

import scala.util.parsing.combinator.RegexParsers
{% endhighlight %}

RegexParsers comes with a few handy functions to use regular expressions and
string literals that will serve as the basic building blocks for our parser.

We again start with an object, but this time let it extend `RegexParsers`.
A `RegexParser` skips whitespace characters by default, but for .. purposes, we
want to do this ourselves, so we disable the skipping of whitespace by setting
the value to false. The `override` keyword is necessary whenever we override a
concrete method or value.

{% highlight scala %}
object Parser extends RegexParsers {
  override val skipWhitespace = false

  // the rest of the code from this chapter comes here
}
{% endhighlight %}

Now, we'll define a parser that recognizes one of the symbols allowed in Scheme
identifiers.

{% highlight scala %}
  val symbol = regex("[!#$%&|\\*-+/:<=>?@^_~]".r)
{% endhighlight %}

The `regex` method we inherited from `RegexParsers` let's us create a parser
from a regular expression. Regular expressions can be created with the `r`
method on any `String` instance. The parser is assigned to the value `symbol`.
The type of `symbol` is `Parser[String]`, but Scala can figure this out from the
`regex` expression, so we can omit the type. If you prefer to see the types in
the source code, you can also write:

{% highlight scala %}
  val symbol: Parser[String] = regex("[!#$%&|\\*-+/:<=>?@^_~]".r)
{% endhighlight %}

Let's define a method to call our parser and handle any possible errors:

{% highlight scala %}

  def readExpr(input: String): String = {
    parse(symbol, input) match {
      case Success(res, _)   => "Found value: «"+ res +"»"
      case NoSuccess(msg, _) => "No match: «"   + msg +"»"
    }
  }
{% endhighlight %}

As you can see from the type signature, readExpr is a method from a `String` to
a `String`. We name the parameter `input`, and pass it, along with the `symbol`
parser we defined above to the method `parse`.

`parse` returns a value of type `ParseResult[String]` and there are exactly two
kinds of results we care about: either the parsed `String` value or an error
message. To distinguish between the two possible cases, we use a
pattern-matching expression. Pattern matching is similar to C and Java's
switch-case statements, except that you're not limited to a predefined set of
types, but can switch on all types and more.

The block with the case statements is a function which transforms a
`ParseResult` into a `String`, and `match` applies the function to the result
of `parse`. Pattern matching is often done on algebraic data types because
Scala provides a convenient way to _deconstruct_ an object and to bind the
object's components to values. In our code, we bind the result to the name
`res`, which can then be used in the code following the _fat arrow_. And in the
failure case, we bind the message to `msg`.

{% highlight scala %}
  case Success(res, _)   => ... res
  case NoSuccess(msg, _) => ... msg
{% endhighlight %}

Don't worry if you don't fully understand pattern matching yet. It's a very
powerful feature and we will be using it a lot in this tutorial.

What about the underscore? `Success` and `NoSuccess` also provide us with the
remainder of the input, but we don't care about it and ignore it by binding it
to `_`. You will encounter the underscore in various places in Scala. It's
typically used when you don't want to give something an explicit name, either
because you want to ignore it or because an explicit name would not provide any
useful additional information.

Finally we also need a `main` method to be able to run our program!

{% highlight scala %}
  def main(args: Array[String]) {
    println(readExpr(args(0)))
  }
{% endhighlight %}

How to run your code depends on your chosen IDE, or you can run your program
from the command line. The command line has the benefit that it's easy to play
with different arguments, which can be painful (i.e. needs several UI actions)
in an IDE. If you follow this tutorial's code organization, you should have a
directory hierarchy with several class files, similar to this:

    ~/Write-Yourself-a-Scheme-Code/.target/scala-2.9.0.1/classes % tree
    .
    ├── chapter1
    │   ├── HelloWorld.class
    │   └── HelloWorld$.class
    ├── chapter2
    │   ├── Parser$$anonfun$10.class
    │   ├── Parser.class
    │   ├── Parser$.class
    │   ├── ...

You can now run your program with `scala chapter2.Parser "-"`. Note that you
need to invoke this from the root of your package hierarchy, don't enter
`chapter2`.

    % scala chapter2.Parser :
    Found value: «:»
    % scala chapter2.Parser abc
    No match: «string matching regex `[!#$%&|\*-+/:<=>?@^_~]' expected but `a' found»

## Whitespace

Next, we'll add a series of improvements to our parser that'll let it recognize
progressively more complicated expressions. The current parser chokes if there's
whitespace preceding our symbol:

    % scala chapter2.Parser " :"
    No match: «string matching regex `[!#$%&|\*-+/:<=>?@^_~]' expected but ` ' found»

Let's fix that, so that we ignore whitespace.

First, lets define a parser that recognizes any number of whitespace characters.
Incidentally, this is why we overrode the `skipWhitespace` val when we derived
our parser, because without it our parser would already skip whitespace by
itself.

{% highlight scala %}
  val space = rep1(" ")
{% endhighlight %}

Our `space` parser simply accepts a non-emtpy sequence of spaces. We also need
to adapt our `readExpr` function to use the parser:

{% highlight scala %}
    parse(space ~> symbol, input) match {
      case Success(res, _) => "Found value: «"+ res +"»"
      case NoSuccess(msg, _) => "No match: «"+ msg +"»"
    }
{% endhighlight %}

We used the `~>` operator to combine the two parsers. `~>` attempts to match the
first parser, then attempts to match the second with the remaining input, and
fail if either fails. The result of the first parser is ignored, that's why the
operator "points" to the second parser.

Compile and run this code. Note that since we defined spaces in terms of
`rep1`, it will no longer recognize a plain old single character. Instead
you have to precede a symbol with some whitespace. We'll see how this is useful
shortly:

    % scala chapter2.Parser "      %"
    Found value: «%»
    % scala chapter2.Parser "%"
    No match: «` ' expected but `%' found»
    % scala chapter2.Parser "     abc"
    No match: «string matching regex `[!#$%&|\*-+/:<=>?@^_~]' expected but `a' found»

## Return Values

Right now, the parser doesn't do much of anything -- it just tells us whether a
given string can be recognized or not. Generally, we want something more out of
our parsers: we want them to convert the input into a data structure that we
can traverse easily. In this section, we learn how to define a data type, and
how to modify our parser so that it returns this data type.

First, we need to define a data type that can hold any Lisp value:

{% highlight scala %}
abstract class LispVal
case class Atom(name: String) extends LispVal
case class LispList(elems: List[LispVal]) extends LispVal
case class DottedList(elems: List[LispVal], last: LispVal) extends LispVal
case class Number(i: Int) extends LispVal
case class LispString(s: String) extends LispVal
case class Bool(b: Boolean) extends LispVal
{% endhighlight %}

This is an example of an algebraic data type: it defines a set of possible
values that a variable of type LispVal can hold. Scala models algebraic data
types with inherintance: we have a `LispVal` class and one derived class for
each of the possible Lisp values our parser will recognize. Our subclasses are
not ordinary classes but _case classes_. In this example, a `LispVal` can be:

1. An `Atom`, which stores a `String` naming the atom
1. A `LispList`, which stores an ordinary Scala  `List` of other LispVals; also called a proper list
1. A `DottedList`, representing the Scheme form `(a b . c)`; also called an improper list. This stores a list of all elements but the last, and then stores the last element as another field
1. A `Number`, containing a Scala Int
1. A `LispString`, containing a Scala `String`
1. A `Bool`, containing a Scala `Boolean` value

The parameter list following the class name names the parameter list of the
primary constructor. You can have several constructors in Scala, but there
always needs to be a primary constructor that has to be called eventually. Because we defined our class to be a `case` class, the Scala compiler will generate several useful methods for us, and most importantly, we can use the classes in pattern matching to access their attributes.

Next, let's add a few more parsing functions to create values of these types. A string is a double quote mark, followed by any number of non-quote characters, followed by a closing quote mark:

{% highlight scala %}
  val parseString = "\"" ~> rep("[^\"]".r) <~ "\"" map {
    ss: List[String] => LispString(ss.mkString)
  }
{% endhighlight %}

Instead of just returning the matched string, we map the Scala `String` into
our own `LispString` data type. The argument to map is a function, which takes
an argument `ss` of type `List[String]` and returns a `LispString`. The
`mkString` method simply concatenates all the single character strings. A case
class also acts like a function, but you could also have written `new
LispString(...)` instead.

Type inference can also help us to shorten anonymous function expressions, so
it's allowed to omit the type annotation of the argument:

{% highlight scala %}
  val parseString = "\"" ~> rep("[^\"]".r) <~ "\"" map {
    ss => LispString(ss.mkString)
  }
{% endhighlight %}

What type does `parseString` have? `symbol` was of type `Parser[String]`, but because we mapped our `String` to a `LispString` (`String => LispString` is the type of the function we passed to map), `parseString` is now of type `Parser[LispString]`.

Now let's move on to Scheme variables. An atom is a letter or symbol, followed by any number of letters, digits, or symbols:

{% highlight scala %}

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
{% endhighlight %}

The `letter` and `digit` parser are straight forward, we simply use a regular
expression to do all the work.

Then, we introduce another parser combinator, the choice operator `|`. This
tries the first parser, then if it fails, tries the second. If either succeeds,
then it returns the value returned by that parser.

We also want to get a `LispVal`, so we have to map the parser's result. Note
that this time, we have to handle the results of two parsers that are combined
with `~`, we don't ignore one side of the parser like before. The `letter | symbol` parser gives us a `String`, and the `rep` combinator will result in a `List[String]`. So to map the
result, we need a function of type `String ~ List[String] => LispVal`.

Now instead of passing just a function to `map`, we pass a pattern matching
expression, which allows us to break down the parser result. We bind the result
of the first parser to `first` and the second one to `rest`.

Once we've read the first character and the rest of the atom, we need to put
them together. The val statement defines a new `String` variable `atom`.

Then we use a pattern matching expression to determine which `LispVal` to
create and return, matching against the literal strings for true and false. The
underscore `_` alternative is a readability trick: case blocks continue until a
`_` case (or fail any case which also causes the failure of the whole case
expression), think of `_` as a wildcard. So if the code falls through to the
`_` case, it always matches, and returns the value of `atom`.

Finally, we create one more parser, for numbers.

{% highlight scala %}
  val parseNumber: Parser[LispVal] = rep1(digit) map {
    n => Number(n.mkString.toInt)
  }
{% endhighlight %}

Let's create a parser that accepts either a string, a number, or an atom:

{% highlight scala %}
  val parseExpr: Parser[LispVal] =  parseString | parseNumber | parseAtom
{% endhighlight %}

And edit readExpr so it calls our new parser:

{% highlight scala %}
 def readExpr(input: String) = {
    parse(parseExpr, input) match {
      case Success(res, _) => "Found value: «"+ res +"»"
      case NoSuccess(msg, _) => "No match: «"+ msg +"»"
    }
  }
{% endhighlight %}

Compile and run this code, and you'll notice that it accepts any number,
string, or symbol, but not other strings:

    % scala chapter2.Parser "\"this is a string\""
    Found value: «LispString(this is a string)»
    % scala chapter2.Parser 25
    Found value: «Number(25)»
    % scala chapter2.Parser symbol
    Found value: «Atom(symbol)»
    % scala chapter2.Parser "(symbol)"
    No match: «`"' expected but `(' found»

### Exercises

1. Change `parseNumber` to support the [Scheme standard for different bases](http://www.schemers.org/Documents/Standards/R5RS/HTML/r5rs-Z-H-9.html#%_sec_6.2.4). 
1. Add a Character constructor to `LispVal`, and create a parser for [character
literals](http://www.schemers.org/Documents/Standards/R5RS/HTML/r5rs-Z-H-9.html#%_sec_6.3.4) as described in R5RS.
1. Add a `Float` constructor to `LispVal`, and support R5RS syntax for [decimals](http://www.schemers.org/Documents/Standards/R5RS/HTML/r5rs-Z-H-9.html#%_sec_6.2.4).
1. Add data types and parsers to support the [full numeric tower](http://www.schemers.org/Documents/Standards/R5RS/HTML/r5rs-Z-H-9.html#%_sec_6.2.1) of Scheme
numeric types. 

## Recursive Parsers: Adding lists, dotted lists, and quoted datums

Next, we add a few more parser actions to our interpreter. Start with the parenthesized lists that make Lisp famous:

{% highlight scala %}
  val parseList = repsep(parseExpr, space) map {
    exprs => LispList(exprs)
  }
{% endhighlight %}

The `repsep` combinator takes two parsers and repeatedly uses `parseExpr' interleaved with `space'. 

The dotted-list parser is somewhat more complex, but still uses only concepts that we're already familiar with:

{% highlight scala %}
  val parseDottedList = rep1(parseExpr <~ space) ~ ("." ~ space ~> parseExpr) map {
    case head ~ tail => DottedList(head, tail)
  }
{% endhighlight %}

With a clever use of `~>` and `<~` we can get a parser that gives us just the
parts we are interested in, which we can then convert to a `DottedList`.

Next, let's add support for the single-quote syntactic sugar of Scheme:

{% highlight scala %}
  val parseQuoted = "'" ~> parseExpr map {
    expr => LispList(Atom("quote") :: expr :: Nil)
  }
{% endhighlight %}

Most of this is fairly familiar stuff: it reads a single quote character, reads
an expression and binds it to `expr`, and then returns a `LispList` with an
additional `quote` atom. The `::` is used to prepend the `quote` atom and the
`expr` to the empty list `Nil`. We could also have written it like this:

{% highlight scala %}
  LispList(List(Atom("quote"), expr))
{% endhighlight %}

Finally, edit our definition of `parseExpr` to include our new parsers:

{% highlight scala %}
  val parseExpr: Parser[LispVal] = parseAtom | parseNumber | parseString | parseQuoted |
    "(" ~> (parseDottedList | parseList) <~ ")"
{% endhighlight %}

Compile and run this code:

    % scala chapter2.Parser "(a test)"        
    Found value: «LispList(List(Atom(a), Atom(test)))»
    % scala chapter2.Parser "(a (nested) test)"
    Found value: «LispList(List(Atom(a), LispList(List(Atom(nested))), Atom(test)))»
    % scala chapter2.Parser "(a (dotted . list) test)"
    Found value: «LispList(List(Atom(a), DottedList(List(Atom(dotted)),Atom(list)), Atom(test)))»
    % scala chapter2.Parser "(a '(quoted (dotted . list)) test)"
    Found value: «LispList(List(Atom(a), LispList(List(Atom(quote), 
        LispList(List(Atom(quoted), DottedList(List(Atom(dotted)),Atom(list)))))), Atom(test)))»
    % scala chapter2.Parser "(a '(imbalanced parens)"           
    No match: «`)' expected but end of source found»

Note that by referring to parseExpr within our parsers, we can nest them
arbitrarily deep. Thus, we get a full Lisp reader with only a few definitions.
That's the power of recursion.

In the next chapter, we're going to [write an evaluator](3_evaluation_1.html)!

{% highlight scala %}
{% endhighlight %}
