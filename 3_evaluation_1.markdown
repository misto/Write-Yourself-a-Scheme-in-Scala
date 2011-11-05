---
layout: default
---

## Evaluation Part 1

Currently, we've just been printing out whether or not we recognize the given
program fragment. We're about to take the first steps towards a working Scheme
interpreter: assigning values to program fragments. We'll be starting with baby
steps, but fairly soon you'll be progressing to doing working computations.

Again, we start with a new package, import the declarations of the previous
chapter and create a new object for our evaluator.

{% highlight scala %}
package chapter3

import chapter2._

object Evaluator {

}
{% endhighlight %}

Let's start by telling Scala how to print out a string representation of the
various possible `LispVals`:

{% highlight scala %}
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
{% endhighlight %}

We are again using pattern matching to handle the different `LispVals`. From the `Bool` case, you can see that pattern matching can also be used to check against concrete values without binding it to a name. The right hand side of each case-clause results in a `String`.

The `LispList` and `DottedList` clauses work similarly, but we need to define a helper function `unwordsList` to convert the contained list into a `String`.

The `unwordsList` function glues together a list of words with spaces. Since we're dealing with a list of `LispVals` instead of `Strings`, we first convert the `LispVals` into their string representations via `showval` and then concatenate them with `mkString`:

{% highlight scala %}
  def unwordsList(lvs: List[LispVal]): String = {
    lvs.map(showVal).mkString(" ")
  }
{% endhighlight %}

Let's try things out by creating a new `readExpr` function so it returns the string representation of the value actually parsed, instead of just "Found value":

{% highlight scala %}
  def readExpr(input: String): String = {
    import Parser._
    parse(parseExpr, input) match {
      case Success(res, _) => 
        showVal(res)
      case NoSuccess(msg, _) => 
        "No match: «"+ msg +"»"
    }
  }
{% endhighlight %}

We are using the `parse` method from Chapter 2's `Parser`, `Success` and `NoSuccess` are also coming from `Parser`. First we import the declarations of the `Parser` object, so we can bring all the definitions we use into our scope. Otherwise, we would need to write `Parser.parse` and `Parser.Success`. We then pattern match on the result as usual but use `showVal` to turn the parse tree into a readable string.

All that's left is a `main` method and then we're ready to run it.

{% highlight scala %}
  def main(args: Array[String]) {
    println(readExpr(args(0)))
  }
{% endhighlight %}

    % scala chapter3.Evaluator "(1 2 2)"
    (1 2 2)
    % scala chapter3.Evaluator "'(1 3 (\"this\" \"one\"))"
    (quote (1 3 ("this" "one")))

### Beginnings of an evaluator: Primitives

Now, we start with the beginnings of an evaluator. The purpose of an evaluator is to map some "code" data type into some "data" data type, the result of the evaluation. In Lisp, the data types for both code and data are the same, so our evaluator will return a `LispVal`. Other languages often have more complicated code structures, with a variety of syntactic forms.

Evaluating numbers, strings, booleans, and quoted lists is fairly simple: return the datum itself.

{% highlight scala %}
  def eval(lv: LispVal): LispVal = lv match {
    case v @ LispString(_) => v
    case v @ Number(_) => v
    case v @ Bool(_) => v
    case LispList(List(Atom("quote"), v)) => v   
}
{% endhighlight %}

This introduces a new type of pattern. The notation `v @ LispString(_)` matches against any `LispVal` that's a `String` and then binds `v` to the whole `LispVal`, and not just the contents of the `LispString`. The result has type `LispVal` instead of type `String`. Again, the underbar is the "don't care" variable, matching any value yet not binding it to a variable. It can be used in any pattern, but is most useful with @-patterns (where you bind the variable to the whole pattern) and with simple type-tests where you're just interested in the type of the matchee.

The last clause is our first introduction to nested patterns. The type of data contained by `LispList` is `List[LispVal]`, a list of `LispVals`. We match that against the specific two-element list `List(Atom("quote"), v)`, a list where the first element is the symbol "quote" and the second element can be anything. Then we return that second element. Another way to write exactly the same pattern is

{% highlight scala %}
    case LispList(Atom("quote") :: v :: Nil) => v
{% endhighlight %}

The cons operator `::` can be used to construct and destruct lists. You can read it as `Atom("quote")` prepended to a `LispVal` bound to `v` prepended to `Nil`, which stands for the empty list.

Let's integrate eval into our existing code. Start by changing `readExpr` back so it returns the expression instead of a string representation of the expression:

{% highlight scala %}
  def readExpr(input: String): LispVal = {
    import Parser._
    parse(parseExpr, input) match {
      case Success(res, _) => 
        res
      case NoSuccess(msg, _) => 
        LispString("No match: «" + msg + "»")
    }
  }
{% endhighlight %}

In the error case, we simply wrap the error message in a `LispString`. And then change our `main` function to read an expression, evaluate it, convert it to a string, and print it out.

{% highlight scala %}
  def main(args: Array[String]) {
    println(showVal(eval(readExpr(args(0)))))
  }
{% endhighlight %}

Compile and run the code the normal way:

    % scala chapter3.Evaluator "'atom" 
    atom
    % scala chapter3.Evaluator 2       
    2
    % scala chapter3.Evaluator "\"a string\""
    "a string"
    % scala chapter3.Evaluator "(+ 2 2)"
    scala.MatchError: LispList(List(Atom(+), Number(2), Number(2))) (of class chapter2.LispList)

We still can't do all that much useful with the program (witness the failed (+ 2 2) call), but the basic skeleton is in place. Soon, we'll be extending it with some functions to make it useful.

### Adding basic primitives

Next, we'll improve our Scheme so we can use it as a simple calculator. It's still not yet a "programming language", but it's getting close.

Begin by adding a clause to eval to handle function application.

{% highlight scala %}
    case LispList(Atom(func) :: args) => 
      apply(func, args map eval)
{% endhighlight %}

This clause matches all lists that start with an atom and a varying number of arguments: `args` simply matches the whole tail of the list. For example, if we passed `(+ 2 2)` to `eval`, `func` would be bound to `+` and `args` would be bound to `List(Number(2), Number( 2))`. 

The rest of the clause consists of a couple of functions we've seen before and one we haven't defined yet. We have to recursively evaluate each argument, so we map `eval` over the `args`. This is what lets us write compound expressions like `(+ 2 (- 3 1) (* 5 4))`. Then we take the resulting list of evaluated arguments, and pass it and the original function to `apply`:

{% highlight scala %}
  def apply(funName: String, lvs: List[LispVal]): LispVal = {
    val fun = primitives.get(funName)
    val result = fun.map(f =>  f(lvs))
    result.getOrElse(Bool(false))
  }
{% endhighlight %}

`primitives` is a map of strings to functions we will define in a moment. The `get` method looks up a key; however, lookup will fail if no entry in the map contains the matching key. To express this, it returns an instance of the type `Option`. This means that `fun` is either some function or `None`, if the user tried to evaluate a non-existing function. We can use `Options` `map` function to safely work with the value wrapped inside the `Option`, in the `None` case, it simply returns another `None`, and if we have a function, we apply it to the arguments in `lvs`. The `result` is again of type `Option`. `getOrElse` allows us the unwrap the value contained in the `Option`. If the function isn't found, we return a `Bool(False)` value, equivalent to `#f` (we'll add more robust error-checking later).

Once you're more familiar with optional values and higher-order functions, you can write the above as:

{% highlight scala %}
  def apply(funName: String, lvs: List[LispVal]): LispVal = {
    primitives.get(funName).map(f =>  f(lvs)).getOrElse(Bool(false))
  }
{% endhighlight %}

Next, we define the map of primitives that we support:

{% highlight scala%}
  val primitives: Map[String, List[LispVal] => LispVal] = Map(
    "+" -> numericBinop((x, y) => x + y),
    "-" -> numericBinop((x, y) => x - y),
    "*" -> numericBinop((x, y) => x * y),
    "/" -> numericBinop((x, y) => x / y),
    "remainder" -> numericBinop((x, y) => x % y)
  )
{% endhighlight %}

Look at the type of `primitives`. It's a map from `String` to `List[LispVal] => LispVal`, so the values are functions! In Scala, you can easily store functions in data structures. The `Map` is built from a number of `Pairs`. The Scala standard library defines a `->` infix operator so pairs can be constructed in a visually pleasing way. Also, the functions that we store are themselves the result of a function, `numericBinop`, which we haven't defined yet. (This is why we say that Scala has first-class functions, it's simly the ability to store, pass as arguments and return functions.)

`numericBinop` takes a function that works on two `Ints` and wraps it with code to unpack an argument list, apply the function to it, and wrap the result up in our `Number` constructor.

{% highlight scala %}
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
    Number(args.map(unpackNum).reduceLeft(op))
  }
{% endhighlight %}

As with R5RS Scheme, we don't limit ourselves to only two arguments. Our numeric operations can work on a list of any length, so (+ 2 3 4) = 2 + 3 + 4, and (- 15 5 3 2) = 15 - 5 - 3 - 2. We first define another helper method `unpackNum` inside `numericBinop`, which converts a `LispVal` into an `Int`.

Unlike R5RS Scheme, we're implementing a form of weak typing. That means that if a value can be interpreted as a number (like the string "2"), we'll use it as one, even if it's tagged as a string. We do this by adding a couple extra clauses to `unpackNum`. 

If we're unpacking a `LispString` that consists of only digits, then we convert it using the `toInt` method.

For lists, we pattern-match against the one-element list and try to unpack that. Anything else falls through to the next case.

If we can't parse the number, for any reason, we'll return 0 for now. We'll fix this shortly so that it signals an error.

Compile and run this the normal way. Note how we get nested expressions "for free" because we call eval on each of the arguments of a function:

    % scala chapter3.Evaluator "(+ 2 2)"
    4
    % scala chapter3.Evaluator "(+ 2 (-4 1))"
    2
    % scala chapter3.Evaluator "(+ 2 (- 4 1))"
    5
    % scala chapter3.Evaluator "(- (+ 4 6 3) 3 5 2)"
    3

Exercises:

1. Add primitives to perform the various [type-testing](http://www.schemers.org/Documents/Standards/R5RS/HTML/r5rs-Z-H-9.html#%_sec_6.3) functions of R5RS: symbol?, string?, number?, etc.
1. Change unpackNum so that it always returns 0 if the value is not a number, even if it's a string or list that could be parsed as a number.
1. Add the [symbol-handling functions](http://www.schemers.org/Documents/Standards/R5RS/HTML/r5rs-Z-H-9.html#%_sec_6.3.3) from R5RS. A symbol is what we've been calling an Atom in our data constructors.

In the next chapter, we’re going to add [error checking](4_error_checking_and_exceptions.html).

