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

It depends on 



{% highlight scala %}
{% endhighlight %}
