*defining variables*

Variable
z   objective variable
;
positive Variables
x1  deluxe belts
x2  regular belts
;

*define equations
Equations Objective, Const1, Const2;


*define objective function
Objective.. z =E= 4*x1 + 3*x2;

*define the constraints
Const1.. x1 + x2 =L= 40;
Const2.. 2*x1 + x2 =L= 60;

*solving stage
model S4Problem1 /all/;

solve S4Problem1 using LP maximizing z;
