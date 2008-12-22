header {
    package com.zutubi.pulse.condition.antlr;
}

{
    import com.zutubi.pulse.bootstrap.ComponentContext;
    import com.zutubi.pulse.condition.*;
}

class NotifyConditionTreeParser extends TreeParser;

{
    private NotifyConditionFactory factory;

    public void setNotifyConditionFactory(NotifyConditionFactory factory)
    {
        this.factory = factory;
    }
}

cond returns [NotifyCondition r]
{
    NotifyCondition a, b;
    r = null;
}
    : #("and" a=cond b=cond) { r = new CompoundNotifyCondition(a, b, false); }
    | #("or" a=cond b=cond) { r = new CompoundNotifyCondition(a, b, true); }
    | #("not" a=cond) { r = new NotNotifyCondition(a); }
    | r=comp
    | r=prev
    | c:boolsymbol { r = factory.createCondition(c.getText()); }
    ;

comp returns [NotifyCondition r]
{
    NotifyValue x, y;
    r = null;
}
    : #(EQUAL x=value y=value) { r = new ComparisonNotifyCondition(x, y, ComparisonNotifyCondition.Op.EQUAL); }
    | #(NOT_EQUAL x=value y=value) { r = new ComparisonNotifyCondition(x, y, ComparisonNotifyCondition.Op.NOT_EQUAL); }
    | #(LESS_THAN x=value y=value) { r = new ComparisonNotifyCondition(x, y, ComparisonNotifyCondition.Op.LESS_THAN); }
    | #(LESS_THAN_OR_EQUAL x=value y=value) { r = new ComparisonNotifyCondition(x, y, ComparisonNotifyCondition.Op.LESS_THAN_OR_EQUAL); }
    | #(GREATER_THAN x=value y=value) { r = new ComparisonNotifyCondition(x, y, ComparisonNotifyCondition.Op.GREATER_THAN); }
    | #(GREATER_THAN_OR_EQUAL x=value y=value) { r = new ComparisonNotifyCondition(x, y, ComparisonNotifyCondition.Op.GREATER_THAN_OR_EQUAL); }
    ;

prev returns [NotifyCondition r]
{
    r = null;
}
    : #("previous" r=cond) { r = factory.build(PreviousNotifyCondition.class, new Class[]{ NotifyCondition.class }, new Object[]{ r }); }
    ;

value returns [NotifyValue r]
{
    r = null;
}
    : r=integer
    | k:strsymbol { r = factory.createStringValue(k.getText()); }
    | l:WORD { r = new LiteralNotifyStringValue(l.getText()); }
    ;

integer returns [NotifyValue r]
{
    r = null;
}
    : i:INTEGER { r = new LiteralNotifyIntegerValue(Integer.parseInt(i.getText())); }
    | r=previnteger
    | j:intsymbol { r = factory.createIntegerValue(j.getText()); }
    ;

previnteger returns [NotifyValue r]
{
    r = null;
}
    : #("previous" r=integer) { r = factory.build(PreviousNotifyIntegerValue.class, new Class[]{ NotifyIntegerValue.class }, new Object[]{ r }); }
    ;
    
boolsymbol
    : "true"
    | "false"
    | "success"
    | "failure"
    | "error"
    | "changed"
    | "changed.by.me"
    | "state.change"
    ;

intsymbol
    : "unsuccessful.count.builds"
    | "unsuccessful.count.days"
    ;

strsymbol
    : "build.specification.name"
    ;

class NotifyConditionParser extends Parser;

options {
        buildAST=true;
        defaultErrorHandler=false;
}

condition
    : orexpression EOF
    ;

orexpression
    :   andexpression ("or"^ andexpression)*
    ;

andexpression
    : notexpression ("and"^ notexpression)*
    ;

notexpression
    : ("not"^)? boolexpression
    ;

boolexpression
    : boolsymbol (LEFT_PAREN! "previous"^ RIGHT_PAREN!)?
    | LEFT_PAREN! orexpression RIGHT_PAREN!
    | compareexpression
    ;

compareexpression
    : integer (EQUAL^ | NOT_EQUAL^ | LESS_THAN^ | LESS_THAN_OR_EQUAL^ | GREATER_THAN^ | GREATER_THAN_OR_EQUAL^) integer
    ;

integer
    : INTEGER
    | intsymbol (LEFT_PAREN! "previous"^ RIGHT_PAREN!)?
    | strsymbol
    | WORD
    ;

boolsymbol
    : "true"
    | "false"
    | "success"
    | "failure"
    | "error"
    | "changed"
    | "changed.by.me"
    | "state.change"
    ;

intsymbol
    : "unsuccessful.count.builds"
    | "unsuccessful.count.days"
    ;
    
strsymbol
    : "build.specification.name"
    ;

class NotifyConditionLexer extends Lexer;

options {
   k = 2;
}

// Words, which include our boolean operators
WORD
options {
    paraphrase = "a word";
}
    : ('a'..'z' | 'A'..'Z' | '.')+ ;

// Integers, used in comparison expressions
INTEGER
options {
    paraphrase = "an integer";
}
    : ('0'..'9')+ ;

// Comparison operators
EQUAL                : "==";
NOT_EQUAL            : "!=";
LESS_THAN            : "<";
LESS_THAN_OR_EQUAL   : "<=";
GREATER_THAN         : ">";
GREATER_THAN_OR_EQUAL: ">=";

// Grouping
LEFT_PAREN
options {
    paraphrase = "an opening parenthesis '('";
}
    : '(';

RIGHT_PAREN
options {
    paraphrase = "a closing parenthesis ')'";
}
    : ')';

WHITESPACE
    :   (' ' | '\t' | '\r' | '\n') { $setType(Token.SKIP); }
    ;