sets
i   canning plants
j   markets
;

Parameters
a(i)    capacity of plant i in cases
b(j)    demand at market j in cases
d(i,j)  distance in thousand miles;

Scalar f    freight in dollars per thousand miles;

**if (not set gdxincname), abort 'no include file name for data file provided';


$gdxIn %gdxincname%
$load i j a b d f
$gdxIn

Parameter c(i,j)    transport cost in thousand of dollars per case ;
c(i,j) = f * d(i,j) / 1000;

Variables
x(i,j)  shipment quantities in cases
z   total transportation costs in thousand of dollars
;

Positive Variable
x;

Equations
cost    define objective function
supply(i)   observe supply limit at plant i
demand(j)   satisfy demand at market j;

cost.. z=E=sum((i,j), c(i,j)*x(i,j));
supply(i).. sum(j, x(i,j)) =L= a(i);
demand(j).. sum(i, x(i,j)) =G= b(j);

model transport /all/;
solve transport using lp minimizing z;
display x.l, x.m;