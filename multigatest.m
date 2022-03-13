%Simple script that tests the trainmultiga implementation. I create a size
%8 random directed adjacency matrix, solve it with brute force, and then
%use the trainmultiga. You can compare the cardinality of plist with p, and
%k with actual k to see correctness.

D=condigraphs(7,1);

[actualk,actualp] = brute_force(D);

[k,plist] = trainmultiga(D);