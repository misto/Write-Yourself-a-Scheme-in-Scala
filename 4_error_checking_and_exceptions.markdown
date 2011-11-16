---
layout: default
---

## Error Checking and Exceptions

Currently, there are a variety of places within the code where we either ignore
errors or silently assign "default" values like `#f` or `0` that make no sense.
Some languages -- like Perl and PHP -- get along fine with this approach.
However, it often means that errors pass silently throughout the program until
they become big problems, which means rather inconvenient debugging sessions for
the programmer. We'd like to signal errors as soon as they happen and
immediately break out of execution.

We start with a fresh package and import the contents of Chapter 2:

{% highlight scala %}
package chapter4

import chapter2._
{% endhighlight %}

To represent the various different errors our program can encounter, we first
define a new data type:

{% highlight scala %}
abstract class LispError
case class NumArgs(num: Int, args: List[LispVal]) extends LispError
case class TypeMismatch(expected: String, actual: LispVal) extends LispError
case class ParseError(message: String) extends LispError
case class BadSpecialForm(message: String, form: LispVal) extends LispError
case class NotFunction(message: String, func: String) extends LispError
case class UnboundVar(message: String, varname: String) extends LispError
case class Default(message: String) extends LispError
{% endhighlight %}

This is a few more constructors than we need at the moment, but we might as well
forsee all the other things that can go wrong in the interpreter later.

Just like in the previous chapters, the rest of this chapter's program will all
be put into an `Evaluator` object. Next, we define how to print out the various
types of errors:

{% highlight scala %}
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
{% endhighlight %}

We can reuse the `showVal` and `unwordsList` functions from the last chapter, so
either copy their definitions to the new `Evaluator` or import them (before the
definition of `showError`):

{% highlight scala %}
import chapter3.Evaluator.{showVal, unwordsList}
{% endhighlight %}

Then we define a type to represent functions that may throw a LispError or
return a value:

{% highlight scala %}
  type ThrowsError[T] = Either[LispError, T]
{% endhighlight %}

In Scala, we use the `Either` type to represent a value of two possible types.
So an `Either[String, Int]` can hold a single value of either type `String` or
`Int`. To create an instance of such an `Either`, we use the `Left` and `Right`
constructors: `Left("someString")` or `Right(42)`.

`Either` also defines several useful methods to transform and extract the value
it contains. For example, the `fold` method takes two functions and, if it's an
instance of `Left`, applies the first function to the value it contains or if
it's a `Right` it applies the second one.

    scala> val e: Either[String, Int] = Left("42")
    e: Either[String,Int] = Left("42")

    scala> e.fold(s => s.toInt, i => i * 2)
    res0: Int = 42

If we only want to transform one type of value and keep the result inside an
`Either`, we can use the `left` and `right` methods to "focus" on one side and
then `map` the value to something else:

    scala> e.left.map(s => s + s)
    res1: Either[String,Int] = Left("4242")

    scala> e.right.map(s => s * 2)
    res2: Either[String,Int] = Left("42")

In the second case, the result hasn't changed because it's not an instance of
`Right`.

In our improved evaluator, many operations will either successfully result in a
`LispVal` or fail with a `LispError`. To make our code more readable, we
introduced the `ThrowsError` type which fixes the first (left) type of `Either`
to `LispError` and keeps the second (right) type variable.

Now that we have all the basic infrastructure, it's time to start using our
error-handling types. Remember how our parser had previously just returned a
`String` saying "No match .." on an error? Let's change it so that it wraps and
returns the original parse errror:

{% highlight scala %}
  def readExpr(input: String): ThrowsError[LispVal] = {
    import Parser._
    parse(parseExpr, input) match {
      case Success(res, _) =>
        Right(res)
      case NoSuccess(msg, _) =>
        Left(ParseError(msg))
    }
  }
{% endhighlight %}

Here, we first wrap the original error's message with the `LispError`
constructor `Parser` and then wrap it up again in a `Left` instance. Since
`readExpr` now returns a `ThrowsError[LispVal]`, we also need to wrap the
success case in a `Right`.

Next, we change the type signature of `eval` to return a `ThrowsError` value,
adjust the return values accordingly, and add a clause at the end to throw an
error if we encounter a pattern that we don't recognize:

{% highlight scala %}
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
{% endhighlight %}

Since the function application clause calls `eval` (which now returns an
`Either` value) recursively, we need to change that clause. First, we want to
evaluate the arguments, and then apply them to the function. If one of the
argument fails to evaluate, we return that failure, if all the arguments
evaluate correctly, the result will be a `List[LispVal]` of the evaluated
arguments.

We start with a helper function `evalArg` that takes a `LispVal` and an
accumulator `acc` to which it simply prepends (`::`) the evaluated argument. If
`eval(lv)` returns a failure, this will also be the return value of the method.
So if we have a `Right`, that is, the argument evaluated successfully, then we
want to prepend it to our result list. Now the problem is that the `acc` might
already have failed, meaning that we only want to prepend our evaluated argument
if `acc` is not a failure, so we again need to use the `acc.right.map` trick to
get at the underlying list.

Note that we need to use `flatMap` instead of `map` in `eval(lv).right.flatMap`.
In addition to `map`, `flatMap` "flattens" the result. Remember that `map`
transforms the value inside the `Right` container, so if the result of an
operation is again a `ThrowsError[LispVal]`, the result of the `map` will be a
`ThrowsError[ThrowsError[LispVal]]`. `flatMap` prevents this by flattening the
result into a `ThrowsError[LispVal]`.

(Hint: if you are having trouble understanding the code, try to introduce some
vals and annotate them with their types.)

With this helper function, we can now simply fold the arguments with an empty
list as a starting point:

{% highlight scala %}
      val acc: ThrowsError[List[LispVal]] = Right(Nil)

      val evaluatedArgs = args.foldRight(acc)(evalArg)
{% endhighlight %}

Finally, we can apply the function to the evaluated arguments:

{% highlight scala %}
      evaluatedArgs.right.flatMap(args => apply(func, args))
{% endhighlight %}

Again, we use `flatMap` because after the next step, `apply` will also return a
`ThrowsError[LispVal]`.

{% highlight scala %}
  def apply(funName: String, lvs: List[LispVal]): ThrowsError[LispVal] = {
    val fun = primitives.get(funName)
    val result = fun.map(f => f(lvs))
    result.getOrElse(Left(NotFunction("Unrecognized primitive function args", funName)))
  }
{% endhighlight %}

We didn't wrap the result of the function application `f(lvs)` in a `Right`,
because we're about to change the type of our primitives, so that the function
returned from the lookup itself returns a ThrowsError action:

{% highlight scala %}
  val primitives: Map[String, List[LispVal] => ThrowsError[LispVal]] = Map(
    "+" -> numericBinop((x, y) => x + y),
    "-" -> numericBinop((x, y) => x - y),
    "*" -> numericBinop((x, y) => x * y),
    "/" -> numericBinop((x, y) => x / y),
    "remainder" -> numericBinop((x, y) => x % y)
  )
{% endhighlight %}

And, of course, we need to change the `numericBinop` function that implements
these primitives so it returns an error if there's only one argument. And, to
make our program complete, `unpackNum` also needs to change, after all,
unpacking a number could fail as well. Let's do `unpackNum` first:

{% highlight scala %}
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
{% endhighlight %}

Now to `numericBinop`: if there's only a single argument, we return a `NumArgs`
error, otherwise we unpack all the arguments and reduce them. Similar to our new
`eval` method, `numericBinop` now also needs to handle failures in `unpackNum`.
We take a slightly different approach here and pattern-match on the two
`ThrowsError[Int]` values, and apply `op` if we get two `Right` values. In all
other cases, we simply return an error wrapped in `Left`.

{% highlight scala %}
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
{% endhighlight %}

If you you prefer the `right.map` approach, then this is how the body of
`reduce` would look like:

{% highlight scala %}
fst.right.flatMap(n1 => snd.right.map(n2 => op(n1, n2)))
{% endhighlight %}

The result of `args.map(unpackNum).reduceRight(reduce)` is a ThrowsError[Int],
but we need a ThrowsError[LispVal], so we have to wrap the value in `Right` in a
`Number` constructor.

Once you feel comfortable working with either and its `map` and `flatMap`
methods, you can use Scala's syntactic sugar for sequence comprehensions, which
makes nested `flatMap` and `map` calls much more readable:

{% highlight scala %}
  for {
    n1 <- fst.right
    n2 <- snd.right
  } yield op(n1, n2)
{% endhighlight %}

This behaves exactly the same as the `fst.right.flatMap ..` call from above.

Finally, we need to change our main function to correctly handle all the
`ThrowsError` values. The procedure should be familiar by now: "focus" on the
success-value and `map` it.

{% highlight scala %}
  def main(args: Array[String]) {
    val expr = readExpr(args(0))
    val evaluated = expr.right.flatMap(eval)
    val result = evaluated.right.map(showVal)
    println(result.left.map(showError).merge)
  }
{% endhighlight %}

The type of `result` is `ThrowsError[String]`, but we need a `String` to be able
to print it. By mapping the left `LispError` to a `String`, we get an
`Either[String, String]`, which we can `merge` to a simple `String`.

Compile and run the new code, and try throwing it a couple errors:

    % scala chapter4.Evaluator "(+ 2 \"two\")"
    Invalid type: expected number, found "two"
    % scala chapter4.Evaluator "(+ 2)"
    Expected 2 args; found values 2
    % scala chapter4.Evaluator "(what? 2)"
    Unrecognized primitive function args: what?

Exercises:

1. Go through this chapter's code and use sequence comprehensions where
possible.
