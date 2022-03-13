function [k, totalplist] = trainmultiga(D)
%This function repeatedly evaluates evalrankMOga while updating the soln
%list every time to find new solutions.

S = cell(0);
[k,totalplist] = evalrankMOga(D,S);
S{1}=totalplist;
currentplist = [];
while ~(size(S{1},1) == size(currentplist,1))
    S{1} = totalplist;
    [k,currentplist] = evalrankMOga(D,S);
    %Merge the new plist with the old and pick out the unique permutation
    %vectors to add in the newly found permutations
    totalplist = unique([currentplist;totalplist],'rows');
end
end