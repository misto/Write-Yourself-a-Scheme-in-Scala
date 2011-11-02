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

{% highlight scala % }
package chapter3

import chapter2._

object Evaluator {

}
{% endhighlight %}

Let's start by telling Scala how to print out a string representation of the
various possible `LispVals`:

{% highlight scala % }
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

{% highlight scala % }
  def unwordsList(lvs: List[LispVal]): String = {
    lvs.map(showVal).mkString(" ")
  }
{% endhighlight %}

Let's try things out by creating a new `readExpr` function so it returns the string representation of the value actually parsed, instead of just "Found value":

{% highlight scala % }
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

{% highlight scala % }
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

{% highlight scala % }
  def eval(lv: LispVal): LispVal = lv match {
    case v @ LispString(_) => v
    case v @ Number(_) => v
    case v @ Bool(_) => v
    case LispList(List(Atom("quote"), v)) => v   
}
{% endhighlight %}

This introduces a new type of pattern. The notation `v @ LispString(_)` matches against any `LispVal` that's a `String` and then binds `v` to the whole `LispVal`, and not just the contents of the `LispString`. The result has type `LispVal` instead of type `String`. Again, the underbar is the "don't care" variable, matching any value yet not binding it to a variable. It can be used in any pattern, but is most useful with @-patterns (where you bind the variable to the whole pattern) and with simple type-tests where you're just interested in the type of the matchee.

The last clause is our first introduction to nested patterns. The type of data contained by `LispList` is `List[LispVal]`, a list of `LispVals`. We match that against the specific two-element list `List(Atom("quote"), v)`, a list where the first element is the symbol "quote" and the second element can be anything. Then we return that second element. Another way to write exactly the same pattern is

{% highlight scala % }
    case LispList(Atom("quote") :: v :: Nil) => v
{% endhighlight %}

The cons operator `::` can be used to construct and destruct lists. You can read it as `Atom("quote")` prepended to a `LispVal` bound to `v` prepended to `Nil`, which stands for the empty list.

Let's integrate eval into our existing code. Start by changing `readExpr` back so it returns the expression instead of a string representation of the expression:

{% highlight scala % }
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

{% highlight scala % }
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

{% highlight scala % }
{% endhighlight %}

