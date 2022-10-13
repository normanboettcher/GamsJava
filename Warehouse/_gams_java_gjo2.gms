$title Warehouse.gms                                                      
                                                                          
$eolcom //                                                                
$SetDDList warehouse store fixed disaggregate // acceptable defines       
$if not set warehouse    $set warehouse   10                              
$if not set store        $set store       50                              
$if not set fixed        $set fixed       20                              
$if not set disaggregate $set disaggregate 1 // indicator for tighter bigM constraint 
$ife %store%<=%warehouse% $abort Increase number of stores (>%warehouse)  
                                                                          
Sets Warehouse  /w1*w%warehouse% /                                        
     Store      /s1*s%store%     /                                        
Alias (Warehouse,w), (Store,s);                                           
Scalar                                                                    
     fixed        fixed cost for opening a warehouse / %fixed% /          
Parameter                                                                 
     capacity(WareHouse)                                                  
     supplyCost(Store,Warehouse);                                         
                                                                          
$eval storeDIVwarehouse trunc(card(store)/card(warehouse))                
capacity(w)     =   %storeDIVwarehouse% + mod(ord(w),%storeDIVwarehouse%);
supplyCost(s,w) = 1+mod(ord(s)+10*ord(w), 100);                           
                                                                          
Variables                                                                 
    open(Warehouse)                                                       
    supply(Store,Warehouse)                                               
    obj;                                                                  
Binary variables open, supply;                                            
                                                                          
Equations                                                                 
    defobj                                                                
    oneWarehouse(s)                                                       
    defopen(w);                                                           
                                                                          
defobj..  obj =e= sum(w, fixed*open(w)) + sum((w,s), supplyCost(s,w)*supply(s,w));  
                                                                          
oneWarehouse(s).. sum(w, supply(s,w)) =e= 1;                              
                                                                          
defopen(w)..      sum(s, supply(s,w)) =l= open(w)*capacity(w);            
                                                                          
$ifthen %disaggregate%==1                                                 
Equations                                                                 
     defopen2(s,w);                                                       
defopen2(s,w).. supply(s,w) =l= open(w);                                  
$endif                                                                    
                                                                          
model distrib /all/;                                                      
solve distrib min obj using mip;                                          
abort$(distrib.solvestat<>%SolveStat.NormalCompletion% or                 
       distrib.modelstat<>%ModelStat.Optimal% and                         
       distrib.modelstat<>%ModelStat.IntegerSolution%) 'No solution!';    
                                                                          
